package com.example.demonicascension.entity;

import com.example.demonicascension.demon.DemonData;
import com.example.demonicascension.demon.ModAttachments;
import com.example.demonicascension.dimension.AbyssManager;
import com.example.demonicascension.dimension.ModDimensions;
import com.example.demonicascension.network.ModNetworking;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.UUID;

public class RiftEntity extends Entity {

    /** Roughly five seconds. */
    public static final int LIFETIME = 100;

    /** Players can't use the rift for the first few ticks, so it visibly opens. */
    private static final int ARM_DELAY = 20;

    /**
     * Whose throne room this rift leads to — not necessarily the traveller's own.
     * Not synced: only {@link #travel} (server-only) ever reads it.
     */
    private UUID ownerId;

    public RiftEntity(EntityType<? extends RiftEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public RiftEntity(Level level, double x, double y, double z) {
        this(ModEntities.RIFT.get(), level);
        this.setPos(x, y, z);
    }

    public void setOwner(UUID ownerId) {
        this.ownerId = ownerId;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // Nothing to sync — clients derive the animation from tickCount.
    }

    @Override
    public void tick() {
        super.tick();

        if (this.tickCount > LIFETIME) {
            closeEffect();
            this.discard();
            return;
        }

        if (this.level().isClientSide()) {
            return;
        }

        serverAmbience();

        if (this.tickCount >= ARM_DELAY) {
            checkForTravellers();
        }
    }

    private void checkForTravellers() {
        AABB mouth = this.getBoundingBox().inflate(0.4);

        List<Player> nearby = this.level().getEntitiesOfClass(Player.class, mouth);
        for (Player player : nearby) {
            if (player instanceof ServerPlayer serverPlayer && travel(serverPlayer)) {
                // Single-use: the rift snaps shut behind whoever just stepped through,
                // rather than staying open for the rest of its LIFETIME.
                closeEffect();
                this.discard();
                return;
            }
        }
    }

    /** Sends the player through — into the abyss, or back out of it. Returns whether they actually travelled. */
    private boolean travel(ServerPlayer player) {
        var server = player.getServer();
        if (server == null) {
            return false;
        }

        DemonData data = player.getData(ModAttachments.DEMON_DATA);
        boolean inAbyss = player.level().dimension().equals(ModDimensions.ABYSS);

        if (inAbyss) {
            // --- Going home ---
            if (!data.hasReturnPoint()) {
                player.sendSystemMessage(Component
                        .literal("The way back is lost. You must find another road.")
                        .withStyle(ChatFormatting.RED));
                return false;
            }

            ResourceLocation dimId = ResourceLocation.tryParse(data.getReturnDimension().orElse(""));
            if (dimId == null) {
                return false;
            }

            ServerLevel destination = server.getLevel(
                    ResourceKey.create(Registries.DIMENSION, dimId));

            if (destination == null) {
                player.sendSystemMessage(Component
                        .literal("The way back has collapsed.")
                        .withStyle(ChatFormatting.RED));
                return false;
            }

            player.teleportTo(destination,
                    data.getReturnX(), data.getReturnY(), data.getReturnZ(),
                    player.getYRot(), player.getXRot());
            player.resetFallDistance();

            data.clearReturnPoint();
            player.setData(ModAttachments.DEMON_DATA, data);
            ModNetworking.syncToAll(player);

            player.sendSystemMessage(Component
                    .literal("You claw your way back into the world.")
                    .withStyle(ChatFormatting.DARK_PURPLE));

        } else {
            // --- Going in ---
            ServerLevel abyss = server.getLevel(ModDimensions.ABYSS);
            if (abyss == null) {
                return false;
            }

            data.setReturnPoint(
                    player.level().dimension().location().toString(),
                    player.getX(), player.getY(), player.getZ());
            player.setData(ModAttachments.DEMON_DATA, data);

            // Leads to whoever opened it, not necessarily the traveller's own hall —
            // falls back to the traveller's own if somehow unset.
            UUID destination = this.ownerId != null ? this.ownerId : player.getUUID();
            AbyssManager.sendToAbyss(player, abyss, destination);
            ModNetworking.syncToAll(player);

            player.sendSystemMessage(Component
                    .literal("The abyss opens, and it knows your name.")
                    .withStyle(ChatFormatting.DARK_PURPLE));
        }

        return true;
    }

    /** Sound only — the animated texture carries the visual on its own. */
    private void serverAmbience() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }

        if (this.tickCount % 30 == 0) {
            level.playSound(null, this.blockPosition(),
                    SoundEvents.PORTAL_AMBIENT, SoundSource.AMBIENT, 0.5F, 0.5F);
        }
    }

    /** One burst as the rift collapses. */
    private void closeEffect() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                this.getX(), this.getY() + 1.0, this.getZ(), 50, 0.3, 0.8, 0.3, 0.25);
        level.playSound(null, this.blockPosition(),
                SoundEvents.SOUL_ESCAPE.value(), SoundSource.AMBIENT, 1.0F, 0.5F);
    }

    // Rifts are scenery: no collision, no damage, no gravity.

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}