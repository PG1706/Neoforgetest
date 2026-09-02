package com.example.demonicascension.network;

import com.example.demonicascension.DemonicAscension;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UnlockSkillPayload(String skillId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<UnlockSkillPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(DemonicAscension.MODID, "unlock_skill"));

    public static final StreamCodec<ByteBuf, UnlockSkillPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, UnlockSkillPayload::skillId,
            UnlockSkillPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}