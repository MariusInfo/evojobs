package org.evocraft.evojobs.client;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ClientJobsData {
    public static class JobInfo {
        public String name;
        public long level;
        public double currentXp;
        public double requiredXp;
        public ItemStack icon;
        public boolean isActive;
        public double xpMultiplier;
        public double moneyMultiplier;
        public String attributeName;
        public double attributePower;
        public double attributeEffectPercent;

        public JobInfo(String name, long level, double currentXp, double requiredXp, ItemStack icon, boolean isActive,
                       double xpMultiplier, double moneyMultiplier, String attributeName, double attributePower,
                       double attributeEffectPercent) {
            this.name = name;
            this.level = level;
            this.currentXp = currentXp;
            this.requiredXp = requiredXp;
            this.icon = icon;
            this.isActive = isActive;
            this.xpMultiplier = xpMultiplier;
            this.moneyMultiplier = moneyMultiplier;
            this.attributeName = attributeName;
            this.attributePower = attributePower;
            this.attributeEffectPercent = attributeEffectPercent;
        }
    }

    public static List<JobInfo> activeJobs = new ArrayList<>();
    public static long totalJobLevel = 0L;
    public static long synergyLevel = 0L;

    public static void updateJobs(List<JobInfo> serverJobs, long totalLevel, long synergy) {
        activeJobs.clear();
        activeJobs.addAll(serverJobs);
        totalJobLevel = totalLevel;
        synergyLevel = synergy;
    }
}
