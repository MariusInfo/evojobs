package org.evocraft.evojobs;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.AnvilRepairEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.evocraft.evocore.data.EconomyManager;

import java.util.*;

@Mod.EventBusSubscriber(modid = "evojobs", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class JobPerks {

    private static final UUID REACH_UUID = UUID.fromString("b3a9c7d4-1f2e-4d5c-8a9b-0c1d2e3f4a5b");
    private static boolean isPerkAction = false;

    public static long getJobLevel(ServerPlayer player, String jobId) {
        Map<String, JobData> jobs = JobManager.get().getActiveJobs(player.getUUID());
        if (jobs != null && jobs.containsKey(jobId)) {
            return Math.max(1L, jobs.get(jobId).level);
        }
        return 0L;
    }

    // ==========================================
    // 1. PASSIVE EFFECTS AND TICKS (Once per second)
    // ==========================================
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side.isClient() || event.phase == TickEvent.Phase.START) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.isSpectator()) return;

        // UNEMPLOYED L100: Mega JackPot
        if (!player.isCreative() && player.tickCount % 72000 == 0) {
            if (getJobLevel(player, "somer") >= 100 && player.getRandom().nextDouble() < 0.05) {
                EconomyManager.get().addBalance(player.getUUID(), 50000.0);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§6§lJACKPOT! §eYou won " + JobProgressionService.formatMoney(50000.0) + " in the automatic lottery!"));
            }
        }

        // UNEMPLOYED L50: Fool's Luck
        if (!player.isCreative() && player.tickCount % 100 == 0 && (player.zza != 0 || player.xxa != 0)) {
            if (getJobLevel(player, "somer") >= 50 && player.getRandom().nextDouble() < 0.05) {
                double found = 10.0 + (player.getRandom().nextDouble() * 40.0);
                EconomyManager.get().addBalance(player.getUUID(), found);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§aYou found " + JobProgressionService.formatMoney(found) + " on the ground while walking!"));
            }
        }

        if (player.tickCount % 20 != 0) return;

        ItemStack mainHand = player.getMainHandItem();
        boolean holdingPickaxe = mainHand.getItem() instanceof PickaxeItem || mainHand.is(ItemTags.PICKAXES);
        boolean holdingAxe = mainHand.getItem() instanceof AxeItem || mainHand.is(ItemTags.AXES);
        boolean holdingShovel = mainHand.getItem() instanceof ShovelItem || mainHand.is(ItemTags.SHOVELS);
        boolean holdingSword = mainHand.getItem() instanceof SwordItem || mainHand.is(ItemTags.SWORDS);
        boolean holdingBlock = mainHand.getItem() instanceof BlockItem;

        // ⛏️ MINER
        long minerLvl = getJobLevel(player, "miner");
        if (holdingPickaxe) {
            if (minerLvl >= 75) player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 60, 1, false, false));
            else if (minerLvl >= 25) player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 60, 0, false, false));
        }
        if (minerLvl >= 100 && player.getY() < 50) player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, false, false));

        // 🪓 WOODCUTTER
        long woodLvl = getJobLevel(player, "woodcutter");
        if (holdingAxe) {
            if (woodLvl >= 75) player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 60, 1, false, false));
            else if (woodLvl >= 25) player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 60, 0, false, false));
        }
        boolean inForest = player.level().getBiome(player.blockPosition()).unwrapKey().map(k -> k.location().getPath().toLowerCase().contains("forest")).orElse(false);
        if (woodLvl >= 75 && holdingAxe && inForest) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0, false, false));
        }

        // 🪚 DIGGER
        long diggerLvl = getJobLevel(player, "digger");
        if (holdingShovel) {
            if (diggerLvl >= 75) player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 60, 1, false, false));
            else if (diggerLvl >= 25) player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 60, 0, false, false));
        }

        // ⚔️ HUNTER
        long hunterLvl = getJobLevel(player, "hunter");
        if (holdingSword) {
            if (hunterLvl >= 25) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, hunterLvl >= 75 ? 1 : 0, false, false));
            }
            if (hunterLvl >= 75) player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, false, false));
        }

        // 🌾 FARMER
        long farmerLvl = getJobLevel(player, "farmer");
        if (farmerLvl >= 25 && player.level().getBlockState(player.blockPosition().below()).is(Blocks.FARMLAND)) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0, false, false));
        }
        if (farmerLvl >= 100 && player.tickCount % 100 == 0) {
            for (BlockPos p : BlockPos.betweenClosed(player.blockPosition().offset(-3, -1, -3), player.blockPosition().offset(3, 1, 3))) {
                BlockState s = player.level().getBlockState(p);
                if (s.isRandomlyTicking()) s.randomTick((ServerLevel) player.level(), p, player.getRandom());
            }
        }

        // 🎣 FISHERMAN
        long fishLvl = getJobLevel(player, "fisherman");
        if (fishLvl >= 25) player.addEffect(new MobEffectInstance(MobEffects.LUCK, 60, fishLvl >= 75 ? 2 : 0, false, false));
        if (fishLvl >= 50 && player.isInWater()) {
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 60, 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 60, 0, false, false));
        }

        // 🧱 BUILDER
        long buildLvl = getJobLevel(player, "builder");
        if (holdingBlock) {
            if (buildLvl >= 25) player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, false, false));
            if (buildLvl >= 75) player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0, false, false));
        }
        AttributeInstance reach = player.getAttribute(ForgeMod.BLOCK_REACH.get());
        if (reach != null) {
            reach.removeModifier(REACH_UUID);
            if (buildLvl >= 50 && holdingBlock) reach.addTransientModifier(new AttributeModifier(REACH_UUID, "BuilderReach", 2.0, AttributeModifier.Operation.ADDITION));
        }

        // 💎 TRADER
        long traderLvl = getJobLevel(player, "trader");
        if (traderLvl >= 50) player.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 60, 2, false, false));
        else if (traderLvl >= 25) player.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 60, 0, false, false));

        // =======================================================
        // 🗜️ BLACKSMITH L100: SUPREME BYPASS "TOO EXPENSIVE"
        // We scan the inventory and remove the penalty NBT (RepairCost).
        // Without this NBT, Minecraft always thinks your item is untouched!
        // =======================================================
        long smithLvl = getJobLevel(player, "fierar");
        if (smithLvl >= 100) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (!stack.isEmpty() && stack.hasTag() && stack.getTag().contains("RepairCost")) {
                    stack.getTag().remove("RepairCost");
                }
            }
        }
    }

    // ==========================================
    // 2. EFFECTS ON BLOCK BREAK / PLACE
    // ==========================================
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || isPerkAction) return;
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (player.isSpectator()) return;
        BlockPos pos = event.getPos();
        BlockState state = event.getState();
        ServerLevel level = (ServerLevel) player.level();

        // 🌾 FARMER
        long farmerLvl = getJobLevel(player, "farmer");
        if (farmerLvl >= 50 && state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
            Item seedItem = crop.getCloneItemStack(level, pos, state).getItem();
            int seedSlot = player.getInventory().findSlotMatchingItem(new ItemStack(seedItem));

            if (seedSlot != -1 || player.isCreative()) {
                if (!player.isCreative()) player.getInventory().getItem(seedSlot).shrink(1);
                event.setCanceled(true);

                Block.dropResources(state, level, pos, null, player, player.getMainHandItem());
                if (!player.isCreative() && farmerLvl >= 75 && player.getRandom().nextDouble() < 0.2) {
                    Block.dropResources(state, level, pos, null, player, player.getMainHandItem());
                }
                level.setBlock(pos, crop.getStateForAge(0), 3);
                return;
            }
        }

        if (player.isCreative()) return;

        // ⛏️ MINER
        long minerLvl = getJobLevel(player, "miner");
        if (minerLvl >= 50 && player.isCrouching() && state.is(Tags.Blocks.ORES) && isHoldingTool(player, true, false, false)) {
            isPerkAction = true;
            try {
                breakGroup(level, pos, state.getBlock(), player, 16, true, false);
            } finally {
                isPerkAction = false;
            }
        }
        if (minerLvl >= 100 && state.is(Tags.Blocks.ORES) && player.getRandom().nextDouble() < 0.1) {
            Block.dropResources(state, level, pos, null, player, player.getMainHandItem());
        }

        // 🪓 WOODCUTTER
        long woodLvl = getJobLevel(player, "woodcutter");
        if (woodLvl >= 50 && player.isCrouching() && state.is(BlockTags.LOGS) && isHoldingTool(player, false, true, false)) {
            isPerkAction = true;
            try {
                breakGroup(level, pos, state.getBlock(), player, 32, false, true);
            } finally {
                isPerkAction = false;
            }

            if (woodLvl >= 100) {
                BlockState stateBelow = level.getBlockState(pos.below());
                if (stateBelow.is(Blocks.DIRT) || stateBelow.is(Blocks.GRASS_BLOCK) || stateBelow.is(Blocks.COARSE_DIRT) || stateBelow.is(Blocks.PODZOL)) {
                    Block sapling = Blocks.OAK_SAPLING;
                    if (state.is(Blocks.SPRUCE_LOG)) sapling = Blocks.SPRUCE_SAPLING;
                    else if (state.is(Blocks.BIRCH_LOG)) sapling = Blocks.BIRCH_SAPLING;
                    else if (state.is(Blocks.JUNGLE_LOG)) sapling = Blocks.JUNGLE_SAPLING;
                    else if (state.is(Blocks.ACACIA_LOG)) sapling = Blocks.ACACIA_SAPLING;
                    else if (state.is(Blocks.DARK_OAK_LOG)) sapling = Blocks.DARK_OAK_SAPLING;
                    else if (state.is(Blocks.CHERRY_LOG)) sapling = Blocks.CHERRY_SAPLING;
                    else if (state.is(Blocks.MANGROVE_LOG)) sapling = Blocks.MANGROVE_PROPAGULE;

                    level.setBlock(pos, sapling.defaultBlockState(), 3);
                }
            }
        }
        if (woodLvl >= 100 && state.is(BlockTags.LOGS) && player.getRandom().nextDouble() < 0.15) {
            Block.dropResources(state, level, pos, null, player, player.getMainHandItem());
        }

        // 🪚 DIGGER
        long digLvl = getJobLevel(player, "digger");
        if (digLvl >= 50 && player.isCrouching()
                && (state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(Tags.Blocks.GRAVEL))
                && isHoldingTool(player, false, false, true)) {
            isPerkAction = true;
            try {
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && z == 0) continue;
                        if (!player.isAlive() || !isHoldingTool(player, false, false, true)) break;
                        BlockPos p = pos.offset(x, 0, z);
                        if (level.getBlockState(p).is(state.getBlock())) {
                            player.gameMode.destroyBlock(p);
                        }
                    }
                }
            } finally {
                isPerkAction = false;
            }
        }
        if (digLvl >= 100 && (state.is(BlockTags.DIRT) || state.is(BlockTags.SAND)) && player.getRandom().nextDouble() < 0.02) {
            Item[] treasures = {Items.BONE, Items.EMERALD, Items.GOLD_NUGGET, Items.DIAMOND};
            Block.popResource(level, pos, new ItemStack(treasures[player.getRandom().nextInt(treasures.length)]));
        }

        // 🗜️ BLACKSMITH (L25 Passive Unbreaking)
        long smithLvl = getJobLevel(player, "fierar");
        if (smithLvl >= 25 && player.getMainHandItem().isDamageableItem() && player.getRandom().nextDouble() < 0.15) {
            ItemStack hand = player.getMainHandItem();
            if (hand.getDamageValue() > 0) hand.setDamageValue(hand.getDamageValue() - 1);
        }
    }

    private static boolean isHoldingTool(ServerPlayer pl, boolean pickaxe, boolean axe, boolean shovel) {
        ItemStack stack = pl.getMainHandItem();
        if (stack.isEmpty()) return false;
        if (pickaxe) return stack.getItem() instanceof PickaxeItem || stack.is(ItemTags.PICKAXES);
        if (axe) return stack.getItem() instanceof AxeItem || stack.is(ItemTags.AXES);
        if (shovel) return stack.getItem() instanceof ShovelItem || stack.is(ItemTags.SHOVELS);
        return false;
    }

    private static void breakGroup(ServerLevel lvl, BlockPos p, Block target, ServerPlayer pl, int max, boolean pickaxe, boolean axe) {
        Queue<BlockPos> q = new LinkedList<>(); q.add(p);
        Set<BlockPos> v = new HashSet<>(); v.add(p);
        int count = 0;
        while (!q.isEmpty() && count < max) {
            if (!pl.isAlive() || !isHoldingTool(pl, pickaxe, axe, false)) break;
            BlockPos curr = q.poll();
            if (!curr.equals(p)) {
                if (pl.gameMode.destroyBlock(curr)) {
                    count++;
                } else {
                    continue;
                }
            }
            for (BlockPos n : BlockPos.betweenClosed(curr.offset(-1,-1,-1), curr.offset(1,1,1))) {
                BlockPos imm = n.immutable();
                if (!v.contains(imm) && lvl.getBlockState(imm).is(target)) {
                    v.add(imm);
                    q.add(imm);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.isCanceled()) return;
        if (event.getEntity() instanceof ServerPlayer player && !player.isCreative() && !player.isSpectator()) {
            BlockState placedState = event.getPlacedBlock();
            Block placedBlock = placedState.getBlock();
            if (placedState.hasBlockEntity()
                    || placedState.is(Tags.Blocks.STORAGE_BLOCKS)
                    || placedState.is(Tags.Blocks.ORES)
                    || placedBlock == Blocks.BEACON
                    || placedBlock == Blocks.ANCIENT_DEBRIS
                    || placedBlock == Blocks.DRAGON_EGG
                    || placedBlock == Blocks.RESPAWN_ANCHOR
                    || placedBlock == Blocks.LODESTONE
                    || placedBlock == Blocks.ENCHANTING_TABLE
                    || placedBlock == Blocks.ANVIL
                    || placedBlock == Blocks.CHIPPED_ANVIL
                    || placedBlock == Blocks.DAMAGED_ANVIL
                    || placedBlock == Blocks.TNT
                    || placedBlock instanceof net.minecraft.world.level.block.BedBlock
                    || placedBlock instanceof net.minecraft.world.level.block.DoorBlock
                    || placedBlock instanceof net.minecraft.world.level.block.ShulkerBoxBlock) {
                return;
            }
            Item blockItem = placedBlock.asItem();
            if (blockItem == Items.AIR) return;
            if (!player.getMainHandItem().is(blockItem) && !player.getOffhandItem().is(blockItem)) return;

            if (getJobLevel(player, "builder") >= 100 && player.getRandom().nextDouble() < 0.10) {
                player.addItem(new ItemStack(blockItem));
            }
        }
    }

    // ==========================================
    // 3. COMBAT (HUNTER / BUILDER)
    // ==========================================
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.isCanceled()) return;
        if (event.getSource().getEntity() instanceof ServerPlayer player && !player.isSpectator()) {
            if (getJobLevel(player, "hunter") >= 50) player.heal(1.0f);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.isCanceled()) return;
        if (event.getSource().getEntity() instanceof ServerPlayer player && !player.isCreative() && !player.isSpectator()
                && getJobLevel(player, "hunter") >= 100) {
            if (player.getRandom().nextDouble() < 0.1) {
                net.minecraft.world.entity.LivingEntity killed = event.getEntity();
                ItemStack head = ItemStack.EMPTY;

                // ==================================================
                // MOB HEADS SYSTEM - NO STEVE FOR MODS!
                // ==================================================
                if (killed instanceof net.minecraft.world.entity.monster.Skeleton) head = new ItemStack(Items.SKELETON_SKULL);
                else if (killed instanceof net.minecraft.world.entity.monster.WitherSkeleton) head = new ItemStack(Items.WITHER_SKELETON_SKULL);
                else if (killed instanceof net.minecraft.world.entity.monster.Creeper) head = new ItemStack(Items.CREEPER_HEAD);
                else if (killed instanceof net.minecraft.world.entity.monster.piglin.Piglin) head = new ItemStack(Items.PIGLIN_HEAD);
                else if (killed instanceof net.minecraft.world.entity.monster.Zombie) head = new ItemStack(Items.ZOMBIE_HEAD);
                else if (killed instanceof net.minecraft.world.entity.monster.EnderMan) { head = new ItemStack(Items.PLAYER_HEAD); head.getOrCreateTag().putString("SkullOwner", "MHF_Enderman"); }
                else if (killed instanceof net.minecraft.world.entity.monster.Spider) { head = new ItemStack(Items.PLAYER_HEAD); head.getOrCreateTag().putString("SkullOwner", "MHF_Spider"); }
                else if (killed instanceof net.minecraft.world.entity.monster.CaveSpider) { head = new ItemStack(Items.PLAYER_HEAD); head.getOrCreateTag().putString("SkullOwner", "MHF_CaveSpider"); }
                else if (killed instanceof net.minecraft.world.entity.monster.Witch) { head = new ItemStack(Items.PLAYER_HEAD); head.getOrCreateTag().putString("SkullOwner", "MHF_Witch"); }
                else if (killed instanceof net.minecraft.world.entity.monster.Slime) { head = new ItemStack(Items.PLAYER_HEAD); head.getOrCreateTag().putString("SkullOwner", "MHF_Slime"); }
                else if (killed instanceof net.minecraft.world.entity.monster.Ghast) { head = new ItemStack(Items.PLAYER_HEAD); head.getOrCreateTag().putString("SkullOwner", "MHF_Ghast"); }
                else if (killed instanceof net.minecraft.world.entity.animal.Cow) { head = new ItemStack(Items.PLAYER_HEAD); head.getOrCreateTag().putString("SkullOwner", "MHF_Cow"); }
                else if (killed instanceof net.minecraft.world.entity.animal.Pig) { head = new ItemStack(Items.PLAYER_HEAD); head.getOrCreateTag().putString("SkullOwner", "MHF_Pig"); }
                else if (killed instanceof net.minecraft.world.entity.animal.Sheep) { head = new ItemStack(Items.PLAYER_HEAD); head.getOrCreateTag().putString("SkullOwner", "MHF_Sheep"); }
                else if (killed instanceof net.minecraft.world.entity.animal.Chicken) { head = new ItemStack(Items.PLAYER_HEAD); head.getOrCreateTag().putString("SkullOwner", "MHF_Chicken"); }
                else if (killed instanceof net.minecraft.world.entity.npc.Villager) { head = new ItemStack(Items.PLAYER_HEAD); head.getOrCreateTag().putString("SkullOwner", "MHF_Villager"); }
                else if (killed instanceof net.minecraft.world.entity.player.Player) {
                    head = new ItemStack(Items.PLAYER_HEAD);
                    head.getOrCreateTag().putString("SkullOwner", killed.getName().getString());
                } else {
                    // FOR POLAR BEARS, MOD MOBS, ETC.
                    // We use a skeleton skull to make it look badass, NOT a Steve head!
                    head = new ItemStack(Items.SKELETON_SKULL);
                    head.setHoverName(net.minecraft.network.chat.Component.literal("§eSkull of " + killed.getName().getString()));
                }

                if (!head.isEmpty()) {
                    event.getDrops().add(new ItemEntity(player.level(), killed.getX(), killed.getY(), killed.getZ(), head));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onFallDamage(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (getJobLevel(player, "builder") >= 100) event.setCanceled(true);
        }
    }

    // ==========================================
    // 4. BLACKSMITH & TRADER & CRAFTER & FISHERMAN
    // ==========================================
    @SubscribeEvent
    public static void onAnvilRepair(AnvilRepairEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !player.isCreative() && !player.isSpectator()) {
            if (event.getLeft().isEmpty() || event.getRight().isEmpty()) return;
            long fierarLvl = getJobLevel(player, "fierar");

            // L50: Solid Anvil (20%)
            if (fierarLvl >= 50 && player.getRandom().nextDouble() < 0.20) {
                event.setBreakChance(0.0f);
            }

            // =======================================================
            // 🗜️ BLACKSMITH L75: ANVIL XP CASHBACK
            // Grants 1 XP level cashback when combining/repairing items,
            // preventing positive XP profit on cheap repairs.
            // =======================================================
            if (fierarLvl >= 75) {
                player.giveExperienceLevels(1);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§a[Blacksmith] You received 1 XP level cashback from the anvil!"));
            }
        }
    }

    private static final Set<String> CRAFT_PERK_BLACKLIST = Set.of(
            "minecraft:iron_block", "minecraft:iron_ingot", "minecraft:iron_nugget",
            "minecraft:gold_block", "minecraft:gold_ingot", "minecraft:gold_nugget",
            "minecraft:diamond_block", "minecraft:diamond",
            "minecraft:emerald_block", "minecraft:emerald",
            "minecraft:redstone_block", "minecraft:redstone",
            "minecraft:lapis_block", "minecraft:lapis_lazuli",
            "minecraft:coal_block", "minecraft:coal",
            "minecraft:copper_block", "minecraft:copper_ingot",
            "minecraft:netherite_block", "minecraft:netherite_ingot", "minecraft:netherite_scrap",
            "minecraft:raw_iron_block", "minecraft:raw_gold_block", "minecraft:raw_copper_block",
            "minecraft:quartz_block", "minecraft:hay_block", "minecraft:wheat",
            "minecraft:bone_block", "minecraft:bone_meal",
            "minecraft:slime_block", "minecraft:slime_ball",
            "minecraft:honey_block", "minecraft:honeycomb_block",
            "minecraft:dried_kelp_block", "minecraft:melon", "minecraft:melon_slice",
            "minecraft:beacon", "minecraft:shulker_box", "minecraft:enchanted_golden_apple"
    );

    @SubscribeEvent
    public static void onCraftItem(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.isCreative() || player.isSpectator()) return;
        ItemStack crafted = event.getCrafting();
        if (crafted.isEmpty() || crafted.isDamageableItem()) return;
        net.minecraft.resources.ResourceLocation id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(crafted.getItem());
        if (id == null || CRAFT_PERK_BLACKLIST.contains(id.toString()) || id.getPath().contains("shulker_box")) return;
        if (crafted.getItem() instanceof BlockItem bi && bi.getBlock().defaultBlockState().hasBlockEntity()) return;

        long craftLvl = getJobLevel(player, "crafter");

        if (craftLvl >= 75 && player.getRandom().nextDouble() < 0.10) {
            for (int i = 0; i < event.getInventory().getContainerSize(); i++) {
                ItemStack ing = event.getInventory().getItem(i);
                if (!ing.isEmpty() && !ing.isDamageableItem() && !ing.hasTag()) {
                    net.minecraft.resources.ResourceLocation ingId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(ing.getItem());
                    if (ingId != null && !CRAFT_PERK_BLACKLIST.contains(ingId.toString())) {
                        player.addItem(new ItemStack(ing.getItem()));
                    }
                    break;
                }
            }
        }
        if (craftLvl >= 100 && player.getRandom().nextDouble() < 0.15) {
            player.addItem(crafted.copy());
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§d[Crafter] You crafted double product for free!"));
        }
    }

    @SubscribeEvent
    public static void onVillagerInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.isCanceled()) return;
        if (!event.getLevel().isClientSide() && event.getTarget() instanceof Villager villager && event.getEntity() instanceof ServerPlayer player) {
            if (getJobLevel(player, "trader") >= 100) {
                villager.getOffers().forEach(net.minecraft.world.item.trading.MerchantOffer::resetUses);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemFished(ItemFishedEvent event) {
        if (event.isCanceled()) return;
        if (event.getEntity() instanceof ServerPlayer player && !player.isCreative() && !player.isSpectator()) {
            long fishLvl = getJobLevel(player, "fisherman");
            if (fishLvl >= 100 && player.getRandom().nextDouble() < 0.05) {
                event.getDrops().add(new ItemStack(Items.ENCHANTED_BOOK));
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§b[Fisherman] You fished a treasure of the sea!"));
            }
        }
    }
}
