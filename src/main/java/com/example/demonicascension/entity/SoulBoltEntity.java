package com.example.demonicascension.entity;

import com.example.demonicascension.config.ModConfigs;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class SoulBoltEntity extends Projectile {

    private static final float BASE_DAMAGE = 18.0F;
    private static final int MAX_LIFETIME = 100;    // 5 seconds
    private static final int MAX_PIERCE = 4;

    private int pierceCount = 0;
    private boolean explosive = false;   // set by Hellfire Barrage
    private boolean homing = false;      // set by Hellfire Barrage
    private float damage = BASE_DAMAGE;

    public SoulBoltEntity(EntityType<? extends SoulBoltEntity> type, Level level) {
        super(type, level);
    }

    public SoulBoltEntity(Level level, LivingEntity owner) {
        this(ModEntities.SOUL_BOLT.get(), level);
        this.setOwner(owner);
        this.setPos(owner.getX(), owner.getEyeY() - 0.2, owner.getZ());
    }

    public void setExplosive(boolean explosive) {
        this.explosive = explosive;
    }

    public void setHoming(boolean homing) {
        this.homing = homing;
    }

    public void setDamage(float damage) {
        this.damage = damage;
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

        if (this.homing) {
            steerTowardTarget();
        }

        // Move, checking for collisions along the way.
        Vec3 motion = this.getDeltaMovement();
        var hit = net.minecraft.world.entity.projectile.ProjectileUtil.getHitResultOnMoveVector(
                this, this::canHitEntity);

        if (hit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
            this.onHit(hit);
        }

        this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);

        // Bolts fly straight — no gravity, slight drag.
        this.setDeltaMovement(motion.scale(0.99));

        spawnTrail();
    }

    private void steerTowardTarget() {
        // Look for something to curve toward within 12 blocks.
        LivingEntity target = this.level().getNearestEntity(
                LivingEntity.class,
                net.minecraft.world.entity.ai.targeting.TargetingConditions.forCombat()
                        .ignoreLineOfSight(),
                null,
                this.getX(), this.getY(), this.getZ(),
                this.getBoundingBox().inflate(12.0));

        if (target == null || target == this.getOwner() || !target.isAlive()) {
            return;
        }

        Vec3 toTarget = target.getEyePosition().subtract(this.position()).normalize();
        Vec3 motion = this.getDeltaMovement();
        double speed = motion.length();

        // Blend current heading toward the target for a curved arc.
        Vec3 steered = motion.normalize().scale(0.85).add(toTarget.scale(0.15)).normalize();
        this.setDeltaMovement(steered.scale(speed));
    }

    private void spawnTrail() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                this.getX(), this.getY(), this.getZ(), 3, 0.05, 0.05, 0.05, 0.01);
        level.sendParticles(ParticleTypes.SOUL,
                this.getX(), this.getY(), this.getZ(), 1, 0.1, 0.1, 0.1, 0.0);
    }

    @Override
    protected boolean canHitEntity(net.minecraft.world.entity.Entity entity) {
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
        target.setRemainingFireTicks(100); // 5 seconds

        // A fraction of the damage returns to the caster.
        if (this.getOwner() instanceof LivingEntity owner) {
            owner.heal(this.damage * ModConfigs.BOLT_LIFESTEAL.get().floatValue());
        }

        if (this.explosive) {
            detonate();
            this.discard();
            return;
        }

        // Otherwise pierce through, up to a limit.
        this.pierceCount++;
        if (this.pierceCount >= MAX_PIERCE) {
            this.discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);

        if (this.level().isClientSide()) {
            return;
        }

        if (this.explosive) {
            detonate();
        } else {
            impactBurst();
        }
        this.discard();
    }

    /** Hellfire Barrage: area damage on impact, no block destruction. */
    private void detonate() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }

        var area = this.getBoundingBox().inflate(3.0);
        var caught = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != this.getOwner() && e.isAlive());

        var source = this.getOwner() instanceof LivingEntity owner
                ? this.damageSources().indirectMagic(this, owner)
                : this.damageSources().magic();

        for (LivingEntity victim : caught) {
            victim.hurt(source, this.damage * 0.6F);
            victim.setRemainingFireTicks(80);
        }

        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                this.getX(), this.getY(), this.getZ(), 60, 1.0, 1.0, 1.0, 0.2);
        level.playSound(null, this.blockPosition(),
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.2F, 0.6F);
    }

    private void impactBurst() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                this.getX(), this.getY(), this.getZ(), 25, 0.3, 0.3, 0.3, 0.1);
        level.playSound(null, this.blockPosition(),
                SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 0.8F, 0.7F);
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }
}