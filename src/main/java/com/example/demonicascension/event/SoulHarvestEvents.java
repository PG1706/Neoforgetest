package com.example.demonicascension.event;

import com.example.demonicascension.DemonicAscension;
import com.example.demonicascension.config.ModConfigs;
import com.example.demonicascension.demon.DemonData;
import com.example.demonicascension.demon.DemonSkill;
import com.example.demonicascension.demon.ModAttachments;
import com.example.demonicascension.item.ModItems;
import com.example.demonicascension.network.ModNetworking;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = DemonicAscension.MODID)
public class SoulHarvestEvents {

    /** Souls are only harvested while transformed. */
    @SubscribeEvent
    public static void onMobDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) {
            return;
        }

        DemonData data = killer.getData(ModAttachments.DEMON_DATA);
        if (!data.isTransformed()) {
            return;
        }

        int souls = soulValue(event.getEntity());
        if (souls <= 0) {
            return;
        }

        int pointsEarned = data.addSouls(souls);
        killer.setData(ModAttachments.DEMON_DATA, data);

        if (pointsEarned > 0) {
            killer.sendSystemMessage(Component.literal(
                            "The abyss rewards you. Skill points: " + data.getSkillPoints())
                    .withStyle(ChatFormatting.DARK_PURPLE));
        }

        ModNetworking.syncToAll(killer);
    }

    private static int soulValue(LivingEntity victim) {
        if (victim instanceof EnderDragon || victim instanceof WitherBoss) {
            return ModConfigs.SOULS_PER_BOSS_KILL.get();
        }
        if (victim instanceof Player) {
            return ModConfigs.SOULS_PER_PLAYER_KILL.get();
        }
        if (victim instanceof Monster) {
            return Math.max(ModConfigs.MONSTER_SOUL_MINIMUM.get(),
                    (int) (victim.getMaxHealth() / ModConfigs.MONSTER_SOUL_HEALTH_DIVISOR.get()));
        }
        return ModConfigs.SOULS_PER_PASSIVE_KILL.get();
    }

    /** Infernal Vigor: 30% damage reduction. */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer defender)) {
            return;
        }

        DemonData data = defender.getData(ModAttachments.DEMON_DATA);
        if (!data.isTransformed() || !data.hasSkill(DemonSkill.INFERNAL_VIGOR)) {
            return;
        }

        event.setAmount(event.getAmount() * (1.0f - ModConfigs.VIGOR_DAMAGE_REDUCTION.get().floatValue()));
    }

    /** Rending Claws: ignite the target and drain life. */
    @SubscribeEvent
    public static void onDealDamage(LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) {
            return;
        }

        DemonData data = attacker.getData(ModAttachments.DEMON_DATA);
        if (!data.isTransformed() || !data.hasSkill(DemonSkill.RENDING_CLAWS)) {
            return;
        }

        event.getEntity().setRemainingFireTicks(100); // 5 seconds

        float healed = event.getNewDamage() * ModConfigs.CLAWS_LIFESTEAL.get().floatValue();
        if (healed > 0.0f) {
            attacker.heal(healed);
        }
    }

    /** Abyssal Sword: wither the target and drain life, independent of the demon form. */
    @SubscribeEvent
    public static void onSwordHit(LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) {
            return;
        }

        if (!attacker.getMainHandItem().is(ModItems.ABYSSAL_SWORD.get())) {
            return;
        }

        event.getEntity().addEffect(new MobEffectInstance(MobEffects.WITHER,
                ModConfigs.SWORD_WITHER_TICKS.get(), ModConfigs.SWORD_WITHER_AMPLIFIER.get()));

        // While the right-click ignite ability is active, hits also set the target
        // ablaze — a capped duration, unlike soul fire's own never-extinguishing
        // block behaviour, since a permanently burning entity would be a bug.
        DemonData data = attacker.getData(ModAttachments.DEMON_DATA);
        if (data.isSwordIgniteActive(attacker.level().getGameTime())) {
            event.getEntity().setRemainingFireTicks(ModConfigs.SWORD_IGNITE_FIRE_TICKS.get());
        }

        float healed = event.getNewDamage() * ModConfigs.SWORD_LIFESTEAL.get().floatValue();
        if (healed > 0.0f) {
            attacker.heal(healed);
        }
    }

    /** The Abyssal Sword refuses anyone who hasn't ascended: it burns and hurts them while held. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        int interval = ModConfigs.SWORD_UNASCENDED_CHECK_INTERVAL_TICKS.get();
        if (player.tickCount % interval != 0) {
            return;
        }

        boolean holdingSword = player.getMainHandItem().is(ModItems.ABYSSAL_SWORD.get())
                || player.getOffhandItem().is(ModItems.ABYSSAL_SWORD.get());
        if (!holdingSword) {
            return;
        }

        DemonData data = player.getData(ModAttachments.DEMON_DATA);
        if (data.hasAscended()) {
            return;
        }

        player.hurt(player.damageSources().magic(), ModConfigs.SWORD_UNASCENDED_DAMAGE.get().floatValue());
        player.setRemainingFireTicks(ModConfigs.SWORD_UNASCENDED_FIRE_TICKS.get());
    }
}