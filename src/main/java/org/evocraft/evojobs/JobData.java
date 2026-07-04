package org.evocraft.evojobs;

public class JobData {
    public long level;
    public double xp;
    public boolean isActive; // True = Currently working, False = Resigned (Paused)

    public JobData() {
        this.level = 1;
        this.xp = 0;
        this.isActive = true;
    }

    public JobData(long level, double xp, boolean isActive) {
        this.level = Math.max(1L, level);
        this.xp = Double.isFinite(xp) && xp > 0.0 ? xp : 0.0;
        this.isActive = isActive;
    }

    public double getRequiredXp() {
        return JobProgressionService.getRequiredXpForNextLevel(level);
    }
}
