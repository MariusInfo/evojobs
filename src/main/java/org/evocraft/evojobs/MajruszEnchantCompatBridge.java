package org.evocraft.evojobs;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Consumer;

/**
 * Optional bridge to Majrusz Library's interaction event. Harvester handles and
 * finishes that event before Forge posts RightClickBlock, so the Forge listener
 * remains only a fallback for versions that allow the event to continue.
 */
public final class MajruszEnchantCompatBridge {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean initialized;
    private static boolean callbackErrorLogged;
    private static Consumer<Object> listenerReference;
    private static Object registrationReference;

    private MajruszEnchantCompatBridge() {
    }

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;

        try {
            Class<?> interactionClass = Class.forName("com.majruszlibrary.events.OnPlayerInteracted");
            Class<?> priorityClass = Class.forName("com.majruszlibrary.events.base.Priority");
            Field playerField = interactionClass.getField("player");
            Field itemStackField = interactionClass.getField("itemStack");
            Field blockResultField = interactionClass.getField("blockResult");

            listenerReference = data -> handleInteraction(data, playerField, itemStackField, blockResultField);
            Method listenMethod = interactionClass.getMethod("listen", Consumer.class);
            registrationReference = listenMethod.invoke(null, listenerReference);

            Object highestPriority = priorityClass.getField("HIGHEST").get(null);
            Method priorityMethod = registrationReference.getClass().getMethod("priority", priorityClass);
            priorityMethod.invoke(registrationReference, highestPriority);
            LOGGER.info("[EvoJobs] Majrusz Harvester compatibility enabled.");
        } catch (ClassNotFoundException ignored) {
            LOGGER.info("[EvoJobs] Majrusz Library is not installed; Harvester compatibility remains inactive.");
        } catch (ReflectiveOperationException | LinkageError exception) {
            listenerReference = null;
            registrationReference = null;
            LOGGER.warn("[EvoJobs] Could not initialize Majrusz Harvester compatibility.", exception);
        }
    }

    private static void handleInteraction(Object data, Field playerField, Field itemStackField, Field blockResultField) {
        try {
            Object playerValue = playerField.get(data);
            Object stackValue = itemStackField.get(data);
            Object hitValue = blockResultField.get(data);
            if (!(playerValue instanceof ServerPlayer player)) return;
            if (!(stackValue instanceof ItemStack stack)) return;
            if (!(hitValue instanceof BlockHitResult hitResult)) return;

            MajruszHarvesterCompatEvents.captureHarvesterUse(player, player.level(), stack, hitResult.getBlockPos());
        } catch (ReflectiveOperationException | LinkageError exception) {
            if (!callbackErrorLogged) {
                callbackErrorLogged = true;
                LOGGER.warn("[EvoJobs] Majrusz Harvester interaction could not be processed.", exception);
            }
        }
    }
}
