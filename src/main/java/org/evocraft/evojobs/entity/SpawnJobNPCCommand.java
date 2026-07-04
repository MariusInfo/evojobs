package org.evocraft.evojobs.entity;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class SpawnJobNPCCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("spawnjobnpc")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();

                    JobNPCEntity npc = EvoJobsEntities.JOB_NPC.get().create(player.serverLevel());

                    if (npc != null) {
                        Vec3 pos = player.position();
                        npc.moveTo(pos.x, pos.y, pos.z, player.getYRot(), player.getXRot());
                        player.serverLevel().addFreshEntity(npc);

                        player.sendSystemMessage(Component.literal("§a[EvoJobs] Job NPC spawned successfully!"));
                        return 1;
                    } else {
                        player.sendSystemMessage(Component.literal("§c[EvoJobs] Error: Could not create the entity!"));
                        return 0;
                    }
                })
        );
    }
}
