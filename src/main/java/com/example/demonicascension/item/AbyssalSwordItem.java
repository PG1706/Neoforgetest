package com.example.demonicascension.item;

import com.example.demonicascension.config.ModConfigs;
import com.example.demonicascension.demon.DemonData;
import com.example.demonicascension.demon.ModAttachments;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The Abyssal Sword: right-click arms a temporary ignite window (see
 * {@link com.example.demonicascension.event.SoulHarvestEvents#onSwordHit} for the
 * on-hit effect this enables).
 */
public class AbyssalSwordItem extends SwordItem {

    public AbyssalSwordItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            DemonData data = player.getData(ModAttachments.DEMON_DATA);
            long now = level.getGameTime();

            if (!data.isSwordIgniteReady(now)) {
                long seconds = (data.getSwordIgniteRemaining(now) + 19) / 20;
                player.displayClientMessage(
                        Component.literal("Soulflame — " + seconds + "s").withStyle(ChatFormatting.GRAY), true);
                player.playNotifySound(SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.5F, 1.2F);
                return InteractionResultHolder.fail(stack);
            }

            data.activateSwordIgnite(now,
                    ModConfigs.SWORD_IGNITE_DURATION_TICKS.get(),
                    ModConfigs.SWORD_IGNITE_COOLDOWN_TICKS.get());
            player.setData(ModAttachments.DEMON_DATA, data);

            player.getCooldowns().addCooldown(this, ModConfigs.SWORD_IGNITE_COOLDOWN_TICKS.get());

            player.displayClientMessage(
                    Component.literal("The blade catches soulflame.").withStyle(ChatFormatting.AQUA), true);

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        player.getX(), player.getY() + 1.0, player.getZ(),
                        40, 0.4, 0.6, 0.4, 0.1);
                serverLevel.playSound(null, player.blockPosition(),
                        SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8F, 1.3F);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
