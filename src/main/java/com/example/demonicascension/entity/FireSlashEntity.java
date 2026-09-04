package com.example.demonicascension.entity;

import com.example.demonicascension.config.ModConfigs;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A curved bolt of soulflame thrown out by swinging the Abyssal Sword while its
 * ignite ability is active. Short-ranged and single-target, unlike Soul Bolt —
 * it's a bonus on top of a melee swing, not a dedicated ranged attack.
 */
public class FireSlashEntity extends Projectile {

    private static final int MAX_LIFETIME = 16; // 0.8 seconds
    private float damage = 10.0F;

    public FireSlashEntity(EntityType<? extends FireSlashEntity> type, Level level) {
        super(type, level);
    }

    public FireSlashEntity(Level level, LivingEntity owner) {
        this(ModEntities.FIRE_SLASH.get(), level);
        this.setOwner(owner);
        this.setPos(owner.getX(), owner.getEyeY() - 0.2, owner.getZ());
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    /**
     * The rendered model (~2.6 blocks, entirely to one side of the entity's tracked
     * position) is far larger than the tiny 0.5×0.3 hitbox used for hit detection —
     * that hitbox is what Minecraft uses to decide whether this entity is on-screen at
     * all, so without this override the far end of the blade gets culled/clipped
     * whenever the near end is only just in view.
     */
    @Override
    public net.minecraft.world.phys.AABB getBoundingBoxForCulling() {
        return this.getBoundingBox().inflate(3.0);
    }

    /**
     * Faces the entity to its flight direction as soon as velocity is assigned —
     * called by AbilityHandler before the entity is ever added to the level — rather
     * than waiting for the first tick(). This entity lives only 16 ticks, so without
     * this the very first tracked frames (including the initial spawn packet, sent
     * before tick() ever runs) would show the default yRot/xRot of 0, which reads as
     * "always facing the same fixed direction" for a meaningful fraction of its life.
     */
    @Override
    public void setDeltaMovement(Vec3 motion) {
        super.setDeltaMovement(motion);
        faceMotion(motion);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        // No synced data needed — motion and position are handled by the base class.
    }

    @Override
    public void tick() {
        super.tick();

        if (this.tickCount > MAX_LIFETIME) {
            this.discard();
            return;
        }

        Vec3 motion = this.getDeltaMovement();
        faceMotion(motion);

        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);

        if (hit.getType() != HitResult.Type.MISS) {
            this.onHit(hit);
        }

        this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);

        spawnTrail();
    }

    /** Orients the entity to its own flight direction, so the 3D model's renderer can point and curve along it. */
    private void faceMotion(Vec3 motion) {
        double horizontal = motion.horizontalDistance();
        this.setYRot((float) (Mth.atan2(motion.x, motion.z) * (180D / Math.PI)));
        this.setXRot((float) (Mth.atan2(motion.y, horizontal) * (180D / Math.PI)));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    private void spawnTrail() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                this.getX(), this.getY(), this.getZ(), 2, 0.08, 0.08, 0.08, 0.01);
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && entity != this.getOwner();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);

        if (this.level().isClientSide()) {
            return;
        }

        if (!(result.getEntity() instanceof LivingEntity target)) {
            return;
        }

        var source = this.getOwner() instanceof LivingEntity owner
                ? this.damageSources().indirectMagic(this, owner)
                : this.damageSources().magic();

        target.hurt(source, this.damage);
        target.setRemainingFireTicks(ModConfigs.SWORD_SLASH_FIRE_TICKS.get());

        impactBurst();
        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);

        if (this.level().isClientSide()) {
            return;
        }

        impactBurst();
        this.discard();
    }

    private void impactBurst() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                this.getX(), this.getY(), this.getZ(), 12, 0.2, 0.2, 0.2, 0.05);
        level.playSound(null, this.blockPosition(),
                SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 0.6F, 1.4F);
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }
}
