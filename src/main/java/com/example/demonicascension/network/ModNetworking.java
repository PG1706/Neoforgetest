package com.example.demonicascension.network;

import com.example.demonicascension.DemonicAscension;
import com.example.demonicascension.demon.DemonData;
import com.example.demonicascension.demon.ModAttachments;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;

@EventBusSubscriber(modid = DemonicAscension.MODID)
public class ModNetworking {

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");

        registrar.playToClient(
                DemonDataPayload.TYPE,
                DemonDataPayload.STREAM_CODEC,
                ClientPayloadHandler::handle);

        registrar.playToServer(
                UseAbilityPayload.TYPE,
                UseAbilityPayload.STREAM_CODEC,
                ServerPayloadHandler::handleAbility);

        registrar.playToServer(
                UnlockSkillPayload.TYPE,
                UnlockSkillPayload.STREAM_CODEC,
                ServerPayloadHandler::handleUnlock);

        registrar.playToClient(
                EclipseStatePayload.TYPE,
                EclipseStatePayload.STREAM_CODEC,
                ClientPayloadHandler::handleEclipse);
    }

    private static DemonDataPayload buildPayload(ServerPlayer owner) {
        DemonData data = owner.getData(ModAttachments.DEMON_DATA);
        return new DemonDataPayload(
                owner.getId(),
                data.isTransformed(),
                data.hasAscended(),
                data.getSkillPoints(),
                data.getSouls(),
                new ArrayList<>(data.getUnlockedSkills()));
    }

    /** Sends a player's data to themselves AND everyone who can see them. */
    public static void syncToAll(ServerPlayer owner) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(owner, buildPayload(owner));
    }

    /** Sends one player's data to one specific viewer, used when tracking begins. */
    public static void syncToViewer(ServerPlayer viewer, ServerPlayer owner) {
        PacketDistributor.sendToPlayer(viewer, buildPayload(owner));
    }

    /** Broadcasts the eclipse's server-wide sky/storm state to everyone connected. */
    public static void broadcastEclipseState(long activeUntilGameTime) {
        PacketDistributor.sendToAllPlayers(new EclipseStatePayload(activeUntilGameTime));
    }

    /** Catches up one player who logged in while the eclipse was already active. */
    public static void syncEclipseStateToViewer(ServerPlayer viewer, long activeUntilGameTime) {
        PacketDistributor.sendToPlayer(viewer, new EclipseStatePayload(activeUntilGameTime));
    }
}