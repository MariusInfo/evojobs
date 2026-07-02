package org.evocraft.evojobs.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import org.evocraft.evojobs.JobMenu;

public class JobNPCEntity extends PathfinderMob {

    public JobNPCEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setNoAi(true); // NPC-ul stă pe loc
        this.setInvulnerable(true); // Nu ia damage
        this.setCustomName(Component.literal("§e§lCentrul de Angajări"));
        this.setCustomNameVisible(true);
        this.setPersistenceRequired(); // Nu dispare
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide() && hand == InteractionHand.MAIN_HAND) {
            // Deschide meniul de joburi pe ecranul jucătorului
            NetworkHooks.openScreen((ServerPlayer) player, new SimpleMenuProvider(
                    (id, inv, p) -> new JobMenu(id, inv),
                    Component.literal("Joburi")
            ));
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean isPushable() { return false; }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity entity) {}

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }
}