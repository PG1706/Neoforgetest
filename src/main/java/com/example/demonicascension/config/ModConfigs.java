package com.example.demonicascension.config;

import com.example.demonicascension.demon.DemonSkill;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.EnumMap;
import java.util.Map;

/**
 * Every balance number in the mod, in one place. Gameplay values live in the
 * SERVER spec (synced to clients, stored per-world) since they must be
 * consistent for everyone; the void sight render distance is purely a local
 * visual and lives in the CLIENT spec instead.
 */
public class ModConfigs {

    public static final ModConfigSpec SERVER_SPEC;
    public static final ModConfigSpec CLIENT_SPEC;

    // --- Souls ---
    public static final ModConfigSpec.IntValue SOULS_PER_PLAYER_KILL;
    public static final ModConfigSpec.IntValue SOULS_PER_BOSS_KILL;
    public static final ModConfigSpec.IntValue SOULS_PER_PASSIVE_KILL;
    public static final ModConfigSpec.DoubleValue MONSTER_SOUL_HEALTH_DIVISOR;
    public static final ModConfigSpec.IntValue MONSTER_SOUL_MINIMUM;
    public static final ModConfigSpec.IntValue SOULS_PER_SKILL_POINT;

    // --- Skill costs, one entry per skill, keyed by its id ---
    public static final Map<DemonSkill, ModConfigSpec.IntValue> SKILL_COSTS;

    // --- Base form ---
    public static final ModConfigSpec.DoubleValue BASE_HEALTH_BONUS;
    public static final ModConfigSpec.DoubleValue BASE_SPEED_BONUS;
    public static final ModConfigSpec.DoubleValue BASE_DAMAGE_BONUS;

    // --- Regeneration granted by both the full transformed form and the ascended passive ---
    public static final ModConfigSpec.IntValue REGENERATION_AMPLIFIER;

    // --- Ascended passive (applies to ascended players even while not transformed) ---
    // Health and speed scale linearly with skills unlocked: 0 at no skills, the "at first
    // skill" value at 1 skill, up to the "at max skills" value once every skill is unlocked.
    public static final ModConfigSpec.DoubleValue ASCENDED_HEALTH_BONUS_AT_FIRST_SKILL;
    public static final ModConfigSpec.DoubleValue ASCENDED_HEALTH_BONUS_AT_MAX_SKILLS;
    public static final ModConfigSpec.DoubleValue ASCENDED_SPEED_BONUS_AT_FIRST_SKILL;
    public static final ModConfigSpec.DoubleValue ASCENDED_SPEED_BONUS_AT_MAX_SKILLS;

    // --- Infernal Vigor ---
    public static final ModConfigSpec.DoubleValue VIGOR_HEALTH_BONUS;
    public static final ModConfigSpec.DoubleValue VIGOR_DAMAGE_REDUCTION;

    // --- Rending Claws ---
    public static final ModConfigSpec.DoubleValue CLAWS_DAMAGE_BONUS;
    public static final ModConfigSpec.DoubleValue CLAWS_LIFESTEAL;

    // --- Cloven Swiftness ---
    public static final ModConfigSpec.DoubleValue SWIFT_SPEED_BONUS;
    public static final ModConfigSpec.DoubleValue SWIFT_ATTACK_SPEED_BONUS;

    // --- Soul Bolt / Hellfire Barrage ---
    public static final ModConfigSpec.DoubleValue BOLT_DAMAGE;
    public static final ModConfigSpec.IntValue BOLT_COOLDOWN_TICKS;
    public static final ModConfigSpec.DoubleValue BOLT_LIFESTEAL;
    public static final ModConfigSpec.DoubleValue BARRAGE_BOLT_DAMAGE;
    public static final ModConfigSpec.IntValue BARRAGE_COOLDOWN_TICKS;
    public static final ModConfigSpec.IntValue BARRAGE_BOLT_COUNT;

    // --- Abyssal Dash / Voidstep ---
    public static final ModConfigSpec.DoubleValue DASH_DAMAGE;
    public static final ModConfigSpec.IntValue DASH_COOLDOWN_TICKS;
    public static final ModConfigSpec.DoubleValue DASH_HIT_RADIUS;
    public static final ModConfigSpec.DoubleValue VOIDSTEP_DAMAGE;
    public static final ModConfigSpec.IntValue VOIDSTEP_COOLDOWN_TICKS;
    public static final ModConfigSpec.DoubleValue VOIDSTEP_RANGE;
    public static final ModConfigSpec.DoubleValue VOIDSTEP_RADIUS;

    // --- Abyssal Rift ---
    public static final ModConfigSpec.IntValue RIFT_COOLDOWN_TICKS;

    // --- Abyssal Soul rejection (used by a second player on a claimed world) ---
    public static final ModConfigSpec.DoubleValue REJECTION_DAMAGE;
    public static final ModConfigSpec.IntValue REJECTION_BURN_TICKS;

    // --- Abyssal Sword (found on the throne room altar) ---
    public static final ModConfigSpec.DoubleValue SWORD_LIFESTEAL;
    public static final ModConfigSpec.IntValue SWORD_WITHER_TICKS;
    public static final ModConfigSpec.IntValue SWORD_WITHER_AMPLIFIER;
    public static final ModConfigSpec.IntValue SWORD_IGNITE_DURATION_TICKS;
    public static final ModConfigSpec.IntValue SWORD_IGNITE_COOLDOWN_TICKS;
    public static final ModConfigSpec.IntValue SWORD_IGNITE_FIRE_TICKS;
    public static final ModConfigSpec.DoubleValue SWORD_UNASCENDED_DAMAGE;
    public static final ModConfigSpec.IntValue SWORD_UNASCENDED_FIRE_TICKS;
    public static final ModConfigSpec.IntValue SWORD_UNASCENDED_CHECK_INTERVAL_TICKS;

    // --- Abyss dimension ---
    public static final ModConfigSpec.IntValue PLATFORM_SPACING;
    public static final ModConfigSpec.IntValue PLATFORM_GRID_WIDTH;

    // --- Client-only ---
    public static final ModConfigSpec.DoubleValue VOID_SIGHT_RANGE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("souls");
        SOULS_PER_PLAYER_KILL = builder
                .comment("Souls harvested for killing another player while transformed.")
                .defineInRange("soulsPerPlayerKill", 50, 0, Integer.MAX_VALUE);
        SOULS_PER_BOSS_KILL = builder
                .comment("Souls harvested for killing the Ender Dragon or the Wither.")
                .defineInRange("soulsPerBossKill", 250, 0, Integer.MAX_VALUE);
        SOULS_PER_PASSIVE_KILL = builder
                .comment("Souls harvested for killing a passive (non-hostile, non-boss) mob.")
                .defineInRange("soulsPerPassiveKill", 1, 0, Integer.MAX_VALUE);
        MONSTER_SOUL_HEALTH_DIVISOR = builder
                .comment("A hostile mob's max health is divided by this to get its soul value.")
                .defineInRange("monsterSoulHealthDivisor", 5.0, 0.01, 1000.0);
        MONSTER_SOUL_MINIMUM = builder
                .comment("Hostile mob kills never award fewer souls than this.")
                .defineInRange("monsterSoulMinimum", 3, 0, Integer.MAX_VALUE);
        SOULS_PER_SKILL_POINT = builder
                .comment("Souls required to earn one skill point.")
                .defineInRange("soulsPerSkillPoint", 25, 1, Integer.MAX_VALUE);
        builder.pop();

        builder.push("skillCosts");
        builder.comment("Skill point cost to unlock each skill. Keyed by skill id.");
        Map<DemonSkill, ModConfigSpec.IntValue> costs = new EnumMap<>(DemonSkill.class);
        for (DemonSkill skill : DemonSkill.values()) {
            costs.put(skill, builder.defineInRange(skill.getId(), skill.getBaseCost(), 0, 99));
        }
        SKILL_COSTS = costs;
        builder.pop();

        builder.push("form").push("base");
        BASE_HEALTH_BONUS = builder.comment("Extra max health while transformed.")
                .defineInRange("healthBonus", 10.0, 0.0, 1000.0);
        BASE_SPEED_BONUS = builder.comment("Movement speed multiplier bonus while transformed (0.20 = +20%).")
                .defineInRange("speedBonus", 0.20, 0.0, 100.0);
        BASE_DAMAGE_BONUS = builder.comment("Extra attack damage while transformed.")
                .defineInRange("damageBonus", 2.0, 0.0, 1000.0);
        REGENERATION_AMPLIFIER = builder.comment(
                "Regeneration level granted while transformed, and to ascended players even while not transformed.",
                "0 = Regeneration I, 1 = II, etc.")
                .defineInRange("regenerationAmplifier", 0, 0, 4);
        builder.pop(2);

        builder.push("form").push("ascended");
        ASCENDED_HEALTH_BONUS_AT_FIRST_SKILL = builder.comment(
                "Extra max health for an ascended player with exactly one skill unlocked, while not transformed.",
                "Scales linearly up to healthBonusAtMaxSkills as more skills unlock; always 0 with no skills unlocked.")
                .defineInRange("healthBonusAtFirstSkill", 2.0, 0.0, 1000.0);
        ASCENDED_HEALTH_BONUS_AT_MAX_SKILLS = builder.comment(
                "Extra max health for an ascended player with every skill unlocked, while not transformed.")
                .defineInRange("healthBonusAtMaxSkills", 10.0, 0.0, 1000.0);
        ASCENDED_SPEED_BONUS_AT_FIRST_SKILL = builder.comment(
                "Movement speed multiplier bonus for an ascended player with exactly one skill unlocked,",
                "while not transformed (0.01 = +1%). Scales linearly up to speedBonusAtMaxSkills.")
                .defineInRange("speedBonusAtFirstSkill", 0.01, 0.0, 100.0);
        ASCENDED_SPEED_BONUS_AT_MAX_SKILLS = builder.comment(
                "Movement speed multiplier bonus for an ascended player with every skill unlocked,",
                "while not transformed (0.05 = +5%).")
                .defineInRange("speedBonusAtMaxSkills", 0.05, 0.0, 100.0);
        builder.pop(2);

        builder.push("form").push("infernalVigor");
        VIGOR_HEALTH_BONUS = builder.comment("Extra max health from Infernal Vigor.")
                .defineInRange("healthBonus", 10.0, 0.0, 1000.0);
        VIGOR_DAMAGE_REDUCTION = builder.comment("Fraction of incoming damage negated by Infernal Vigor (0.30 = 30%).")
                .defineInRange("damageReduction", 0.30, 0.0, 1.0);
        builder.pop(2);

        builder.push("form").push("rendingClaws");
        CLAWS_DAMAGE_BONUS = builder.comment("Extra attack damage from Rending Claws.")
                .defineInRange("damageBonus", 7.0, 0.0, 1000.0);
        CLAWS_LIFESTEAL = builder.comment("Fraction of melee damage dealt returned as healing (0.25 = 25%).")
                .defineInRange("lifesteal", 0.25, 0.0, 1.0);
        builder.pop(2);

        builder.push("form").push("clovenSwiftness");
        SWIFT_SPEED_BONUS = builder.comment("Movement speed multiplier bonus from Cloven Swiftness (0.40 = +40%).")
                .defineInRange("speedBonus", 0.40, 0.0, 100.0);
        SWIFT_ATTACK_SPEED_BONUS = builder.comment("Attack speed multiplier bonus from Cloven Swiftness (0.30 = +30%).")
                .defineInRange("attackSpeedBonus", 0.30, 0.0, 100.0);
        builder.pop(2);

        builder.push("abilities").push("soulBolt");
        BOLT_DAMAGE = builder.comment("Damage dealt by a Soul Bolt (or the base bolt of Hellfire Barrage).")
                .defineInRange("damage", 18.0, 0.0, 10000.0);
        BOLT_COOLDOWN_TICKS = builder.comment("Cooldown in ticks (20 per second) before the bolt slot can be used again.")
                .defineInRange("cooldownTicks", 40, 0, Integer.MAX_VALUE);
        BOLT_LIFESTEAL = builder.comment("Fraction of bolt damage dealt returned as healing to the caster (0.50 = 50%).")
                .defineInRange("lifesteal", 0.50, 0.0, 1.0);
        builder.pop();

        builder.push("hellfireBarrage");
        BARRAGE_BOLT_DAMAGE = builder.comment("Damage dealt by each Hellfire Barrage bolt.")
                .defineInRange("boltDamage", 20.0, 0.0, 10000.0);
        BARRAGE_COOLDOWN_TICKS = builder.comment("Cooldown in ticks before the bolt slot can be used again.")
                .defineInRange("cooldownTicks", 120, 0, Integer.MAX_VALUE);
        BARRAGE_BOLT_COUNT = builder.comment("Number of homing bolts fired per Hellfire Barrage.")
                .defineInRange("boltCount", 5, 1, 100);
        builder.pop(2);

        builder.push("abilities").push("abyssalDash");
        DASH_DAMAGE = builder.comment("Damage dealt to anything struck along the dash path.")
                .defineInRange("damage", 12.0, 0.0, 10000.0);
        DASH_COOLDOWN_TICKS = builder.comment("Cooldown in ticks before the dash slot can be used again.")
                .defineInRange("cooldownTicks", 80, 0, Integer.MAX_VALUE);
        DASH_HIT_RADIUS = builder.comment("How far from the dash path an entity can be and still get hit.")
                .defineInRange("hitRadius", 3.0, 0.0, 1000.0);
        builder.pop();

        builder.push("voidstep");
        VOIDSTEP_DAMAGE = builder.comment("Damage dealt to everything caught in the arrival burst.")
                .defineInRange("damage", 25.0, 0.0, 10000.0);
        VOIDSTEP_COOLDOWN_TICKS = builder.comment("Cooldown in ticks before the dash slot can be used again.")
                .defineInRange("cooldownTicks", 140, 0, Integer.MAX_VALUE);
        VOIDSTEP_RANGE = builder.comment("Maximum blink distance.")
                .defineInRange("range", 40.0, 0.0, 100000.0);
        VOIDSTEP_RADIUS = builder.comment("Radius of the damaging burst at the arrival point.")
                .defineInRange("radius", 5.0, 0.0, 1000.0);
        builder.pop(2);

        builder.push("abilities").push("abyssalRift");
        RIFT_COOLDOWN_TICKS = builder.comment("Cooldown in ticks before the rift slot can be used again.")
                .defineInRange("cooldownTicks", 600, 0, Integer.MAX_VALUE);
        builder.pop(2);

        builder.push("abyssalSoul");
        REJECTION_DAMAGE = builder.comment("Damage dealt to a player who uses the Abyssal Soul when another player has already claimed it.")
                .defineInRange("rejectionDamage", 8.0, 0.0, 10000.0);
        REJECTION_BURN_TICKS = builder.comment("How long the rejected player burns for, in ticks.")
                .defineInRange("rejectionBurnTicks", 120, 0, Integer.MAX_VALUE);
        builder.pop();

        builder.push("abyssalSword");
        SWORD_LIFESTEAL = builder.comment("Fraction of melee damage dealt with the Abyssal Sword returned as healing (0.30 = 30%).")
                .defineInRange("lifesteal", 0.30, 0.0, 1.0);
        SWORD_WITHER_TICKS = builder.comment("Duration of the Wither effect applied on hit, in ticks.")
                .defineInRange("witherTicks", 100, 0, Integer.MAX_VALUE);
        SWORD_WITHER_AMPLIFIER = builder.comment("Wither effect level applied on hit. 0 = Wither I, 1 = II, etc.")
                .defineInRange("witherAmplifier", 0, 0, 4);
        SWORD_IGNITE_DURATION_TICKS = builder.comment(
                "How long the right-click ignite ability stays active once triggered, in ticks.",
                "While active, hits also set the target ablaze (see igniteFireTicks).")
                .defineInRange("igniteDurationTicks", 200, 1, Integer.MAX_VALUE);
        SWORD_IGNITE_COOLDOWN_TICKS = builder.comment("Cooldown in ticks before the ignite ability can be triggered again.")
                .defineInRange("igniteCooldownTicks", 400, 0, Integer.MAX_VALUE);
        SWORD_IGNITE_FIRE_TICKS = builder.comment(
                "How long a struck target burns for while the ignite ability is active, in ticks.",
                "Capped and finite even though soul fire itself never burns out as a block —",
                "an entity set ablaze forever would be a bug, not a feature.")
                .defineInRange("igniteFireTicks", 100, 1, Integer.MAX_VALUE);
        SWORD_UNASCENDED_DAMAGE = builder.comment("Damage dealt per check to a non-ascended player holding the sword.")
                .defineInRange("unascendedDamage", 1.0, 0.0, 10000.0);
        SWORD_UNASCENDED_FIRE_TICKS = builder.comment("Fire duration applied per check while a non-ascended player holds the sword.")
                .defineInRange("unascendedFireTicks", 40, 1, Integer.MAX_VALUE);
        SWORD_UNASCENDED_CHECK_INTERVAL_TICKS = builder.comment("How often (in ticks) the sword checks whether its non-ascended holder should be punished.")
                .defineInRange("unascendedCheckIntervalTicks", 20, 1, Integer.MAX_VALUE);
        builder.pop();

        builder.push("abyssDimension");
        PLATFORM_SPACING = builder.comment("Blocks between each player's platform in the abyss, so nobody stumbles into a neighbour's.")
                .defineInRange("platformSpacing", 20000, 16, Integer.MAX_VALUE);
        PLATFORM_GRID_WIDTH = builder.comment("Platforms per row before the layout wraps to the next row.")
                .defineInRange("platformGridWidth", 1000, 1, Integer.MAX_VALUE);
        builder.pop();

        SERVER_SPEC = builder.build();

        ModConfigSpec.Builder clientBuilder = new ModConfigSpec.Builder();
        clientBuilder.push("voidSight");
        VOID_SIGHT_RANGE = clientBuilder.comment("How far Void Sight can see hostiles and players through walls, in blocks.")
                .defineInRange("range", 32.0, 1.0, 1000.0);
        clientBuilder.pop();
        CLIENT_SPEC = clientBuilder.build();
    }

    /** Current cost to unlock a skill, honouring any config override. */
    public static int skillCost(DemonSkill skill) {
        return SKILL_COSTS.get(skill).get();
    }
}
