package com.example.demonicascension.event;

import com.example.demonicascension.DemonicAscension;
import com.example.demonicascension.demon.RitualHandler;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = DemonicAscension.MODID)
public class RitualEvents {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        // The event fires once per hand; only act on the first so one click can't double-trigger.
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!level.getBlockState(event.getPos()).is(Blocks.POLISHED_BLACKSTONE)) {
            return;
        }

        RitualHandler.tryActivate(level, event.getPos(), player);
    }
}
