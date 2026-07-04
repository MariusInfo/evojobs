package org.evocraft.evojobs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class JobProgressionConfigManager {
    private static JobProgressionConfigManager INSTANCE;

    private final File configFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private ProgressionConfig config;

    public JobProgressionConfigManager() {
        this.configFile = FMLPaths.CONFIGDIR.get().resolve("evojobs_progression.json").toFile();
        load();
    }

    public static void initialize() {
        if (INSTANCE == null) INSTANCE = new JobProgressionConfigManager();
    }

    public static JobProgressionConfigManager get() {
        if (INSTANCE == null) initialize();
        return INSTANCE;
    }

    public ProgressionConfig config() {
        if (config == null) config = createDefaultConfig();
        return config;
    }

    public void load() {
        if (!configFile.exists()) {
            config = createDefaultConfig();
            save();
            return;
        }

        try (Reader reader = new FileReader(configFile)) {
            config = gson.fromJson(reader, ProgressionConfig.class);
        } catch (Exception e) {
            System.out.println("[EvoJobs] Could not load evojobs_progression.json. Using safe defaults.");
            e.printStackTrace();
            config = createDefaultConfig();
        }

        if (config == null) config = createDefaultConfig();
        validateConfig(config);
        save();
    }

    private void save() {
        try {
            File parent = configFile.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            try (Writer writer = new FileWriter(configFile)) {
                gson.toJson(config, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ProgressionConfig createDefaultConfig() {
        ProgressionConfig cfg = new ProgressionConfig();

        cfg.progression.required_xp.base = 100.0;
        cfg.progression.required_xp.exponent = 1.55;
        cfg.progression.xp_reward.sqrt_multiplier = 0.5;
        cfg.progression.money_reward.sqrt_multiplier = 0.2;
        cfg.progression.job_attribute.base_multiplier = 2.0;

        cfg.progression.base_xp_multipliers.default_multiplier = 2.0;
        cfg.progression.base_xp_multipliers.job_multipliers.put("miner", 5.0);

        cfg.total_job_level.enabled = true;
        cfg.total_job_level.milestones.put("25", bonus(1.0, 0.0, 0.0, 0.0));
        cfg.total_job_level.milestones.put("50", bonus(0.0, 1.0, 0.0, 0.0));
        cfg.total_job_level.milestones.put("100", bonus(0.0, 0.0, 1.0, 0.0));
        cfg.total_job_level.milestones.put("150", bonus(2.0, 0.0, 0.0, 0.0));
        cfg.total_job_level.milestones.put("200", bonus(0.0, 2.0, 0.0, 0.0));
        cfg.total_job_level.milestones.put("300", bonus(0.0, 0.0, 0.0, 1.0));
        cfg.total_job_level.milestones.put("500", bonus(5.0, 0.0, 0.0, 0.0));
        cfg.total_job_level.milestones.put("750", bonus(0.0, 5.0, 0.0, 0.0));
        cfg.total_job_level.milestones.put("1000", bonus(0.0, 0.0, 5.0, 0.0));

        cfg.synergy.enabled = true;
        cfg.synergy.reward_mode = "HIGHEST_ONLY";
        cfg.synergy.milestones.put("10", bonus(0.0, 5.0, 0.0, 0.0));
        cfg.synergy.milestones.put("25", bonus(5.0, 10.0, 0.0, 0.0));
        cfg.synergy.milestones.put("50", bonus(10.0, 15.0, 5.0, 0.0));
        cfg.synergy.milestones.put("100", bonus(15.0, 25.0, 10.0, 0.0));

        addDefaultJobAttributes(cfg);
        addDefaultLevelRewards(cfg);
        return cfg;
    }

    private void addDefaultJobAttributes(ProgressionConfig cfg) {
        cfg.job_attributes.put("miner", attribute("Mining Fortune", "MINING_FORTUNE", 2.0, 0.5, 75.0, false));
        cfg.job_attributes.put("farmer", attribute("Harvest Fortune", "HARVEST_FORTUNE", 2.0, 0.5, 75.0, false));
        cfg.job_attributes.put("hunter", attribute("PvE Damage", "PVE_DAMAGE", 2.0, 0.15, 50.0, false));
        cfg.job_attributes.put("fisherman", attribute("Fishing Luck", "FISHING_LUCK", 2.0, 0.35, 75.0, false));
        cfg.job_attributes.put("woodcutter", attribute("Wood Fortune", "WOOD_FORTUNE", 2.0, 0.5, 75.0, false));
        cfg.job_attributes.put("digger", attribute("Digging Fortune", "DIGGING_FORTUNE", 2.0, 0.5, 75.0, false));
        cfg.job_attributes.put("builder", attribute("Builder Precision", "BUILDING_UTILITY", 2.0, 0.25, 50.0, false));
        cfg.job_attributes.put("crafter", attribute("Crafting Mastery", "CRAFTING_MASTERY", 2.0, 0.25, 50.0, false));
        cfg.job_attributes.put("trader", attribute("Trade Influence", "TRADE_INFLUENCE", 2.0, 0.25, 50.0, false));
        cfg.job_attributes.put("somer", attribute("Idle Luck", "IDLE_LUCK", 2.0, 0.25, 50.0, false));
        cfg.job_attributes.put("fierar", attribute("Anvil Mastery", "ANVIL_MASTERY", 2.0, 0.25, 50.0, false));
    }

    private void addDefaultLevelRewards(ProgressionConfig cfg) {
        LevelRewardRule every5 = new LevelRewardRule();
        every5.interval = 5;
        every5.actions.add(action("MONEY", "100 + level * 20", null, null, null, 0.0, 0, null, 0.0));
        cfg.level_rewards.put("every_5", every5);

        LevelRewardRule every10 = new LevelRewardRule();
        every10.interval = 10;
        every10.actions.add(action("TEMP_XP_BOOST", null, null, null, null, 1.25, 900, null, 0.0));
        cfg.level_rewards.put("every_10", every10);

        LevelRewardRule every25 = new LevelRewardRule();
        every25.interval = 25;
        every25.actions.add(action("COMMAND", null, "give {player} minecraft:diamond 1", null, null, 0.0, 0, null, 0.0));
        cfg.level_rewards.put("every_25", every25);

        LevelRewardRule every50 = new LevelRewardRule();
        every50.interval = 50;
        every50.actions.add(action("PERMANENT_JOB_BONUS", null, null, null, null, 0.0, 0, "ATTRIBUTE_EFFECTIVENESS", 1.0));
        cfg.level_rewards.put("every_50", every50);

        LevelRewardRule every100 = new LevelRewardRule();
        every100.interval = 100;
        every100.actions.add(action("COMMAND", null, "crate give {player} job_mastery 1", null, null, 0.0, 0, null, 0.0));
        cfg.level_rewards.put("every_100", every100);
    }

    private JobBonus bonus(double money, double xp, double attribute, double speed) {
        JobBonus b = new JobBonus();
        b.global_money_percent = money;
        b.global_xp_percent = xp;
        b.global_job_attribute_percent = attribute;
        b.movement_speed_percent = speed;
        return b;
    }

    private JobAttributeConfig attribute(String name, String type, double powerMultiplier, double conversion, double maxEffect, boolean pvp) {
        JobAttributeConfig cfg = new JobAttributeConfig();
        cfg.name = name;
        cfg.type = type;
        cfg.power_multiplier = powerMultiplier;
        cfg.conversion = conversion;
        cfg.max_effect_percent = maxEffect;
        cfg.affects_pvp = pvp;
        return cfg;
    }

    private RewardAction action(String type, String formula, String command, String message, String sound, double multiplier, int duration, String bonusType, double percent) {
        RewardAction action = new RewardAction();
        action.type = type;
        action.amount_formula = formula;
        action.command = command;
        action.message = message;
        action.sound = sound;
        action.multiplier = multiplier;
        action.duration_seconds = duration;
        action.bonus_type = bonusType;
        action.percent = percent;
        return action;
    }

    private void validateConfig(ProgressionConfig cfg) {
        if (cfg.progression == null) cfg.progression = new ProgressionSettings();
        if (cfg.progression.required_xp == null) cfg.progression.required_xp = new RequiredXpSettings();
        if (cfg.progression.xp_reward == null) cfg.progression.xp_reward = new SqrtScalingSettings();
        if (cfg.progression.money_reward == null) cfg.progression.money_reward = new SqrtScalingSettings();
        if (cfg.progression.job_attribute == null) cfg.progression.job_attribute = new AttributeScalingSettings();
        if (cfg.progression.base_xp_multipliers == null) cfg.progression.base_xp_multipliers = new BaseXpMultipliers();
        if (cfg.total_job_level == null) cfg.total_job_level = new TotalLevelSettings();
        if (cfg.synergy == null) cfg.synergy = new SynergySettings();
        if (cfg.job_attributes == null) cfg.job_attributes = new LinkedHashMap<>();
        if (cfg.level_rewards == null) cfg.level_rewards = new LinkedHashMap<>();

        if (!isPositiveFinite(cfg.progression.required_xp.base)) {
            warn("progression.required_xp.base must be > 0. Falling back to 100.0");
            cfg.progression.required_xp.base = 100.0;
        }
        if (!isPositiveFinite(cfg.progression.required_xp.exponent)) {
            warn("progression.required_xp.exponent must be > 0. Falling back to 1.55");
            cfg.progression.required_xp.exponent = 1.55;
        }
        if (!isNonNegativeFinite(cfg.progression.xp_reward.sqrt_multiplier)) cfg.progression.xp_reward.sqrt_multiplier = 0.5;
        if (!isNonNegativeFinite(cfg.progression.money_reward.sqrt_multiplier)) cfg.progression.money_reward.sqrt_multiplier = 0.2;
        if (!isNonNegativeFinite(cfg.progression.job_attribute.base_multiplier)) cfg.progression.job_attribute.base_multiplier = 2.0;
        if (!isPositiveFinite(cfg.progression.base_xp_multipliers.default_multiplier)) cfg.progression.base_xp_multipliers.default_multiplier = 2.0;

        cfg.synergy.reward_mode = normalizeRewardMode(cfg.synergy.reward_mode);
        validateMilestones("total_job_level", cfg.total_job_level.milestones);
        validateMilestones("synergy", cfg.synergy.milestones);
        validateLevelRewards(cfg);
    }

    private void validateMilestones(String path, Map<String, JobBonus> milestones) {
        if (milestones == null) return;
        milestones.entrySet().removeIf(entry -> parsePositiveLong(entry.getKey()) <= 0);
        for (Map.Entry<String, JobBonus> entry : milestones.entrySet()) {
            if (entry.getValue() == null) entry.setValue(new JobBonus());
            sanitizeBonus(path + "." + entry.getKey(), entry.getValue());
        }
    }

    private void validateLevelRewards(ProgressionConfig cfg) {
        cfg.level_rewards.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue().interval <= 0);
        for (LevelRewardRule rule : cfg.level_rewards.values()) {
            if (rule.actions == null) rule.actions = new ArrayList<>();
            rule.actions.removeIf(action -> action == null || action.type == null || action.type.trim().isEmpty());
            for (RewardAction action : rule.actions) {
                action.type = action.type.toUpperCase(Locale.ROOT);
                if (action.multiplier < 0 || !Double.isFinite(action.multiplier)) action.multiplier = 0.0;
                if (action.duration_seconds < 0) action.duration_seconds = 0;
                if (action.percent < 0 || !Double.isFinite(action.percent)) action.percent = 0.0;
            }
        }
    }

    private void sanitizeBonus(String path, JobBonus bonus) {
        if (!isNonNegativeFinite(bonus.global_money_percent)) {
            warn(path + ".global_money_percent is invalid. Falling back to 0.");
            bonus.global_money_percent = 0.0;
        }
        if (!isNonNegativeFinite(bonus.global_xp_percent)) bonus.global_xp_percent = 0.0;
        if (!isNonNegativeFinite(bonus.global_job_attribute_percent)) bonus.global_job_attribute_percent = 0.0;
        if (!isNonNegativeFinite(bonus.movement_speed_percent)) bonus.movement_speed_percent = 0.0;
    }

    private String normalizeRewardMode(String mode) {
        if (mode == null) return "HIGHEST_ONLY";
        String normalized = mode.toUpperCase(Locale.ROOT);
        if (!normalized.equals("HIGHEST_ONLY") && !normalized.equals("CUMULATIVE")) {
            warn("synergy.reward_mode must be HIGHEST_ONLY or CUMULATIVE. Falling back to HIGHEST_ONLY.");
            return "HIGHEST_ONLY";
        }
        return normalized;
    }

    private boolean isPositiveFinite(double value) {
        return Double.isFinite(value) && value > 0.0;
    }

    private boolean isNonNegativeFinite(double value) {
        return Double.isFinite(value) && value >= 0.0;
    }

    private long parsePositiveLong(String value) {
        try {
            long parsed = Long.parseLong(value);
            return parsed > 0 ? parsed : -1;
        } catch (Exception ignored) {
            return -1;
        }
    }

    private void warn(String message) {
        System.out.println("[EvoJobs] Progression config warning: " + message);
    }

    public static class ProgressionConfig {
        public int dataVersion = 2;
        public ProgressionSettings progression = new ProgressionSettings();
        public TotalLevelSettings total_job_level = new TotalLevelSettings();
        public SynergySettings synergy = new SynergySettings();
        public Map<String, JobAttributeConfig> job_attributes = new LinkedHashMap<>();
        public Map<String, LevelRewardRule> level_rewards = new LinkedHashMap<>();
    }

    public static class ProgressionSettings {
        public RequiredXpSettings required_xp = new RequiredXpSettings();
        public SqrtScalingSettings xp_reward = new SqrtScalingSettings();
        public SqrtScalingSettings money_reward = new SqrtScalingSettings();
        public AttributeScalingSettings job_attribute = new AttributeScalingSettings();
        public BaseXpMultipliers base_xp_multipliers = new BaseXpMultipliers();
    }

    public static class RequiredXpSettings {
        public double base = 100.0;
        public double exponent = 1.55;
    }

    public static class SqrtScalingSettings {
        public double sqrt_multiplier = 0.0;
    }

    public static class AttributeScalingSettings {
        public double base_multiplier = 2.0;
    }

    public static class BaseXpMultipliers {
        public double default_multiplier = 2.0;
        public Map<String, Double> job_multipliers = new LinkedHashMap<>();
    }

    public static class TotalLevelSettings {
        public boolean enabled = true;
        public Map<String, JobBonus> milestones = new LinkedHashMap<>();
    }

    public static class SynergySettings {
        public boolean enabled = true;
        public String reward_mode = "HIGHEST_ONLY";
        public Map<String, JobBonus> milestones = new LinkedHashMap<>();
    }

    public static class JobBonus {
        public double global_money_percent = 0.0;
        public double global_xp_percent = 0.0;
        public double global_job_attribute_percent = 0.0;
        public double movement_speed_percent = 0.0;
    }

    public static class JobAttributeConfig {
        public String name = "Job Attribute";
        public String type = "GENERIC";
        public double power_multiplier = 2.0;
        public double conversion = 0.25;
        public double max_effect_percent = 50.0;
        public boolean affects_pvp = false;
    }

    public static class LevelRewardRule {
        public long interval = 0;
        public List<RewardAction> actions = new ArrayList<>();
    }

    public static class RewardAction {
        public String type = "MESSAGE";
        public String amount_formula;
        public String command;
        public String message;
        public String sound;
        public double multiplier = 0.0;
        public int duration_seconds = 0;
        public String bonus_type;
        public double percent = 0.0;
    }
}
