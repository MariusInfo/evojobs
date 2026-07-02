package org.evocraft.evojobs;

public class JobData {
    public int level;
    public double xp;
    public boolean isActive; // True = Currently working, False = Resigned (Paused)

    public JobData() {
        this.level = 1;
        this.xp = 0;
        this.isActive = true;
    }

    public JobData(int level, double xp, boolean isActive) {
        this.level = level;
        this.xp = xp;
        this.isActive = isActive;
    }

    public double getRequiredXp() {
        return 100 * Math.pow(level, 1.5);
    }
}