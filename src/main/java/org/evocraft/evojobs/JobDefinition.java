package org.evocraft.evojobs;

import java.util.HashMap;
import java.util.Map;

public class JobDefinition {
    public String id;
    public String displayName;
    public String icon;
    public String description;

    public Map<String, Map<String, Double>> rewards = new HashMap<>();

    public JobDefinition(String id, String displayName, String icon, String description) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.description = description;
    }

    public boolean hasTrigger(String action) {
        return rewards != null && rewards.containsKey(action);
    }

    public void addReward(String action, String target, double price) {
        rewards.computeIfAbsent(action, k -> new HashMap<>()).put(target, price);
    }

    public double getReward(String action, String target) {
        if (!rewards.containsKey(action)) return 0.0;
        Map<String, Double> targets = rewards.get(action);

        if (targets.containsKey(target)) return targets.get(target);
        if (target.startsWith("minecraft:")) {
            String shortTarget = target.replace("minecraft:", "");
            if (targets.containsKey(shortTarget)) return targets.get(shortTarget);
        }
        if (targets.containsKey("*")) return targets.get("*");

        return 0.0;
    }
}