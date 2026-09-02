package com.example.demonicascension.event;

import com.example.demonicascension.DemonicAscension;
import com.example.demonicascension.demon.DemonData;
import com.example.demonicascension.demon.DemonSkill;
import com.example.demonicascension.demon.ModAttachments;
import com.example.demonicascension.network.ModNetworking;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
            return 250;
        }
        if (victim instanceof Player) {
            return 50;
        }
        if (victim instanceof Monster) {
            return Math.max(3, (int) (victim.getMaxHealth() / 5.0));
        }
        return 1;
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

        event.setAmount(event.getAmount() * 0.70f);
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

        float healed = event.getNewDamage() * 0.25f;
        if (healed > 0.0f) {
            attacker.heal(healed);
        }
    }
}