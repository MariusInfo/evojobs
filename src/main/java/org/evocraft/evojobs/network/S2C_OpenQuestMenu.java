package org.evocraft.evojobs.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.evocraft.evojobs.quest.QuestManager;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class S2C_OpenQuestMenu {
    public final List<String> jobs;
    public final List<String> targets;
    public final List<String> rarities;
    public final int[] requirements;
    public final double[] money;
    public final double[] xp;
    public final int[] progressData;

    public S2C_OpenQuestMenu(int[] progressData) {
        this.progressData = progressData;
        jobs = new ArrayList<>(); targets = new ArrayList<>(); rarities = new ArrayList<>();
        requirements = new int[5]; money = new double[5]; xp = new double[5];

        for (int i = 0; i < 5; i++) {
            if (i < QuestManager.TODAYS_QUESTS.size()) {
                QuestManager.DailyQuest q = QuestManager.TODAYS_QUESTS.get(i);
                jobs.add(q.template.jobId);
                targets.add(q.template.displayName);
                rarities.add(q.rarity.name);
                requirements[i] = q.requiredAmount;
                money[i] = q.rewardMoney;
                xp[i] = q.rewardXp;
            } else {
                jobs.add(""); targets.add(""); rarities.add("");
            }
        }
    }

    public S2C_OpenQuestMenu(FriendlyByteBuf buf) {
        jobs = new ArrayList<>(); targets = new ArrayList<>(); rarities = new ArrayList<>();
        requirements = new int[5]; money = new double[5]; xp = new double[5];
        for (int i = 0; i < 5; i++) {
            jobs.add(buf.readUtf()); targets.add(buf.readUtf()); rarities.add(buf.readUtf());
            requirements[i] = buf.readInt(); money[i] = buf.readDouble(); xp[i] = buf.readDouble();
        }
        progressData = buf.readVarIntArray();
    }

    public void toBytes(FriendlyByteBuf buf) {
        for (int i = 0; i < 5; i++) {
            buf.writeUtf(jobs.get(i)); buf.writeUtf(targets.get(i)); buf.writeUtf(rarities.get(i));
            buf.writeInt(requirements[i]); buf.writeDouble(money[i]); buf.writeDouble(xp[i]);
        }
        buf.writeVarIntArray(progressData);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Strict client-side guard to avoid server crashes.
            if (ctx.get().getDirection().getReceptionSide().isClient()) {
                org.evocraft.evojobs.client.ClientQuestHandler.openScreen(this);
            }
        });
        ctx.get().setPacketHandled(true);
        return true;
    }
}
