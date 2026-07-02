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
    }

    private void checkAndCreateDefaults() {
        if (!loadedJobs.containsKey("miner")) {
            JobDefinition miner = new JobDefinition("miner", "Miner", "minecraft:iron_pickaxe", "Sparge minereuri.");
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
            JobDefinition woodcutter = new JobDefinition("woodcutter", "Padurar", "minecraft:iron_axe", "Taie copaci.");
            woodcutter.addReward("BREAK", "*", 0.3);
            saveJob(woodcutter); loadedJobs.put(woodcutter.id, woodcutter);
        }
        if (!loadedJobs.containsKey("digger")) {
            JobDefinition digger = new JobDefinition("digger", "Sapator", "minecraft:iron_shovel", "Sapa pamant, nisip si pietris.");
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
            JobDefinition hunter = new JobDefinition("hunter", "Vanator", "minecraft:iron_sword", "Omoara monstrii.");
            hunter.addReward("KILL", "*", 0.4);
            hunter.addReward("KILL", "minecraft:ender_dragon", 1000.0);
            hunter.addReward("KILL", "minecraft:wither", 500.0);
            saveJob(hunter); loadedJobs.put(hunter.id, hunter);
        }
        if (!loadedJobs.containsKey("farmer")) {
            JobDefinition farmer = new JobDefinition("farmer", "Fermier", "minecraft:wheat", "Agricultura.");
            farmer.addReward("BREAK", "*", 0.2);
            farmer.addReward("BREED", "*", 2.0);
            farmer.addReward("TAME", "*", 3.0);
            saveJob(farmer); loadedJobs.put(farmer.id, farmer);
        }
        if (!loadedJobs.containsKey("fisherman")) {
            JobDefinition fisherman = new JobDefinition("fisherman", "Pescar", "minecraft:fishing_rod", "Prinde peste.");
            fisherman.addReward("FISH", "*", 5.0);
            fisherman.addReward("FISH", "minecraft:pufferfish", 15.0);
            saveJob(fisherman); loadedJobs.put(fisherman.id, fisherman);
        }
        if (!loadedJobs.containsKey("builder")) {
            JobDefinition builder = new JobDefinition("builder", "Constructor", "minecraft:bricks", "Plaseaza blocuri.");
            builder.addReward("PLACE", "*", 0.1);
            saveJob(builder); loadedJobs.put(builder.id, builder);
        }
        if (!loadedJobs.containsKey("crafter")) {
            JobDefinition crafter = new JobDefinition("crafter", "Mester", "minecraft:crafting_table", "Crafteaza iteme.");
            crafter.addReward("CRAFT", "*", 0.2);
            crafter.addReward("CRAFT", "minecraft:beacon", 50.0);
            saveJob(crafter); loadedJobs.put(crafter.id, crafter);
        }
        if (!loadedJobs.containsKey("trader")) {
            JobDefinition trader = new JobDefinition("trader", "Negustor", "minecraft:emerald", "Fa comert cu satenii.");
            trader.addReward("TRADE", "*", 0.5);
            saveJob(trader); loadedJobs.put(trader.id, trader);
        }
        if (!loadedJobs.containsKey("somer")) {
            JobDefinition somer = new JobDefinition("somer", "Somer", "minecraft:painting", "Castigi bani stand degeaba.");
            saveJob(somer); loadedJobs.put(somer.id, somer);
        }

        // ===============================================
        // JOBUL NOU: FIERAR
        // ===============================================
        if (!loadedJobs.containsKey("fierar")) {
            JobDefinition fierar = new JobDefinition("fierar", "Fierar", "minecraft:anvil", "Repara si redenumeste iteme.");
            fierar.addReward("USE_ANVIL", "*", 15.0);
            saveJob(fierar); loadedJobs.put(fierar.id, fierar);
        }
    }

    private void saveJob(JobDefinition job) {
        try (Writer writer = new FileWriter(new File(jobsFolder, job.id + ".json"))) {
            gson.toJson(job, writer);
        } catch (IOException e) { e.printStackTrace(); }
    }
}