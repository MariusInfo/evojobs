package org.evocraft.evojobs;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraftforge.server.ServerLifecycleHooks;

import org.evocraft.evocore.database.DatabaseManager;
import org.evocraft.evocore.data.PlayerStatsManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.stream.Collectors;

public class JobManager {
    private static JobManager INSTANCE;

    private final Map<UUID, Map<String, JobData>> playerJobs = new HashMap<>();

    public JobManager() {
        loadAllFromDatabase();
    }

    public static void initialize() { if (INSTANCE == null) INSTANCE = new JobManager(); }
    public static JobManager get() { return INSTANCE; }

    public void loadAllFromDatabase() {
        playerJobs.clear();
        String query = "SELECT * FROM player_jobs";

        Connection conn = DatabaseManager.get().getConnection();
        if (conn == null) return;

        try (PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                String jobId = rs.getString("job_id");

                JobData data = new JobData();
                data.level = rs.getInt("level");
                data.xp = rs.getDouble("xp");
                data.isActive = rs.getBoolean("is_active");

                playerJobs.computeIfAbsent(uuid, k -> new HashMap<>()).put(jobId, data);
            }
            System.out.println("[EvoJobs] Jobs have been loaded from the MariaDB/MySQL Database!");
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void saveJobToDatabase(UUID uuid, String jobId, JobData data) {
        String query = "REPLACE INTO player_jobs (uuid, player_name, job_id, level, xp, is_active) VALUES (?, ?, ?, ?, ?, ?)";

        Connection conn = DatabaseManager.get().getConnection();
        if (conn == null) return;

        String playerName = PlayerStatsManager.get().getNameByUUID(uuid);
        if (playerName == null || playerName.equals("Unknown") || playerName.equals("Necunoscut")) {
            ServerPlayer p = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(uuid);
            if (p != null) playerName = p.getGameProfile().getName();
            else playerName = "Offline";
        }

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, playerName);
            stmt.setString(3, jobId);
            stmt.setInt(4, data.level);
            stmt.setDouble(5, data.xp);
            stmt.setBoolean(6, data.isActive);
            stmt.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public Map<String, JobData> getAllJobsHistory(UUID player) {
        return playerJobs.computeIfAbsent(player, k -> new HashMap<>());
    }

    public Map<String, JobData> getActiveJobs(UUID player) {
        return getAllJobsHistory(player).entrySet().stream()
                .filter(entry -> entry.getValue().isActive)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public void syncJobsToClient(ServerPlayer player) {
        try {
            if (player == null) return;
            Map<String, JobData> allJobs = getAllJobsHistory(player.getUUID());
            List<S2C_SyncJobsPacket.JobSyncData> jobsToSync = new ArrayList<>();

            if (allJobs != null) {
                for (Map.Entry<String, JobData> entry : allJobs.entrySet()) {
                    if (entry.getKey() == null || entry.getValue() == null) continue;

                    JobDefinition def = JobConfigManager.get().getJob(entry.getKey());
                    if (def != null) {
                        double reqXp = entry.getValue().getRequiredXp();
                        if (reqXp <= 0) reqXp = 100.0;
                        String iconId = (def.icon != null && !def.icon.isEmpty()) ? def.icon : "minecraft:paper";

                        jobsToSync.add(new S2C_SyncJobsPacket.JobSyncData(
                                def.displayName != null ? def.displayName : "Unknown",
                                entry.getValue().level,
                                entry.getValue().xp,
                                reqXp,
                                iconId,
                                entry.getValue().isActive
                        ));
                    }
                }
            }

            org.evocraft.evojobs.network.EvoJobsPacketHandler.sendToPlayer(new S2C_SyncJobsPacket(jobsToSync), player);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public boolean joinJob(UUID uuid, String jobName) {
        Map<String, JobData> active = getActiveJobs(uuid);
        if (active.size() >= 3) return false;

        Map<String, JobData> history = getAllJobsHistory(uuid);
        int currentLevel = 1;
        JobData jobData;

        if (history.containsKey(jobName)) {
            jobData = history.get(jobName);
            if (jobData.isActive) return false;
            jobData.isActive = true;
            currentLevel = jobData.level;
        } else {
            jobData = new JobData();
            jobData.isActive = true;
            jobData.level = 1;
            jobData.xp = 0;
            history.put(jobName, jobData);
        }

        saveJobToDatabase(uuid, jobName, jobData);

        ServerPlayer player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(uuid);
        if (player != null) {
            syncJobScoreboard(player, jobName, currentLevel);
            syncJobsToClient(player);
        }
        return true;
    }

    public void leaveJob(UUID playerUUID, String jobName) {
        Map<String, JobData> history = getAllJobsHistory(playerUUID);
        if (history.containsKey(jobName)) {
            JobData jobData = history.get(jobName);
            jobData.isActive = false;
            saveJobToDatabase(playerUUID, jobName, jobData);

            ServerPlayer player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(playerUUID);
            if (player != null) syncJobsToClient(player);
        }
    }

    public boolean setJobLevel(UUID uuid, String jobName, int newLevel) {
        Map<String, JobData> history = getAllJobsHistory(uuid);
        JobData data;
        int oldLevel = 1;

        if (history.containsKey(jobName)) {
            data = history.get(jobName);
            oldLevel = data.level;
        } else {
            data = new JobData();
            data.isActive = false;
            history.put(jobName, data);
        }

        data.level = newLevel;
        data.xp = 0;

        saveJobToDatabase(uuid, jobName, data);

        ServerPlayer player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(uuid);
        if (player != null) {
            syncJobScoreboard(player, jobName, data.level);
            syncJobsToClient(player);
            player.sendSystemMessage(Component.literal("§a[EvoJobs] An admin set your level to §e" + newLevel + " §afor the job §b" + jobName.toUpperCase() + "§a!"));

            // REPAIR FOR ACHIEVEMENTS: Automatically grant all past achievements
            for (int i = oldLevel + 1; i <= newLevel; i++) {
                try {
                    JobAchievements.checkLevelUp(player, jobName, i);
                } catch (Exception ignored) { }
            }

            RankInfo nextRank = getRankInfo(jobName, data.level);
            if (data.level == 10 || data.level == 25 || data.level == 50 || data.level == 75 || data.level == 100) {
                player.sendSystemMessage(Component.literal("§6§lNEW RANK UNLOCKED: §e§l" + nextRank.title + " §8(+" + (int)nextRank.boostPercent + "% Money)"));
                if (data.level >= 25) {
                    player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.MASTER, 1.0f, 1.0f);
                    player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
                    player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§6§lRANK UP!")));
                    player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§e" + jobName.toUpperCase() + " §f- §b" + nextRank.title)));
                } else {
                    player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 1.0f, 1.0f);
                    player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 40, 10));
                    player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§aLevel Up!")));
                    player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§eLevel " + data.level + " in " + jobName.toUpperCase())));
                }
            }
        }
        return true;
    }

    public boolean addXp(UUID uuid, String jobName, double amount) {
        Map<String, JobData> history = getAllJobsHistory(uuid);
        if (!history.containsKey(jobName)) return false;

        JobData data = history.get(jobName);
        if (!data.isActive) return false;

        data.xp += amount;
        boolean leveledUp = false;

        while (data.xp >= data.getRequiredXp()) {
            data.xp -= data.getRequiredXp();
            data.level++;
            leveledUp = true;

            ServerPlayer player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(uuid);
            if (player != null) {
                RankInfo nextRank = getRankInfo(jobName, data.level);
                player.sendSystemMessage(Component.literal("§a[Job] You reached level §e" + data.level + " §ain §b" + jobName.toUpperCase() + "§a!"));

                if (data.level == 10 || data.level == 25 || data.level == 50 || data.level == 75 || data.level == 100) {
                    player.sendSystemMessage(Component.literal("§6§lNEW RANK UNLOCKED: §e§l" + nextRank.title + " §8(+" + (int)nextRank.boostPercent + "% Money)"));

                    if (data.level >= 25) {
                        player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.MASTER, 1.0f, 1.0f);

                        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
                        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§6§lRANK UP!")));
                        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§e" + jobName.toUpperCase() + " §f- §b" + nextRank.title)));
                    } else {
                        player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 1.0f, 1.0f);

                        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 40, 10));
                        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§aLevel Up!")));
                        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§eLevel " + data.level + " in " + jobName.toUpperCase())));
                    }
                }

                try {
                    JobAchievements.checkLevelUp(player, jobName, data.level);
                } catch (Exception ignored) { }

                syncJobScoreboard(player, jobName, data.level);
                syncJobsToClient(player);
            }
        }

        saveJobToDatabase(uuid, jobName, data);
        return leveledUp;
    }

    public List<Map.Entry<UUID, JobData>> getTopPlayersForJob(String jobId) {
        String targetJob = jobId.toLowerCase();
        List<Map.Entry<UUID, JobData>> list = new ArrayList<>();

        for (Map.Entry<UUID, Map<String, JobData>> entry : playerJobs.entrySet()) {
            if (entry.getValue().containsKey(targetJob)) {
                list.add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue().get(targetJob)));
            }
        }

        list.sort((a, b) -> {
            int lvlCompare = Integer.compare(b.getValue().level, a.getValue().level);
            if (lvlCompare != 0) return lvlCompare;
            return Double.compare(b.getValue().xp, a.getValue().xp);
        });

        return list;
    }

    public void onPlayerJoin(ServerPlayer player) {
        try {
            Map<String, JobData> jobs = getAllJobsHistory(player.getUUID());

            if (jobs != null) {
                for (Map.Entry<String, JobData> entry : jobs.entrySet()) {
                    String jobName = entry.getKey();
                    JobData data = entry.getValue();
                    if (jobName == null || data == null) continue;

                    boolean leveledUp = false;
                    double reqXp = data.getRequiredXp();
                    if (reqXp <= 0) reqXp = 100.0;

                    while (data.xp >= reqXp && reqXp > 0) {
                        data.xp -= reqXp;
                        data.level++;
                        leveledUp = true;
                        reqXp = data.getRequiredXp();
                        if (reqXp <= 0) reqXp = 100.0;
                    }

                    if (leveledUp) {
                        saveJobToDatabase(player.getUUID(), jobName, data);
                        player.sendSystemMessage(Component.literal("§a[Job] The system updated your remaining level for §e" + jobName + "§a! You are now level §e" + data.level));
                    }
                    syncJobScoreboard(player, jobName, data.level);

                    // --- ADDED: RETROACTIVE ACHIEVEMENTS CHECK ON LOGIN ---
                    for (int i = 5; i <= data.level; i++) {
                        if (i == 5 || i == 10 || i == 20 || i == 25 || i == 30 || i == 40 || i == 50 || i == 60 || i == 70 || i == 75 || i == 80 || i == 90 || i == 100) {
                            try {
                                JobAchievements.checkLevelUp(player, jobName, i);
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }

            syncJobsToClient(player);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void syncJobScoreboard(ServerPlayer player, String jobName, int level) {
        try {
            if (player == null) return;
            MinecraftServer server = player.getServer();
            if (server == null) return;

            Scoreboard scoreboard = server.getScoreboard();
            String safeJobName = jobName.toLowerCase().replaceAll("[^a-z0-9_]", "_");
            String objectiveName = "evo_job_" + safeJobName;

            if (objectiveName.length() > 16) objectiveName = objectiveName.substring(0, 16);

            Objective objective = scoreboard.getObjective(objectiveName);
            if (objective == null) {
                objective = scoreboard.addObjective(
                        objectiveName,
                        ObjectiveCriteria.DUMMY,
                        Component.literal("Job: " + jobName),
                        ObjectiveCriteria.RenderType.INTEGER
                );
            }
            scoreboard.getOrCreatePlayerScore(player.getScoreboardName(), objective).setScore(level);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static class RankInfo {
        public String title; public double boostPercent; public int nextLevelReq;
        public RankInfo(String t, double b, int n) { title = t; boostPercent = b; nextLevelReq = n; }
    }

    public static RankInfo getRankInfo(String jobId, int level) {
        String title = "Amateur";
        double boost = 0.0;
        int nextReq = 10;

        if (level >= 100) { boost = 200.0; nextReq = 999; }
        else if (level >= 75) { boost = 100.0; nextReq = 100; }
        else if (level >= 50) { boost = 50.0; nextReq = 75; }
        else if (level >= 25) { boost = 25.0; nextReq = 50; }
        else if (level >= 10) { boost = 10.0; nextReq = 25; }

        switch (jobId.toLowerCase()) {
            case "miner": if (level >= 100) title = "Diamond God"; else if (level >= 75) title = "Underground Legend"; else if (level >= 50) title = "Master Miner"; else if (level >= 25) title = "Pro Miner"; else if (level >= 10) title = "Rock Digger"; else title = "Beginner Miner"; break;
            case "woodcutter": if (level >= 100) title = "God of Nature"; else if (level >= 75) title = "King of the Woods"; else if (level >= 50) title = "Supreme Logger"; else if (level >= 25) title = "Expert Forester"; else if (level >= 10) title = "Wood Breaker"; else title = "Amateur Cutter"; break;
            case "digger": if (level >= 100) title = "Lord of the Depths"; else if (level >= 75) title = "Earth Titan"; else if (level >= 50) title = "Human Machine"; else if (level >= 25) title = "Soil Specialist"; else if (level >= 10) title = "Excavator"; else title = "Worker"; break;
            case "hunter": if (level >= 100) title = "God of War"; else if (level >= 75) title = "Legendary Assassin"; else if (level >= 50) title = "Monster Terror"; else if (level >= 25) title = "Elite Hunter"; else if (level >= 10) title = "Zombie Killer"; else title = "Novice"; break;
            case "farmer": if (level >= 100) title = "God of the Earth"; else if (level >= 75) title = "King of Harvests"; else if (level >= 50) title = "Master Farmer"; else if (level >= 25) title = "Skilled Farmer"; else if (level >= 10) title = "Cultivator"; else title = "Peasant"; break;
            case "fisherman": if (level >= 100) title = "God of the Seas"; else if (level >= 75) title = "Legend of the Oceans"; else if (level >= 50) title = "Master of Waters"; else if (level >= 25) title = "Hardcore Fisher"; else if (level >= 10) title = "Seagull"; else title = "Amateur Fisher"; break;
            case "builder": if (level >= 100) title = "Creator of Worlds"; else if (level >= 75) title = "Legendary Engineer"; else if (level >= 50) title = "Master Builder"; else if (level >= 25) title = "Architect"; else if (level >= 10) title = "Chief Mason"; else title = "Mason"; break;
            case "crafter": if (level >= 100) title = "God of Crafting"; else if (level >= 75) title = "Legend of Creation"; else if (level >= 50) title = "Master Artisan"; else if (level >= 25) title = "Artisan"; else if (level >= 10) title = "Craftsman"; else title = "Apprentice"; break;
            case "trader": if (level >= 100) title = "God of Commerce"; else if (level >= 75) title = "Tycoon"; else if (level >= 50) title = "Successful Businessman"; else if (level >= 25) title = "Expert Merchant"; else if (level >= 10) title = "Hustler"; else title = "Beginner Merchant"; break;
            case "fierar": if (level >= 100) title = "God of Metal"; else if (level >= 75) title = "Legendary Forger"; else if (level >= 50) title = "Master of the Anvil"; else if (level >= 25) title = "Skilled Forger"; else if (level >= 10) title = "Apprentice Blacksmith"; else title = "Hammerer"; break;
            default: if (level >= 100) title = "Supreme God"; else if (level >= 75) title = "Legend"; else if (level >= 50) title = "Master"; else if (level >= 25) title = "Expert"; else if (level >= 10) title = "Advanced"; break;
        }
        return new RankInfo(title, boost, nextReq);
    }
}