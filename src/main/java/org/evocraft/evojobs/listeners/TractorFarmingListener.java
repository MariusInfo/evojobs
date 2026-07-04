package org.evocraft.evojobs.listeners;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.evocraft.evojobs.JobEvents;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "evojobs", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TractorFarmingListener {

    // Mature crops that the tractor is about to drive over.
    private static final Map<UUID, Map<BlockPos, String>> trackedCrops = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side.isClient() || event.phase == TickEvent.Phase.START) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        Entity vehicle = player.getVehicle();

        // Check whether the player is driving an Automobility vehicle.
        if (vehicle != null && vehicle.getType().getDescriptionId().contains("automobility")) {

            Map<BlockPos, String> previousCrops = trackedCrops.computeIfAbsent(player.getUUID(), k -> new HashMap<>());
            Map<BlockPos, String> currentCrops = new HashMap<>();

            // 1. Check the crops remembered a fraction of a second ago.
            for (Map.Entry<BlockPos, String> entry : previousCrops.entrySet()) {
                BlockPos pos = entry.getKey();
                String blockId = entry.getValue();

                // Forget crops that are now too far away.
                if (player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) > 100.0) continue;

                BlockState state = player.level().getBlockState(pos);
                Block block = state.getBlock();

                boolean harvested = false;

                // Check whether the mature crop became a young seedling.
                if (block instanceof CropBlock crop) {
                    if (!crop.isMaxAge(state)) harvested = true;
                }
                // Or if it disappeared completely.
                else if (state.isAir()) {
                    harvested = true;
                }

                // Keep untouched crops in memory for the next short tick window.
                if (!harvested) {
                    if (block instanceof CropBlock crop && crop.isMaxAge(state)) currentCrops.put(pos, blockId);
                    else if (block == Blocks.MELON || block == Blocks.PUMPKIN) currentCrops.put(pos, blockId);
                }
                // If it was harvested, pay the reward.
                else {
                    JobEvents.processJobAction(player, "BREAK", blockId, 1.0, "farmer");
                }
            }

            // 2. Radar: scan a 3-block cube around the tractor for nearby crops.
            BlockPos playerPos = player.blockPosition();
            for (int x = -3; x <= 3; x++) {
                for (int y = -2; y <= 2; y++) {
                    for (int z = -3; z <= 3; z++) {
                        BlockPos scanPos = playerPos.offset(x, y, z);

                        if (!currentCrops.containsKey(scanPos)) {
                            BlockState state = player.level().getBlockState(scanPos);
                            Block block = state.getBlock();

                            // Add fully mature crops and melons/pumpkins to the watch list.
                            if ((block instanceof CropBlock crop && crop.isMaxAge(state)) || block == Blocks.MELON || block == Blocks.PUMPKIN) {
                                ResourceLocation key = ForgeRegistries.BLOCKS.getKey(block);
                                if (key != null) {
                                    currentCrops.put(scanPos, key.toString());
                                }
                            }
                        }
                    }
                }
            }

            // Save what the camera sees so we can check it on the next tick.
            trackedCrops.put(player.getUUID(), currentCrops);

        } else {
            // Stop tracking once the player exits the vehicle.
            trackedCrops.remove(player.getUUID());
        }
    }
}
