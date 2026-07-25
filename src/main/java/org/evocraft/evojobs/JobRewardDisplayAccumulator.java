package org.evocraft.evojobs;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.UUID;

public final class JobRewardDisplayAccumulator {
    private static final double MAX_DISPLAY_TOTAL = 1.0E18;
    private static final Map<UUID, RewardSession> SESSIONS = new HashMap<>();

    private JobRewardDisplayAccumulator() {
    }

    public static void addAndDisplay(ServerPlayer player, double money, double xp,
                                     Collection<String> jobNames) {
        double safeMoney = sanitize(money);
        double safeXp = sanitize(xp);
        if (safeMoney <= 0.0 && safeXp <= 0.0) return;

        long currentTick = player.getServer() != null
                ? player.getServer().getTickCount()
                : player.tickCount;
        long windowTicks = getWindowTicks();
        RewardSession session = SESSIONS.computeIfAbsent(player.getUUID(), ignored -> new RewardSession());

        if (windowTicks <= 0L || session.lastRewardTick < 0L ||
                currentTick < session.lastRewardTick ||
                currentTick - session.lastRewardTick >= windowTicks) {
            session.reset();
        }

        session.money = addSafely(session.money, safeMoney);
        session.xp = addSafely(session.xp, safeXp);
        session.lastRewardTick = currentTick;
        if (jobNames != null) {
            for (String jobName : jobNames) {
                if (jobName != null && !jobName.isBlank()) {
                    session.jobNames.add(jobName.trim());
                }
            }
        }

        String jobs = session.jobNames.isEmpty() ? "Jobs" : String.join(", ", session.jobNames);
        String message = "\u00A7a+ " + JobProgressionService.formatMoney(session.money) +
                " \u00A7f| \u00A7b+ " + JobProgressionService.formatNumber(session.xp) +
                " XP \u00A77(" + jobs + ")";
        player.displayClientMessage(Component.literal(message), true);
    }

    public static void reset(UUID playerId) {
        if (playerId != null) SESSIONS.remove(playerId);
    }

    public static void cleanup(long currentTick) {
        long windowTicks = Math.max(1L, getWindowTicks());
        SESSIONS.entrySet().removeIf(entry -> {
            long lastTick = entry.getValue().lastRewardTick;
            return lastTick < 0L || currentTick < lastTick || currentTick - lastTick >= windowTicks;
        });
    }

    private static long getWindowTicks() {
        double seconds = JobProgressionConfigManager.get().config()
                .reward_display.accumulation_window_seconds;
        if (!Double.isFinite(seconds) || seconds <= 0.0) return 0L;

        double ticks = Math.ceil(seconds * 20.0);
        return ticks >= Long.MAX_VALUE ? Long.MAX_VALUE : Math.max(1L, (long) ticks);
    }

    private static double sanitize(double value) {
        if (!Double.isFinite(value) || value <= 0.0) return 0.0;
        return Math.min(value, MAX_DISPLAY_TOTAL);
    }

    private static double addSafely(double current, double amount) {
        if (current >= MAX_DISPLAY_TOTAL || amount >= MAX_DISPLAY_TOTAL - current) {
            return MAX_DISPLAY_TOTAL;
        }
        return current + amount;
    }

    private static final class RewardSession {
        private double money;
        private double xp;
        private long lastRewardTick = -1L;
        private final LinkedHashSet<String> jobNames = new LinkedHashSet<>();

        private void reset() {
            money = 0.0;
            xp = 0.0;
            lastRewardTick = -1L;
            jobNames.clear();
        }
    }
}
