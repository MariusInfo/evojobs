package org.evocraft.evojobs;

import org.evocraft.evocore.data.EconomyManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AnvilRepairEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

@Mod.EventBusSubscriber(modid = "evojobs", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class JobEvents {

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AntiExploitManager.get().markBlockPlaced(player.level(), event.getPos());
            if (player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty()) return;
            Block block = event.getPlacedBlock().getBlock();

            if (block instanceof CropBlock || block instanceof BushBlock ||
                    block instanceof SaplingBlock || block instanceof FlowerBlock ||
                    block == Blocks.SUGAR_CANE || block == Blocks.CACTUS || block == Blocks.BAMBOO ||
                    block == Blocks.KELP || block == Blocks.SEA_PICKLE) {
                return;
            }

            ResourceLocation key = ForgeRegistries.BLOCKS.getKey(block);
            if (key != null) processJobAction(player, "PLACE", key.toString(), 1.0, "builder");
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {

            BlockState state = event.getState();
            Block block = state.getBlock();
            ResourceLocation key = ForgeRegistries.BLOCKS.getKey(block);

            boolean isCrop = block instanceof CropBlock || block instanceof SweetBerryBushBlock || block instanceof CocoaBlock;

            if (isCrop) {
                if (block instanceof CropBlock crop && !crop.isMaxAge(state)) return;
                if (block instanceof SweetBerryBushBlock && state.getValue(SweetBerryBushBlock.AGE) < 3) return;
                if (block instanceof CocoaBlock && state.getValue(CocoaBlock.AGE) < 2) return;

                AntiExploitManager.get().isExploitAndRemove(player.level(), event.getPos());
                if (key != null) {
                    processJobAction(player, "BREAK", key.toString(), 1.0, "farmer");
                    applyBlockFortuneAttribute(player, "farmer", state, event.getPos());
                }
                return;
            }

            if (AntiExploitManager.get().isExploitAndRemove(player.level(), event.getPos())) {
                return;
            }

            ItemStack heldItem = player.getMainHandItem();
            if (key != null) {
                String blockId = key.toString();
                if (heldItem.getEnchantmentLevel(Enchantments.SILK_TOUCH) > 0) blockId = "*";

                if (block == Blocks.GRASS || block == Blocks.TALL_GRASS || block == Blocks.FERN ||
                        block instanceof net.minecraft.world.level.block.LeavesBlock || block instanceof FlowerBlock) {
                    return;
                }

                if (state.is(BlockTags.MINEABLE_WITH_PICKAXE) || block == Blocks.DEEPSLATE || block == Blocks.COBBLED_DEEPSLATE || block == Blocks.TUFF || block == Blocks.BASALT || block == Blocks.BLACKSTONE || block == Blocks.NETHERRACK) {
                    if (heldItem.getItem() instanceof PickaxeItem || heldItem.is(ItemTags.PICKAXES)) {
                        processJobAction(player, "BREAK", blockId, 1.0, "miner");
                        if (heldItem.getEnchantmentLevel(Enchantments.SILK_TOUCH) <= 0) {
                            applyBlockFortuneAttribute(player, "miner", state, event.getPos());
                        }
                    }
                }
                else if (state.is(BlockTags.MINEABLE_WITH_SHOVEL) || block == Blocks.SAND || block == Blocks.RED_SAND || block == Blocks.GRAVEL || block == Blocks.DIRT || block == Blocks.GRASS_BLOCK || block == Blocks.PODZOL || block == Blocks.COARSE_DIRT || block == Blocks.ROOTED_DIRT) {
                    if (heldItem.getItem() instanceof ShovelItem || heldItem.is(ItemTags.SHOVELS)) {
                        processJobAction(player, "BREAK", blockId, 1.0, "digger");
                        applyBlockFortuneAttribute(player, "digger", state, event.getPos());
                    }
                }
                else if (state.is(BlockTags.LOGS) || state.is(BlockTags.MINEABLE_WITH_AXE)) {
                    if (heldItem.getItem() instanceof AxeItem || heldItem.is(ItemTags.AXES)) {
                        processJobAction(player, "BREAK", blockId, 1.0, "woodcutter");
                        applyBlockFortuneAttribute(player, "woodcutter", state, event.getPos());
                    }
                }
                else if (block == Blocks.MELON || block == Blocks.PUMPKIN || block == Blocks.SUGAR_CANE || block == Blocks.CACTUS) {
                    processJobAction(player, "BREAK", blockId, 1.0, "farmer");
                    applyBlockFortuneAttribute(player, "farmer", state, event.getPos());
                }
            }
        }
    }

    @SubscribeEvent
    public static void onVillagerTrade(net.minecraftforge.event.entity.player.TradeWithVillagerEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) processJobAction(player, "TRADE", "*", 1.0, "trader");
    }

    @SubscribeEvent
    public static void onItemCraft(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ResourceLocation key = ForgeRegistries.ITEMS.getKey(event.getCrafting().getItem());
            if (key != null) {
                String itemId = key.toString();

                List<String> antiLoopBlacklist = Arrays.asList(
                        "minecraft:iron_block", "minecraft:iron_ingot", "minecraft:iron_nugget",
                        "minecraft:gold_block", "minecraft:gold_ingot", "minecraft:gold_nugget",
                        "minecraft:diamond_block", "minecraft:diamond",
                        "minecraft:emerald_block", "minecraft:emerald",
                        "minecraft:redstone_block", "minecraft:redstone",
                        "minecraft:lapis_block", "minecraft:lapis_lazuli",
                        "minecraft:coal_block", "minecraft:coal",
                        "minecraft:copper_block", "minecraft:copper_ingot",
                        "minecraft:netherite_block", "minecraft:netherite_ingot",
                        "minecraft:raw_iron_block", "minecraft:raw_gold_block", "minecraft:raw_copper_block",
                        "minecraft:hay_block", "minecraft:wheat",
                        "minecraft:bone_block", "minecraft:bone_meal",
                        "minecraft:slime_block", "minecraft:slime_ball",
                        "minecraft:melon", "minecraft:melon_slice"
                );

                if (antiLoopBlacklist.contains(itemId)) return;

                int slotsUsed = 0;
                for (int i = 0; i < event.getInventory().getContainerSize(); i++) {
                    if (!event.getInventory().getItem(i).isEmpty()) slotsUsed++;
                }
                if (slotsUsed == 0) slotsUsed = 1;

                double complexityMultiplier = 1.0;
                switch (slotsUsed) {
                    case 1: complexityMultiplier = 0.2; break;
                    case 2: complexityMultiplier = 0.5; break;
                    case 3: complexityMultiplier = 1.0; break;
                    case 4: complexityMultiplier = 1.5; break;
                    case 5: case 6: case 7: case 8: complexityMultiplier = 2.5; break;
                    case 9: complexityMultiplier = 4.0; break;
                }

                double totalMultiplier = event.getCrafting().getCount() * complexityMultiplier;
                processJobAction(player, "CRAFT", itemId, totalMultiplier, "crafter");
            }
        }
    }

    @SubscribeEvent
    public static void onAnvilRepair(AnvilRepairEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            processJobAction(player, "USE_ANVIL", "*", 1.0, "fierar");
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType());
            String entityId = (key != null) ? key.toString() : "unknown";
            processJobAction(player, "KILL", entityId, 1.0, "hunter");
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onFish(ItemFishedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            for (ItemStack stack : event.getDrops()) {
                ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
                if (key != null) processJobAction(player, "FISH", key.toString(), stack.getCount(), "fisherman");
            }
        }
    }

    @SubscribeEvent
    public static void onBabySpawn(BabyEntitySpawnEvent event) {
        if (event.getCausedByPlayer() instanceof ServerPlayer player) {
            ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(event.getParentA().getType());
            if (key != null) processJobAction(player, "BREED", key.toString(), 1.0, "farmer");
        }
    }

    @SubscribeEvent
    public static void onTame(AnimalTameEvent event) {
        if (event.getTamer() instanceof ServerPlayer player) {
            ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(event.getAnimal().getType());
            if (key != null) processJobAction(player, "TAME", key.toString(), 1.0, "farmer");
        }
    }

    @SubscribeEvent
    public static void onBlockInteract(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof ServerPlayer player) {
            net.minecraft.core.BlockPos pos = event.getPos();

            if (JobStationManager.get().isStation(player.level(), pos)) {
                event.setCanceled(true);

                player.openMenu(new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return Component.literal("Jobs");
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                        return new JobMenu(id, inv);
                    }
                });
            }
        }
    }

    private static void applyBlockFortuneAttribute(ServerPlayer player, String jobId, BlockState state, BlockPos pos) {
        if (!(player.level() instanceof ServerLevel level)) return;

        JobData data = JobManager.get().getActiveJobs(player.getUUID()).get(jobId);
        if (data == null || !data.isActive) return;

        JobProgressionService.AttributeInfo attribute = JobProgressionService.getJobAttributeInfo(jobId, data.level, player);
        String type = attribute.type == null ? "" : attribute.type.toUpperCase(java.util.Locale.ROOT);
        if (!type.equals("MINING_FORTUNE") && !type.equals("HARVEST_FORTUNE") &&
                !type.equals("WOOD_FORTUNE") && !type.equals("DIGGING_FORTUNE")) {
            return;
        }

        double rolls = attribute.effectPercent / 100.0;
        if (!Double.isFinite(rolls) || rolls <= 0.0) return;

        int guaranteed = (int) Math.floor(rolls);
        double chance = rolls - guaranteed;
        int extraDrops = Math.min(guaranteed, 10);
        if (extraDrops < 10 && player.getRandom().nextDouble() < chance) {
            extraDrops++;
        }

        for (int i = 0; i < extraDrops; i++) {
            Block.dropResources(state, level, pos, null, player, player.getMainHandItem());
        }
    }

    public static void processJobAction(ServerPlayer player, String actionType, String targetId, double multiplier, String forcedJobId) {
        Map<String, JobData> activeJobs = JobManager.get().getActiveJobs(player.getUUID());
        if (activeJobs.isEmpty()) return;

        double totalMoney = 0;
        double totalXp = 0;
        Set<String> jobs = new LinkedHashSet<>();

        for (String jobId : activeJobs.keySet()) {
            if (forcedJobId != null && !jobId.equals(forcedJobId)) continue;

            JobDefinition jobDef = JobConfigManager.get().getJob(jobId);
            if (jobDef == null || !jobDef.hasTrigger(actionType)) continue;

            double basePrice = jobDef.getReward(actionType, targetId);
            if (basePrice == 0 && targetId.startsWith("minecraft:")) basePrice = jobDef.getReward(actionType, targetId.replace("minecraft:", ""));
            if (basePrice == 0) basePrice = jobDef.getReward(actionType, "*");

            if (basePrice > 0) {
                JobData data = activeJobs.get(jobId);
                if (data == null) continue;
                JobManager.RankInfo rankInfo = JobManager.getRankInfo(jobId, data.level);
                double rankMultiplier = 1.0 + (rankInfo.boostPercent / 100.0);
                double safeMultiplier = Double.isFinite(multiplier) && multiplier > 0.0 ? multiplier : 0.0;

                double baseMoney = basePrice * safeMultiplier;
                double enchantXpMultiplier = JobEnchantCompatService.getXpMultiplier(player, actionType, jobId);
                double enchantMoneyMultiplier = JobEnchantCompatService.getMoneyMultiplier(player, actionType, jobId);
                double baseXp = basePrice * JobProgressionService.getBaseXpMultiplier(jobId) * safeMultiplier * enchantXpMultiplier;
                double finalMoney = JobProgressionService.calculateScaledMoneyReward(baseMoney, data.level, player, rankMultiplier * enchantMoneyMultiplier);
                double xpGain = JobProgressionService.calculateScaledXpReward(baseXp, data.level, player);

                if (finalMoney > 0.0) EconomyManager.get().addBalance(player.getUUID(), finalMoney);
                if (xpGain > 0.0) JobManager.get().addXp(player.getUUID(), jobId, xpGain);

                // --- ADDED: TRANSMIT THE INFO TO THE QUEST SYSTEM ---
                org.evocraft.evojobs.quest.QuestManager.onAction(player, jobId, actionType, targetId, (int) Math.max(1, multiplier));

                totalMoney += finalMoney;
                totalXp += xpGain;
                jobs.add(jobDef.displayName);
            }
        }

        if (totalMoney > 0.0 || totalXp > 0.0) {
            JobRewardDisplayAccumulator.addAndDisplay(player, totalMoney, totalXp, jobs);
            JobManager.get().syncJobsToClient(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerJoin(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            JobRewardDisplayAccumulator.reset(player.getUUID());
            if (player.getServer() != null) {
                player.getServer().tell(new net.minecraft.server.TickTask(player.getServer().getTickCount() + 10, () -> {
                    try {
                        JobManager.get().onPlayerJoin(player);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }));
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        JobRewardDisplayAccumulator.reset(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase == net.minecraftforge.event.TickEvent.Phase.END && event.getServer().getTickCount() % 100 == 0) {
            JobRewardDisplayAccumulator.cleanup(event.getServer().getTickCount());
            JobManager manager = JobManager.get();
            if (manager != null) {
                manager.flushDirtySaves(false);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if (event.side.isServer() && event.phase == net.minecraftforge.event.TickEvent.Phase.END) {
            if (event.player instanceof ServerPlayer player) {
                if (player.tickCount % 100 == 0) {
                    if (player.isPassenger()) {
                        Map<String, JobData> activeJobs = JobManager.get().getActiveJobs(player.getUUID());
                        if (activeJobs.containsKey("somer")) {
                            JobData somerData = activeJobs.get("somer");
                            JobManager.RankInfo rankInfo = JobManager.getRankInfo("somer", somerData.level);
                            double money = JobProgressionService.calculateScaledMoneyReward(0.10, somerData.level, player, 1.0 + (rankInfo.boostPercent / 100.0));
                            double xp = JobProgressionService.calculateScaledXpReward(1.0, somerData.level, player);
                            if (money > 0.0) EconomyManager.get().addBalance(player.getUUID(), money);
                            if (xp > 0.0) JobManager.get().addXp(player.getUUID(), "somer", xp);
                            JobRewardDisplayAccumulator.addAndDisplay(
                                    player, money, xp, java.util.List.of("Unemployed"));
                            JobManager.get().syncJobsToClient(player);
                        }
                    }
                }
            }
        }
    }
}
