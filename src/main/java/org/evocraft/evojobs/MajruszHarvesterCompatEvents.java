package org.evocraft.evojobs;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = Evojobs.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MajruszHarvesterCompatEvents {
    private static final Map<UUID, List<PendingHarvest>> PENDING = new ConcurrentHashMap<>();
    private static final Map<String, Long> COOLDOWNS = new ConcurrentHashMap<>();

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.isCanceled() || event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        JobEnchantCompatConfigManager.HarvesterCompat cfg = JobEnchantCompatConfigManager.get().config().harvester;
        if (!cfg.enabled) return;

        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || !(stack.getItem() instanceof HoeItem)) return;

        Enchantment enchantment = getConfiguredEnchantment(cfg.enchantment_id);
        if (enchantment == null) return;

        int enchantLevel = stack.getEnchantmentLevel(enchantment);
        if (enchantLevel <= 0) return;

        BlockPos clickedPos = event.getPos();
        BlockState clickedState = event.getLevel().getBlockState(clickedPos);
        if (!isMatureCrop(clickedState)) return;

        long nowTick = player.getServer() != null ? player.getServer().getTickCount() : player.tickCount;
        String cooldownKey = player.getUUID() + "|" + clickedPos.asLong();
        Long cooldownUntil = COOLDOWNS.get(cooldownKey);
        if (cooldownUntil != null && cooldownUntil > nowTick) return;
        if (cfg.cooldown_ticks > 0) {
            COOLDOWNS.put(cooldownKey, nowTick + cfg.cooldown_ticks);
        }

        int radius = Math.max(0, enchantLevel * Math.max(0, cfg.radius_per_level) + cfg.radius_offset);
        radius = Math.min(radius, Math.max(0, cfg.max_radius));

        List<CropSnapshot> snapshots = collectMatureCrops(event.getLevel(), clickedPos, radius, cfg.max_crops_per_use);
        if (snapshots.isEmpty()) return;

        long executeTick = nowTick + Math.max(1, cfg.reward_delay_ticks);
        PendingHarvest pending = new PendingHarvest(executeTick, cfg.job_id, cfg.action, snapshots);
        PENDING.computeIfAbsent(player.getUUID(), ignored -> new ArrayList<>()).add(pending);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        MinecraftServer server = event.getServer();
        long tick = server.getTickCount();
        if (!COOLDOWNS.isEmpty() && tick % 200 == 0) {
            COOLDOWNS.entrySet().removeIf(entry -> entry.getValue() <= tick);
        }

        Iterator<Map.Entry<UUID, List<PendingHarvest>>> playerIterator = PENDING.entrySet().iterator();
        while (playerIterator.hasNext()) {
            Map.Entry<UUID, List<PendingHarvest>> entry = playerIterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                playerIterator.remove();
                continue;
            }

            Iterator<PendingHarvest> harvestIterator = entry.getValue().iterator();
            while (harvestIterator.hasNext()) {
                PendingHarvest harvest = harvestIterator.next();
                if (harvest.executeTick > tick) continue;
                rewardChangedCrops(player, harvest);
                harvestIterator.remove();
            }

            if (entry.getValue().isEmpty()) {
                playerIterator.remove();
            }
        }
    }

    private static Enchantment getConfiguredEnchantment(String enchantmentId) {
        try {
            ResourceLocation id = new ResourceLocation(enchantmentId);
            return ForgeRegistries.ENCHANTMENTS.getValue(id);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static List<CropSnapshot> collectMatureCrops(Level level, BlockPos center, int radius, int maxCrops) {
        List<CropSnapshot> snapshots = new ArrayList<>();
        int limit = Math.max(1, maxCrops);
        for (int z = -radius; z <= radius; z++) {
            for (int x = -radius; x <= radius; x++) {
                BlockPos pos = center.offset(x, 0, z);
                BlockState state = level.getBlockState(pos);
                if (!isMatureCrop(state)) continue;

                String blockId = getBlockId(state);
                snapshots.add(new CropSnapshot(pos.immutable(), blockId));
                if (snapshots.size() >= limit) return snapshots;
            }
        }
        return snapshots;
    }

    private static void rewardChangedCrops(ServerPlayer player, PendingHarvest harvest) {
        Map<String, Integer> harvestedByBlock = new HashMap<>();

        for (CropSnapshot snapshot : harvest.crops) {
            BlockState current = player.level().getBlockState(snapshot.pos);
            String currentId = getBlockId(current);
            if (!snapshot.blockId.equals(currentId) || !isMatureCrop(current)) {
                harvestedByBlock.merge(snapshot.blockId, 1, Integer::sum);
            }
        }

        for (Map.Entry<String, Integer> entry : harvestedByBlock.entrySet()) {
            int count = Math.max(1, entry.getValue());
            JobEvents.processJobAction(player, harvest.action, entry.getKey(), count, harvest.jobId.toLowerCase(Locale.ROOT));
        }
    }

    private static boolean isMatureCrop(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof CropBlock crop) return crop.isMaxAge(state);
        if (block instanceof SweetBerryBushBlock) return state.getValue(SweetBerryBushBlock.AGE) >= 3;
        if (block instanceof CocoaBlock) return state.getValue(CocoaBlock.AGE) >= 2;
        if (block instanceof NetherWartBlock) return state.getValue(NetherWartBlock.AGE) >= 3;
        return false;
    }

    private static String getBlockId(BlockState state) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return id != null ? id.toString() : "unknown";
    }

    private static class PendingHarvest {
        private final long executeTick;
        private final String jobId;
        private final String action;
        private final List<CropSnapshot> crops;

        private PendingHarvest(long executeTick, String jobId, String action, List<CropSnapshot> crops) {
            this.executeTick = executeTick;
            this.jobId = jobId;
            this.action = action;
            this.crops = crops;
        }
    }

    private record CropSnapshot(BlockPos pos, String blockId) {
    }
}
