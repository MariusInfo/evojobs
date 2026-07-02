package org.evocraft.evojobs.client;

import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

public class ClientJobsData {
    public static class JobInfo {
        public String name;
        public int level;
        public double currentXp;
        public double requiredXp;
        public ItemStack icon;
        public boolean isActive;

        public JobInfo(String name, int level, double currentXp, double requiredXp, ItemStack icon, boolean isActive) {
            this.name = name; this.level = level; this.currentXp = currentXp; this.requiredXp = requiredXp; this.icon = icon; this.isActive = isActive;
        }
    }

    public static List<JobInfo> activeJobs = new ArrayList<>();

    public static void updateJobs(List<JobInfo> serverJobs) {
        activeJobs.clear();
        activeJobs.addAll(serverJobs);
    }
}