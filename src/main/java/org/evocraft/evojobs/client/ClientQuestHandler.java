package org.evocraft.evojobs.client;

import net.minecraft.client.Minecraft;
import org.evocraft.evojobs.gui.QuestScreen;
import org.evocraft.evojobs.network.S2C_OpenQuestMenu;

public class ClientQuestHandler {

    // This function is called only by the client player, never by the server.
    public static void openScreen(S2C_OpenQuestMenu data) {
        Minecraft.getInstance().setScreen(new QuestScreen(data));
    }
}
