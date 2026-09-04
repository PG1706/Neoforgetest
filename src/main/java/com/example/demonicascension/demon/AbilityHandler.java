package com.example.demonicascension.demon;

import com.example.demonicascension.config.ModConfigs;
import com.example.demonicascension.entity.FireSlashEntity;
import com.example.demonicascension.entity.RiftEntity;
import com.example.demonicascension.entity.SoulBoltEntity;
import com.example.demonicascension.event.EclipseHandler;
import com.example.demonicascension.item.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class AbilityHandler {

    // --- Abyssal Dash tuning (not config-exposed; a fixed part of the ability's feel) ---
    private static final double DASH_POWER = 3.0;

    // --- Bolt tuning (not config-exposed; a fixed part of the ability's feel) ---
    private static final double BOLT_SPEED = 1.6;

    // --- Fire slash tuning (not config-exposed; a fixed part of the ability's feel) ---
    private static final double SLASH_SPEED = 1.4;

    /** 1.0 normally; the configured eclipse multiplier while that ultimate's buff window is open. */
    private static float eclipseMultiplier(ServerPlayer player) {
        DemonData data = player.getData(ModAttachments.DEMON_DATA);
        return data.isEclipseBuffActive(player.level().getGameTime())
                ? ModConfigs.ECLIPSE_BUFF_MULTIPLIER.get().floatValue()
                : 1.0F;
    }

    /** How long the light-source entity lingers, in ticks. */
    private static final int LIGHT_LIFETIME = 30; // 1.5 seconds

    // ==================== FEEDBACK ====================

    /** Action bar message plus a dull thud, so a failed press never feels broken. */
    private static void deny(ServerPlayer player, String message) {
        player.displayClientMessage(
                Component.literal(message).withStyle(ChatFormatting.DARK_RED), true);
        player.playNotifySound(SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6F, 0.8F);
    }

    private static void denyCooldown(ServerPlayer player, String abilityName, long ticksLeft) {
        // Round up, so "1s" never shows when it's really about to be ready.
        long seconds = (ticksLeft + 19) / 20;
        player.displayClientMessage(
                Component.literal(abilityName + " — " + seconds + "s")
                        .withStyle(ChatFormatting.GRAY), true);
        player.playNotifySound(SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.5F, 1.2F);
    }

    private static void denyNotTransformed(ServerPlayer player) {
        deny(player, "Only the demon may call upon this.");
    }

    // ==================== TRANSFORM SLOT ====================

    /** Toggles the demon form for an ascended player without needing the Abyssal Soul in hand. */
    public static void useTransformSlot(ServerPlayer player) {
        DemonData data = player.getData(ModAttachments.DEMON_DATA);

        if (!data.hasAscended()) {
            deny(player, "You have not ascended.");
            return;
        }

        DemonFormHandler.toggleTransform(player);
    }

    // ==================== DASH SLOT ====================

    public static void useDashSlot(ServerPlayer player) {
        DemonData data = player.getData(ModAttachments.DEMON_DATA);

        if (!data.isTransformed()) {
            denyNotTransformed(player);
            return;
        }

        boolean hasVoidstep = data.hasSkill(DemonSkill.VOIDSTEP);
        boolean hasDash = data.hasSkill(DemonSkill.ABYSSAL_DASH);

        if (!hasVoidstep && !hasDash) {
            deny(player, "You have not learned this power.");
            return;
        }

        long now = player.level().getGameTime();
        if (!data.isDashReady(now)) {
            denyCooldown(player, hasVoidstep ? "Voidstep" : "Abyssal Dash",
                    data.getDashRemaining(now));
            return;
        }

        if (hasVoidstep) {
            voidstep(player);
            data.setDashCooldown(now, ModConfigs.VOIDSTEP_COOLDOWN_TICKS.get());
        } else {
            abyssalDash(player);
            data.setDashCooldown(now, ModConfigs.DASH_COOLDOWN_TICKS.get());
        }

        player.setData(ModAttachments.DEMON_DATA, data);
    }

    private static void abyssalDash(ServerPlayer player) {
        Vec3 look = player.getLookAngle().normalize();
        Vec3 start = player.position();

        // The slight upward bias stops the dash grinding into the floor.
        player.setDeltaMovement(look.x * DASH_POWER,
                Math.max(look.y * DASH_POWER, 0.15),
                look.z * DASH_POWER);
        player.hurtMarked = true; // forces the velocity change to sync to the client

        player.invulnerableTime = 40;

        Vec3 end = start.add(look.scale(DASH_POWER * 4));
        AABB sweep = new AABB(start, end).inflate(ModConfigs.DASH_HIT_RADIUS.get());

        List<LivingEntity> hits = player.level().getEntitiesOfClass(
                LivingEntity.class, sweep, e -> e != player && e.isAlive());

        float dashDamage = ModConfigs.DASH_DAMAGE.get().floatValue() * eclipseMultiplier(player);
        for (LivingEntity target : hits) {
            target.hurt(player.damageSources().playerAttack(player), dashDamage);
            Vec3 push = target.position().subtract(start).normalize().scale(1.8);
            target.setDeltaMovement(push.x, 0.6, push.z);
            target.hurtMarked = true;
        }

        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    start.x, start.y + 1.0, start.z, 40, 0.4, 0.6, 0.4, 0.15);
            level.playSound(null, player.blockPosition(),
                    SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.7F, 1.4F);
        }
    }

    private static void voidstep(ServerPlayer player) {
        Vec3 origin = player.position();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 far = eye.add(look.scale(ModConfigs.VOIDSTEP_RANGE.get()));

        // Raycast so we land in front of a wall rather than inside it.
        BlockHitResult hit = player.level().clip(new ClipContext(
                eye, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

        Vec3 destination;
        if (hit.getType() == HitResult.Type.MISS) {
            destination = far;
        } else {
            destination = hit.getLocation().subtract(look.scale(1.5));
        }

        departureEffect(player, origin);

        player.teleportTo(destination.x, destination.y, destination.z);
        player.resetFallDistance();

        arrivalBurst(player, player.position());

        if (player.level() instanceof ServerLevel level) {
            level.playSound(null, player.blockPosition(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.6F);
        }
    }

    /** Subtle wisps left behind where the player vanished. No damage here. */
    private static void departureEffect(ServerPlayer player, Vec3 center) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        level.sendParticles(ParticleTypes.SOUL,
                center.x, center.y + 1.0, center.z, 25, 0.4, 0.8, 0.4, 0.02);
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 0.7F, 0.8F);
    }

    /** Big soul fire bloom at the destination, plus the damage. */
    private static void arrivalBurst(ServerPlayer player, Vec3 center) {
        AABB area = new AABB(center, center).inflate(ModConfigs.VOIDSTEP_RADIUS.get());

        List<LivingEntity> hits = player.level().getEntitiesOfClass(
                LivingEntity.class, area, e -> e != player && e.isAlive());

        float voidstepDamage = ModConfigs.VOIDSTEP_DAMAGE.get().floatValue() * eclipseMultiplier(player);
        for (LivingEntity target : hits) {
            target.hurt(player.damageSources().magic(), voidstepDamage);
        }

        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        // Dense core.
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                center.x, center.y + 1.0, center.z, 150, 1.2, 1.0, 1.2, 0.08);

        // Outward shell, thrown wider and faster.
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                center.x, center.y + 1.0, center.z, 100, 2.5, 1.5, 2.5, 0.35);

        // Drifting wisps for the lingering afterglow.
        level.sendParticles(ParticleTypes.SOUL,
                center.x, center.y + 1.0, center.z, 60, 2.0, 1.2, 2.0, 0.05);

        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.4F, 0.5F);
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.FIRE_AMBIENT, SoundSource.PLAYERS, 1.2F, 0.6F);

        spawnTemporaryLight(level, center);
    }

    /**
     * Drops a short-lived glowstone item at the arrival point. Dynamic light
     * mods illuminate dropped items, so this briefly lights the area without
     * placing any blocks. Harmless if no such mod is installed.
     */
    private static void spawnTemporaryLight(ServerLevel level, Vec3 center) {
        ItemEntity light = new ItemEntity(level,
                center.x, center.y + 1.0, center.z,
                new ItemStack(Items.GLOWSTONE));

        light.setNoGravity(true);
        light.setNeverPickUp();
        light.setInvulnerable(true);
        light.setSilent(true);
        light.setUnlimitedLifetime();

        level.addFreshEntity(light);

        level.getServer().execute(() -> scheduleRemoval(level, light, LIGHT_LIFETIME));
    }

    /** Removes the light entity after the given number of ticks. */
    private static void scheduleRemoval(ServerLevel level, ItemEntity entity, int ticksLeft) {
        if (ticksLeft <= 0) {
            entity.discard();
            return;
        }
        level.getServer().execute(() -> scheduleRemoval(level, entity, ticksLeft - 1));
    }

    // ==================== BOLT SLOT ====================

    public static void useBoltSlot(ServerPlayer player) {
        DemonData data = player.getData(ModAttachments.DEMON_DATA);

        if (!data.isTransformed()) {
            denyNotTransformed(player);
            return;
        }

        boolean hasBarrage = data.hasSkill(DemonSkill.HELLFIRE_BARRAGE);
        boolean hasBolt = data.hasSkill(DemonSkill.SOUL_BOLT);

        if (!hasBarrage && !hasBolt) {
            deny(player, "You have not learned this power.");
            return;
        }

        long now = player.level().getGameTime();
        if (!data.isBolterReady(now)) {
            denyCooldown(player, hasBarrage ? "Hellfire Barrage" : "Soul Bolt",
                    data.getBolterRemaining(now));
            return;
        }

        if (hasBarrage) {
            fireBarrage(player);
            data.setBolterCooldown(now, ModConfigs.BARRAGE_COOLDOWN_TICKS.get());
        } else {
            fireBolt(player, 0.0);
            data.setBolterCooldown(now, ModConfigs.BOLT_COOLDOWN_TICKS.get());
        }

        player.setData(ModAttachments.DEMON_DATA, data);
    }

    private static void fireBolt(ServerPlayer player, double spread) {
        Vec3 look = player.getLookAngle();

        SoulBoltEntity bolt = new SoulBoltEntity(player.level(), player);
        bolt.setDamage(ModConfigs.BOLT_DAMAGE.get().floatValue() * eclipseMultiplier(player));

        Vec3 velocity = look.scale(BOLT_SPEED);
        if (spread > 0.0) {
            var rng = player.getRandom();
            velocity = velocity.add(
                    (rng.nextDouble() - 0.5) * spread,
                    (rng.nextDouble() - 0.5) * spread,
                    (rng.nextDouble() - 0.5) * spread);
        }
        bolt.setDeltaMovement(velocity);

        player.level().addFreshEntity(bolt);

        player.level().playSound(null, player.blockPosition(),
                SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 0.9F, 1.3F);
    }

    private static void fireBarrage(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        var rng = player.getRandom();
        float barrageDamage = ModConfigs.BARRAGE_BOLT_DAMAGE.get().floatValue() * eclipseMultiplier(player);

        for (int i = 0; i < ModConfigs.BARRAGE_BOLT_COUNT.get(); i++) {
            SoulBoltEntity bolt = new SoulBoltEntity(player.level(), player);

            bolt.setExplosive(true);
            bolt.setHoming(true);
            bolt.setDamage(barrageDamage);

            Vec3 velocity = look.scale(BOLT_SPEED).add(
                    (rng.nextDouble() - 0.5) * 0.35,
                    (rng.nextDouble() - 0.5) * 0.35,
                    (rng.nextDouble() - 0.5) * 0.35);
            bolt.setDeltaMovement(velocity);

            player.level().addFreshEntity(bolt);
        }

        player.level().playSound(null, player.blockPosition(),
                SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 1.2F, 0.7F);
    }

    // ==================== RIFT SLOT ====================

    public static void useRiftSlot(ServerPlayer player) {
        DemonData data = player.getData(ModAttachments.DEMON_DATA);

        if (!data.isTransformed()) {
            denyNotTransformed(player);
            return;
        }

        if (!data.hasSkill(DemonSkill.ABYSSAL_RIFT)) {
            deny(player, "The abyss does not yet answer to you.");
            return;
        }

        long now = player.level().getGameTime();
        if (!data.isRiftReady(now)) {
            denyCooldown(player, "Abyssal Rift", data.getRiftRemaining(now));
            return;
        }

        // Open it a couple of blocks ahead, at the player's feet.
        Vec3 look = player.getLookAngle();
        Vec3 spot = player.position().add(look.x * 2.0, 0.0, look.z * 2.0);

        RiftEntity rift = new RiftEntity(player.level(), spot.x, spot.y, spot.z);
        rift.setYRot(player.getYRot());
        rift.setOwner(player.getUUID());
        player.level().addFreshEntity(rift);

        data.setRiftCooldown(now, ModConfigs.RIFT_COOLDOWN_TICKS.get());
        player.setData(ModAttachments.DEMON_DATA, data);

        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    spot.x, spot.y + 1.0, spot.z, 80, 0.4, 1.0, 0.4, 0.3);
            level.playSound(null, player.blockPosition(),
                    SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.8F, 0.4F);
        }
    }

    // ==================== ECLIPSE SLOT ====================

    public static void useEclipseSlot(ServerPlayer player) {
        DemonData data = player.getData(ModAttachments.DEMON_DATA);

        if (!data.isTransformed()) {
            denyNotTransformed(player);
            return;
        }

        if (!data.hasSkill(DemonSkill.ABYSSAL_ECLIPSE)) {
            deny(player, "The abyss does not yet answer to you.");
            return;
        }

        long now = player.level().getGameTime();
        if (!data.isEclipseReady(now)) {
            denyCooldown(player, "Abyssal Eclipse", data.getEclipseRemaining(now));
            return;
        }

        MinecraftServer server = player.getServer();
        ServerLevel overworld = server != null ? server.getLevel(Level.OVERWORLD) : null;
        if (overworld == null) {
            return;
        }

        int durationTicks = ModConfigs.ECLIPSE_DURATION_TICKS.get();
        data.activateEclipse(now, durationTicks, ModConfigs.ECLIPSE_COOLDOWN_TICKS.get());
        player.setData(ModAttachments.DEMON_DATA, data);

        EclipseHandler.activate(player, overworld, durationTicks);

        player.sendSystemMessage(Component
                .literal("You call out, and the abyss answers — the sky itself blackens.")
                .withStyle(ChatFormatting.DARK_PURPLE));
    }

    // ==================== SWORD FIRE SLASH ====================

    /**
     * Called on every sword swing (both an air swing and a swing that connects — see
     * {@link com.example.demonicascension.network.ServerPayloadHandler#handleAbility}
     * and {@link com.example.demonicascension.event.SoulHarvestEvents#onAttackEntity}).
     * Silently does nothing outside the ignite window or its own short cooldown — this
     * isn't a deliberate keypress, so there's no denial feedback to show.
     */
    public static void trySwordSlash(ServerPlayer player, Vec3 direction) {
        if (!player.getMainHandItem().is(ModItems.ABYSSAL_SWORD.get())) {
            return;
        }

        DemonData data = player.getData(ModAttachments.DEMON_DATA);
        long now = player.level().getGameTime();

        if (!data.isSwordIgniteActive(now) || !data.isSlashReady(now)) {
            return;
        }

        data.setSlashCooldown(now, ModConfigs.SWORD_SLASH_COOLDOWN_TICKS.get());
        player.setData(ModAttachments.DEMON_DATA, data);

        FireSlashEntity slash = new FireSlashEntity(player.level(), player);
        slash.setDamage(ModConfigs.SWORD_SLASH_DAMAGE.get().floatValue());
        slash.setDeltaMovement(direction.normalize().scale(SLASH_SPEED));

        player.level().addFreshEntity(slash);

        player.level().playSound(null, player.blockPosition(),
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.7F, 1.6F);
    }
}