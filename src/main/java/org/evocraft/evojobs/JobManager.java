package org.evocraft.evojobs;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.evocraft.evocore.data.PlayerStatsManager;
import org.evocraft.evocore.database.DatabaseManager;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class JobManager {
    private static JobManager INSTANCE;
    private static final int DATA_VERSION = 2;
    private static final String MIGRATION_BACKUP_TABLE = "player_jobs_backup_before_progression_v2";
    private static final long DIRTY_SAVE_INTERVAL_MS = 5000L;
    private static final int MAX_LEVEL_UPS_PER_XP_GAIN = 10_000;
    private static final int[] ACHIEVEMENT_LEVELS = {5, 10, 20, 25, 30, 40, 50, 60, 70, 75, 80, 90, 100};

    private final Map<UUID, Map<String, JobData>> playerJobs = new HashMap<>();
    private final Set<String> dirtyJobSaves = ConcurrentHashMap.newKeySet();
    private long lastDirtyFlushMs = 0L;
    private boolean dataVersionColumnAvailable = true;

    public JobManager() {
        loadAllFromDatabase();
    }

    public static void initialize() {
        if (INSTANCE == null) INSTANCE = new JobManager();
    }

    public static JobManager get() {
        return INSTANCE;
    }

    public void loadAllFromDatabase() {
        playerJobs.clear();
        Connection conn = DatabaseManager.get().getConnection();
        if (conn == null) return;

        migrateDatabaseSchema(conn);

        String query = "SELECT * FROM player_jobs";
        int legacyRowsQueued = 0;
        try (PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                String jobId = rs.getString("job_id");
                if (jobId == null || jobId.isBlank()) continue;
                jobId = jobId.toLowerCase(Locale.ROOT);

                JobData data = new JobData();
                data.level = Math.max(1L, rs.getLong("level"));
                data.xp = sanitizeXp(rs.getDouble("xp"));
                data.isActive = rs.getBoolean("is_active");

                playerJobs.computeIfAbsent(uuid, k -> new HashMap<>()).put(jobId, data);
                if (readDataVersion(rs) < DATA_VERSION) {
                    queueJobSave(uuid, jobId);
                    legacyRowsQueued++;
                }
            }
            System.out.println("[EvoJobs] Jobs have been loaded from the MariaDB/MySQL Database!");
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (legacyRowsQueued > 0) {
            flushDirtySaves(true);
            System.out.println("[EvoJobs] Migrated " + legacyRowsQueued + " legacy player job rows to data_version " + DATA_VERSION + " without resetting level or XP.");
        }
    }

    private void migrateDatabaseSchema(Connection conn) {
        backupLegacyPlayerJobs(conn);

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS player_jobs (" +
                    "uuid VARCHAR(36) NOT NULL, " +
                    "player_name VARCHAR(32) NOT NULL DEFAULT 'Offline', " +
                    "job_id VARCHAR(64) NOT NULL, " +
                    "level BIGINT NOT NULL DEFAULT 1, " +
                    "xp DOUBLE NOT NULL DEFAULT 0, " +
                    "is_active BOOLEAN NOT NULL DEFAULT TRUE, " +
                    "data_version INT NOT NULL DEFAULT " + DATA_VERSION + ", " +
                    "PRIMARY KEY (uuid, job_id))");
        } catch (Exception e) {
            System.out.println("[EvoJobs] Could not create player_jobs table during migration.");
            e.printStackTrace();
        }

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE player_jobs MODIFY level BIGINT NOT NULL DEFAULT 1");
        } catch (Exception e) {
            System.out.println("[EvoJobs] Schema migration warning: could not alter player_jobs.level to BIGINT. Existing schema may already be compatible.");
        }

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE player_jobs ADD COLUMN data_version INT NOT NULL DEFAULT 1");
            dataVersionColumnAvailable = true;
        } catch (Exception e) {
            String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            dataVersionColumnAvailable = message.contains("duplicate") || message.contains("exists");
            if (!dataVersionColumnAvailable) {
                System.out.println("[EvoJobs] Schema migration warning: data_version column is not available. Saving in legacy format.");
            }
        }
    }

    private void backupLegacyPlayerJobs(Connection conn) {
        try {
            if (!tableExists(conn, "player_jobs") || tableExists(conn, MIGRATION_BACKUP_TABLE)) return;
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE " + MIGRATION_BACKUP_TABLE + " AS SELECT * FROM player_jobs");
                System.out.println("[EvoJobs] Legacy player_jobs backup created: " + MIGRATION_BACKUP_TABLE);
            }
        } catch (Exception e) {
            System.out.println("[EvoJobs] Migration warning: could not create player_jobs backup. Migration will continue without deleting legacy data.");
            e.printStackTrace();
        }
    }

    private boolean tableExists(Connection conn, String tableName) {
        try {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet tables = metaData.getTables(conn.getCatalog(), null, tableName, null)) {
                if (tables.next()) return true;
            }
        } catch (Exception ignored) {
        }

        try (Statement stmt = conn.createStatement();
             ResultSet ignored = stmt.executeQuery("SELECT 1 FROM " + tableName + " LIMIT 1")) {
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private int readDataVersion(ResultSet rs) {
        if (!dataVersionColumnAvailable) return 1;
        try {
            int version = rs.getInt("data_version");
            return version > 0 ? version : 1;
        } catch (Exception ignored) {
            return 1;
        }
    }

    public boolean saveJobToDatabase(UUID uuid, String jobId, JobData data) {
        Connection conn = DatabaseManager.get().getConnection();
        if (conn == null) return false;

        String query = dataVersionColumnAvailable
                ? "INSERT INTO player_jobs (uuid, player_name, job_id, level, xp, is_active, data_version) VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "player_name = IF(VALUES(player_name) IN ('Offline', 'Unknown', 'Necunoscut'), player_name, VALUES(player_name)), " +
                "level = VALUES(level), xp = VALUES(xp), is_active = VALUES(is_active), data_version = VALUES(data_version)"
                : "INSERT INTO player_jobs (uuid, player_name, job_id, level, xp, is_active) VALUES (?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "player_name = IF(VALUES(player_name) IN ('Offline', 'Unknown', 'Necunoscut'), player_name, VALUES(player_name)), " +
                "level = VALUES(level), xp = VALUES(xp), is_active = VALUES(is_active)";

        String playerName = resolvePlayerName(uuid);
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, playerName);
            stmt.setString(3, jobId);
            stmt.setLong(4, Math.max(1L, data.level));
            stmt.setDouble(5, sanitizeXp(data.xp));
            stmt.setBoolean(6, data.isActive);
            if (dataVersionColumnAvailable) {
                stmt.setInt(7, DATA_VERSION);
            }
            stmt.executeUpdate();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private String resolvePlayerName(UUID uuid) {
        String playerName = PlayerStatsManager.get().getNameByUUID(uuid);
        if (playerName == null || playerName.equals("Unknown") || playerName.equals("Necunoscut")) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            ServerPlayer p = server != null ? server.getPlayerList().getPlayer(uuid) : null;
            playerName = p != null ? p.getGameProfile().getName() : "Offline";
        }
        return playerName;
    }

    private String saveKey(UUID uuid, String jobId) {
        return uuid + "|" + jobId;
    }

    private void saveJobNowOrQueue(UUID uuid, String jobId, JobData data) {
        if (!saveJobToDatabase(uuid, jobId, data)) {
            queueJobSave(uuid, jobId);
        }
    }

    private void queueJobSave(UUID uuid, String jobId) {
        dirtyJobSaves.add(saveKey(uuid, jobId));
    }

    public void flushDirtySaves(boolean force) {
        if (dirtyJobSaves.isEmpty()) return;

        long now = System.currentTimeMillis();
        if (!force && now - lastDirtyFlushMs < DIRTY_SAVE_INTERVAL_MS) return;
        lastDirtyFlushMs = now;

        List<String> keysToSave = new ArrayList<>(dirtyJobSaves);
        for (String key : keysToSave) {
            try {
                String[] parts = key.split("\\|", 2);
                if (parts.length != 2) {
                    dirtyJobSaves.remove(key);
                    continue;
                }

                UUID uuid = UUID.fromString(parts[0]);
                String jobId = parts[1];
                Map<String, JobData> history = playerJobs.get(uuid);
                boolean saved = true;
                if (history != null) {
                    JobData data = history.get(jobId);
                    if (data != null) {
                        saved = saveJobToDatabase(uuid, jobId, data);
                    }
                }
                if (saved) {
                    dirtyJobSaves.remove(key);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void shutdown() {
        flushDirtySaves(true);
    }

    public Map<String, JobData> getAllJobsHistory(UUID player) {
        return playerJobs.computeIfAbsent(player, k -> new HashMap<>());
    }

    public Map<String, JobData> getActiveJobs(UUID player) {
        return getAllJobsHistory(player).entrySet().stream()
                .filter(entry -> entry.getValue().isActive)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public long getTotalJobLevel(ServerPlayer player) {
        return JobProgressionService.getTotalJobLevel(player);
    }

    public long getTotalJobLevel(UUID uuid) {
        return JobProgressionService.getTotalJobLevel(uuid);
    }

    public long getSynergyLevel(ServerPlayer player) {
        return JobProgressionService.getSynergyLevel(player);
    }

    public long getSynergyLevel(UUID uuid) {
        return JobProgressionService.getSynergyLevel(uuid);
    }

    public void syncJobsToClient(ServerPlayer player) {
        try {
            if (player == null) return;
            Map<String, JobData> allJobs = getAllJobsHistory(player.getUUID());
            List<S2C_SyncJobsPacket.JobSyncData> jobsToSync = new ArrayList<>();
            long totalJobLevel = getTotalJobLevel(player);
            long synergyLevel = getSynergyLevel(player);

            for (JobDefinition def : JobConfigManager.get().getAllJobs()) {
                if (def == null || def.id == null) continue;
                JobData data = allJobs.get(def.id);
                long level = data != null ? Math.max(1L, data.level) : 1L;
                double xp = data != null ? sanitizeXp(data.xp) : 0.0;
                double reqXp = JobProgressionService.getRequiredXpForNextLevel(level);
                String iconId = (def.icon != null && !def.icon.isEmpty()) ? def.icon : "minecraft:paper";
                JobProgressionService.AttributeInfo attribute = JobProgressionService.getJobAttributeInfo(def.id, level, player);
                RankInfo rankInfo = getRankInfo(def.id, level);
                double rankMultiplier = 1.0 + (rankInfo.boostPercent / 100.0);

                jobsToSync.add(new S2C_SyncJobsPacket.JobSyncData(
                        def.displayName != null ? def.displayName : "Unknown",
                        level,
                        xp,
                        reqXp,
                        iconId,
                        data != null && data.isActive,
                        JobProgressionService.calculateScaledXpReward(1.0, level, player),
                        JobProgressionService.calculateScaledMoneyReward(1.0, level, player, rankMultiplier),
                        attribute.name,
                        attribute.rawPower,
                        attribute.effectPercent
                ));
            }

            org.evocraft.evojobs.network.EvoJobsPacketHandler.sendToPlayer(new S2C_SyncJobsPacket(jobsToSync, totalJobLevel, synergyLevel), player);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean joinJob(UUID uuid, String jobName) {
        Map<String, JobData> active = getActiveJobs(uuid);
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        ServerPlayer player = server != null ? server.getPlayerList().getPlayer(uuid) : null;
        int maxActiveJobs = JobPermissionManager.getMaxJobs(player);
        if (active.size() >= maxActiveJobs) return false;

        Map<String, JobData> history = getAllJobsHistory(uuid);
        long currentLevel = 1L;
        JobData jobData;

        if (history.containsKey(jobName)) {
            jobData = history.get(jobName);
            if (jobData.isActive) return false;
            jobData.isActive = true;
            jobData.level = Math.max(1L, jobData.level);
            jobData.xp = sanitizeXp(jobData.xp);
            currentLevel = jobData.level;
        } else {
            jobData = new JobData();
            jobData.isActive = true;
            jobData.level = 1L;
            jobData.xp = 0.0;
            history.put(jobName, jobData);
        }

        saveJobNowOrQueue(uuid, jobName, jobData);

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
            saveJobNowOrQueue(playerUUID, jobName, jobData);

            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            ServerPlayer player = server != null ? server.getPlayerList().getPlayer(playerUUID) : null;
            if (player != null) syncJobsToClient(player);
        }
    }

    public boolean setJobLevel(UUID uuid, String jobName, long newLevel) {
        long safeNewLevel = Math.max(1L, newLevel);
        Map<String, JobData> history = getAllJobsHistory(uuid);
        JobData data;
        long oldLevel = 1L;

        if (history.containsKey(jobName)) {
            data = history.get(jobName);
            oldLevel = Math.max(1L, data.level);
        } else {
            data = new JobData();
            data.isActive = false;
            history.put(jobName, data);
        }

        data.level = safeNewLevel;
        data.xp = 0.0;

        saveJobNowOrQueue(uuid, jobName, data);

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        ServerPlayer player = server != null ? server.getPlayerList().getPlayer(uuid) : null;
        if (player != null) {
            syncJobScoreboard(player, jobName, data.level);
            syncJobsToClient(player);
            player.sendSystemMessage(Component.literal("\u00A7a[EvoJobs] An admin set your level to \u00A7e" + safeNewLevel + " \u00A7afor the job \u00A7b" + jobName.toUpperCase() + "\u00A7a!"));

            grantAchievementsBetween(player, jobName, oldLevel, safeNewLevel);
            if (safeNewLevel > oldLevel) {
                JobProgressionService.processLevelRewards(player, jobName, oldLevel, safeNewLevel);
            }
            sendRankUnlockFeedback(player, jobName, safeNewLevel);
        }
        return true;
    }

    public boolean addXp(UUID uuid, String jobName, double amount) {
        double safeAmount = sanitizeXp(amount);
        if (safeAmount <= 0.0) return false;

        Map<String, JobData> history = getAllJobsHistory(uuid);
        if (!history.containsKey(jobName)) return false;

        JobData data = history.get(jobName);
        if (!data.isActive) return false;

        data.level = Math.max(1L, data.level);
        data.xp = sanitizeXp(data.xp) + safeAmount;

        long oldLevel = data.level;
        boolean leveledUp = processStoredOverflow(data);

        ServerPlayer player = getOnlinePlayer(uuid);
        if (player != null && leveledUp) {
            sendLevelUpFeedback(player, jobName, oldLevel, data.level);
            grantAchievementsBetween(player, jobName, oldLevel, data.level);
            JobProgressionService.processLevelRewards(player, jobName, oldLevel, data.level);
            syncJobScoreboard(player, jobName, data.level);
            syncJobsToClient(player);
        }

        queueJobSave(uuid, jobName);
        return leveledUp;
    }

    private boolean processStoredOverflow(JobData data) {
        boolean leveledUp = false;
        for (int i = 0; i < MAX_LEVEL_UPS_PER_XP_GAIN; i++) {
            double required = data.getRequiredXp();
            if (!Double.isFinite(required) || required <= 0.0) {
                required = JobProgressionService.getRequiredXpForNextLevel(data.level);
            }
            if (data.xp < required) break;
            data.xp -= required;
            if (data.level == Long.MAX_VALUE) {
                data.xp = 0.0;
                break;
            }
            data.level++;
            leveledUp = true;
        }

        if (data.xp < 0.0 || !Double.isFinite(data.xp)) {
            data.xp = 0.0;
        }
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
            int lvlCompare = Long.compare(b.getValue().level, a.getValue().level);
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

                    long oldLevel = Math.max(1L, data.level);
                    data.level = oldLevel;
                    data.xp = sanitizeXp(data.xp);
                    boolean leveledUp = processStoredOverflow(data);

                    if (leveledUp) {
                        saveJobNowOrQueue(player.getUUID(), jobName, data);
                        player.sendSystemMessage(Component.literal("\u00A7a[Job] The system updated your remaining XP for \u00A7e" + jobName + "\u00A7a. You are now level \u00A7e" + data.level));
                        grantAchievementsBetween(player, jobName, oldLevel, data.level);
                        JobProgressionService.processLevelRewards(player, jobName, oldLevel, data.level);
                    }

                    syncJobScoreboard(player, jobName, data.level);
                    grantAchievementsBetween(player, jobName, 1L, data.level);
                }
            }

            syncJobsToClient(player);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendLevelUpFeedback(ServerPlayer player, String jobName, long oldLevel, long newLevel) {
        JobProgressionService.AttributeInfo oldAttribute = JobProgressionService.getJobAttributeInfo(jobName, oldLevel, player);
        JobProgressionService.AttributeInfo newAttribute = JobProgressionService.getJobAttributeInfo(jobName, newLevel, player);
        player.sendSystemMessage(Component.literal("\u00A7a[Job] " + jobName.toUpperCase() + " Level Up! \u00A7e" + oldLevel + " -> " + newLevel));
        player.sendSystemMessage(Component.literal("\u00A7b" + newAttribute.name + ": \u00A7f" +
                JobProgressionService.formatNumber(oldAttribute.rawPower) + " -> " +
                JobProgressionService.formatNumber(newAttribute.rawPower)));
        sendRankUnlockFeedback(player, jobName, newLevel);
    }

    private void sendRankUnlockFeedback(ServerPlayer player, String jobName, long level) {
        if (!isRankMilestone(level)) return;

        RankInfo nextRank = getRankInfo(jobName, level);
        player.sendSystemMessage(Component.literal("\u00A76\u00A7lNEW RANK UNLOCKED: \u00A7e\u00A7l" + nextRank.title + " \u00A78(+" + (int) nextRank.boostPercent + "% Money)"));

        if (level >= 25) {
            player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.MASTER, 1.0f, 1.0f);
            player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
            player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("\u00A76\u00A7lRANK UP!")));
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("\u00A7e" + jobName.toUpperCase() + " \u00A7f- \u00A7b" + nextRank.title)));
        } else {
            player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 1.0f, 1.0f);
            player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 40, 10));
            player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("\u00A7aLevel Up!")));
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("\u00A7eLevel " + level + " in " + jobName.toUpperCase())));
        }
    }

    private boolean isRankMilestone(long level) {
        return level == 10 || level == 25 || level == 50 || level == 75 || level == 100;
    }

    private void grantAchievementsBetween(ServerPlayer player, String jobName, long oldLevel, long newLevel) {
        for (int milestone : ACHIEVEMENT_LEVELS) {
            if (milestone > oldLevel && milestone <= newLevel) {
                try {
                    JobAchievements.checkLevelUp(player, jobName, milestone);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private ServerPlayer getOnlinePlayer(UUID uuid) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server != null ? server.getPlayerList().getPlayer(uuid) : null;
    }

    private void syncJobScoreboard(ServerPlayer player, String jobName, long level) {
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
            scoreboard.getOrCreatePlayerScore(player.getScoreboardName(), objective).setScore(clampScore(level));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int clampScore(long level) {
        if (level > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        if (level < Integer.MIN_VALUE) return Integer.MIN_VALUE;
        return (int) level;
    }

    private double sanitizeXp(double value) {
        if (!Double.isFinite(value) || value <= 0.0) return 0.0;
        return Math.min(value, 1.0E18);
    }

    public static class RankInfo {
        public String title;
        public double boostPercent;
        public long nextLevelReq;

        public RankInfo(String t, double b, long n) {
            title = t;
            boostPercent = b;
            nextLevelReq = n;
        }
    }

    public static RankInfo getRankInfo(String jobId, long level) {
        String title = "Amateur";
        double boost = 0.0;
        long nextReq = 10;

        if (level >= 100) {
            boost = 200.0;
            nextReq = Long.MAX_VALUE;
        } else if (level >= 75) {
            boost = 100.0;
            nextReq = 100;
        } else if (level >= 50) {
            boost = 50.0;
            nextReq = 75;
        } else if (level >= 25) {
            boost = 25.0;
            nextReq = 50;
        } else if (level >= 10) {
            boost = 10.0;
            nextReq = 25;
        }

        switch (jobId.toLowerCase()) {
            case "miner":
                if (level >= 100) title = "Diamond God";
                else if (level >= 75) title = "Underground Legend";
                else if (level >= 50) title = "Master Miner";
                else if (level >= 25) title = "Pro Miner";
                else if (level >= 10) title = "Rock Digger";
                else title = "Beginner Miner";
                break;
            case "woodcutter":
                if (level >= 100) title = "God of Nature";
                else if (level >= 75) title = "King of the Woods";
                else if (level >= 50) title = "Supreme Logger";
                else if (level >= 25) title = "Expert Forester";
                else if (level >= 10) title = "Wood Breaker";
                else title = "Amateur Cutter";
                break;
            case "digger":
                if (level >= 100) title = "Lord of the Depths";
                else if (level >= 75) title = "Earth Titan";
                else if (level >= 50) title = "Human Machine";
                else if (level >= 25) title = "Soil Specialist";
                else if (level >= 10) title = "Excavator";
                else title = "Worker";
                break;
            case "hunter":
                if (level >= 100) title = "God of War";
                else if (level >= 75) title = "Legendary Assassin";
                else if (level >= 50) title = "Monster Terror";
                else if (level >= 25) title = "Elite Hunter";
                else if (level >= 10) title = "Zombie Killer";
                else title = "Novice";
                break;
            case "farmer":
                if (level >= 100) title = "God of the Earth";
                else if (level >= 75) title = "King of Harvests";
                else if (level >= 50) title = "Master Farmer";
                else if (level >= 25) title = "Skilled Farmer";
                else if (level >= 10) title = "Cultivator";
                else title = "Peasant";
                break;
            case "fisherman":
                if (level >= 100) title = "God of the Seas";
                else if (level >= 75) title = "Legend of the Oceans";
                else if (level >= 50) title = "Master of Waters";
                else if (level >= 25) title = "Hardcore Fisher";
                else if (level >= 10) title = "Seagull";
                else title = "Amateur Fisher";
                break;
            case "builder":
                if (level >= 100) title = "Creator of Worlds";
                else if (level >= 75) title = "Legendary Engineer";
                else if (level >= 50) title = "Master Builder";
                else if (level >= 25) title = "Architect";
                else if (level >= 10) title = "Chief Mason";
                else title = "Mason";
                break;
            case "crafter":
                if (level >= 100) title = "God of Crafting";
                else if (level >= 75) title = "Legend of Creation";
                else if (level >= 50) title = "Master Artisan";
                else if (level >= 25) title = "Artisan";
                else if (level >= 10) title = "Craftsman";
                else title = "Apprentice";
                break;
            case "trader":
                if (level >= 100) title = "God of Commerce";
                else if (level >= 75) title = "Tycoon";
                else if (level >= 50) title = "Successful Businessman";
                else if (level >= 25) title = "Expert Merchant";
                else if (level >= 10) title = "Hustler";
                else title = "Beginner Merchant";
                break;
            case "fierar":
                if (level >= 100) title = "God of Metal";
                else if (level >= 75) title = "Legendary Forger";
                else if (level >= 50) title = "Master of the Anvil";
                else if (level >= 25) title = "Skilled Forger";
                else if (level >= 10) title = "Apprentice Blacksmith";
                else title = "Hammerer";
                break;
            default:
                if (level >= 100) title = "Supreme God";
                else if (level >= 75) title = "Legend";
                else if (level >= 50) title = "Master";
                else if (level >= 25) title = "Expert";
                else if (level >= 10) title = "Advanced";
                break;
        }
        return new RankInfo(title, boost, nextReq);
    }
}
