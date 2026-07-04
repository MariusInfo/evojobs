package org.evocraft.evojobs;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.evocraft.evojobs.client.ClientJobsData;
import org.evocraft.evojobs.client.ClientJobsData.JobInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class S2C_SyncJobsPacket {
    public static class JobSyncData {
        public String name;
        public long level;
        public double currentXp;
        public double requiredXp;
        public String iconId;
        public boolean isActive;
        public double xpMultiplier;
        public double moneyMultiplier;
        public String attributeName;
        public double attributePower;
        public double attributeEffectPercent;

        public JobSyncData(String name, long level, double currentXp, double requiredXp, String iconId, boolean isActive,
                           double xpMultiplier, double moneyMultiplier, String attributeName, double attributePower,
                           double attributeEffectPercent) {
            this.name = name;
            this.level = level;
            this.currentXp = currentXp;
            this.requiredXp = requiredXp;
            this.iconId = iconId;
            this.isActive = isActive;
            this.xpMultiplier = xpMultiplier;
            this.moneyMultiplier = moneyMultiplier;
            this.attributeName = attributeName;
            this.attributePower = attributePower;
            this.attributeEffectPercent = attributeEffectPercent;
        }
    }

    private final List<JobSyncData> jobs;
    private final long totalJobLevel;
    private final long synergyLevel;

    public S2C_SyncJobsPacket(List<JobSyncData> jobs, long totalJobLevel, long synergyLevel) {
        this.jobs = jobs;
        this.totalJobLevel = totalJobLevel;
        this.synergyLevel = synergyLevel;
    }

    public S2C_SyncJobsPacket(FriendlyByteBuf buf) {
        totalJobLevel = buf.readLong();
        synergyLevel = buf.readLong();
        jobs = new ArrayList<>();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            jobs.add(new JobSyncData(
                    buf.readUtf(),
                    buf.readLong(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readUtf(),
                    buf.readBoolean(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readUtf(),
                    buf.readDouble(),
                    buf.readDouble()
            ));
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeLong(totalJobLevel);
        buf.writeLong(synergyLevel);
        buf.writeInt(jobs.size());
        for (JobSyncData job : jobs) {
            buf.writeUtf(job.name != null ? job.name : "Unknown");
            buf.writeLong(Math.max(1L, job.level));
            buf.writeDouble(job.currentXp);
            buf.writeDouble(job.requiredXp);
            buf.writeUtf(job.iconId != null ? job.iconId : "minecraft:paper");
            buf.writeBoolean(job.isActive);
            buf.writeDouble(job.xpMultiplier);
            buf.writeDouble(job.moneyMultiplier);
            buf.writeUtf(job.attributeName != null ? job.attributeName : "Job Attribute");
            buf.writeDouble(job.attributePower);
            buf.writeDouble(job.attributeEffectPercent);
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
                clientJobs.add(new JobInfo(
                        data.name,
                        data.level,
                        data.currentXp,
                        data.requiredXp,
                        icon,
                        data.isActive,
                        data.xpMultiplier,
                        data.moneyMultiplier,
                        data.attributeName,
                        data.attributePower,
                        data.attributeEffectPercent
                ));
            }
            ClientJobsData.updateJobs(clientJobs, totalJobLevel, synergyLevel);
        });
        context.setPacketHandled(true);
        return true;
    }
}
