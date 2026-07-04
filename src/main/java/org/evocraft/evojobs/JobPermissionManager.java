package org.evocraft.evojobs;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Method;
import java.util.UUID;

public class JobPermissionManager {
    public static final int DEFAULT_MAX_JOBS = 3;
    public static final int HIGHEST_SUPPORTED_MAX_JOBS = 6;

    public static int getMaxJobs(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            return getMaxJobs(serverPlayer);
        }
        return DEFAULT_MAX_JOBS;
    }

    public static int getMaxJobs(ServerPlayer player) {
        if (player == null) return DEFAULT_MAX_JOBS;

        for (int maxJobs = HIGHEST_SUPPORTED_MAX_JOBS; maxJobs > DEFAULT_MAX_JOBS; maxJobs--) {
            if (hasPermission(player, "evojobs.maxjobs." + maxJobs) || hasPermission(player, "evojobs.jobs." + maxJobs)) {
                return maxJobs;
            }
        }

        return DEFAULT_MAX_JOBS;
    }

    private static boolean hasPermission(ServerPlayer player, String permissionNode) {
        return hasLuckPermsPermission(player.getUUID(), permissionNode);
    }

    private static boolean hasLuckPermsPermission(UUID uuid, String permissionNode) {
        try {
            Class<?> providerClass = Class.forName("net.luckperms.api.LuckPermsProvider");
            Object luckPerms = providerClass.getMethod("get").invoke(null);

            Object userManager = luckPerms.getClass().getMethod("getUserManager").invoke(luckPerms);
            Method getUser = userManager.getClass().getMethod("getUser", UUID.class);
            Object user = getUser.invoke(userManager, uuid);
            if (user == null) return false;

            Object cachedData = user.getClass().getMethod("getCachedData").invoke(user);
            Object permissionData = cachedData.getClass().getMethod("getPermissionData").invoke(cachedData);
            Object tristate = permissionData.getClass().getMethod("checkPermission", String.class).invoke(permissionData, permissionNode);
            Object allowed = tristate.getClass().getMethod("asBoolean").invoke(tristate);

            return Boolean.TRUE.equals(allowed);
        } catch (ClassNotFoundException ignored) {
            return false;
        } catch (Exception ignored) {
            return false;
        }
    }
}
