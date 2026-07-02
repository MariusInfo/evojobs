package org.evocraft.evojobs;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class JobAchievements {

    public static void checkLevelUp(ServerPlayer player, String jobId, int newLevel) {
        // We look for the exact JSON file from the DataPack: e.g. "evojobs:miner/level_10"
        ResourceLocation advancementId = new ResourceLocation("evojobs", jobId.toLowerCase() + "/level_" + newLevel);

        if (player.getServer() != null) {
            Advancement adv = player.getServer().getAdvancements().getAdvancement(advancementId);

            if (adv != null) {
                AdvancementProgress progress = player.getAdvancements().getOrStartProgress(adv);
                if (!progress.isDone()) {
                    // We grant the advancement to the player.
                    // Minecraft will handle the onscreen Toast, the sound, and the chat message itself!
                    for (String criterion : progress.getRemainingCriteria()) {
                        player.getAdvancements().award(adv, criterion);
                    }
                }
            }
        }
    }
}