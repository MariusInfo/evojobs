package org.evocraft.evojobs.client;

import net.minecraft.client.Minecraft;
import org.evocraft.evojobs.gui.QuestScreen;
import org.evocraft.evojobs.network.S2C_OpenQuestMenu;

public class ClientQuestHandler {

    // Această funcție este apelată exclusiv de jucător, niciodată de server!
    public static void openScreen(S2C_OpenQuestMenu data) {
        Minecraft.getInstance().setScreen(new QuestScreen(data));
    }
}