package org.evocraft.evojobs.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.evocraft.evojobs.quest.QuestManager;
import org.evocraft.evojobs.network.S2C_OpenQuestMenu;
import org.evocraft.evojobs.network.EvoJobsPacketHandler; // Importăm corect clasa ta de pachete

public class QuestNPC extends PathfinderMob {

    public QuestNPC(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setNoAi(true); // Îl ține locului, static
        this.setNoGravity(true);
        this.setInvulnerable(true);
        this.noPhysics = true;

        // Titlul de deasupra capului
        this.setCustomName(Component.literal("§e§lMisiuni Zilnice"));
        this.setCustomNameVisible(true);
        this.setPersistenceRequired(); // Nu va dispărea la restart / chunk unload
    }

    // Oprește despawn-ul la distanță
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    @Override
    public boolean isPushable() { return false; }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity entityIn) { }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        // Asigură-te că se execută o singură dată (pe MAIN_HAND)
        if (!this.level().isClientSide() && hand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer sp) {

            // Preluăm progresul din baza de date
            int[] progress = QuestManager.getPlayerProgress(sp);

            // FIX: Folosim metoda ta existentă 'sendToPlayer' care funcționează perfect pe 1.20.1!
            EvoJobsPacketHandler.sendToPlayer(new S2C_OpenQuestMenu(progress), sp);
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }
}