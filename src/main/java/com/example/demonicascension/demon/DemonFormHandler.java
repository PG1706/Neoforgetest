package com.example.demonicascension.demon;

import com.example.demonicascension.DemonicAscension;
import com.example.demonicascension.compat.WingsIntegration;
import com.example.demonicascension.config.ModConfigs;
import com.example.demonicascension.network.ModNetworking;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

    // Ascended passive modifiers (active for ascended players even while not transformed)
    private static final ResourceLocation ASCENDED_HEALTH_ID = id("ascended_health");
    private static final ResourceLocation ASCENDED_SPEED_ID = id("ascended_speed");

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(DemonicAscension.MODID, path);
    }

    /** Applies or removes all demon form effects based on current state. */
    public static void updateForm(Player player) {
        DemonData data = player.getData(ModAttachments.DEMON_DATA);
        if (data.isTransformed()) {
            applyForm(player, data);
        } else if (data.hasAscended()) {
            applyAscendedPassive(player, data);
        } else {
            removeForm(player);
        }
    }

    /**
     * Flips an already-ascended player's transformed state, applies the resulting
     * form, and syncs it. Callers (the Abyssal Soul item, the transform keybind) are
     * responsible for checking {@link DemonData#hasAscended()} first — this method
     * doesn't ascend anyone, it only toggles the form of someone who already has.
     */
    public static void toggleTransform(ServerPlayer player) {
        DemonData data = player.getData(ModAttachments.DEMON_DATA);

        data.setTransformed(!data.isTransformed());
        player.setData(ModAttachments.DEMON_DATA, data);

        player.sendSystemMessage(Component.literal(
                data.isTransformed() ? "You embrace your demonic form." : "You return to mortal form.")
                .withStyle(data.isTransformed() ? ChatFormatting.DARK_PURPLE : ChatFormatting.GRAY));

        updateForm(player);
        playTransformEffects(player, data.isTransformed());

        ModNetworking.syncToAll(player);
    }

    public static void applyForm(Player player, DemonData data) {
        // The full form supersedes the ascended passive; clear it so the bonuses don't stack.
        removeModifier(player, Attributes.MAX_HEALTH, ASCENDED_HEALTH_ID);
        removeModifier(player, Attributes.MOVEMENT_SPEED, ASCENDED_SPEED_ID);

        // Only the SKILLS' own numbers burn fiercer during the eclipse — the base form
        // (granted just by transforming, not a skill) is untouched.
        double buff = data.isEclipseBuffActive(player.level().getGameTime())
                ? ModConfigs.ECLIPSE_BUFF_MULTIPLIER.get() : 1.0;

        // --- Base form ---
        addModifier(player, Attributes.MAX_HEALTH, HEALTH_ID, ModConfigs.BASE_HEALTH_BONUS.get(),
                AttributeModifier.Operation.ADD_VALUE);
        addModifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID, ModConfigs.BASE_SPEED_BONUS.get(),
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        addModifier(player, Attributes.ATTACK_DAMAGE, DAMAGE_ID, ModConfigs.BASE_DAMAGE_BONUS.get(),
                AttributeModifier.Operation.ADD_VALUE);

        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,
                MobEffectInstance.INFINITE_DURATION, 0, false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION,
                MobEffectInstance.INFINITE_DURATION, ModConfigs.REGENERATION_AMPLIFIER.get(),
                false, false, false));

        // --- Infernal Vigor ---
        if (data.hasSkill(DemonSkill.INFERNAL_VIGOR)) {
            addModifier(player, Attributes.MAX_HEALTH, VIGOR_HEALTH_ID, ModConfigs.VIGOR_HEALTH_BONUS.get() * buff,
                    AttributeModifier.Operation.ADD_VALUE);
        } else {
            removeModifier(player, Attributes.MAX_HEALTH, VIGOR_HEALTH_ID);
        }

        // --- Rending Claws ---
        if (data.hasSkill(DemonSkill.RENDING_CLAWS)) {
            addModifier(player, Attributes.ATTACK_DAMAGE, CLAWS_DAMAGE_ID, ModConfigs.CLAWS_DAMAGE_BONUS.get() * buff,
                    AttributeModifier.Operation.ADD_VALUE);
        } else {
            removeModifier(player, Attributes.ATTACK_DAMAGE, CLAWS_DAMAGE_ID);
        }

        // --- Cloven Swiftness ---
        if (data.hasSkill(DemonSkill.CLOVEN_SWIFTNESS)) {
            addModifier(player, Attributes.MOVEMENT_SPEED, SWIFT_SPEED_ID, ModConfigs.SWIFT_SPEED_BONUS.get() * buff,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            addModifier(player, Attributes.ATTACK_SPEED, SWIFT_ATTACK_ID, ModConfigs.SWIFT_ATTACK_SPEED_BONUS.get() * buff,
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

    /** Strips every full-transformed-form modifier and effect. Ascended status is untouched. */
    public static void removeForm(Player player) {
        clearTransformedModifiersAndEffects(player);
        removeModifier(player, Attributes.MAX_HEALTH, ASCENDED_HEALTH_ID);
        removeModifier(player, Attributes.MOVEMENT_SPEED, ASCENDED_SPEED_ID);
        clampHealth(player);
    }

    /**
     * The minor permanent buff ascended players keep even when not transformed, so ascension
     * means something between transformations without making the form itself less worthwhile.
     * Health and speed scale with skills unlocked; fire resistance and regeneration are flat.
     */
    private static void applyAscendedPassive(Player player, DemonData data) {
        clearTransformedModifiersAndEffects(player);

        int skillsUnlocked = data.getUnlockedSkills().size();
        int maxSkills = DemonSkill.values().length;

        double healthBonus = scaleByUnlockedSkills(skillsUnlocked, maxSkills,
                ModConfigs.ASCENDED_HEALTH_BONUS_AT_FIRST_SKILL.get(), ModConfigs.ASCENDED_HEALTH_BONUS_AT_MAX_SKILLS.get());
        double speedBonus = scaleByUnlockedSkills(skillsUnlocked, maxSkills,
                ModConfigs.ASCENDED_SPEED_BONUS_AT_FIRST_SKILL.get(), ModConfigs.ASCENDED_SPEED_BONUS_AT_MAX_SKILLS.get());

        addModifier(player, Attributes.MAX_HEALTH, ASCENDED_HEALTH_ID, healthBonus,
                AttributeModifier.Operation.ADD_VALUE);
        addModifier(player, Attributes.MOVEMENT_SPEED, ASCENDED_SPEED_ID, speedBonus,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,
                MobEffectInstance.INFINITE_DURATION, 0, false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION,
                MobEffectInstance.INFINITE_DURATION, ModConfigs.REGENERATION_AMPLIFIER.get(),
                false, false, false));

        clampHealth(player);
    }

    /**
     * Linear ramp from 0 at no skills, through {@code valueAtFirstSkill} at exactly one skill,
     * up to {@code valueAtMaxSkills} once every skill is unlocked.
     */
    private static double scaleByUnlockedSkills(int skillsUnlocked, int maxSkills,
                                                double valueAtFirstSkill, double valueAtMaxSkills) {
        if (skillsUnlocked <= 0) {
            return 0.0;
        }
        if (skillsUnlocked >= maxSkills || maxSkills <= 1) {
            return valueAtMaxSkills;
        }
        double progress = (skillsUnlocked - 1) / (double) (maxSkills - 1);
        return valueAtFirstSkill + progress * (valueAtMaxSkills - valueAtFirstSkill);
    }

    private static void clearTransformedModifiersAndEffects(Player player) {
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
        player.removeEffect(MobEffects.REGENERATION);

        WingsIntegration.updateWings(player, false);
    }

    private static void clampHealth(Player player) {
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
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