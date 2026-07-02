package org.evocraft.evojobs.quest;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.evocraft.evocore.data.EconomyManager;
import org.evocraft.evocore.database.DatabaseManager;
import org.evocraft.evojobs.Evojobs;
import org.evocraft.evojobs.JobManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.*;

@Mod.EventBusSubscriber(modid = Evojobs.MODID)
public class QuestManager {

    public enum Rarity {
        COMMON("§7Common", 1.0, 1.0),
        RARE("§9Rare", 1.5, 2.0),
        EPIC("§dEpic", 2.5, 4.0),
        LEGENDARY("§6Legendary", 5.0, 8.0);

        public final String name;
        public final double amountMult;
        public final double rewardMult;

        Rarity(String name, double amountMult, double rewardMult) {
            this.name = name; this.amountMult = amountMult; this.rewardMult = rewardMult;
        }
    }

    public static class QuestTemplate {
        public String jobId; public String action; public String target; public String displayName;
        public int baseAmount; public double baseMoney; public double baseXp;

        public QuestTemplate(String jobId, String action, String target, String displayName, int baseAmount, double baseMoney, double baseXp) {
            this.jobId = jobId; this.action = action; this.target = target; this.displayName = displayName;
            this.baseAmount = baseAmount; this.baseMoney = baseMoney; this.baseXp = baseXp;
        }
    }

    public static class DailyQuest {
        public QuestTemplate template; public Rarity rarity;
        public int requiredAmount; public double rewardMoney; public double rewardXp;

        public DailyQuest(QuestTemplate template, Rarity rarity) {
            this.template = template; this.rarity = rarity;
            this.requiredAmount = (int) (template.baseAmount * rarity.amountMult);
            this.rewardMoney = template.baseMoney * rarity.rewardMult;
            this.rewardXp = template.baseXp * rarity.rewardMult;
        }
    }

    public static final List<QuestTemplate> POOL = new ArrayList<>();
    public static final List<DailyQuest> TODAYS_QUESTS = new ArrayList<>();
    private static long currentSeed = -1;

    public static void initialize() {
        // ===============================================
        // ⛏️ MINER (15 Quests)
        // ===============================================
        POOL.add(new QuestTemplate("miner", "BREAK", "minecraft:coal_ore", "Coal Ore", 100, 1500, 100));
        POOL.add(new QuestTemplate("miner", "BREAK", "minecraft:iron_ore", "Iron Ore", 50, 2000, 150));
        POOL.add(new QuestTemplate("miner", "BREAK", "minecraft:copper_ore", "Copper Ore", 64, 1800, 120));
        POOL.add(new QuestTemplate("miner", "BREAK", "minecraft:gold_ore", "Gold Ore", 30, 2500, 200));
        POOL.add(new QuestTemplate("miner", "BREAK", "minecraft:lapis_ore", "Lapis Lazuli Ore", 40, 2200, 180));
        POOL.add(new QuestTemplate("miner", "BREAK", "minecraft:redstone_ore", "Redstone Ore", 50, 2000, 160));
        POOL.add(new QuestTemplate("miner", "BREAK", "minecraft:diamond_ore", "Diamond Ore", 10, 5000, 400));
        POOL.add(new QuestTemplate("miner", "BREAK", "minecraft:emerald_ore", "Emerald Ore", 5, 6000, 500));
        POOL.add(new QuestTemplate("miner", "BREAK", "minecraft:nether_quartz_ore", "Nether Quartz", 128, 2500, 200));
        POOL.add(new QuestTemplate("miner", "BREAK", "minecraft:nether_gold_ore", "Nether Gold", 64, 2000, 150));
        POOL.add(new QuestTemplate("miner", "BREAK", "minecraft:ancient_debris", "Ancient Debris", 3, 8000, 600));
        POOL.add(new QuestTemplate("miner", "BREAK", "minecraft:deepslate_iron_ore", "Deepslate Iron", 40, 2200, 170));
        POOL.add(new QuestTemplate("miner", "BREAK", "minecraft:deepslate_gold_ore", "Deepslate Gold", 25, 2700, 220));
        POOL.add(new QuestTemplate("miner", "BREAK", "minecraft:deepslate_diamond_ore", "Deepslate Diamond", 10, 5500, 450));
        POOL.add(new QuestTemplate("miner", "BREAK", "minecraft:amethyst_cluster", "Amethyst Cluster", 32, 3000, 250));

        // ===============================================
        // 🪓 WOODCUTTER (12 Quests)
        // ===============================================
        POOL.add(new QuestTemplate("woodcutter", "BREAK", "minecraft:oak_log", "Oak Logs", 128, 1200, 100));
        POOL.add(new QuestTemplate("woodcutter", "BREAK", "minecraft:spruce_log", "Spruce Logs", 128, 1200, 100));
        POOL.add(new QuestTemplate("woodcutter", "BREAK", "minecraft:birch_log", "Birch Logs", 128, 1200, 100));
        POOL.add(new QuestTemplate("woodcutter", "BREAK", "minecraft:jungle_log", "Jungle Logs", 128, 1300, 110));
        POOL.add(new QuestTemplate("woodcutter", "BREAK", "minecraft:acacia_log", "Acacia Logs", 128, 1300, 110));
        POOL.add(new QuestTemplate("woodcutter", "BREAK", "minecraft:dark_oak_log", "Dark Oak Logs", 128, 1300, 110));
        POOL.add(new QuestTemplate("woodcutter", "BREAK", "minecraft:cherry_log", "Cherry Logs", 64, 1500, 130));
        POOL.add(new QuestTemplate("woodcutter", "BREAK", "minecraft:mangrove_log", "Mangrove Logs", 64, 1500, 130));
        POOL.add(new QuestTemplate("woodcutter", "BREAK", "minecraft:crimson_stem", "Crimson Stems", 64, 1600, 150));
        POOL.add(new QuestTemplate("woodcutter", "BREAK", "minecraft:warped_stem", "Warped Stems", 64, 1600, 150));
        POOL.add(new QuestTemplate("woodcutter", "BREAK", "minecraft:oak_leaves", "Oak Leaves", 256, 1000, 80));
        POOL.add(new QuestTemplate("woodcutter", "BREAK", "minecraft:spruce_leaves", "Spruce Leaves", 256, 1000, 80));

        // ===============================================
        // 🪚 DIGGER (10 Quests)
        // ===============================================
        POOL.add(new QuestTemplate("digger", "BREAK", "minecraft:dirt", "Dirt Blocks", 256, 1000, 80));
        POOL.add(new QuestTemplate("digger", "BREAK", "minecraft:sand", "Sand Blocks", 256, 1200, 100));
        POOL.add(new QuestTemplate("digger", "BREAK", "minecraft:gravel", "Gravel", 256, 1100, 90));
        POOL.add(new QuestTemplate("digger", "BREAK", "minecraft:clay", "Clay Blocks", 64, 1500, 120));
        POOL.add(new QuestTemplate("digger", "BREAK", "minecraft:soul_sand", "Soul Sand", 128, 1800, 150));
        POOL.add(new QuestTemplate("digger", "BREAK", "minecraft:soul_soil", "Soul Soil", 128, 1800, 150));
        POOL.add(new QuestTemplate("digger", "BREAK", "minecraft:mud", "Mud Blocks", 128, 1400, 110));
        POOL.add(new QuestTemplate("digger", "BREAK", "minecraft:red_sand", "Red Sand", 128, 1300, 110));
        POOL.add(new QuestTemplate("digger", "BREAK", "minecraft:mycelium", "Mycelium", 64, 2000, 180));
        POOL.add(new QuestTemplate("digger", "BREAK", "minecraft:podzol", "Podzol", 64, 1500, 120));

        // ===============================================
        // ⚔️ HUNTER (15 Quests)
        // ===============================================
        POOL.add(new QuestTemplate("hunter", "KILL", "minecraft:zombie", "Zombies", 40, 2000, 150));
        POOL.add(new QuestTemplate("hunter", "KILL", "minecraft:skeleton", "Skeletons", 40, 2500, 180));
        POOL.add(new QuestTemplate("hunter", "KILL", "minecraft:creeper", "Creepers", 20, 3000, 250));
        POOL.add(new QuestTemplate("hunter", "KILL", "minecraft:spider", "Spiders", 30, 2200, 160));
        POOL.add(new QuestTemplate("hunter", "KILL", "minecraft:enderman", "Endermen", 15, 4000, 350));
        POOL.add(new QuestTemplate("hunter", "KILL", "minecraft:blaze", "Blazes", 20, 4500, 380));
        POOL.add(new QuestTemplate("hunter", "KILL", "minecraft:wither_skeleton", "Wither Skeletons", 15, 5000, 400));
        POOL.add(new QuestTemplate("hunter", "KILL", "minecraft:ghast", "Ghasts", 5, 5000, 400));
        POOL.add(new QuestTemplate("hunter", "KILL", "minecraft:slime", "Slimes", 25, 2500, 200));
        POOL.add(new QuestTemplate("hunter", "KILL", "minecraft:magma_cube", "Magma Cubes", 25, 2800, 220));
        POOL.add(new QuestTemplate("hunter", "KILL", "minecraft:piglin", "Piglins", 30, 3000, 250));
        POOL.add(new QuestTemplate("hunter", "KILL", "minecraft:zombified_piglin", "Zombified Piglins", 40, 2500, 200));
        POOL.add(new QuestTemplate("hunter", "KILL", "minecraft:drowned", "Drowned", 25, 2500, 200));
        POOL.add(new QuestTemplate("hunter", "KILL", "minecraft:phantom", "Phantoms", 10, 3500, 300));
        POOL.add(new QuestTemplate("hunter", "KILL", "minecraft:witch", "Witches", 10, 4000, 350));

        // ===============================================
        // 🌾 FARMER (12 Quests)
        // ===============================================
        POOL.add(new QuestTemplate("farmer", "BREAK", "minecraft:wheat", "Wheat Crops", 128, 1500, 100));
        POOL.add(new QuestTemplate("farmer", "BREAK", "minecraft:carrots", "Carrots", 128, 1500, 100));
        POOL.add(new QuestTemplate("farmer", "BREAK", "minecraft:potatoes", "Potatoes", 128, 1500, 100));
        POOL.add(new QuestTemplate("farmer", "BREAK", "minecraft:beetroots", "Beetroots", 128, 1600, 110));
        POOL.add(new QuestTemplate("farmer", "BREAK", "minecraft:melon", "Melon Blocks", 64, 1800, 130));
        POOL.add(new QuestTemplate("farmer", "BREAK", "minecraft:pumpkin", "Pumpkins", 64, 1800, 130));
        POOL.add(new QuestTemplate("farmer", "BREAK", "minecraft:sugar_cane", "Sugar Cane", 128, 1400, 90));
        POOL.add(new QuestTemplate("farmer", "BREAK", "minecraft:cocoa", "Cocoa Beans", 64, 1700, 120));
        POOL.add(new QuestTemplate("farmer", "BREAK", "minecraft:nether_wart", "Nether Wart", 64, 2500, 200));
        POOL.add(new QuestTemplate("farmer", "BREAK", "minecraft:sweet_berry_bush", "Sweet Berries", 128, 1300, 90));
        POOL.add(new QuestTemplate("farmer", "BREAK", "minecraft:cactus", "Cactus", 64, 1400, 100));
        POOL.add(new QuestTemplate("farmer", "BREAK", "minecraft:brown_mushroom", "Brown Mushrooms", 32, 1200, 80));

        // ===============================================
        // 🎣 FISHERMAN (5 Quests)
        // ===============================================
        POOL.add(new QuestTemplate("fisherman", "FISH", "*", "Any Fish/Treasure", 30, 3500, 250));
        POOL.add(new QuestTemplate("fisherman", "FISH", "minecraft:cod", "Raw Cod", 15, 2000, 150));
        POOL.add(new QuestTemplate("fisherman", "FISH", "minecraft:salmon", "Raw Salmon", 10, 2500, 180));
        POOL.add(new QuestTemplate("fisherman", "FISH", "minecraft:pufferfish", "Pufferfish", 5, 3000, 250));
        POOL.add(new QuestTemplate("fisherman", "FISH", "minecraft:tropical_fish", "Tropical Fish", 5, 3500, 300));

        // ===============================================
        // 🧱 BUILDER (12 Quests)
        // ===============================================
        POOL.add(new QuestTemplate("builder", "PLACE", "minecraft:stone", "Stone Blocks", 128, 1500, 100));
        POOL.add(new QuestTemplate("builder", "PLACE", "minecraft:cobblestone", "Cobblestone", 256, 1200, 80));
        POOL.add(new QuestTemplate("builder", "PLACE", "minecraft:oak_planks", "Oak Planks", 128, 1200, 80));
        POOL.add(new QuestTemplate("builder", "PLACE", "minecraft:glass", "Glass Blocks", 64, 1600, 120));
        POOL.add(new QuestTemplate("builder", "PLACE", "minecraft:bricks", "Brick Blocks", 64, 1800, 140));
        POOL.add(new QuestTemplate("builder", "PLACE", "minecraft:stone_bricks", "Stone Bricks", 128, 1600, 110));
        POOL.add(new QuestTemplate("builder", "PLACE", "minecraft:white_concrete", "White Concrete", 128, 2000, 150));
        POOL.add(new QuestTemplate("builder", "PLACE", "minecraft:terracotta", "Terracotta", 64, 1700, 130));
        POOL.add(new QuestTemplate("builder", "PLACE", "minecraft:smooth_stone", "Smooth Stone", 64, 1500, 100));
        POOL.add(new QuestTemplate("builder", "PLACE", "minecraft:spruce_planks", "Spruce Planks", 128, 1200, 80));
        POOL.add(new QuestTemplate("builder", "PLACE", "minecraft:lantern", "Lanterns", 32, 2000, 160));
        POOL.add(new QuestTemplate("builder", "PLACE", "minecraft:bookshelf", "Bookshelves", 16, 2500, 200));

        // ===============================================
        // 🛠️ CRAFTER (15 Quests)
        // ===============================================
        POOL.add(new QuestTemplate("crafter", "CRAFT", "minecraft:chest", "Chests", 32, 1200, 90));
        POOL.add(new QuestTemplate("crafter", "CRAFT", "minecraft:furnace", "Furnaces", 32, 1200, 90));
        POOL.add(new QuestTemplate("crafter", "CRAFT", "minecraft:crafting_table", "Crafting Tables", 16, 1000, 80));
        POOL.add(new QuestTemplate("crafter", "CRAFT", "minecraft:stick", "Sticks", 128, 800, 50));
        POOL.add(new QuestTemplate("crafter", "CRAFT", "minecraft:torch", "Torches", 128, 1000, 80));
        POOL.add(new QuestTemplate("crafter", "CRAFT", "minecraft:bread", "Bread", 64, 1500, 120));
        POOL.add(new QuestTemplate("crafter", "CRAFT", "minecraft:iron_pickaxe", "Iron Pickaxes", 10, 2000, 150));
        POOL.add(new QuestTemplate("crafter", "CRAFT", "minecraft:iron_sword", "Iron Swords", 10, 2000, 150));
        POOL.add(new QuestTemplate("crafter", "CRAFT", "minecraft:golden_apple", "Golden Apples", 5, 3000, 250));
        POOL.add(new QuestTemplate("crafter", "CRAFT", "minecraft:bookshelf", "Bookshelves", 16, 2500, 200));
        POOL.add(new QuestTemplate("crafter", "CRAFT", "minecraft:anvil", "Anvils", 2, 3500, 300));
        POOL.add(new QuestTemplate("crafter", "CRAFT", "minecraft:barrel", "Barrels", 32, 1400, 100));
        POOL.add(new QuestTemplate("crafter", "CRAFT", "minecraft:campfire", "Campfires", 16, 1600, 130));
        POOL.add(new QuestTemplate("crafter", "CRAFT", "minecraft:stone_bricks", "Stone Bricks", 128, 1500, 110));
        POOL.add(new QuestTemplate("crafter", "CRAFT", "minecraft:glass_pane", "Glass Panes", 128, 1200, 90));

        // ===============================================
        // 💎 BLACKSMITH (Fierar) (3 Quests)
        // ===============================================
        POOL.add(new QuestTemplate("fierar", "USE_ANVIL", "*", "Use Anvil (Repair/Rename)", 10, 4000, 350));
        POOL.add(new QuestTemplate("fierar", "USE_ANVIL", "*", "Craft/Repair Gear", 15, 6000, 500));
        POOL.add(new QuestTemplate("fierar", "USE_ANVIL", "*", "Master Blacksmithing", 20, 8000, 700));

        // ===============================================
        // 💰 TRADER (3 Quests)
        // ===============================================
        POOL.add(new QuestTemplate("trader", "TRADE", "*", "Villager Trades", 15, 4500, 400));
        POOL.add(new QuestTemplate("trader", "TRADE", "*", "Merchant Deals", 25, 7000, 600));
        POOL.add(new QuestTemplate("trader", "TRADE", "*", "Market Monopoly", 40, 10000, 900));

        try {
            Connection conn = DatabaseManager.get().getConnection();
            if (conn != null && !conn.isClosed()) {
                conn.createStatement().execute("CREATE TABLE IF NOT EXISTS daily_quests (" +
                        "uuid VARCHAR(36) PRIMARY KEY, date_id BIGINT, " +
                        "q0_prog INT DEFAULT 0, q0_claimed BOOLEAN DEFAULT FALSE, " +
                        "q1_prog INT DEFAULT 0, q1_claimed BOOLEAN DEFAULT FALSE, " +
                        "q2_prog INT DEFAULT 0, q2_claimed BOOLEAN DEFAULT FALSE, " +
                        "q3_prog INT DEFAULT 0, q3_claimed BOOLEAN DEFAULT FALSE, " +
                        "q4_prog INT DEFAULT 0, q4_claimed BOOLEAN DEFAULT FALSE)");
            }
        } catch (Exception e) { e.printStackTrace(); }

        generateQuestsForToday();
    }

    public static void generateQuestsForToday() {
        long today = LocalDate.now().toEpochDay();
        if (currentSeed == today && !TODAYS_QUESTS.isEmpty()) return;

        currentSeed = today;
        TODAYS_QUESTS.clear();
        Random rand = new Random(today);

        List<QuestTemplate> shuffled = new ArrayList<>(POOL);
        Collections.shuffle(shuffled, rand);

        Set<String> usedJobs = new HashSet<>();

        for (QuestTemplate qt : shuffled) {
            if (TODAYS_QUESTS.size() >= 5) break;
            if (!usedJobs.contains(qt.jobId)) {
                usedJobs.add(qt.jobId);
                int r = rand.nextInt(100);
                Rarity rarity = (r < 50) ? Rarity.COMMON : (r < 80) ? Rarity.RARE : (r < 95) ? Rarity.EPIC : Rarity.LEGENDARY;
                TODAYS_QUESTS.add(new DailyQuest(qt, rarity));
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.getServer().getTickCount() % 1200 == 0) {
            generateQuestsForToday();
        }
    }

    public static void onAction(ServerPlayer player, String jobId, String action, String target, int amount) {
        if (TODAYS_QUESTS.isEmpty()) return;

        for (int i = 0; i < TODAYS_QUESTS.size(); i++) {
            DailyQuest q = TODAYS_QUESTS.get(i);
            if (q.template.jobId.equals(jobId) && q.template.action.equals(action)) {
                if (q.template.target.equals("*") || q.template.target.equals(target) || target.endsWith(q.template.target.replace("minecraft:", ""))) {
                    addProgress(player, i, amount, q);
                    break;
                }
            }
        }
    }

    private static void addProgress(ServerPlayer player, int questIndex, int amount, DailyQuest quest) {
        new Thread(() -> {
            synchronized (DatabaseManager.get()) {
                try {
                    Connection conn = DatabaseManager.get().getConnection();
                    if (conn == null || conn.isClosed()) return;

                    String uuid = player.getUUID().toString();
                    int currentProg = 0;
                    boolean claimed = false;

                    try (PreparedStatement checkStmt = conn.prepareStatement("SELECT date_id, q" + questIndex + "_prog, q" + questIndex + "_claimed FROM daily_quests WHERE uuid = ?")) {
                        checkStmt.setString(1, uuid);
                        try (ResultSet rs = checkStmt.executeQuery()) {
                            if (rs.next()) {
                                long dbDate = rs.getLong("date_id");
                                if (dbDate == currentSeed) {
                                    currentProg = rs.getInt("q" + questIndex + "_prog");
                                    claimed = rs.getBoolean("q" + questIndex + "_claimed");
                                } else {
                                    conn.createStatement().execute("UPDATE daily_quests SET date_id=" + currentSeed + ", q0_prog=0, q0_claimed=FALSE, q1_prog=0, q1_claimed=FALSE, q2_prog=0, q2_claimed=FALSE, q3_prog=0, q3_claimed=FALSE, q4_prog=0, q4_claimed=FALSE WHERE uuid='" + uuid + "'");
                                }
                            } else {
                                conn.createStatement().execute("INSERT INTO daily_quests (uuid, date_id) VALUES ('" + uuid + "', " + currentSeed + ")");
                            }
                        }
                    }

                    if (claimed || currentProg >= quest.requiredAmount) return;

                    int newProg = Math.min(currentProg + amount, quest.requiredAmount);

                    try (PreparedStatement updateStmt = conn.prepareStatement("UPDATE daily_quests SET q" + questIndex + "_prog = ? WHERE uuid = ?")) {
                        updateStmt.setInt(1, newProg);
                        updateStmt.setString(2, uuid);
                        updateStmt.executeUpdate();
                    }

                    if (newProg >= quest.requiredAmount && currentProg < quest.requiredAmount) {
                        conn.createStatement().execute("UPDATE daily_quests SET q" + questIndex + "_claimed = TRUE WHERE uuid = '" + uuid + "'");

                        player.getServer().execute(() -> {
                            EconomyManager.get().addBalance(player.getUUID(), quest.rewardMoney);
                            JobManager.get().addXp(player.getUUID(), quest.template.jobId, quest.rewardXp);
                            JobManager.get().syncJobsToClient(player);

                            player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
                            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§a+" + (int)quest.rewardMoney + " Lei §f| §b+" + (int)quest.rewardXp + " XP")));
                            player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§e§lDAILY QUEST COMPLETED!")));
                            player.level().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.MASTER, 1.0f, 1.0f);
                        });
                    }

                } catch (Exception e) { e.printStackTrace(); }
            }
        }).start();
    }

    public static int[] getPlayerProgress(ServerPlayer player) {
        int[] data = new int[10];
        try {
            Connection conn = DatabaseManager.get().getConnection();
            if (conn != null && !conn.isClosed()) {
                try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM daily_quests WHERE uuid = ? AND date_id = ?")) {
                    stmt.setString(1, player.getUUID().toString());
                    stmt.setLong(2, currentSeed);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            for (int i = 0; i < 5; i++) {
                                data[i*2] = rs.getInt("q" + i + "_prog");
                                data[i*2+1] = rs.getBoolean("q" + i + "_claimed") ? 1 : 0;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return data;
    }
}