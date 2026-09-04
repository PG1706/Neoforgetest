package com.example.demonicascension.network;

import com.example.demonicascension.demon.DemonData;
import com.example.demonicascension.demon.ModAttachments;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;
import java.util.Optional;

public class ClientPayloadHandler {

    public static void handle(final DemonDataPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) {
                return;
            }

            Entity entity = mc.level.getEntity(payload.entityId());
            if (!(entity instanceof Player player)) {
                return;
            }

            // The return point stays server-side — the client has no use for it.
            DemonData data = new DemonData(
                    payload.transformed(),
                    payload.hasAscended(),
                    payload.skillPoints(),
                    payload.souls(),
                    payload.unlockedSkills(),
                    Optional.empty(),
                    List.of());

            player.setData(ModAttachments.DEMON_DATA, data);
        });
    }

    public static void handleEclipse(final EclipseStatePayload payload, final IPayloadContext context) {
        context.enqueueWork(() ->
                com.example.demonicascension.client.EclipseClientState.setActiveUntil(payload.activeUntilGameTime()));
    }
}