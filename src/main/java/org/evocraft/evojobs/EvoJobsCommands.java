package org.evocraft.evojobs;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import org.evocraft.evojobs.entity.SpawnJobNPCCommand;
import org.evocraft.evojobs.entity.SpawnQuestNPCCommand;

import java.util.Map;

public class EvoJobsCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(Commands.literal("jobadmin")
                .requires(source -> source.hasPermission(2)) // OP / Admin only.
                .then(Commands.literal("setstation")
                        .executes(EvoJobsCommands::setStation)
                )
                .then(Commands.literal("setlevel")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("job", StringArgumentType.word())
                                        .then(Commands.argument("level", LongArgumentType.longArg(1L))
                                                .executes(EvoJobsCommands::setLevel)
                                        )
                                )
                        )
                )
        );

        SpawnJobNPCCommand.register(dispatcher);
        SpawnQuestNPCCommand.register(dispatcher);

        dispatcher.register(Commands.literal("craft").executes(EvoJobsCommands::openCraftingTable));
        dispatcher.register(Commands.literal("ec").executes(EvoJobsCommands::openEnderChest));
    }

    private static int setLevel(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            String job = StringArgumentType.getString(context, "job").toLowerCase();
            long level = LongArgumentType.getLong(context, "level");

            if (JobConfigManager.get().getJob(job) == null) {
                context.getSource().sendFailure(Component.literal("Job '" + job + "' does not exist!"));
                return 0;
            }

            JobManager.get().setJobLevel(target.getUUID(), job, level);
            context.getSource().sendSuccess(() -> Component.literal("§a[EvoJobs] Set job §e" + job + " §ato level §b" + level + " §afor player §d" + target.getName().getString()), true);

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error while executing the setlevel command."));
        }
        return 1;
    }

    private static int openCraftingTable(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            Map<String, JobData> jobs = JobManager.get().getActiveJobs(player.getUUID());

            if (jobs != null && jobs.containsKey("crafter") && jobs.get("crafter").isActive && jobs.get("crafter").level >= 25) {

                // Keep the virtual crafting table valid even without a physical block.
                player.openMenu(new SimpleMenuProvider(
                        (id, inv, p) -> new CraftingMenu(id, inv, ContainerLevelAccess.create(p.level(), p.blockPosition())) {
                            @Override
                            public boolean stillValid(Player playerIn) {
                                return true;
                            }
                        },
                        Component.translatable("container.crafting")
                ));
            } else {
                player.sendSystemMessage(Component.literal("§c[!] You must be a Crafter (Level 25+) with the job active to use this command!"));
            }
        } catch (Exception ignored) {}
        return 1;
    }

    private static int openEnderChest(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            Map<String, JobData> jobs = JobManager.get().getActiveJobs(player.getUUID());

            if (jobs != null && jobs.containsKey("crafter") && jobs.get("crafter").isActive && jobs.get("crafter").level >= 50) {
                player.openMenu(new SimpleMenuProvider(
                        (id, inv, p) -> ChestMenu.threeRows(id, inv, p.getEnderChestInventory()),
                        Component.translatable("container.enderchest")
                ));
            } else {
                player.sendSystemMessage(Component.literal("§c[!] You must be a Crafter (Level 50+) with the job active to use this command!"));
            }
        } catch (Exception ignored) {}
        return 1;
    }

    private static int setStation(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            HitResult hitResult = player.pick(5.0D, 0.0F, false);

            if (hitResult.getType() == HitResult.Type.BLOCK) {
                BlockPos pos = ((BlockHitResult) hitResult).getBlockPos();
                JobStationManager.get().addStation(player.level(), pos);
                player.sendSystemMessage(Component.literal("§a[EvoJobs] Job station saved successfully at: " + pos.toShortString()));
            } else {
                player.sendSystemMessage(Component.literal("§c[EvoJobs] You must look at a block to set it as a job station!"));
            }

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error while executing the command."));
        }
        return 1;
    }
}
