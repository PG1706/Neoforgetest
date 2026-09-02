package com.example.demonicascension.network;

import com.example.demonicascension.DemonicAscension;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record DemonDataPayload(int entityId, boolean transformed, boolean hasAscended,
                               int skillPoints, int souls, List<String> unlockedSkills)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<DemonDataPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(DemonicAscension.MODID, "demon_data_sync"));

    public static final StreamCodec<ByteBuf, DemonDataPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DemonDataPayload::entityId,
            ByteBufCodecs.BOOL, DemonDataPayload::transformed,
            ByteBufCodecs.BOOL, DemonDataPayload::hasAscended,
            ByteBufCodecs.VAR_INT, DemonDataPayload::skillPoints,
            ByteBufCodecs.VAR_INT, DemonDataPayload::souls,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), DemonDataPayload::unlockedSkills,
            DemonDataPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}