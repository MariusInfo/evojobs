package org.evocraft.evojobs;

import org.evocraft.evojobs.init.EvoJobsMenuTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JobMenu extends AbstractContainerMenu {
    public final SimpleContainer container;
    public final Player player;
    public final List<JobDefinition> availableJobs;

    public JobMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(27));
    }

    public JobMenu(int containerId, Inventory playerInventory, SimpleContainer container) {
        super(EvoJobsMenuTypes.JOB_MENU.get(), containerId);
        this.container = container;
        this.player = playerInventory.player;
        this.availableJobs = new ArrayList<>(JobConfigManager.get().getAllJobs());

        for (int i = 0; i < 27; i++) {
            this.addSlot(new Slot(container, i, -10000, -10000) {
                @Override public boolean mayPickup(Player p) { return false; }
                @Override public boolean mayPlace(ItemStack s) { return false; }
            });
        }

        if (!player.level().isClientSide) {
            populateJobs();
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= 0 && id < availableJobs.size()) {
            JobDefinition job = availableJobs.get(id);
            Map<String, JobData> activeJobs = JobManager.get().getActiveJobs(player.getUUID());

            if (activeJobs.containsKey(job.id)) {
                JobManager.get().leaveJob(player.getUUID(), job.id);
                player.sendSystemMessage(Component.literal("§cYou resigned from the position of " + job.displayName));
            } else {
                if (activeJobs.size() >= 3) {
                    player.sendSystemMessage(Component.literal("§cYou reached the limit of 3 jobs!"));
                } else {
                    JobManager.get().joinJob(player.getUUID(), job.id);
                    player.sendSystemMessage(Component.literal("§aCongratulations! You got hired as " + job.displayName));
                }
            }
            populateJobs();
            return true;
        }
        return false;
    }

    public void populateJobs() {
        container.clearContent();
        Map<String, JobData> history = JobManager.get().getAllJobsHistory(player.getUUID());

        for (int i = 0; i < availableJobs.size(); i++) {
            JobDefinition job = availableJobs.get(i);
            String[] parts = job.icon.split(":");
            ResourceLocation loc = parts.length == 2 ? new ResourceLocation(parts[0], parts[1]) : new ResourceLocation("minecraft", parts[0]);
            Item item = ForgeRegistries.ITEMS.getValue(loc);
            if (item == null) item = Items.BARRIER;

            ItemStack displayStack = new ItemStack(item);
            CompoundTag tag = displayStack.getOrCreateTag();

            tag.putString("Job_ID", job.id);
            tag.putString("Job_Name", job.displayName);
            tag.putString("Job_Desc", job.description);
            tag.putInt("Job_Index", i);

            JobData data = history.get(job.id);
            boolean isActive = (data != null && data.isActive);
            tag.putBoolean("Job_Active", isActive);

            if (data != null) {
                tag.putInt("Job_Level", data.level);
                tag.putDouble("Job_XP", data.xp);
                tag.putDouble("Job_ReqXP", data.getRequiredXp());
            }
            container.setItem(i, displayStack);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
    @Override
    public boolean stillValid(Player player) { return true; }
}