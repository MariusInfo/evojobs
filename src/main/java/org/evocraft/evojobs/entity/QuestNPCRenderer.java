package org.evocraft.evojobs.entity;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;

public class QuestNPCRenderer extends LivingEntityRenderer<QuestNPC, PlayerModel<QuestNPC>> {

    public QuestNPCRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(QuestNPC entity) {
        // Reads the custom skin from the mod resources.
        return new ResourceLocation("evojobs", "textures/entity/quest_npc.png");
    }
}
