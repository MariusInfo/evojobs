package org.evocraft.evojobs;

import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent; // Import adaugat
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import org.evocraft.evojobs.init.EvoJobsMenuTypes;
import org.evocraft.evojobs.entity.EvoJobsEntities;
import org.evocraft.evojobs.entity.JobNPCEntity;
import org.evocraft.evojobs.entity.QuestNPC;
import org.evocraft.evojobs.quest.QuestManager;
import org.slf4j.Logger;

@Mod(Evojobs.MODID)
public class Evojobs {

    public static final String MODID = "evojobs";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Evojobs() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        EvoJobsEntities.ENTITIES.register(modEventBus);
        EvoJobsMenuTypes.MENUS.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            LOGGER.info("[EvoJobs] Initializing the job system...");
            JobConfigManager.initialize();
            JobProgressionConfigManager.initialize();
            JobStationManager.initialize();
            AntiExploitManager.initialize();
            JobManager.initialize();
            org.evocraft.evojobs.network.EvoJobsPacketHandler.register();
        });
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(EvoJobsMenuTypes.JOB_MENU.get(), org.evocraft.evojobs.gui.JobScreen::new);
        });
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("[EvoJobs] Serverul porneste. Incarcam config-urile si bazele de date...");
        JobConfigManager.get().load();
        JobProgressionConfigManager.get().load();
        JobStationManager.get().load();
        AntiExploitManager.get().load();

        // --- INITIALIZARE QUEST-URI ---
        QuestManager.initialize();
    }

    // --- EVENIMENT ADAUGAT PENTRU SALVARE LA OPRIREA SERVERULUI ---
    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("[EvoJobs] Serverul se opreste. Salvam datele ramase in memorie...");
        QuestManager.shutdown();
        if (JobManager.get() != null) {
            JobManager.get().shutdown();
        }
        AntiExploitManager.get().shutdown();
    }

    @SubscribeEvent
    public void onRegisterCommands(net.minecraftforge.event.RegisterCommandsEvent event) {
        org.evocraft.evojobs.EvoJobsCommands.register(event.getDispatcher());
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @SubscribeEvent
        public static void onAttributeCreate(EntityAttributeCreationEvent event) {
            // Both NPCs receive the correct attributes.
            event.put(EvoJobsEntities.JOB_NPC.get(), JobNPCEntity.createAttributes().build());
            event.put(EvoJobsEntities.QUEST_NPC.get(), QuestNPC.createAttributes().build());
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static class ClientEvents {
        @SubscribeEvent
        public static void onRegisterRenderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(EvoJobsEntities.JOB_NPC.get(), org.evocraft.evojobs.entity.JobNPCRenderer::new);
            event.registerEntityRenderer(EvoJobsEntities.QUEST_NPC.get(), org.evocraft.evojobs.entity.QuestNPCRenderer::new);
        }
    }
}
