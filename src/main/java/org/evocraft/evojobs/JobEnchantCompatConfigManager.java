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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JobEnchantCompatConfigManager {
    private static JobEnchantCompatConfigManager INSTANCE;

    private final File configFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private Config config;

    public JobEnchantCompatConfigManager() {
        this.configFile = FMLPaths.CONFIGDIR.get().resolve("evojobs_enchant_compat.json").toFile();
        load();
    }

    public static void initialize() {
        if (INSTANCE == null) INSTANCE = new JobEnchantCompatConfigManager();
    }

    public static JobEnchantCompatConfigManager get() {
        if (INSTANCE == null) initialize();
        return INSTANCE;
    }

    public Config config() {
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
            config = gson.fromJson(reader, Config.class);
        } catch (Exception e) {
            System.out.println("[EvoJobs] Could not load evojobs_enchant_compat.json. Using safe defaults.");
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

    private Config createDefaultConfig() {
        Config cfg = new Config();
        cfg.harvester.enabled = true;
        cfg.harvester.enchantment_id = "majruszsenchantments:harvester";
        cfg.harvester.job_id = "farmer";
        cfg.harvester.action = "BREAK";
        cfg.harvester.radius_per_level = 1;
        cfg.harvester.radius_offset = -1;
        cfg.harvester.max_radius = 2;
        cfg.harvester.reward_delay_ticks = 1;
        cfg.harvester.cooldown_ticks = 3;
        cfg.harvester.max_crops_per_use = 25;
        addDefaultRewardRules(cfg);
        return cfg;
    }

    private void validateConfig(Config cfg) {
        if (cfg.harvester == null) cfg.harvester = new HarvesterCompat();
        if (cfg.harvester.enchantment_id == null || cfg.harvester.enchantment_id.isBlank()) {
            cfg.harvester.enchantment_id = "majruszsenchantments:harvester";
        }
        if (cfg.harvester.job_id == null || cfg.harvester.job_id.isBlank()) {
            cfg.harvester.job_id = "farmer";
        }
        if (cfg.harvester.action == null || cfg.harvester.action.isBlank()) {
            cfg.harvester.action = "BREAK";
        }
        if (cfg.harvester.radius_per_level < 0) cfg.harvester.radius_per_level = 1;
        if (cfg.harvester.max_radius < 0) cfg.harvester.max_radius = 0;
        if (cfg.harvester.reward_delay_ticks < 1) cfg.harvester.reward_delay_ticks = 1;
        if (cfg.harvester.cooldown_ticks < 0) cfg.harvester.cooldown_ticks = 0;
        if (cfg.harvester.max_crops_per_use < 1) cfg.harvester.max_crops_per_use = 25;

        if (cfg.enchantment_rewards == null) cfg.enchantment_rewards = new LinkedHashMap<>();
        addDefaultRewardRules(cfg);
        for (EnchantmentRewardRule rule : cfg.enchantment_rewards.values()) {
            validateRule(rule);
        }
    }

    public static class Config {
        public HarvesterCompat harvester = new HarvesterCompat();
        public Map<String, EnchantmentRewardRule> enchantment_rewards = new LinkedHashMap<>();
    }

    public static class HarvesterCompat {
        public boolean enabled = true;
        public String enchantment_id = "majruszsenchantments:harvester";
        public String job_id = "farmer";
        public String action = "BREAK";
        public int radius_per_level = 1;
        public int radius_offset = -1;
        public int max_radius = 2;
        public int reward_delay_ticks = 1;
        public int cooldown_ticks = 3;
        public int max_crops_per_use = 25;
    }

    public static class EnchantmentRewardRule {
        public boolean enabled = false;
        public String enchantment_id = "";
        public List<String> actions = new ArrayList<>();
        public List<String> jobs = new ArrayList<>();
        public boolean scan_main_hand = true;
        public boolean scan_offhand = true;
        public boolean scan_armor = false;
        public double xp_percent_per_level = 0.0;
        public double money_percent_per_level = 0.0;
        public double max_xp_percent = 100.0;
        public double max_money_percent = 100.0;
    }

    private void addDefaultRewardRules(Config cfg) {
        addRuleIfMissing(cfg, "absorber", rule(false, "majruszsenchantments:absorber", any(), any(), false, true, true, 0.0, 0.0, 0.0, 0.0));
        addRuleIfMissing(cfg, "death_wish", rule(true, "majruszsenchantments:death_wish", list("KILL"), list("hunter"), true, true, false, 2.0, 2.0, 20.0, 20.0));
        addRuleIfMissing(cfg, "dodge", rule(false, "majruszsenchantments:dodge", any(), any(), false, false, true, 0.0, 0.0, 0.0, 0.0));
        addRuleIfMissing(cfg, "enlightenment", rule(true, "majruszsenchantments:enlightenment", any(), any(), true, true, true, 3.0, 0.0, 36.0, 0.0));
        addRuleIfMissing(cfg, "fishing_fanatic", rule(true, "majruszsenchantments:fishing_fanatic", list("FISH"), list("fisherman"), true, true, false, 5.0, 5.0, 40.0, 40.0));
        addRuleIfMissing(cfg, "fuse_cutter", rule(false, "majruszsenchantments:fuse_cutter", any(), any(), true, true, false, 0.0, 0.0, 0.0, 0.0));
        addRuleIfMissing(cfg, "gold_fuelled", rule(true, "majruszsenchantments:gold_fuelled", list("BREAK"), list("miner", "digger", "woodcutter"), true, true, false, 1.0, 1.0, 10.0, 10.0));
        addRuleIfMissing(cfg, "harvester", rule(true, "majruszsenchantments:harvester", list("BREAK"), list("farmer"), true, true, false, 2.0, 2.0, 10.0, 10.0));
        addRuleIfMissing(cfg, "horse_frost_walker", rule(false, "majruszsenchantments:horse_frost_walker", any(), any(), false, false, false, 0.0, 0.0, 0.0, 0.0));
        addRuleIfMissing(cfg, "horse_protection", rule(false, "majruszsenchantments:horse_protection", any(), any(), false, false, false, 0.0, 0.0, 0.0, 0.0));
        addRuleIfMissing(cfg, "horse_swiftness", rule(false, "majruszsenchantments:horse_swiftness", any(), any(), false, false, false, 0.0, 0.0, 0.0, 0.0));
        addRuleIfMissing(cfg, "hunter", rule(true, "majruszsenchantments:hunter", list("KILL"), list("hunter"), true, true, false, 3.0, 3.0, 30.0, 30.0));
        addRuleIfMissing(cfg, "immortality", rule(false, "majruszsenchantments:immortality", any(), any(), false, true, true, 0.0, 0.0, 0.0, 0.0));
        addRuleIfMissing(cfg, "leech", rule(true, "majruszsenchantments:leech", list("KILL"), list("hunter"), true, true, false, 2.0, 2.0, 20.0, 20.0));
        addRuleIfMissing(cfg, "magic_protection", rule(false, "majruszsenchantments:magic_protection", any(), any(), false, false, true, 0.0, 0.0, 0.0, 0.0));
        addRuleIfMissing(cfg, "misanthropy", rule(true, "majruszsenchantments:misanthropy", list("KILL"), list("hunter"), true, true, false, 2.0, 2.0, 20.0, 20.0));
        addRuleIfMissing(cfg, "repulsion", rule(false, "majruszsenchantments:repulsion", any(), any(), false, true, false, 0.0, 0.0, 0.0, 0.0));
        addRuleIfMissing(cfg, "sixth_sense", rule(false, "majruszsenchantments:sixth_sense", any(), any(), true, true, false, 0.0, 0.0, 0.0, 0.0));
        addRuleIfMissing(cfg, "smelter", rule(true, "majruszsenchantments:smelter", list("BREAK"), list("miner", "digger"), true, true, false, 2.0, 2.0, 20.0, 20.0));
        addRuleIfMissing(cfg, "telekinesis", rule(true, "majruszsenchantments:telekinesis", list("BREAK", "FISH"), list("miner", "digger", "woodcutter", "fisherman"), true, true, false, 1.0, 1.0, 10.0, 10.0));

        addRuleIfMissing(cfg, "breaking_curse", rule(false, "majruszsenchantments:breaking_curse", any(), any(), true, true, true, 0.0, 0.0, 0.0, 0.0));
        addRuleIfMissing(cfg, "corrosion_curse", rule(false, "majruszsenchantments:corrosion_curse", any(), any(), false, false, true, 0.0, 0.0, 0.0, 0.0));
        addRuleIfMissing(cfg, "fatigue_curse", rule(false, "majruszsenchantments:fatigue_curse", any(), any(), true, true, true, 0.0, 0.0, 0.0, 0.0));
        addRuleIfMissing(cfg, "incompatibility_curse", rule(false, "majruszsenchantments:incompatibility_curse", any(), any(), true, true, true, 0.0, 0.0, 0.0, 0.0));
        addRuleIfMissing(cfg, "slippery_curse", rule(false, "majruszsenchantments:slippery_curse", any(), any(), true, true, true, 0.0, 0.0, 0.0, 0.0));
        addRuleIfMissing(cfg, "vampirism_curse", rule(false, "majruszsenchantments:vampirism_curse", any(), any(), false, false, true, 0.0, 0.0, 0.0, 0.0));
    }

    private void addRuleIfMissing(Config cfg, String key, EnchantmentRewardRule rule) {
        cfg.enchantment_rewards.putIfAbsent(key, rule);
    }

    private EnchantmentRewardRule rule(boolean enabled, String enchantmentId, List<String> actions, List<String> jobs,
                                       boolean scanMainHand, boolean scanOffhand, boolean scanArmor,
                                       double xpPercent, double moneyPercent, double maxXpPercent, double maxMoneyPercent) {
        EnchantmentRewardRule rule = new EnchantmentRewardRule();
        rule.enabled = enabled;
        rule.enchantment_id = enchantmentId;
        rule.actions = actions;
        rule.jobs = jobs;
        rule.scan_main_hand = scanMainHand;
        rule.scan_offhand = scanOffhand;
        rule.scan_armor = scanArmor;
        rule.xp_percent_per_level = xpPercent;
        rule.money_percent_per_level = moneyPercent;
        rule.max_xp_percent = maxXpPercent;
        rule.max_money_percent = maxMoneyPercent;
        return rule;
    }

    private List<String> list(String... values) {
        return new ArrayList<>(Arrays.asList(values));
    }

    private List<String> any() {
        return list("*");
    }

    private void validateRule(EnchantmentRewardRule rule) {
        if (rule == null) return;
        if (rule.enchantment_id == null) rule.enchantment_id = "";
        if (rule.actions == null || rule.actions.isEmpty()) rule.actions = any();
        if (rule.jobs == null || rule.jobs.isEmpty()) rule.jobs = any();
        if (!Double.isFinite(rule.xp_percent_per_level)) rule.xp_percent_per_level = 0.0;
        if (!Double.isFinite(rule.money_percent_per_level)) rule.money_percent_per_level = 0.0;
        if (!Double.isFinite(rule.max_xp_percent) || rule.max_xp_percent < 0.0) rule.max_xp_percent = 0.0;
        if (!Double.isFinite(rule.max_money_percent) || rule.max_money_percent < 0.0) rule.max_money_percent = 0.0;
    }
}
