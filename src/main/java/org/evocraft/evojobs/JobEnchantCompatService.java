package org.evocraft.evojobs;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;
import org.evocraft.evojobs.JobEnchantCompatConfigManager.EnchantmentRewardRule;

import java.util.Locale;
import java.util.Map;

public class JobEnchantCompatService {
    public static double getXpMultiplier(ServerPlayer player, String action, String jobId) {
        return getMultiplier(player, action, jobId, true);
    }

    public static double getMoneyMultiplier(ServerPlayer player, String action, String jobId) {
        return getMultiplier(player, action, jobId, false);
    }

    private static double getMultiplier(ServerPlayer player, String action, String jobId, boolean xp) {
        if (player == null) return 1.0;

        double percent = 0.0;
        Map<String, EnchantmentRewardRule> rules = JobEnchantCompatConfigManager.get().config().enchantment_rewards;
        if (rules == null || rules.isEmpty()) return 1.0;

        for (EnchantmentRewardRule rule : rules.values()) {
            if (rule == null || !rule.enabled) continue;
            if (!matches(rule.actions, action) || !matches(rule.jobs, jobId)) continue;

            Enchantment enchantment = getEnchantment(rule.enchantment_id);
            if (enchantment == null) continue;

            int level = getTotalLevel(player, enchantment, rule);
            if (level <= 0) continue;

            double perLevel = xp ? rule.xp_percent_per_level : rule.money_percent_per_level;
            double cap = xp ? rule.max_xp_percent : rule.max_money_percent;
            if (!Double.isFinite(perLevel) || perLevel == 0.0) continue;

            double value = perLevel * level;
            if (Double.isFinite(cap) && cap > 0.0) {
                if (value >= 0.0) value = Math.min(value, cap);
                else value = Math.max(value, -cap);
            }
            if (Double.isFinite(value)) {
                percent += value;
            }
        }

        if (!Double.isFinite(percent)) return 1.0;
        percent = Math.max(-100.0, Math.min(1_000.0, percent));
        return Math.max(0.0, 1.0 + (percent / 100.0));
    }

    private static int getTotalLevel(ServerPlayer player, Enchantment enchantment, EnchantmentRewardRule rule) {
        int total = 0;
        if (rule.scan_main_hand) total += getLevel(player.getMainHandItem(), enchantment);
        if (rule.scan_offhand) total += getLevel(player.getOffhandItem(), enchantment);
        if (rule.scan_armor) {
            for (ItemStack stack : player.getArmorSlots()) {
                total += getLevel(stack, enchantment);
            }
        }
        return Math.max(0, total);
    }

    private static int getLevel(ItemStack stack, Enchantment enchantment) {
        if (stack == null || stack.isEmpty() || enchantment == null) return 0;
        return Math.max(0, stack.getEnchantmentLevel(enchantment));
    }

    private static Enchantment getEnchantment(String id) {
        if (id == null || id.isBlank()) return null;
        try {
            return ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation(id));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean matches(Iterable<String> values, String candidate) {
        if (values == null) return true;
        String normalized = candidate == null ? "" : candidate.toLowerCase(Locale.ROOT);
        for (String value : values) {
            if (value == null) continue;
            String ruleValue = value.toLowerCase(Locale.ROOT);
            if ("*".equals(ruleValue) || ruleValue.equals(normalized)) return true;
        }
        return false;
    }
}
