package org.evocraft.evojobs.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.evocraft.evojobs.S2C_SyncJobsPacket;

public class EvoJobsPacketHandler {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("evojobs", "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        int id = 0;

        // 1. Register the packet that sends jobs to the client and Hub.
        INSTANCE.registerMessage(id++, S2C_SyncJobsPacket.class, S2C_SyncJobsPacket::toBytes, S2C_SyncJobsPacket::new, S2C_SyncJobsPacket::handle);

        // 2. Register the packet for the quest menu.
        INSTANCE.registerMessage(id++, S2C_OpenQuestMenu.class, S2C_OpenQuestMenu::toBytes, S2C_OpenQuestMenu::new, S2C_OpenQuestMenu::handle);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}
