package org.evocraft.evojobs.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.evocraft.evojobs.Evojobs;

public class EvoJobsEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Evojobs.MODID);

    public static final RegistryObject<EntityType<JobNPCEntity>> JOB_NPC = ENTITIES.register("job_npc",
            () -> EntityType.Builder.of(JobNPCEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F) // Normal player size.
                    .clientTrackingRange(8)
                    .build("job_npc"));

    // --- NOU: ENTITATEA PENTRU QUEST-URI (ASTA LIPSEA!) ---
    public static final RegistryObject<EntityType<QuestNPC>> QUEST_NPC = ENTITIES.register("quest_npc",
            () -> EntityType.Builder.of(QuestNPC::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(8)
                    .build("quest_npc"));
}
