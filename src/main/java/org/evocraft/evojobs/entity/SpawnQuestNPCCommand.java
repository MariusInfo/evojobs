package org.evocraft.evojobs.entity;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class SpawnQuestNPCCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("spawnquestnpc")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();

                    QuestNPC npc = EvoJobsEntities.QUEST_NPC.get().create(player.serverLevel());

                    if (npc != null) {
                        Vec3 pos = player.position();
                        npc.moveTo(pos.x, pos.y, pos.z, player.getYRot(), player.getXRot());

                        // Match the player's facing direction.
                        npc.setYHeadRot(player.getYRot());
                        npc.setYBodyRot(player.getYRot());

                        player.serverLevel().addFreshEntity(npc);

                        player.sendSystemMessage(Component.literal("§a[EvoJobs] Daily Quest NPC spawned successfully!"));
                        return 1;
                    } else {
                        player.sendSystemMessage(Component.literal("§c[EvoJobs] Error: Could not create the quest entity!"));
                        return 0;
                    }
                })
        );
    }
}
