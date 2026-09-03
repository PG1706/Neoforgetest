package com.example.demonicascension.network;

import com.example.demonicascension.demon.AbilityHandler;
import com.example.demonicascension.demon.DemonSkill;
import com.example.demonicascension.demon.SkillUnlockHandler;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ServerPayloadHandler {

    public static void handleAbility(final UseAbilityPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            UseAbilityPayload.Ability[] abilities = UseAbilityPayload.Ability.values();
            int index = payload.abilityIndex();
            if (index < 0 || index >= abilities.length) {
                return;
            }

            switch (abilities[index]) {
                case BOLT -> AbilityHandler.useBoltSlot(player);
                case DASH -> AbilityHandler.useDashSlot(player);
                case RIFT -> AbilityHandler.useRiftSlot(player);
                case TRANSFORM -> AbilityHandler.useTransformSlot(player);
            }
        });
    }

    public static void handleUnlock(final UnlockSkillPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            // Never trust the client — revalidate everything server-side.
            var maybeSkill = DemonSkill.byId(payload.skillId());
            if (maybeSkill.isEmpty()) {
                return;
            }

            DemonSkill skill = maybeSkill.get();
            var failure = SkillUnlockHandler.tryUnlock(player, skill);

            if (failure.isPresent()) {
                player.sendSystemMessage(Component.literal(failure.get())
                        .withStyle(ChatFormatting.RED));
            } else {
                player.sendSystemMessage(Component.literal("You claim " + skill.getDisplayName() + ".")
                        .withStyle(ChatFormatting.DARK_PURPLE));
            }
        });
    }
}