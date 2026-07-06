package org.evocraft.evojobs.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.evocraft.evojobs.JobProgressionService;
import org.evocraft.evojobs.network.S2C_OpenQuestMenu;

public class QuestScreen extends Screen {

    private final S2C_OpenQuestMenu data;
    private static final int GUI_WIDTH = 460;
    private static final int GUI_HEIGHT = 300;

    // Evo V3.1 Supreme Colors
    private static final int BG_COLOR = 0xF00B120D;
    private static final int HEADER_BG = 0xAA101A12;
    private static final int BORDER_COLOR = 0xFF8FD867;
    private static final int CARD_BG = 0xCC121D14;
    private static final int CARD_BORDER = 0xFF456936;
    private static final int CARD_BG_CLAIMED = 0xCC17351A;
    private static final int TEXT_COLOR = 0xFFEAF1E6;
    private static final int MUTED_TEXT = 0xFF9BA999;
    private static final int PROGRESS_BG = 0xEE070907;
    private static final int PROGRESS_FILL = 0xFFFFAA00;
    private static final int PROGRESS_CLAIMED = 0xFF55FF55;

    public QuestScreen(S2C_OpenQuestMenu data) {
        super(Component.literal("Daily Quests"));
        this.data = data;
    }

    private void fillRounded(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x + 1, y, x + w - 1, y + h, color);
        g.fill(x, y + 1, x + w, y + h - 1, color);
    }

    private void outlineRounded(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x + 1, y, x + w - 1, y + 1, color);
        g.fill(x + 1, y + h - 1, x + w - 1, y + h, color);
        g.fill(x, y + 1, x + 1, y + h - 1, color);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        int guiWidth = Math.min(GUI_WIDTH, this.width - 34);
        int guiHeight = Math.min(GUI_HEIGHT, this.height - 34);
        int x = (this.width - guiWidth) / 2;
        int y = (this.height - guiHeight) / 2;

        // Main Background
        fillRounded(g, x, y, guiWidth, guiHeight, BG_COLOR);
        outlineRounded(g, x, y, guiWidth, guiHeight, BORDER_COLOR);
        g.fill(x + 2, y + 2, x + guiWidth - 2, y + 38, HEADER_BG);
        g.fill(x + 18, y + 43, x + guiWidth - 18, y + 44, 0x553A592D);

        // Header
        g.pose().pushPose();
        g.pose().translate(x + guiWidth / 2f, y + 11, 0);
        g.pose().scale(1.2f, 1.2f, 1.0f);
        g.drawCenteredString(this.font, "§lDAILY QUESTS", 0, 0, BORDER_COLOR);
        g.pose().popPose();

        g.drawCenteredString(this.font, "These quests reset every day at midnight.", x + guiWidth / 2, y + 28, MUTED_TEXT);

        int cardX = x + 18;
        int cardWidth = guiWidth - 36;
        int startY = y + 56;
        int cardHeight = 40;
        int spacing = 7;

        for (int i = 0; i < 5; i++) {
            if (i >= data.jobs.size() || data.jobs.get(i).isEmpty()) continue;

            int cardY = startY + (i * (cardHeight + spacing));
            boolean isClaimed = data.progressData[i * 2 + 1] == 1;
            int prog = data.progressData[i * 2];
            int req = data.requirements[i];

            // Card Background
            fillRounded(g, cardX, cardY, cardWidth, cardHeight, isClaimed ? CARD_BG_CLAIMED : CARD_BG);
            outlineRounded(g, cardX, cardY, cardWidth, cardHeight, CARD_BORDER);

            String jobName = formatJobName(data.jobs.get(i));
            String targetName = data.targets.get(i);

            // Draw Icon
            ItemStack icon = getQuestIcon(jobName, targetName);
            g.pose().pushPose();
            g.pose().translate(cardX + 16, cardY + 10, 0);
            g.pose().scale(1.3f, 1.3f, 1.0f);
            g.renderItem(icon, 0, 0);
            g.pose().popPose();

            int barWidth = Math.min(120, Math.max(88, cardWidth / 3));
            int barX = cardX + cardWidth - barWidth - 16;
            int barY = cardY + 15;
            int textX = cardX + 58;
            int textMaxWidth = Math.max(90, barX - textX - 12);

            // Text Info
            String rarityPrefix = data.rarities.get(i) + " §8| ";
            int rarityWidth = this.font.width(rarityPrefix);
            g.drawString(this.font, rarityPrefix, textX, cardY + 8, TEXT_COLOR, false);
            g.drawString(this.font, fitText("§f" + jobName + " §8- §7" + targetName, textMaxWidth - rarityWidth), textX + rarityWidth, cardY + 8, TEXT_COLOR, false);
            g.drawString(this.font, "§e" + JobProgressionService.formatMoney(data.money[i]) + " §8| §b" + (int)data.xp[i] + " XP", textX, cardY + 22, TEXT_COLOR, false);

            // Progress Bar
            fillRounded(g, barX, barY, barWidth, 10, PROGRESS_BG);

            float pct = req <= 0 ? 1.0f : Math.min(1.0f, (float)prog / req);
            int fillWidth = (int)(barWidth * pct);
            if (fillWidth > 0) {
                fillRounded(g, barX, barY, fillWidth, 10, isClaimed ? PROGRESS_CLAIMED : PROGRESS_FILL);
            }

            String progTxt = isClaimed ? "COMPLETED" : prog + " / " + req;
            g.pose().pushPose();
            g.pose().translate(barX + barWidth / 2f, barY + 2, 0);
            g.pose().scale(0.8f, 0.8f, 1.0f);
            g.drawCenteredString(this.font, progTxt, 0, 0, 0xFFFFFF);
            g.pose().popPose();
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    private String fitText(String text, int maxWidth) {
        if (maxWidth <= 10) return "";
        if (this.font.width(text) <= maxWidth) return text;
        String ellipsis = "...";
        String trimmed = this.font.plainSubstrByWidth(text, Math.max(10, maxWidth - this.font.width(ellipsis))).stripTrailing();
        return trimmed + ellipsis;
    }

    private String formatJobName(String jobId) {
        if (jobId == null || jobId.isBlank()) return "";
        return switch (jobId.toLowerCase()) {
            case "woodcutter" -> "Lumberjack";
            case "fierar" -> "Blacksmith";
            case "somer" -> "Unemployed";
            default -> jobId.substring(0, 1).toUpperCase() + jobId.substring(1);
        };
    }

    // Smart Icon Mapper based on target names
    private ItemStack getQuestIcon(String job, String target) {
        String t = target.toLowerCase();

        // Miner
        if (t.contains("coal")) return new ItemStack(Items.COAL_ORE);
        if (t.contains("iron")) return new ItemStack(Items.IRON_ORE);
        if (t.contains("diamond")) return new ItemStack(Items.DIAMOND_ORE);
        if (t.contains("gold")) return new ItemStack(Items.GOLD_ORE);
        if (t.contains("emerald")) return new ItemStack(Items.EMERALD_ORE);
        if (t.contains("lapis")) return new ItemStack(Items.LAPIS_ORE);
        if (t.contains("redstone")) return new ItemStack(Items.REDSTONE_ORE);
        if (t.contains("copper")) return new ItemStack(Items.COPPER_ORE);
        if (t.contains("netherite") || t.contains("debris")) return new ItemStack(Items.ANCIENT_DEBRIS);
        if (t.contains("quartz")) return new ItemStack(Items.NETHER_QUARTZ_ORE);

        // Woodcutter
        if (t.contains("oak")) return new ItemStack(Items.OAK_LOG);
        if (t.contains("spruce")) return new ItemStack(Items.SPRUCE_LOG);
        if (t.contains("birch")) return new ItemStack(Items.BIRCH_LOG);
        if (t.contains("jungle")) return new ItemStack(Items.JUNGLE_LOG);
        if (t.contains("acacia")) return new ItemStack(Items.ACACIA_LOG);
        if (t.contains("dark oak")) return new ItemStack(Items.DARK_OAK_LOG);
        if (t.contains("cherry")) return new ItemStack(Items.CHERRY_LOG);
        if (t.contains("mangrove")) return new ItemStack(Items.MANGROVE_LOG);
        if (t.contains("crimson")) return new ItemStack(Items.CRIMSON_STEM);
        if (t.contains("warped")) return new ItemStack(Items.WARPED_STEM);

        // Digger
        if (t.contains("dirt")) return new ItemStack(Items.DIRT);
        if (t.contains("sand")) return new ItemStack(Items.SAND);
        if (t.contains("gravel")) return new ItemStack(Items.GRAVEL);
        if (t.contains("clay")) return new ItemStack(Items.CLAY);
        if (t.contains("soul")) return new ItemStack(Items.SOUL_SAND);
        if (t.contains("mud")) return new ItemStack(Items.MUD);

        // Hunter
        if (t.contains("zombie")) return new ItemStack(Items.ZOMBIE_HEAD);
        if (t.contains("skeleton")) return new ItemStack(Items.SKELETON_SKULL);
        if (t.contains("creeper")) return new ItemStack(Items.CREEPER_HEAD);
        if (t.contains("spider")) return new ItemStack(Items.SPIDER_EYE);
        if (t.contains("enderman")) return new ItemStack(Items.ENDER_PEARL);
        if (t.contains("blaze")) return new ItemStack(Items.BLAZE_ROD);
        if (t.contains("slime")) return new ItemStack(Items.SLIME_BALL);
        if (t.contains("ghast")) return new ItemStack(Items.GHAST_TEAR);
        if (t.contains("piglin")) return new ItemStack(Items.PIGLIN_HEAD);

        // Farmer
        if (t.contains("wheat")) return new ItemStack(Items.WHEAT);
        if (t.contains("carrot")) return new ItemStack(Items.CARROT);
        if (t.contains("potato")) return new ItemStack(Items.POTATO);
        if (t.contains("beetroot")) return new ItemStack(Items.BEETROOT);
        if (t.contains("melon")) return new ItemStack(Items.MELON);
        if (t.contains("pumpkin")) return new ItemStack(Items.PUMPKIN);
        if (t.contains("sugar")) return new ItemStack(Items.SUGAR_CANE);

        // Builder
        if (t.contains("stone")) return new ItemStack(Items.STONE);
        if (t.contains("planks")) return new ItemStack(Items.OAK_PLANKS);
        if (t.contains("glass")) return new ItemStack(Items.GLASS);
        if (t.contains("brick")) return new ItemStack(Items.BRICKS);
        if (t.contains("concrete")) return new ItemStack(Items.WHITE_CONCRETE);
        if (t.contains("terracotta")) return new ItemStack(Items.TERRACOTTA);

        // Crafter
        if (t.contains("chest")) return new ItemStack(Items.CHEST);
        if (t.contains("furnace")) return new ItemStack(Items.FURNACE);
        if (t.contains("crafting")) return new ItemStack(Items.CRAFTING_TABLE);
        if (t.contains("stick")) return new ItemStack(Items.STICK);
        if (t.contains("torch")) return new ItemStack(Items.TORCH);
        if (t.contains("bread")) return new ItemStack(Items.BREAD);

        // Fisher & General Trader/Smith
        if (t.contains("fish")) return new ItemStack(Items.COD);
        if (t.contains("trade")) return new ItemStack(Items.EMERALD);
        if (t.contains("repair") || t.contains("anvil")) return new ItemStack(Items.ANVIL);

        // Fallbacks based on job type
        return switch (job.toLowerCase()) {
            case "miner" -> new ItemStack(Items.DIAMOND_PICKAXE);
            case "woodcutter" -> new ItemStack(Items.DIAMOND_AXE);
            case "digger" -> new ItemStack(Items.DIAMOND_SHOVEL);
            case "hunter" -> new ItemStack(Items.DIAMOND_SWORD);
            case "farmer" -> new ItemStack(Items.DIAMOND_HOE);
            case "fisherman" -> new ItemStack(Items.FISHING_ROD);
            case "builder" -> new ItemStack(Items.BRICK);
            case "crafter" -> new ItemStack(Items.CRAFTING_TABLE);
            case "fierar", "blacksmith" -> new ItemStack(Items.ANVIL);
            case "trader" -> new ItemStack(Items.EMERALD);
            default -> new ItemStack(Items.PAPER);
        };
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
