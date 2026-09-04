package com.example.demonicascension.network;

import com.example.demonicascension.DemonicAscension;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Broadcast to every player (not just those near the caster) when the Abyssal Eclipse
 * starts, since the darkened sky and storm are a server-wide spectacle. Carries the
 * absolute game-time it ends at rather than a duration, so a client that receives this
 * late (or a client that joins mid-eclipse, replayed on login) still lands on the
 * correct remaining time without needing a separate "it's over" packet.
 */
public record EclipseStatePayload(long activeUntilGameTime) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<EclipseStatePayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(DemonicAscension.MODID, "eclipse_state"));

    public static final StreamCodec<ByteBuf, EclipseStatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, EclipseStatePayload::activeUntilGameTime,
            EclipseStatePayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
