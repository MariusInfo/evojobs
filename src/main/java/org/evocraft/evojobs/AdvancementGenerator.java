package org.evocraft.evojobs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class AdvancementGenerator {

    public static void main(String[] args) {
        Path basePath = Path.of("src/main/resources/data/evojobs/advancements/");

        try {
            Files.createDirectories(basePath);
            write(basePath.resolve("root.json"), rootJson());
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        Map<String, String> jobIcons = getJobIcons();
        Map<String, String> jobDisplayNames = getJobDisplayNames();
        Map<String, Map<Integer, String>> allAchievements = getAchievementsMap();

        for (Map.Entry<String, Map<Integer, String>> jobEntry : allAchievements.entrySet()) {
            String job = jobEntry.getKey();
            String icon = jobIcons.getOrDefault(job, "minecraft:paper");
            String displayName = jobDisplayNames.getOrDefault(job, capitalize(job));
            Path jobDir = basePath.resolve(job);

            try {
                Files.createDirectories(jobDir);
                String jobStartId = "evojobs:" + job + "/start";
                write(jobDir.resolve("start.json"), startJson(icon, displayName));

                int[] levels = {5, 10, 20, 25, 30, 40, 50, 60, 70, 75, 80, 90, 100};
                String currentParent = jobStartId;

                for (int level : levels) {
                    String title = jobEntry.getValue().get(level);
                    if (title == null) continue;

                    String frame = (level == 100) ? "challenge" : (level >= 50 ? "goal" : "task");
                    write(jobDir.resolve("level_" + level + ".json"), levelJson(currentParent, icon, title, displayName, level, frame));
                    currentParent = "evojobs:" + job + "/level_" + level;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Done! Advancement files were generated in English.");
    }

    private static void write(Path path, String content) throws IOException {
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    private static String rootJson() {
        return "{\n" +
                "  \"display\": {\n" +
                "    \"icon\": {\n" +
                "      \"item\": \"minecraft:experience_bottle\"\n" +
                "    },\n" +
                "    \"title\": \"\\u00a76\\u00a7lEvoJobs\",\n" +
                "    \"description\": \"All jobs in one place!\",\n" +
                "    \"background\": \"minecraft:textures/gui/advancements/backgrounds/stone.png\",\n" +
                "    \"show_toast\": false,\n" +
                "    \"announce_to_chat\": false,\n" +
                "    \"hidden\": false\n" +
                "  },\n" +
                "  \"criteria\": {\n" +
                "    \"auto\": {\n" +
                "      \"trigger\": \"minecraft:tick\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
    }

    private static String startJson(String icon, String displayName) {
        return "{\n" +
                "  \"parent\": \"evojobs:root\",\n" +
                "  \"display\": {\n" +
                "    \"icon\": {\n" +
                "      \"item\": \"" + icon + "\"\n" +
                "    },\n" +
                "    \"title\": \"\\u00a7eCareer: " + displayName + "\",\n" +
                "    \"description\": \"Start your adventure as " + displayName + "!\",\n" +
                "    \"frame\": \"task\",\n" +
                "    \"show_toast\": false,\n" +
                "    \"announce_to_chat\": false,\n" +
                "    \"hidden\": false\n" +
                "  },\n" +
                "  \"criteria\": {\n" +
                "    \"auto\": {\n" +
                "      \"trigger\": \"minecraft:tick\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
    }

    private static String levelJson(String parent, String icon, String title, String displayName, int level, String frame) {
        return "{\n" +
                "  \"parent\": \"" + parent + "\",\n" +
                "  \"display\": {\n" +
                "    \"icon\": {\n" +
                "      \"item\": \"" + icon + "\"\n" +
                "    },\n" +
                "    \"title\": \"" + title + "\",\n" +
                "    \"description\": \"" + displayName + " Job - Level " + level + "\",\n" +
                "    \"frame\": \"" + frame + "\",\n" +
                "    \"show_toast\": true,\n" +
                "    \"announce_to_chat\": true,\n" +
                "    \"hidden\": false\n" +
                "  },\n" +
                "  \"criteria\": {\n" +
                "    \"trigger\": {\n" +
                "      \"trigger\": \"minecraft:impossible\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
    }

    private static Map<String, String> getJobIcons() {
        Map<String, String> jobIcons = new HashMap<>();
        jobIcons.put("miner", "minecraft:iron_pickaxe");
        jobIcons.put("woodcutter", "minecraft:iron_axe");
        jobIcons.put("digger", "minecraft:iron_shovel");
        jobIcons.put("hunter", "minecraft:iron_sword");
        jobIcons.put("farmer", "minecraft:wheat");
        jobIcons.put("fisherman", "minecraft:fishing_rod");
        jobIcons.put("builder", "minecraft:bricks");
        jobIcons.put("crafter", "minecraft:crafting_table");
        jobIcons.put("trader", "minecraft:emerald");
        jobIcons.put("somer", "minecraft:painting");
        jobIcons.put("fierar", "minecraft:anvil");
        return jobIcons;
    }

    private static Map<String, String> getJobDisplayNames() {
        Map<String, String> displayNames = new HashMap<>();
        displayNames.put("miner", "Miner");
        displayNames.put("woodcutter", "Lumberjack");
        displayNames.put("digger", "Digger");
        displayNames.put("hunter", "Hunter");
        displayNames.put("farmer", "Farmer");
        displayNames.put("fisherman", "Fisherman");
        displayNames.put("builder", "Builder");
        displayNames.put("crafter", "Crafter");
        displayNames.put("trader", "Trader");
        displayNames.put("somer", "Unemployed");
        displayNames.put("fierar", "Blacksmith");
        return displayNames;
    }

    private static Map<String, Map<Integer, String>> getAchievementsMap() {
        Map<String, Map<Integer, String>> map = new LinkedHashMap<>();

        Map<Integer, String> miner = new LinkedHashMap<>();
        miner.put(5, "Stone Seeker"); miner.put(10, "Tunnel Driller"); miner.put(20, "Rock Breaker"); miner.put(25, "Stonebreaker"); miner.put(30, "Tunnel Digger"); miner.put(40, "Experienced Miner"); miner.put(50, "Ore Master"); miner.put(60, "Treasure Seeker"); miner.put(70, "Obsidian Crusher"); miner.put(75, "Heart of Stone"); miner.put(80, "Cave Destroyer"); miner.put(90, "Depths Legend"); miner.put(100, "Underground God");
        map.put("miner", miner);

        Map<Integer, String> wood = new LinkedHashMap<>();
        wood.put(5, "Branch Cutter"); wood.put(10, "Wood Gatherer"); wood.put(20, "Tree Chopper"); wood.put(25, "Lumberjack"); wood.put(30, "Trunk Crusher"); wood.put(40, "Forester"); wood.put(50, "Axe Druid"); wood.put(60, "Tree Butcher"); wood.put(70, "Forest Master"); wood.put(75, "Ecosystem Breaker"); wood.put(80, "Woodland Legend"); wood.put(90, "Ent Terror"); wood.put(100, "King of Nature");
        map.put("woodcutter", wood);

        Map<Integer, String> digger = new LinkedHashMap<>();
        digger.put(5, "Dirt Scratcher"); digger.put(10, "Curious Mole"); digger.put(20, "Trench Digger"); digger.put(25, "Human Excavator"); digger.put(30, "Sand Mover"); digger.put(40, "Professional Digger"); digger.put(50, "Foundation Breaker"); digger.put(60, "Crater Maker"); digger.put(70, "Shovel Master"); digger.put(75, "Earth Eater"); digger.put(80, "Terrain Sculptor"); digger.put(90, "Seismologist"); digger.put(100, "God of the Earth");
        map.put("digger", digger);

        Map<Integer, String> hunter = new LinkedHashMap<>();
        hunter.put(5, "Hunter Apprentice"); hunter.put(10, "Zombie Hunter"); hunter.put(20, "Skeleton Slayer"); hunter.put(25, "Terror of the Night"); hunter.put(30, "Monster Hunter"); hunter.put(40, "Elite Hunter"); hunter.put(50, "Nether Terror"); hunter.put(60, "Wither Hunter"); hunter.put(70, "Sword Master"); hunter.put(75, "Shadow Eradicator"); hunter.put(80, "Dragon Hunter"); hunter.put(90, "Relentless Assassin"); hunter.put(100, "God of War");
        map.put("hunter", hunter);

        Map<Integer, String> farmer = new LinkedHashMap<>();
        farmer.put(5, "Mud Digger"); farmer.put(10, "Seed Planter"); farmer.put(20, "Animal Caretaker"); farmer.put(25, "Hardworking Harvester"); farmer.put(30, "Skilled Farmer"); farmer.put(40, "Elite Agriculturist"); farmer.put(50, "Lord of Harvests"); farmer.put(60, "Beast Tamer"); farmer.put(70, "Plantation Master"); farmer.put(75, "Fertilizer Master"); farmer.put(80, "Cooperative Member"); farmer.put(90, "King of Grain"); farmer.put(100, "God of Agriculture");
        map.put("farmer", farmer);

        Map<Integer, String> fisher = new LinkedHashMap<>();
        fisher.put(5, "Pond Fisher"); fisher.put(10, "Salmon Catcher"); fisher.put(20, "River Fisher"); fisher.put(25, "Terror of Fish"); fisher.put(30, "Sea Fisher"); fisher.put(40, "Navigator"); fisher.put(50, "Rod Master"); fisher.put(60, "Shark Hunter"); fisher.put(70, "King of Waters"); fisher.put(75, "Ocean Terror"); fisher.put(80, "Master of Waves"); fisher.put(90, "Ship Captain"); fisher.put(100, "God of the Seas");
        map.put("fisherman", fisher);

        Map<Integer, String> builder = new LinkedHashMap<>();
        builder.put(5, "Beginner Bricklayer"); builder.put(10, "Block Placer"); builder.put(20, "Skilled Mason"); builder.put(25, "House Builder"); builder.put(30, "Architect Apprentice"); builder.put(40, "Construction Engineer"); builder.put(50, "Chief Architect"); builder.put(60, "Castle Builder"); builder.put(70, "Master Builder"); builder.put(75, "Base Forger"); builder.put(80, "City Creator"); builder.put(90, "Supreme Designer"); builder.put(100, "World Forger");
        map.put("builder", builder);

        Map<Integer, String> crafter = new LinkedHashMap<>();
        crafter.put(5, "Wood Binder"); crafter.put(10, "Workbench Apprentice"); crafter.put(20, "Tool Creator"); crafter.put(25, "Skilled Artisan"); crafter.put(30, "Iron Craftsman"); crafter.put(40, "Diamond Craftsman"); crafter.put(50, "Recipe Master"); crafter.put(60, "Armor Creator"); crafter.put(70, "Redstone Mechanic"); crafter.put(75, "Magic Forger"); crafter.put(80, "Inventor"); crafter.put(90, "Technical Genius"); crafter.put(100, "Supreme Creator");
        map.put("crafter", crafter);

        Map<Integer, String> trader = new LinkedHashMap<>();
        trader.put(5, "Weak Negotiator"); trader.put(10, "Beginner Hustler"); trader.put(20, "Traveling Seller"); trader.put(25, "Local Hustler"); trader.put(30, "Merchant"); trader.put(40, "Respected Trader"); trader.put(50, "Wall Street Wolf"); trader.put(60, "Entrepreneur"); trader.put(70, "Business Master"); trader.put(75, "Millionaire"); trader.put(80, "Magnate"); trader.put(90, "King of Economy"); trader.put(100, "Supreme Monopolist");
        map.put("trader", trader);

        Map<Integer, String> somer = new LinkedHashMap<>();
        somer.put(5, "Beginner Slacker"); somer.put(10, "Time Waster"); somer.put(20, "Sleepyhead"); somer.put(25, "Champion of Doing Nothing"); somer.put(30, "Welfare Expert"); somer.put(40, "King of Beds"); somer.put(50, "Laziness Expert"); somer.put(60, "Sleep Master"); somer.put(70, "Special Pensioner"); somer.put(75, "Legend of Inactivity"); somer.put(80, "Living Statue"); somer.put(90, "Server Ghost"); somer.put(100, "God of Inactivity");
        map.put("somer", somer);

        Map<Integer, String> fierar = new LinkedHashMap<>();
        fierar.put(5, "Iron Hammerer"); fierar.put(10, "Beginner Forger"); fierar.put(20, "Repairman"); fierar.put(25, "Skilled Forger"); fierar.put(30, "Metal Smelter"); fierar.put(40, "Anvil Apprentice"); fierar.put(50, "Anvil Master"); fierar.put(60, "Sword Forger"); fierar.put(70, "Armor Forger"); fierar.put(75, "Alloy Expert"); fierar.put(80, "Metal Wizard"); fierar.put(90, "Forge Legend"); fierar.put(100, "God of Metal");
        map.put("fierar", fierar);

        return map;
    }

    private static String capitalize(String value) {
        if (value == null || value.isEmpty()) return "";
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }
}
