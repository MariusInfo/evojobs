package org.evocraft.evojobs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class JobConfigManager {
    private static JobConfigManager INSTANCE;

    private final Map<String, JobDefinition> loadedJobs = new HashMap<>();
    private final File jobsFolder;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public JobConfigManager() {
        this.jobsFolder = FMLPaths.CONFIGDIR.get().resolve("evojobs").toFile();
        if (!jobsFolder.exists()) {
            jobsFolder.mkdirs();
        }
        load();
    }

    public static void initialize() {
        if (INSTANCE == null) INSTANCE = new JobConfigManager();
    }

    public static JobConfigManager get() { return INSTANCE; }

    public Collection<JobDefinition> getAllJobs() { return loadedJobs.values(); }
    public JobDefinition getJob(String id) { return loadedJobs.get(id); }
    public Map<String, JobDefinition> getJobs() { return loadedJobs; }

    public void load() {
        loadedJobs.clear();
        File[] files = jobsFolder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                try (Reader reader = new FileReader(file)) {
                    JobDefinition job = gson.fromJson(reader, JobDefinition.class);
                    job.id = file.getName().replace(".json", "");
                    loadedJobs.put(job.id, job);
                } catch (Exception e) { e.printStackTrace(); }
            }
        }
        checkAndCreateDefaults();
        migrateLegacyDefaultTexts();
    }

    private void checkAndCreateDefaults() {
        if (!loadedJobs.containsKey("miner")) {
            JobDefinition miner = new JobDefinition("miner", "Miner", "minecraft:iron_pickaxe", "Break ores.");
            miner.addReward("BREAK", "*", 0.2);
            miner.addReward("BREAK", "minecraft:stone", 0.3);
            miner.addReward("BREAK", "minecraft:cobblestone", 0.2);
            miner.addReward("BREAK", "minecraft:deepslate", 0.4);
            miner.addReward("BREAK", "minecraft:cobbled_deepslate", 0.3);
            miner.addReward("BREAK", "minecraft:tuff", 0.4);
            miner.addReward("BREAK", "minecraft:diamond_ore", 10.0);
            miner.addReward("BREAK", "minecraft:emerald_ore", 15.0);
            miner.addReward("BREAK", "minecraft:deepslate_diamond_ore", 12.5);
            saveJob(miner); loadedJobs.put(miner.id, miner);
        }
        if (!loadedJobs.containsKey("woodcutter")) {
            JobDefinition woodcutter = new JobDefinition("woodcutter", "Lumberjack", "minecraft:iron_axe", "Cut down trees.");
            woodcutter.addReward("BREAK", "*", 0.3);
            saveJob(woodcutter); loadedJobs.put(woodcutter.id, woodcutter);
        }
        if (!loadedJobs.containsKey("digger")) {
            JobDefinition digger = new JobDefinition("digger", "Digger", "minecraft:iron_shovel", "Dig dirt, sand, and gravel.");
            digger.addReward("BREAK", "*", 0.1);
            digger.addReward("BREAK", "minecraft:dirt", 0.1);
            digger.addReward("BREAK", "minecraft:grass_block", 0.2);
            digger.addReward("BREAK", "minecraft:sand", 0.2);
            digger.addReward("BREAK", "minecraft:red_sand", 0.2);
            digger.addReward("BREAK", "minecraft:gravel", 0.2);
            digger.addReward("BREAK", "minecraft:clay", 1.0);
            saveJob(digger); loadedJobs.put(digger.id, digger);
        }
        if (!loadedJobs.containsKey("hunter")) {
            JobDefinition hunter = new JobDefinition("hunter", "Hunter", "minecraft:iron_sword", "Kill monsters.");
            hunter.addReward("KILL", "*", 0.4);
            hunter.addReward("KILL", "minecraft:ender_dragon", 1000.0);
            hunter.addReward("KILL", "minecraft:wither", 500.0);
            saveJob(hunter); loadedJobs.put(hunter.id, hunter);
        }
        if (!loadedJobs.containsKey("farmer")) {
            JobDefinition farmer = new JobDefinition("farmer", "Farmer", "minecraft:wheat", "Farm crops and animals.");
            farmer.addReward("BREAK", "*", 0.2);
            farmer.addReward("BREED", "*", 2.0);
            farmer.addReward("TAME", "*", 3.0);
            saveJob(farmer); loadedJobs.put(farmer.id, farmer);
        }
        if (!loadedJobs.containsKey("fisherman")) {
            JobDefinition fisherman = new JobDefinition("fisherman", "Fisherman", "minecraft:fishing_rod", "Catch fish.");
            fisherman.addReward("FISH", "*", 5.0);
            fisherman.addReward("FISH", "minecraft:pufferfish", 15.0);
            saveJob(fisherman); loadedJobs.put(fisherman.id, fisherman);
        }
        if (!loadedJobs.containsKey("builder")) {
            JobDefinition builder = new JobDefinition("builder", "Builder", "minecraft:bricks", "Place blocks.");
            builder.addReward("PLACE", "*", 0.1);
            saveJob(builder); loadedJobs.put(builder.id, builder);
        }
        if (!loadedJobs.containsKey("crafter")) {
            JobDefinition crafter = new JobDefinition("crafter", "Crafter", "minecraft:crafting_table", "Craft items.");
            crafter.addReward("CRAFT", "*", 0.2);
            crafter.addReward("CRAFT", "minecraft:beacon", 50.0);
            saveJob(crafter); loadedJobs.put(crafter.id, crafter);
        }
        if (!loadedJobs.containsKey("trader")) {
            JobDefinition trader = new JobDefinition("trader", "Trader", "minecraft:emerald", "Trade with villagers.");
            trader.addReward("TRADE", "*", 0.5);
            saveJob(trader); loadedJobs.put(trader.id, trader);
        }
        if (!loadedJobs.containsKey("somer")) {
            JobDefinition somer = new JobDefinition("somer", "Unemployed", "minecraft:painting", "Earn a little by staying idle.");
            saveJob(somer); loadedJobs.put(somer.id, somer);
        }

        // ===============================================
        // New job: Blacksmith.
        // ===============================================
        if (!loadedJobs.containsKey("fierar")) {
            JobDefinition fierar = new JobDefinition("fierar", "Blacksmith", "minecraft:anvil", "Repair and rename items.");
            fierar.addReward("USE_ANVIL", "*", 15.0);
            saveJob(fierar); loadedJobs.put(fierar.id, fierar);
        }
    }

    private void saveJob(JobDefinition job) {
        try (Writer writer = new FileWriter(new File(jobsFolder, job.id + ".json"))) {
            gson.toJson(job, writer);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void migrateLegacyDefaultTexts() {
        migrateLegacyDefaultText("miner", "Miner", "Break ores.",
                new String[] {},
                new String[] {"Sparge minereuri."});
        migrateLegacyDefaultText("woodcutter", "Lumberjack", "Cut down trees.",
                new String[] {"Padurar", "Pădurar"},
                new String[] {"Taie copaci."});
        migrateLegacyDefaultText("digger", "Digger", "Dig dirt, sand, and gravel.",
                new String[] {"Sapator", "Săpător"},
                new String[] {"Sapa pamant, nisip si pietris.", "Sapă pământ, nisip și pietriș."});
        migrateLegacyDefaultText("hunter", "Hunter", "Kill monsters.",
                new String[] {"Vanator", "Vânător"},
                new String[] {"Omoara monstrii.", "Omoară monștri."});
        migrateLegacyDefaultText("farmer", "Farmer", "Farm crops and animals.",
                new String[] {"Fermier"},
                new String[] {"Agricultura.", "Agricultură."});
        migrateLegacyDefaultText("fisherman", "Fisherman", "Catch fish.",
                new String[] {"Pescar"},
                new String[] {"Prinde peste.", "Prinde pește."});
        migrateLegacyDefaultText("builder", "Builder", "Place blocks.",
                new String[] {"Constructor"},
                new String[] {"Plaseaza blocuri.", "Plasează blocuri."});
        migrateLegacyDefaultText("crafter", "Crafter", "Craft items.",
                new String[] {"Mester", "Meșter"},
                new String[] {"Crafteaza iteme.", "Craftează iteme."});
        migrateLegacyDefaultText("trader", "Trader", "Trade with villagers.",
                new String[] {"Negustor"},
                new String[] {"Fa comert cu satenii.", "Fă comerț cu sătenii."});
        migrateLegacyDefaultText("somer", "Unemployed", "Earn a little by staying idle.",
                new String[] {"Somer", "Șomer"},
                new String[] {"Castigi bani stand degeaba.", "Câștigi bani stând degeaba."});
        migrateLegacyDefaultText("fierar", "Blacksmith", "Repair and rename items.",
                new String[] {"Fierar"},
                new String[] {"Repara si redenumeste iteme.", "Repară și redenumește iteme."});
    }

    private void migrateLegacyDefaultText(String id, String displayName, String description, String[] legacyNames, String[] legacyDescriptions) {
        JobDefinition job = loadedJobs.get(id);
        if (job == null) return;

        boolean changed = false;
        if (matchesLegacyDefault(job.displayName, legacyNames) && !displayName.equals(job.displayName)) {
            job.displayName = displayName;
            changed = true;
        }
        if (matchesLegacyDefault(job.description, legacyDescriptions) && !description.equals(job.description)) {
            job.description = description;
            changed = true;
        }

        if (changed) {
            saveJob(job);
        }
    }

    private boolean matchesLegacyDefault(String value, String[] legacyValues) {
        if (value == null || value.isBlank()) return true;
        for (String legacyValue : legacyValues) {
            if (value.equals(legacyValue)) return true;
        }
        return false;
    }
}
