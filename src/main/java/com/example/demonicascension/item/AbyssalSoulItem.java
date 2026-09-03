package com.example.demonicascension.item;

import com.example.demonicascension.config.ModConfigs;
import com.example.demonicascension.demon.AscensionState;
import com.example.demonicascension.demon.DemonData;
import com.example.demonicascension.demon.DemonFormHandler;
import com.example.demonicascension.demon.ModAttachments;
import com.example.demonicascension.network.ModNetworking;

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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class AbyssalSoulItem extends Item {

    public AbyssalSoulItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            var server = serverPlayer.getServer();
            if (server == null) {
                return InteractionResultHolder.fail(stack);
            }

            AscensionState state = AscensionState.get(server);

            // The abyss takes one host per world, and never another.
            if (state.isClaimed() && !state.isAscendedPlayer(player.getUUID())) {
                rejectPlayer(serverPlayer, state.getAscendedName());
                return InteractionResultHolder.fail(stack);
            }

            DemonData data = player.getData(ModAttachments.DEMON_DATA);

            if (!data.hasAscended()) {
                state.claim(player.getUUID(), player.getGameProfile().getName());

                data.setAscended(true);
                data.setTransformed(true);
                data.addSkillPoints(1);
                player.setData(ModAttachments.DEMON_DATA, data);
                player.sendSystemMessage(Component
                        .literal("The abyss claims you. You have ascended.")
                        .withStyle(ChatFormatting.DARK_PURPLE));

                DemonFormHandler.updateForm(player);
                DemonFormHandler.playTransformEffects(player, true);
                ModNetworking.syncToAll(serverPlayer);

                // The soul's job is done — ascension is permanent, and the demon form
                // is reachable from now on through the transform keybind instead.
                stack.shrink(1);
            } else {
                DemonFormHandler.toggleTransform(serverPlayer);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** The soul burns anyone who is not its chosen host. */
    private void rejectPlayer(ServerPlayer player, String ownerName) {
        player.hurt(player.damageSources().magic(), ModConfigs.REJECTION_DAMAGE.get().floatValue());
        player.setRemainingFireTicks(ModConfigs.REJECTION_BURN_TICKS.get());

        player.sendSystemMessage(Component
                .literal("The soul recoils. It is bound to another.")
                .withStyle(ChatFormatting.DARK_RED));

        if (!ownerName.isEmpty()) {
            player.sendSystemMessage(Component
                    .literal("The abyss already has its host: " + ownerName)
                    .withStyle(ChatFormatting.GRAY));
        }

        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    40, 0.4, 0.8, 0.4, 0.2);
            level.sendParticles(ParticleTypes.LARGE_SMOKE,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    20, 0.3, 0.6, 0.3, 0.05);
            level.playSound(null, player.blockPosition(),
                    SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 0.5F);
        }
    }
}