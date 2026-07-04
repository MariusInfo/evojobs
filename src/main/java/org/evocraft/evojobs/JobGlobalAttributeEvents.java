package org.evocraft.evojobs;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "evojobs", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class JobGlobalAttributeEvents {
    private static final UUID GLOBAL_MOVEMENT_SPEED_UUID = UUID.fromString("af943b6a-e2f2-4c17-8f09-22f82f59df36");

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side.isClient() || event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.tickCount % 20 != 0) return;

        AttributeInstance movement = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement == null) return;

        movement.removeModifier(GLOBAL_MOVEMENT_SPEED_UUID);
        double speedPercent = JobProgressionService.getGlobalMovementSpeedPercent(player);
        if (speedPercent <= 0.0) return;

        movement.addTransientModifier(new AttributeModifier(
                GLOBAL_MOVEMENT_SPEED_UUID,
                "EvoJobsTotalLevelMovementSpeed",
                speedPercent / 100.0,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        ));
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) return;

        Map<String, JobData> activeJobs = JobManager.get().getActiveJobs(attacker.getUUID());
        JobData hunter = activeJobs.get("hunter");
        if (hunter == null || !hunter.isActive) return;

        JobProgressionService.AttributeInfo attribute = JobProgressionService.getJobAttributeInfo("hunter", hunter.level, attacker);
        if (!"PVE_DAMAGE".equalsIgnoreCase(attribute.type) || attribute.effectPercent <= 0.0) return;
        if (event.getEntity() instanceof Player && !attribute.affectsPvp) return;

        float scaledAmount = (float) Math.min(Float.MAX_VALUE, event.getAmount() * (1.0 + (attribute.effectPercent / 100.0)));
        if (Float.isFinite(scaledAmount) && scaledAmount >= 0.0f) {
            event.setAmount(scaledAmount);
        }
    }
}
