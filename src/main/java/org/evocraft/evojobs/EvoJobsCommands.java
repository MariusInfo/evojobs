package org.evocraft.evojobs;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
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
// --- IMPORTUL LIPSĂ ADĂUGAT ---
import org.evocraft.evojobs.entity.SpawnQuestNPCCommand;

import java.util.Map;

public class EvoJobsCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(Commands.literal("jobadmin")
                .requires(source -> source.hasPermission(2)) // Doar OP / Admini
                .then(Commands.literal("setstation")
                        .executes(EvoJobsCommands::setStation)
                )
                .then(Commands.literal("setlevel")
                        .then(Commands.argument("jucator", EntityArgument.player())
                                .then(Commands.argument("job", StringArgumentType.word())
                                        .then(Commands.argument("nivel", IntegerArgumentType.integer(1))
                                                .executes(EvoJobsCommands::setLevel)
                                        )
                                )
                        )
                )
        );

        // --- AICI ÎNREGISTRĂM AMBELE COMENZI PENTRU NPC-URI ---
        SpawnJobNPCCommand.register(dispatcher);
        SpawnQuestNPCCommand.register(dispatcher);

        dispatcher.register(Commands.literal("craft").executes(EvoJobsCommands::openCraftingTable));
        dispatcher.register(Commands.literal("ec").executes(EvoJobsCommands::openEnderChest));
    }

    private static int setLevel(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "jucator");
            String job = StringArgumentType.getString(context, "job").toLowerCase();
            int level = IntegerArgumentType.getInteger(context, "nivel");

            if (JobConfigManager.get().getJob(job) == null) {
                context.getSource().sendFailure(Component.literal("Jobul '" + job + "' nu există!"));
                return 0;
            }

            JobManager.get().setJobLevel(target.getUUID(), job, level);
            context.getSource().sendSuccess(() -> Component.literal("§a[EvoJobs] Ai setat jobul §e" + job + " §ala nivelul §b" + level + " §apentru jucătorul §d" + target.getName().getString()), true);

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Eroare la executarea comenzii setlevel."));
        }
        return 1;
    }

    private static int openCraftingTable(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            Map<String, JobData> jobs = JobManager.get().getActiveJobs(player.getUUID());

            if (jobs != null && jobs.containsKey("crafter") && jobs.get("crafter").isActive && jobs.get("crafter").level >= 25) {

                // AICI E REPARATIA: Suprascriem stillValid ca să permitem Shift-Click și folosirea Mesei din mers!
                player.openMenu(new SimpleMenuProvider(
                        (id, inv, p) -> new CraftingMenu(id, inv, ContainerLevelAccess.create(p.level(), p.blockPosition())) {
                            @Override
                            public boolean stillValid(Player playerIn) {
                                return true; // Spunem jocului că masa este mereu validă, chiar dacă nu e un bloc fizic
                            }
                        },
                        Component.translatable("container.crafting")
                ));
            } else {
                player.sendSystemMessage(Component.literal("§c[!] Trebuie să fii Meșter (Level 25+) cu jobul activ pentru a folosi această comandă!"));
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
                player.sendSystemMessage(Component.literal("§c[!] Trebuie să fii Meșter (Level 50+) cu jobul activ pentru a folosi această comandă!"));
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
                player.sendSystemMessage(Component.literal("§a[EvoJobs] Stație de Job salvată cu succes la: " + pos.toShortString()));
            } else {
                player.sendSystemMessage(Component.literal("§c[EvoJobs] Trebuie să te uiți la un bloc pentru a-l face stație de job!"));
            }

        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Eroare la executarea comenzii."));
        }
        return 1;
    }
}
