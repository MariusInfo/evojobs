package org.evocraft.evojobs.entity;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.evocraft.evojobs.Evojobs;

public class JobNPCRenderer extends MobRenderer<JobNPCEntity, PlayerModel<JobNPCEntity>> {
    // Aici citește textura. Trebuie să pui o textură la: src/main/resources/assets/evojobs/textures/entity/job_npc.png
    private static final ResourceLocation TEXTURE = new ResourceLocation(Evojobs.MODID, "textures/entity/job_npc.png");

    public JobNPCRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(JobNPCEntity entity) {
        return TEXTURE;
    }
}