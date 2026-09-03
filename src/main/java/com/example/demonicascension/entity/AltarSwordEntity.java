package com.example.demonicascension.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The sword found on the throne room altar: hovers in place rather than falling and
 * bobbing like a normal dropped item, and is claimed only by right-clicking it — a
 * deliberate moment rather than an incidental walk-over pickup.
 */
public class AltarSwordEntity extends ItemEntity {

    public AltarSwordEntity(EntityType<? extends AltarSwordEntity> type, Level level) {
        super(type, level);
    }

    public AltarSwordEntity(Level level, double x, double y, double z, ItemStack stack) {
        this(ModEntities.ALTAR_SWORD.get(), level);
        this.setPos(x, y, z);
        this.setItem(stack);
        this.setNoGravity(true);
        this.setUnlimitedLifetime();
    }

    /** Walking into it does nothing — claiming it takes a deliberate right-click. */
    @Override
    public void playerTouch(Player player) {
    }

    /**
     * Entity.isPickable() defaults to false, and ItemEntity never overrides it since a
     * normal dropped item isn't meant to be right-clicked — but that's exactly the flag
     * the crosshair's entity targeting checks, so without this override interact() is
     * never even reached.
     */
    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (this.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ItemStack stack = this.getItem().copy();
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }

        ServerLevel level = (ServerLevel) this.level();
        level.playSound(null, this.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                this.getX(), this.getY() + 0.5, this.getZ(), 20, 0.3, 0.3, 0.3, 0.05);

        this.discard();
        return InteractionResult.CONSUME;
    }
}
