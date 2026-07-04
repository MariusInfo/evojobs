package org.evocraft.evojobs;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.evocraft.evocore.data.EconomyManager;
import org.evocraft.evojobs.JobProgressionConfigManager.JobAttributeConfig;
import org.evocraft.evojobs.JobProgressionConfigManager.JobBonus;
import org.evocraft.evojobs.JobProgressionConfigManager.LevelRewardRule;
import org.evocraft.evojobs.JobProgressionConfigManager.ProgressionConfig;
import org.evocraft.evojobs.JobProgressionConfigManager.RewardAction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class JobProgressionService {
    private static final double MAX_SAFE_REWARD = 1.0E12;
    private static final double MAX_SAFE_REQUIRED_XP = 1.0E18;
    private static final int MAX_REWARD_ACTIONS_PER_LEVEL_CHANGE = 10_000;

    private static final Map<UUID, TemporaryBoosts> TEMPORARY_BOOSTS = new ConcurrentHashMap<>();

    public static double getRequiredXpForNextLevel(long level) {
        long safeLevel = Math.max(1L, level);
        ProgressionConfig cfg = config();
        double required = cfg.progression.required_xp.base * Math.pow((double) safeLevel, cfg.progression.required_xp.exponent);
        if (!Double.isFinite(required) || required <= 0.0) return MAX_SAFE_REQUIRED_XP;
        return Math.ceil(Math.min(required, MAX_SAFE_REQUIRED_XP));
    }

    public static double calculateScaledXpReward(double baseXp, long jobLevel, ServerPlayer player) {
        double reward = sanitizeNonNegative(baseXp);
        reward *= getIndividualXpMultiplier(jobLevel);
        reward *= 1.0 + (getTotalJobBonusPercent(player, BonusType.XP) / 100.0);
        reward *= 1.0 + (getSynergyBonusPercent(player, BonusType.XP) / 100.0);
        reward *= getTemporaryXpMultiplier(player);
        return sanitizeReward(reward);
    }

    public static double calculateScaledMoneyReward(double baseMoney, long jobLevel, ServerPlayer player, double existingBoosterMultiplier) {
        double reward = sanitizeNonNegative(baseMoney);
        reward *= getIndividualMoneyMultiplier(jobLevel);
        reward *= 1.0 + (getTotalJobBonusPercent(player, BonusType.MONEY) / 100.0);
        reward *= 1.0 + (getSynergyBonusPercent(player, BonusType.MONEY) / 100.0);
        reward *= Math.max(0.0, existingBoosterMultiplier);
        reward *= getTemporaryMoneyMultiplier(player);
        return sanitizeReward(reward);
    }

    public static double getBaseXpMultiplier(String jobId) {
        ProgressionConfig cfg = config();
        Double jobMultiplier = cfg.progression.base_xp_multipliers.job_multipliers.get(jobId.toLowerCase(Locale.ROOT));
        if (jobMultiplier != null && Double.isFinite(jobMultiplier) && jobMultiplier > 0.0) return jobMultiplier;
        return Math.max(0.0, cfg.progression.base_xp_multipliers.default_multiplier);
    }

    public static double getIndividualXpMultiplier(long level) {
        return sqrtMultiplier(level, config().progression.xp_reward.sqrt_multiplier);
    }

    public static double getIndividualMoneyMultiplier(long level) {
        return sqrtMultiplier(level, config().progression.money_reward.sqrt_multiplier);
    }

    private static double sqrtMultiplier(long level, double sqrtMultiplier) {
        long safeLevel = Math.max(1L, level);
        double result = 1.0 + Math.max(0.0, sqrtMultiplier) * Math.sqrt((double) safeLevel);
        if (!Double.isFinite(result) || result < 1.0) return 1.0;
        return result;
    }

    public static long getTotalJobLevel(ServerPlayer player) {
        return player == null ? 0L : getTotalJobLevel(player.getUUID());
    }

    public static long getTotalJobLevel(UUID uuid) {
        long total = 0L;
        for (JobDefinition job : JobConfigManager.get().getAllJobs()) {
            total = saturatedAdd(total, getEffectiveJobLevel(uuid, job.id));
        }
        return total;
    }

    public static long getSynergyLevel(ServerPlayer player) {
        return player == null ? 0L : getSynergyLevel(player.getUUID());
    }

    public static long getSynergyLevel(UUID uuid) {
        long min = Long.MAX_VALUE;
        boolean found = false;
        for (JobDefinition job : JobConfigManager.get().getAllJobs()) {
            min = Math.min(min, getEffectiveJobLevel(uuid, job.id));
            found = true;
        }
        return found ? min : 0L;
    }

    public static long getEffectiveJobLevel(UUID uuid, String jobId) {
        JobData data = JobManager.get().getAllJobsHistory(uuid).get(jobId);
        // Registered jobs with no stored progress count as level 1, so new jobs predictably affect Synergy Level.
        if (data == null) return 1L;
        return Math.max(1L, data.level);
    }

    public static AttributeInfo getJobAttributeInfo(String jobId, long level, ServerPlayer player) {
        JobAttributeConfig cfg = getAttributeConfig(jobId);
        double powerMultiplier = cfg.power_multiplier > 0.0 ? cfg.power_multiplier : config().progression.job_attribute.base_multiplier;
        double rawPower = powerMultiplier * Math.sqrt((double) Math.max(1L, level));
        rawPower *= 1.0 + (getTotalJobBonusPercent(player, BonusType.ATTRIBUTE) / 100.0);
        rawPower *= 1.0 + (getSynergyBonusPercent(player, BonusType.ATTRIBUTE) / 100.0);
        rawPower *= 1.0 + (getPermanentJobBonusPercent(jobId, level, "ATTRIBUTE_EFFECTIVENESS") / 100.0);

        if (!Double.isFinite(rawPower) || rawPower < 0.0) rawPower = 0.0;
        double effect = rawPower * Math.max(0.0, cfg.conversion);
        if (Double.isFinite(cfg.max_effect_percent) && cfg.max_effect_percent > 0.0) {
            effect = Math.min(effect, cfg.max_effect_percent);
        }
        if (!Double.isFinite(effect) || effect < 0.0) effect = 0.0;
        return new AttributeInfo(cfg.name, cfg.type, rawPower, effect, cfg.affects_pvp);
    }

    public static double getTotalJobBonusPercent(ServerPlayer player, BonusType type) {
        if (player == null || !config().total_job_level.enabled) return 0.0;
        long totalLevel = getTotalJobLevel(player);
        double value = 0.0;
        for (Map.Entry<Long, JobBonus> entry : sortedMilestones(config().total_job_level.milestones)) {
            if (totalLevel >= entry.getKey()) value += getBonusValue(entry.getValue(), type);
        }
        return sanitizePercent(value);
    }

    public static double getSynergyBonusPercent(ServerPlayer player, BonusType type) {
        if (player == null || !config().synergy.enabled) return 0.0;
        long synergy = getSynergyLevel(player);
        double value = 0.0;
        boolean cumulative = "CUMULATIVE".equalsIgnoreCase(config().synergy.reward_mode);
        for (Map.Entry<Long, JobBonus> entry : sortedMilestones(config().synergy.milestones)) {
            if (synergy >= entry.getKey()) {
                if (cumulative) value += getBonusValue(entry.getValue(), type);
                else value = getBonusValue(entry.getValue(), type);
            }
        }
        return sanitizePercent(value);
    }

    public static double getGlobalMovementSpeedPercent(ServerPlayer player) {
        return getTotalJobBonusPercent(player, BonusType.MOVEMENT_SPEED);
    }

    public static void processLevelRewards(ServerPlayer player, String jobId, long oldLevel, long newLevel) {
        if (player == null || newLevel <= oldLevel) return;
        int executedActions = 0;
        for (Map.Entry<String, LevelRewardRule> entry : config().level_rewards.entrySet()) {
            LevelRewardRule rule = entry.getValue();
            if (rule == null || rule.interval <= 0 || rule.actions == null || rule.actions.isEmpty()) continue;

            long first = nextMultipleAfter(oldLevel, rule.interval);
            for (long level = first; level <= newLevel && level > 0; level += rule.interval) {
                for (RewardAction action : rule.actions) {
                    if (executedActions >= MAX_REWARD_ACTIONS_PER_LEVEL_CHANGE) {
                        player.sendSystemMessage(Component.literal("\u00A7e[EvoJobs] Level reward execution was capped for this update to protect server performance."));
                        return;
                    }
                    executeRewardAction(player, jobId, level, action);
                    executedActions++;
                }
                if (Long.MAX_VALUE - level < rule.interval) break;
            }
        }
    }

    public static double getPermanentJobBonusPercent(String jobId, long level, String bonusType) {
        double total = 0.0;
        for (LevelRewardRule rule : config().level_rewards.values()) {
            if (rule == null || rule.interval <= 0 || rule.actions == null) continue;
            long hits = Math.max(0L, level / rule.interval);
            if (hits <= 0L) continue;
            for (RewardAction action : rule.actions) {
                if (action == null || !"PERMANENT_JOB_BONUS".equalsIgnoreCase(action.type)) continue;
                if (bonusType.equalsIgnoreCase(String.valueOf(action.bonus_type))) {
                    total += hits * Math.max(0.0, action.percent);
                }
            }
        }
        return sanitizePercent(total);
    }

    private static void executeRewardAction(ServerPlayer player, String jobId, long level, RewardAction action) {
        if (action == null || action.type == null) return;
        String type = action.type.toUpperCase(Locale.ROOT);
        switch (type) {
            case "MONEY" -> {
                double amount = evaluateRewardFormula(action.amount_formula, player, jobId, level);
                if (amount > 0.0) {
                    EconomyManager.get().addBalance(player.getUUID(), amount);
                    player.sendSystemMessage(Component.literal("\u00A7a[EvoJobs] Milestone reward: +" + formatNumber(amount) + " Lei"));
                }
            }
            case "COMMAND" -> runCommand(player, replacePlaceholders(action.command, player, jobId, level));
            case "MESSAGE" -> {
                String message = action.message != null ? action.message : "\u00A7a[EvoJobs] Milestone reached for {job} level {level}!";
                player.sendSystemMessage(Component.literal(replacePlaceholders(message, player, jobId, level)));
            }
            case "SOUND" -> player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.MASTER, 1.0f, 1.0f);
            case "TEMP_XP_BOOST" -> addTemporaryBoost(player, Math.max(1.0, action.multiplier), 1.0, action.duration_seconds);
            case "TEMP_MONEY_BOOST" -> addTemporaryBoost(player, 1.0, Math.max(1.0, action.multiplier), action.duration_seconds);
            case "PERMANENT_JOB_BONUS" -> {
                if (action.percent > 0.0) {
                    player.sendSystemMessage(Component.literal("\u00A7a[EvoJobs] Permanent " + action.bonus_type + " bonus +" + formatPercent(action.percent) + " unlocked for " + jobId + "!"));
                }
            }
            default -> player.sendSystemMessage(Component.literal("\u00A77[EvoJobs] Unknown milestone reward action: " + type));
        }
    }

    private static void runCommand(ServerPlayer player, String command) {
        if (command == null || command.trim().isEmpty() || player.getServer() == null) return;
        CommandSourceStack source = player.getServer().createCommandSourceStack().withSuppressedOutput();
        player.getServer().getCommands().performPrefixedCommand(source, command);
    }

    private static void addTemporaryBoost(ServerPlayer player, double xpMultiplier, double moneyMultiplier, int durationSeconds) {
        if (durationSeconds <= 0) return;
        long expiresAt = System.currentTimeMillis() + (durationSeconds * 1000L);
        TEMPORARY_BOOSTS.compute(player.getUUID(), (uuid, old) -> {
            TemporaryBoosts boosts = old != null ? old : new TemporaryBoosts();
            boosts.xpMultiplier = Math.max(boosts.xpMultiplier, xpMultiplier);
            boosts.moneyMultiplier = Math.max(boosts.moneyMultiplier, moneyMultiplier);
            boosts.expiresAtMs = Math.max(boosts.expiresAtMs, expiresAt);
            return boosts;
        });
        player.sendSystemMessage(Component.literal("\u00A7a[EvoJobs] Temporary booster active for " + durationSeconds + " seconds."));
    }

    private static double getTemporaryXpMultiplier(ServerPlayer player) {
        return getTemporaryMultiplier(player, true);
    }

    private static double getTemporaryMoneyMultiplier(ServerPlayer player) {
        return getTemporaryMultiplier(player, false);
    }

    private static double getTemporaryMultiplier(ServerPlayer player, boolean xp) {
        if (player == null) return 1.0;
        TemporaryBoosts boosts = TEMPORARY_BOOSTS.get(player.getUUID());
        if (boosts == null) return 1.0;
        if (System.currentTimeMillis() > boosts.expiresAtMs) {
            TEMPORARY_BOOSTS.remove(player.getUUID());
            return 1.0;
        }
        return Math.max(1.0, xp ? boosts.xpMultiplier : boosts.moneyMultiplier);
    }

    private static double evaluateRewardFormula(String formula, ServerPlayer player, String jobId, long level) {
        if (formula == null || formula.trim().isEmpty()) return 0.0;
        Map<String, Double> vars = new HashMap<>();
        vars.put("level", (double) level);
        vars.put("total_job_level", (double) getTotalJobLevel(player));
        vars.put("synergy_level", (double) getSynergyLevel(player));
        try {
            double result = new FormulaParser(formula, vars).parse();
            return sanitizeReward(result);
        } catch (Exception e) {
            System.out.println("[EvoJobs] Invalid level reward formula: " + formula);
            return 0.0;
        }
    }

    private static String replacePlaceholders(String input, ServerPlayer player, String jobId, long level) {
        if (input == null) return "";
        return input
                .replace("{player}", player.getGameProfile().getName())
                .replace("{uuid}", player.getUUID().toString())
                .replace("{job}", jobId)
                .replace("{level}", Long.toString(level))
                .replace("{total_job_level}", Long.toString(getTotalJobLevel(player)))
                .replace("{synergy_level}", Long.toString(getSynergyLevel(player)));
    }

    private static List<Map.Entry<Long, JobBonus>> sortedMilestones(Map<String, JobBonus> milestones) {
        List<Map.Entry<Long, JobBonus>> parsed = new ArrayList<>();
        if (milestones == null) return parsed;
        for (Map.Entry<String, JobBonus> entry : milestones.entrySet()) {
            try {
                long level = Long.parseLong(entry.getKey());
                if (level > 0 && entry.getValue() != null) {
                    parsed.add(Map.entry(level, entry.getValue()));
                }
            } catch (Exception ignored) {}
        }
        parsed.sort(Comparator.comparingLong(Map.Entry::getKey));
        return parsed;
    }

    private static JobAttributeConfig getAttributeConfig(String jobId) {
        JobAttributeConfig cfg = config().job_attributes.get(jobId.toLowerCase(Locale.ROOT));
        if (cfg != null) return cfg;
        JobAttributeConfig fallback = new JobAttributeConfig();
        fallback.name = "Job Mastery";
        fallback.type = "GENERIC";
        return fallback;
    }

    private static double getBonusValue(JobBonus bonus, BonusType type) {
        return switch (type) {
            case MONEY -> bonus.global_money_percent;
            case XP -> bonus.global_xp_percent;
            case ATTRIBUTE -> bonus.global_job_attribute_percent;
            case MOVEMENT_SPEED -> bonus.movement_speed_percent;
        };
    }

    private static long nextMultipleAfter(long level, long interval) {
        if (interval <= 0) return Long.MAX_VALUE;
        long next = ((Math.max(0L, level) / interval) + 1L) * interval;
        return next > 0 ? next : Long.MAX_VALUE;
    }

    private static long saturatedAdd(long left, long right) {
        if (right > 0 && left > Long.MAX_VALUE - right) return Long.MAX_VALUE;
        return left + right;
    }

    private static double sanitizeNonNegative(double value) {
        if (!Double.isFinite(value) || value <= 0.0) return 0.0;
        return value;
    }

    private static double sanitizeReward(double value) {
        if (!Double.isFinite(value) || value <= 0.0) return 0.0;
        return Math.min(value, MAX_SAFE_REWARD);
    }

    private static double sanitizePercent(double value) {
        if (!Double.isFinite(value) || value < 0.0) return 0.0;
        return Math.min(value, 1_000_000.0);
    }

    public static String formatNumber(double value) {
        if (!Double.isFinite(value)) return "0";
        double abs = Math.abs(value);
        if (abs >= 1_000_000_000.0) return String.format(Locale.US, "%.2fB", value / 1_000_000_000.0);
        if (abs >= 1_000_000.0) return String.format(Locale.US, "%.2fM", value / 1_000_000.0);
        if (abs >= 1_000.0) return String.format(Locale.US, "%,.0f", value);
        if (abs < 10.0) return String.format(Locale.US, "%.2f", value);
        return String.format(Locale.US, "%.1f", value);
    }

    public static String formatPercent(double value) {
        return String.format(Locale.US, "%.2f%%", value);
    }

    private static ProgressionConfig config() {
        return JobProgressionConfigManager.get().config();
    }

    public enum BonusType {
        MONEY,
        XP,
        ATTRIBUTE,
        MOVEMENT_SPEED
    }

    public static class AttributeInfo {
        public final String name;
        public final String type;
        public final double rawPower;
        public final double effectPercent;
        public final boolean affectsPvp;

        public AttributeInfo(String name, String type, double rawPower, double effectPercent, boolean affectsPvp) {
            this.name = name;
            this.type = type;
            this.rawPower = rawPower;
            this.effectPercent = effectPercent;
            this.affectsPvp = affectsPvp;
        }
    }

    private static class TemporaryBoosts {
        double xpMultiplier = 1.0;
        double moneyMultiplier = 1.0;
        long expiresAtMs = 0L;
    }

    private static class FormulaParser {
        private final String text;
        private final Map<String, Double> variables;
        private int pos = -1;
        private int ch;

        FormulaParser(String text, Map<String, Double> variables) {
            this.text = text;
            this.variables = variables;
        }

        double parse() {
            nextChar();
            double value = parseExpression();
            if (!Double.isFinite(value)) return 0.0;
            return value;
        }

        private void nextChar() {
            ch = (++pos < text.length()) ? text.charAt(pos) : -1;
        }

        private boolean eat(int charToEat) {
            while (ch == ' ') nextChar();
            if (ch == charToEat) {
                nextChar();
                return true;
            }
            return false;
        }

        private double parseExpression() {
            double value = parseTerm();
            for (;;) {
                if (eat('+')) value += parseTerm();
                else if (eat('-')) value -= parseTerm();
                else return value;
            }
        }

        private double parseTerm() {
            double value = parseFactor();
            for (;;) {
                if (eat('*')) value *= parseFactor();
                else if (eat('/')) {
                    double divisor = parseFactor();
                    value = divisor == 0.0 ? 0.0 : value / divisor;
                } else return value;
            }
        }

        private double parseFactor() {
            if (eat('+')) return parseFactor();
            if (eat('-')) return -parseFactor();

            double value;
            int startPos = this.pos;
            if (eat('(')) {
                value = parseExpression();
                eat(')');
            } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                value = Double.parseDouble(text.substring(startPos, this.pos));
            } else if (Character.isLetter(ch) || ch == '_') {
                while (Character.isLetterOrDigit(ch) || ch == '_') nextChar();
                String name = text.substring(startPos, this.pos).toLowerCase(Locale.ROOT);
                value = variables.getOrDefault(name, 0.0);
            } else {
                nextChar();
                return 0.0;
            }

            return value;
        }
    }
}
