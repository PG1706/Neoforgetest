package com.example.demonicascension.demon;

import com.example.demonicascension.DemonicAscension;
import com.example.demonicascension.compat.WingsIntegration;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

public class DemonFormHandler {

    // Base form modifiers
    private static final ResourceLocation HEALTH_ID = id("demon_health");
    private static final ResourceLocation SPEED_ID = id("demon_speed");
    private static final ResourceLocation DAMAGE_ID = id("demon_damage");

    // Skill modifiers
    private static final ResourceLocation VIGOR_HEALTH_ID = id("vigor_health");
    private static final ResourceLocation CLAWS_DAMAGE_ID = id("claws_damage");
    private static final ResourceLocation SWIFT_SPEED_ID = id("swift_speed");
    private static final ResourceLocation SWIFT_ATTACK_ID = id("swift_attack");
    private static final ResourceLocation FALL_ID = id("demon_fall");

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(DemonicAscension.MODID, path);
    }

    /** Applies or removes all demon form effects based on current state. */
    public static void updateForm(Player player) {
        DemonData data = player.getData(ModAttachments.DEMON_DATA);
        if (data.isTransformed()) {
            applyForm(player, data);
        } else {
            removeForm(player);
        }
    }

    public static void applyForm(Player player, DemonData data) {
        // --- Base form ---
        addModifier(player, Attributes.MAX_HEALTH, HEALTH_ID, 10.0,
                AttributeModifier.Operation.ADD_VALUE);
        addModifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID, 0.20,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        addModifier(player, Attributes.ATTACK_DAMAGE, DAMAGE_ID, 2.0,
                AttributeModifier.Operation.ADD_VALUE);

        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,
                MobEffectInstance.INFINITE_DURATION, 0, false, false, false));

        // --- Infernal Vigor ---
        if (data.hasSkill(DemonSkill.INFERNAL_VIGOR)) {
            addModifier(player, Attributes.MAX_HEALTH, VIGOR_HEALTH_ID, 10.0,
                    AttributeModifier.Operation.ADD_VALUE);
        } else {
            removeModifier(player, Attributes.MAX_HEALTH, VIGOR_HEALTH_ID);
        }

        // --- Rending Claws ---
        if (data.hasSkill(DemonSkill.RENDING_CLAWS)) {
            addModifier(player, Attributes.ATTACK_DAMAGE, CLAWS_DAMAGE_ID, 7.0,
                    AttributeModifier.Operation.ADD_VALUE);
        } else {
            removeModifier(player, Attributes.ATTACK_DAMAGE, CLAWS_DAMAGE_ID);
        }

        // --- Cloven Swiftness ---
        if (data.hasSkill(DemonSkill.CLOVEN_SWIFTNESS)) {
            addModifier(player, Attributes.MOVEMENT_SPEED, SWIFT_SPEED_ID, 0.40,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            addModifier(player, Attributes.ATTACK_SPEED, SWIFT_ATTACK_ID, 0.30,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            addModifier(player, Attributes.SAFE_FALL_DISTANCE, FALL_ID, 1000.0,
                    AttributeModifier.Operation.ADD_VALUE);
        } else {
            removeModifier(player, Attributes.MOVEMENT_SPEED, SWIFT_SPEED_ID);
            removeModifier(player, Attributes.ATTACK_SPEED, SWIFT_ATTACK_ID);
            removeModifier(player, Attributes.SAFE_FALL_DISTANCE, FALL_ID);
        }

        // --- Void Sight ---
        if (data.hasSkill(DemonSkill.VOID_SIGHT)) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,
                    MobEffectInstance.INFINITE_DURATION, 0, false, false, false));
        } else {
            player.removeEffect(MobEffects.NIGHT_VISION);
        }

        WingsIntegration.updateWings(player, true);
    }

    public static void removeForm(Player player) {
        removeModifier(player, Attributes.MAX_HEALTH, HEALTH_ID);
        removeModifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID);
        removeModifier(player, Attributes.ATTACK_DAMAGE, DAMAGE_ID);

        removeModifier(player, Attributes.MAX_HEALTH, VIGOR_HEALTH_ID);
        removeModifier(player, Attributes.ATTACK_DAMAGE, CLAWS_DAMAGE_ID);
        removeModifier(player, Attributes.MOVEMENT_SPEED, SWIFT_SPEED_ID);
        removeModifier(player, Attributes.ATTACK_SPEED, SWIFT_ATTACK_ID);
        removeModifier(player, Attributes.SAFE_FALL_DISTANCE, FALL_ID);

        player.removeEffect(MobEffects.FIRE_RESISTANCE);
        player.removeEffect(MobEffects.NIGHT_VISION);

        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }

        WingsIntegration.updateWings(player, false);
    }

    private static void addModifier(Player player, Holder<Attribute> attribute,
                                    ResourceLocation id, double amount,
                                    AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(id);
        instance.addTransientModifier(new AttributeModifier(id, amount, operation));
    }

    private static void removeModifier(Player player, Holder<Attribute> attribute,
                                       ResourceLocation id) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(id);
        }
    }

    /** Particle burst and sound at the moment of transformation. */
    public static void playTransformEffects(Player player, boolean becomingDemon) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        serverLevel.sendParticles(
                becomingDemon ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.SMOKE,
                player.getX(), player.getY() + 1.0, player.getZ(),
                60, 0.5, 1.0, 0.5, 0.05);

        serverLevel.playSound(null, player.blockPosition(),
                becomingDemon ? SoundEvents.WITHER_SPAWN : SoundEvents.FIRE_EXTINGUISH,
                SoundSource.PLAYERS, 0.6f, becomingDemon ? 0.8f : 1.2f);
    }
}