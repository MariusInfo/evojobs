package org.evocraft.evojobs;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.evocraft.evojobs.client.ClientJobsData;
import org.evocraft.evojobs.client.ClientJobsData.JobInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class S2C_SyncJobsPacket {
    public static class JobSyncData {
        public String name; public int level; public double currentXp; public double requiredXp; public String iconId; public boolean isActive;
        public JobSyncData(String n, int l, double x, double r, String i, boolean a) { name=n; level=l; currentXp=x; requiredXp=r; iconId=i; isActive=a; }
    }

    private final List<JobSyncData> jobs;

    public S2C_SyncJobsPacket(List<JobSyncData> jobs) { this.jobs = jobs; }

    public S2C_SyncJobsPacket(FriendlyByteBuf buf) {
        jobs = new ArrayList<>();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            jobs.add(new JobSyncData(
                    buf.readUtf(), buf.readInt(), buf.readDouble(), buf.readDouble(), buf.readUtf(), buf.readBoolean()
            ));
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(jobs.size());
        for (JobSyncData job : jobs) {
            // AICI ERA BUBIȚA! Dacă venea ceva null, dădea "Invalid Player Data"
            buf.writeUtf(job.name != null ? job.name : "Unknown");
            buf.writeInt(job.level);
            buf.writeDouble(job.currentXp);
            buf.writeDouble(job.requiredXp);
            buf.writeUtf(job.iconId != null ? job.iconId : "minecraft:paper");
            buf.writeBoolean(job.isActive);
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            List<JobInfo> clientJobs = new ArrayList<>();
            for (JobSyncData data : jobs) {
                ItemStack icon = new ItemStack(Items.PAPER);
                if (data.iconId != null && !data.iconId.isEmpty()) {
                    String[] parts = data.iconId.split(":");
                    ResourceLocation loc = parts.length == 2 ? new ResourceLocation(parts[0], parts[1]) : new ResourceLocation("minecraft", parts[0]);
                    var item = ForgeRegistries.ITEMS.getValue(loc);
                    if (item != null) icon = new ItemStack(item);
                }
                clientJobs.add(new JobInfo(data.name, data.level, data.currentXp, data.requiredXp, icon, data.isActive));
            }
            ClientJobsData.updateJobs(clientJobs);
        });
        context.setPacketHandled(true);
        return true;
    }
}