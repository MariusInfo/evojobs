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

    // Aici ținem minte recoltele mature pe care tractorul urmează să le calce
    private static final Map<UUID, Map<BlockPos, String>> trackedCrops = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side.isClient() || event.phase == TickEvent.Phase.START) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        Entity vehicle = player.getVehicle();

        // Verificăm dacă ești la volanul unui vehicul din Automobility
        if (vehicle != null && vehicle.getType().getDescriptionId().contains("automobility")) {

            Map<BlockPos, String> previousCrops = trackedCrops.computeIfAbsent(player.getUUID(), k -> new HashMap<>());
            Map<BlockPos, String> currentCrops = new HashMap<>();

            // 1. Verificăm recoltele pe care le-am memorat acum o fracțiune de secundă
            for (Map.Entry<BlockPos, String> entry : previousCrops.entrySet()) {
                BlockPos pos = entry.getKey();
                String blockId = entry.getValue();

                // Dacă ai condus prea departe și nu ai luat planta, o uităm
                if (player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) > 100.0) continue;

                BlockState state = player.level().getBlockState(pos);
                Block block = state.getBlock();

                boolean harvested = false;

                // Verificăm dacă recolta matură a devenit sămânță mică (tractorul a cules-o)
                if (block instanceof CropBlock crop) {
                    if (!crop.isMaxAge(state)) harvested = true;
                }
                // Sau dacă a dispărut de tot (Aer)
                else if (state.isAir()) {
                    harvested = true;
                }

                // Dacă nu a atins-o încă, o păstrăm în memorie pentru următoarea fracțiune de secundă
                if (!harvested) {
                    if (block instanceof CropBlock crop && crop.isMaxAge(state)) currentCrops.put(pos, blockId);
                    else if (block == Blocks.MELON || block == Blocks.PUMPKIN) currentCrops.put(pos, blockId);
                }
                // Dacă A FOST RECOLTATĂ -> PLĂTIM!
                else {
                    JobEvents.processJobAction(player, "BREAK", blockId, 1.0, "farmer");
                }
            }

            // 2. Radarul: Scanăm 3 cuburi în jurul tractorului ca să vedem ce recolte noi se apropie
            BlockPos playerPos = player.blockPosition();
            for (int x = -3; x <= 3; x++) {
                for (int y = -2; y <= 2; y++) {
                    for (int z = -3; z <= 3; z++) {
                        BlockPos scanPos = playerPos.offset(x, y, z);

                        if (!currentCrops.containsKey(scanPos)) {
                            BlockState state = player.level().getBlockState(scanPos);
                            Block block = state.getBlock();

                            // Dacă găsim o plantă 100% matură (sau pepeni/dovleci), o adăugăm pe "lista neagră"
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

            // Salvăm ce vede camera pentru a verifica în milisecunda următoare
            trackedCrops.put(player.getUUID(), currentCrops);

        } else {
            // Dacă ai coborât din mașină, oprim camera
            trackedCrops.remove(player.getUUID());
        }
    }
}