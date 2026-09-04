package com.example.demonicascension.event;

import com.example.demonicascension.DemonicAscension;
import com.example.demonicascension.demon.DemonFormHandler;
import com.example.demonicascension.network.ModNetworking;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = DemonicAscension.MODID)
public class PlayerSyncEvents {

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DemonFormHandler.updateForm(player);
            ModNetworking.syncToAll(player);

            if (EclipseHandler.isActive(player.level().getGameTime())) {
                ModNetworking.syncEclipseStateToViewer(player, EclipseHandler.getActiveUntilGameTime());
            }
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DemonFormHandler.updateForm(player);
            ModNetworking.syncToAll(player);
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DemonFormHandler.updateForm(player);
            ModNetworking.syncToAll(player);
        }
    }

    /** When one player comes into view of another, send them the newcomer's data. */
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer viewer
                && event.getTarget() instanceof ServerPlayer target) {
            ModNetworking.syncToViewer(viewer, target);
        }
    }
}