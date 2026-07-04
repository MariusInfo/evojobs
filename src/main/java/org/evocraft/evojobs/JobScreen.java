package org.evocraft.evojobs.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import org.evocraft.evojobs.JobMenu;
import org.evocraft.evojobs.JobProgressionService;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class JobScreen extends AbstractContainerScreen<JobMenu> {

    // --- SUPREME EVO V3.1 COLORS ---
    private static final int BG_COLOR = 0xEE0D140D;
    private static final int BORDER_COLOR = 0xFF83B755;
    private static final int CARD_BG = 0xAA141C14;
    private static final int CARD_BORDER = 0xFF3A592D;
    private static final int HOVER_COLOR = 0xFF6C9945;
    private static final int EDIT_COLOR = 0xFFFFAA00;
    private static final int TEXT_COLOR = 0xFFDDDDDD;

    private int selectedVisualSlot = -1;
    private float currentSlide = 0f;
    private float targetSlide = 0f;
    private final int infoPanelWidth = 200;

    private int currentPage = 0;

    private final List<CustomButton> buttons = new ArrayList<>();
    CustomButton btnClose, btnAction, btnPrev, btnNext;

    public JobScreen(JobMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 500;
        this.imageHeight = 335;
        this.titleLabelX = 10000;
        this.inventoryLabelX = 10000;
    }

    private float getScale() {
        double guiScale = this.minecraft != null ? this.minecraft.getWindow().getGuiScale() : 1.0;
        if (guiScale >= 4) return 0.65f;
        if (guiScale >= 3) return 0.85f;
        return 1.0f;
    }

    @Override
    protected void init() {
        super.init();
        buttons.clear();

        btnPrev = new CustomButton("◀ Previous Page", 0, 0, 120, 20, () -> { if (currentPage > 0) currentPage--; });
        btnNext = new CustomButton("Next Page ▶", 0, 0, 120, 20, () -> {
            int maxPages = (this.menu.availableJobs.size() - 1) / 12;
            if (currentPage < maxPages) currentPage++;
        });

        btnClose = new CustomButton("Close", 0, 0, 70, 20, this::onClose);

        btnAction = new CustomButton("\u00A7aGET HIRED", 0, 0, 160, 25, () -> {
            if (selectedVisualSlot != -1) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, selectedVisualSlot);
            }
        });

        buttons.addAll(List.of(btnPrev, btnNext, btnClose, btnAction));
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
    public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float delta) {
        currentSlide += (targetSlide - currentSlide) * 0.2f;
        if (Math.abs(currentSlide - targetSlide) < 0.005f) currentSlide = targetSlide;

        float scale = getScale();
        int sw = (int) (this.width / scale);
        int sh = (int) (this.height / scale);

        int shift = (int) (currentSlide * (infoPanelWidth / 2f + 5));
        this.leftPos = (sw - this.imageWidth) / 2 - shift;
        this.topPos = (sh - this.imageHeight) / 2;

        int smx = (int) (mouseX / scale);
        int smy = (int) (mouseY / scale);

        this.renderBackground(g);

        g.pose().pushPose();
        g.pose().scale(scale, scale, 1.0f);

        renderCustomBg(g, smx, smy);

        for (CustomButton b : buttons) {
            if (b.visible) b.render(g, smx, smy, this.font);
        }

        g.pose().popPose();
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics g, float pt, int mouseX, int mouseY) {}

    private void renderCustomBg(GuiGraphics g, int smx, int smy) {
        fillRounded(g, this.leftPos, this.topPos, this.imageWidth, this.imageHeight, BG_COLOR);
        outlineRounded(g, this.leftPos, this.topPos, this.imageWidth, this.imageHeight, BORDER_COLOR);

        g.pose().pushPose();
        g.pose().translate(this.leftPos + this.imageWidth / 2.0, this.topPos - 25, 0);
        g.pose().scale(1.5f, 1.5f, 1.5f);
        g.drawCenteredString(this.font, "EVO JOBS", 0, 0, BORDER_COLOR);
        g.pose().popPose();

        int maxPages = (this.menu.availableJobs.size() - 1) / 12;

        btnPrev.visible = maxPages > 0 && currentPage > 0;
        btnNext.visible = maxPages > 0 && currentPage < maxPages;

        if (maxPages > 0) {
            g.drawString(this.font, (currentPage + 1) + "/" + (maxPages + 1), this.leftPos + 35, this.topPos + this.imageHeight - 22, BORDER_COLOR, false);
        }

        CompoundTag totalsTag = getTotalsTag();
        if (totalsTag != null) {
            String totalText = "Total Job Level: " + formatLong(totalsTag.getLong("Total_Job_Level"));
            String synergyText = "Synergy: " + formatLong(totalsTag.getLong("Synergy_Level"));
            g.pose().pushPose();
            g.pose().translate(this.leftPos + 15, this.topPos + this.imageHeight - 43, 0);
            g.pose().scale(0.85f, 0.85f, 1.0f);
            g.drawString(this.font, totalText + "  |  " + synergyText, 0, 0, TEXT_COLOR, false);
            g.pose().popPose();
        }

        btnPrev.x = this.leftPos + 75; btnPrev.y = this.topPos + this.imageHeight - 28;
        btnNext.x = this.leftPos + 205; btnNext.y = this.topPos + this.imageHeight - 28;
        btnClose.x = this.leftPos + 415; btnClose.y = this.topPos + this.imageHeight - 28;

        int cardW = 150, cardH = 60, gapX = 10, gapY = 10;
        int startX = 15, startY = 15;

        int startIndex = currentPage * 12;
        int endIndex = Math.min(startIndex + 12, this.menu.availableJobs.size());

        for (int i = startIndex; i < endIndex; i++) {
            Slot slot = this.menu.slots.get(i);
            if (!slot.hasItem()) continue;

            int localIdx = i - startIndex;
            int col = localIdx % 3;
            int row = localIdx / 3;
            int cx = this.leftPos + startX + col * (cardW + gapX);
            int cy = this.topPos + startY + row * (cardH + gapY);

            boolean isHovered = smx >= cx && smx <= cx + cardW && smy >= cy && smy <= cy + cardH;
            int outline = (i == selectedVisualSlot) ? EDIT_COLOR : (isHovered ? HOVER_COLOR : CARD_BORDER);

            fillRounded(g, cx, cy, cardW, cardH, CARD_BG);
            outlineRounded(g, cx, cy, cardW, cardH, outline);

            ItemStack stack = slot.getItem();
            CompoundTag tag = stack.getTag();
            if (tag == null) continue;

            boolean isActive = tag.getBoolean("Job_Active");
            String name = tag.getString("Job_Name");
            long level = tag.getLong("Job_Level");
            double xp = tag.getDouble("Job_XP");
            double reqXp = tag.getDouble("Job_ReqXP");

            g.pose().pushPose();
            g.pose().translate(cx + 8, cy + 15, 100);
            g.pose().scale(1.7f, 1.7f, 1.0f);
            g.renderItem(stack, 0, 0);
            g.pose().popPose();

            g.drawString(this.font, name.toUpperCase(), cx + 42, cy + 8, TEXT_COLOR, false);

            int barW = 75;
            fillRounded(g, cx + 42, cy + 22, barW, 6, 0xFF333333);
            if (isActive && reqXp > 0) {
                int progressW = (int) ((xp / reqXp) * barW);
                fillRounded(g, cx + 42, cy + 22, Math.min(progressW, barW), 6, BORDER_COLOR);
                int percent = (int) ((xp / reqXp) * 100);
                g.pose().pushPose();
                g.pose().translate(cx + 42 + barW + 5, cy + 21, 0);
                g.pose().scale(0.85f, 0.85f, 1.0f);
                g.drawString(this.font, percent + "%", 0, 0, BORDER_COLOR, false);
                g.pose().popPose();
            } else {
                g.pose().pushPose();
                g.pose().translate(cx + 42 + barW + 5, cy + 21, 0);
                g.pose().scale(0.85f, 0.85f, 1.0f);
                g.drawString(this.font, "0%", 0, 0, 0xFFAAAAAA, false);
                g.pose().popPose();
            }

            g.pose().pushPose();
            g.pose().translate(cx + 42, cy + 38, 0);
            g.pose().scale(0.95f, 0.95f, 1.0f);
            if (isActive) {
                g.drawString(this.font, "Level: " + formatLong(level), 0, 0, TEXT_COLOR, false);
            } else {
                g.drawString(this.font, "Unemployed", 0, 0, 0xFFAAAAAA, false);
            }
            g.pose().popPose();
        }

        boolean showInfo = currentSlide > 0.01f && selectedVisualSlot != -1;
        btnAction.visible = showInfo;

        if (showInfo) {
            int infoX = this.leftPos + this.imageWidth + 8;
            int infoY = this.topPos;

            fillRounded(g, infoX, infoY, infoPanelWidth, this.imageHeight, BG_COLOR);
            outlineRounded(g, infoX, infoY, infoPanelWidth, this.imageHeight, BORDER_COLOR);

            ItemStack stack = this.menu.getSlot(selectedVisualSlot).getItem();
            if (!stack.isEmpty() && stack.hasTag()) {
                CompoundTag tag = stack.getTag();
                String jobId = tag.getString("Job_ID");
                String name = tag.getString("Job_Name");
                String desc = tag.getString("Job_Desc");
                boolean isActive = tag.getBoolean("Job_Active");
                long level = tag.getLong("Job_Level");
                double xp = tag.getDouble("Job_XP");
                double reqXp = tag.getDouble("Job_ReqXP");
                double xpMultiplier = tag.getDouble("Job_XPMult");
                double moneyMultiplier = tag.getDouble("Job_MoneyMult");
                String attributeName = tag.getString("Job_AttrName");
                double attributePower = tag.getDouble("Job_AttrPower");
                double attributeEffect = tag.getDouble("Job_AttrEffect");

                g.drawCenteredString(this.font, "JOB INFO: " + name.toUpperCase(), infoX + infoPanelWidth / 2, infoY + 10, BORDER_COLOR);

                g.pose().pushPose();
                g.pose().translate(infoX + infoPanelWidth / 2.0f, infoY + 65, 150);
                g.pose().scale(3.5f, 3.5f, 3.5f);
                g.renderItem(stack, -8, -8);
                g.pose().popPose();

                int descY = infoY + 105;
                g.pose().pushPose();
                g.pose().translate(infoX + 12, descY, 0);
                g.pose().scale(0.85f, 0.85f, 1.0f);

                List<net.minecraft.util.FormattedCharSequence> lines = this.font.split(Component.literal(desc), (int)((infoPanelWidth - 24) / 0.85f));
                for (int i = 0; i < lines.size(); i++) {
                    g.drawString(this.font, lines.get(i), 0, i * 10, 0xFFAAAAAA, false);
                }
                g.pose().popPose();

                int statsY = descY + (lines.size() * 9) + 5;

                g.pose().pushPose();
                g.pose().translate(infoX + 12, statsY, 0);
                g.pose().scale(0.85f, 0.85f, 1.0f);

                if (isActive) {
                    LocalRankInfo currentRank = getLocalRankInfo(jobId, level);
                    g.drawString(this.font, "RANK: \u00A76" + currentRank.title.toUpperCase() + " \u00A78(Lvl. " + formatLong(level) + ")", 0, 0, TEXT_COLOR, false);
                    g.drawString(this.font, "XP: \u00A7b" + formatNumber(xp) + " / " + formatNumber(reqXp), 0, 10, TEXT_COLOR, false);
                    g.drawString(this.font, "XP Multiplier: \u00A7bx" + formatMultiplier(xpMultiplier), 0, 20, TEXT_COLOR, false);
                    g.drawString(this.font, "Money Multiplier: \u00A7ax" + formatMultiplier(moneyMultiplier), 0, 30, TEXT_COLOR, false);
                    g.drawString(this.font, attributeName + ":", 0, 42, BORDER_COLOR, false);
                    g.drawString(this.font, formatNumber(attributePower) + " power / " + formatNumber(attributeEffect) + "% effect", 0, 52, TEXT_COLOR, false);

                    // ==========================================
                    // LIST OF PERKS (UNLOCKED OR LOCKED)
                    // ==========================================
                    g.drawString(this.font, "JOB PERKS:", 0, 70, BORDER_COLOR, false);
                    List<PerkInfo> perks = getJobPerks(jobId);
                    int py = 82;
                    for (PerkInfo p : perks) {
                        boolean unlocked = level >= p.lvl;
                        String prefix = unlocked ? "\u00A7aOK " : "\u00A7cLOCK ";
                        int color = unlocked ? 0xFFFFFFFF : 0xFFAAAAAA;
                        g.drawString(this.font, prefix + "[Lvl. " + p.lvl + "] " + p.shortText, 0, py, color, false);
                        py += 11;
                    }

                    btnAction.text = "\u00A7cRESIGN";
                } else {
                    g.drawString(this.font, "\u00A7cYou are not hired.", 0, 0, TEXT_COLOR, false);
                    g.drawString(this.font, "Level: \u00A7f" + formatLong(level), 0, 12, TEXT_COLOR, false);
                    g.drawString(this.font, attributeName + ": \u00A7f" + formatNumber(attributePower), 0, 24, TEXT_COLOR, false);

                    // Shows the perks even if not hired (all grayed out)
                    g.drawString(this.font, "JOB PERKS:", 0, 42, BORDER_COLOR, false);
                    List<PerkInfo> perks = getJobPerks(jobId);
                    int py = 54;
                    for (PerkInfo p : perks) {
                        g.drawString(this.font, "\u00A7cLOCK [Lvl. " + p.lvl + "] " + p.shortText, 0, py, 0xFFAAAAAA, false);
                        py += 11;
                    }

                    btnAction.text = "\u00A7aGET HIRED";
                }
                g.pose().popPose();

                btnAction.x = infoX + 20;
                btnAction.y = infoY + this.imageHeight - 35;
            }
        }
    }

    private String formatNumber(double num) {
        return JobProgressionService.formatNumber(num);
    }

    private String formatLong(long num) {
        if (num >= 1_000_000_000L) return String.format(Locale.US, "%.2fB", num / 1_000_000_000.0);
        if (num >= 1_000_000L) return String.format(Locale.US, "%.2fM", num / 1_000_000.0);
        if (num >= 1_000L) return String.format(Locale.US, "%,d", num);
        return Long.toString(num);
    }

    private String formatMultiplier(double multiplier) {
        if (!Double.isFinite(multiplier)) return "1.00";
        return String.format(Locale.US, "%.2f", multiplier);
    }

    private CompoundTag getTotalsTag() {
        for (Slot slot : this.menu.slots) {
            if (slot.hasItem() && slot.getItem().hasTag()) {
                return slot.getItem().getTag();
            }
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float scale = getScale();
        int smx = (int) (mouseX / scale);
        int smy = (int) (mouseY / scale);

        for (CustomButton b : buttons) {
            if (b.checkClick(smx, smy)) return true;
        }

        int cardW = 150, cardH = 60, gapX = 10, gapY = 10;
        int startX = 15, startY = 15;
        int startIndex = currentPage * 12;
        int endIndex = Math.min(startIndex + 12, this.menu.availableJobs.size());

        for (int i = startIndex; i < endIndex; i++) {
            Slot slot = this.menu.slots.get(i);
            if (!slot.hasItem()) continue;

            int localIdx = i - startIndex;
            int col = localIdx % 3;
            int row = localIdx / 3;
            int cx = this.leftPos + startX + col * (cardW + gapX);
            int cy = this.topPos + startY + row * (cardH + gapY);

            if (smx >= cx && smx <= cx + cardW && smy >= cy && smy <= cy + cardH) {
                if (selectedVisualSlot == i) {
                    targetSlide = 0f;
                    selectedVisualSlot = -1;
                } else {
                    selectedVisualSlot = i;
                    targetSlide = 1.0f;
                }
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    // ===============================================
    // DATA STRUCTURES FOR SHORT PERKS TEXTS
    // ===============================================
    private static class PerkInfo {
        int lvl; String shortText;
        PerkInfo(int l, String t) { lvl = l; shortText = t; }
    }

    private List<PerkInfo> getJobPerks(String jobId) {
        List<PerkInfo> list = new ArrayList<>();
        switch(jobId.toLowerCase()) {
            case "miner":
                list.add(new PerkInfo(25, "Haste I (Pickaxe)"));
                list.add(new PerkInfo(50, "VeinMiner (Shift)"));
                list.add(new PerkInfo(75, "Haste II (Pickaxe)"));
                list.add(new PerkInfo(100, "NightVision + 2x Loot"));
                break;
            case "woodcutter":
                list.add(new PerkInfo(25, "Haste I (Axe)"));
                list.add(new PerkInfo(50, "TreeCapitator (Shift)"));
                list.add(new PerkInfo(75, "Haste II + Speed (Forest)"));
                list.add(new PerkInfo(100, "Auto-Plant + 2x Wood"));
                break;
            case "digger":
                list.add(new PerkInfo(25, "Haste I (Shovel)"));
                list.add(new PerkInfo(50, "Excavator 3x3 (Shift)"));
                list.add(new PerkInfo(75, "Haste II (Shovel)"));
                list.add(new PerkInfo(100, "Archeologist (Treasures)"));
                break;
            case "fierar":
                list.add(new PerkInfo(25, "Hidden Unbreaking (15%)"));
                list.add(new PerkInfo(50, "Solid Anvil (20%)"));
                list.add(new PerkInfo(75, "50% Anvil XP Discount"));
                list.add(new PerkInfo(100, "Bypass 'Too Expensive'"));
                break;
            case "builder":
                list.add(new PerkInfo(25, "Architect's Vision"));
                list.add(new PerkInfo(50, "Extended Reach (+2 Blocks)"));
                list.add(new PerkInfo(75, "Light Step (Speed I)"));
                list.add(new PerkInfo(100, "Acrobat (No Fall Dmg)"));
                break;
            case "hunter":
                list.add(new PerkInfo(25, "Agility & Power (Sword)"));
                list.add(new PerkInfo(50, "Lifesteal (Half Heart)"));
                list.add(new PerkInfo(75, "Killer Instinct (Resist)"));
                list.add(new PerkInfo(100, "Loot Hunter (Heads & 2x)"));
                break;
            case "farmer":
                list.add(new PerkInfo(25, "Speed I (On Plantation)"));
                list.add(new PerkInfo(50, "Auto-Replant (Inventory)"));
                list.add(new PerkInfo(75, "Green Thumb (20% Double)"));
                list.add(new PerkInfo(100, "Nature's Aura (Growth)"));
                break;
            case "fisherman":
                list.add(new PerkInfo(25, "Hidden Luck of the Sea I"));
                list.add(new PerkInfo(50, "Dolphin's Grace & Resp."));
                list.add(new PerkInfo(75, "Hidden Lure (Bites)"));
                list.add(new PerkInfo(100, "Ocean's Prey (Treasures)"));
                break;
            case "crafter":
                list.add(new PerkInfo(25, "Free /craft Anywhere"));
                list.add(new PerkInfo(50, "Free /ec Anywhere"));
                list.add(new PerkInfo(75, "Recycler (10% material)"));
                list.add(new PerkInfo(100, "Double (15% 2x Craft)"));
                break;
            case "trader":
                list.add(new PerkInfo(25, "Hero of the Village I"));
                list.add(new PerkInfo(50, "Hero of the Village III"));
                list.add(new PerkInfo(75, "Charisma (2x Money/XP)"));
                list.add(new PerkInfo(100, "No Limits (Infinite Trade)"));
                break;
            case "somer":
                list.add(new PerkInfo(25, "Increased Welfare"));
                list.add(new PerkInfo(50, "Fool's Luck (Walking)"));
                list.add(new PerkInfo(75, "Max Welfare"));
                list.add(new PerkInfo(100, "Mega JackPot (50k Lottery)"));
                break;
        }
        return list;
    }

    private static class LocalRankInfo {
        String title; double boostPercent; long nextLevelReq;
        LocalRankInfo(String t, double b, long n) { title = t; boostPercent = b; nextLevelReq = n; }
    }

    private LocalRankInfo getLocalRankInfo(String jobId, long level) {
        String title = "Amateur";
        double boost = 0.0;
        long nextReq = 10;

        if (level >= 100) { boost = 200.0; nextReq = Long.MAX_VALUE; }
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
        return new LocalRankInfo(title, boost, nextReq);
    }

    class CustomButton {
        String text; int x, y, w, h; Runnable action; boolean visible = true;
        public CustomButton(String text, int x, int y, int w, int h, Runnable action) {
            this.text = text; this.x = x; this.y = y; this.w = w; this.h = h; this.action = action;
        }
        public void render(GuiGraphics g, int mx, int my, net.minecraft.client.gui.Font font) {
            boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + h;
            fillRounded(g, x, y, w, h, CARD_BG);
            outlineRounded(g, x, y, w, h, hover ? HOVER_COLOR : CARD_BORDER);
            g.pose().pushPose();
            g.pose().translate(x + w / 2f, y + (h - 8) / 2f, 0);
            g.drawCenteredString(font, text, 0, 0, hover ? 0xFFFFFF : TEXT_COLOR);
            g.pose().popPose();
        }
        public boolean checkClick(int mx, int my) {
            if (visible && mx >= x && mx <= x + w && my >= y && my <= y + h) {
                action.run(); return true;
            }
            return false;
        }
    }
}
