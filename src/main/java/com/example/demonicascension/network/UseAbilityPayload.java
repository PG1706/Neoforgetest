package com.example.demonicascension.network;

import com.example.demonicascension.DemonicAscension;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client tells the server "I pressed an ability key". Ability index maps to Ability.values(). */
public record UseAbilityPayload(int abilityIndex) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<UseAbilityPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(DemonicAscension.MODID, "use_ability"));

    public static final StreamCodec<ByteBuf, UseAbilityPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, UseAbilityPayload::abilityIndex,
            UseAbilityPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** The two ability slots. Tier-3 skills upgrade these rather than adding new keys. */
    public enum Ability {
        BOLT,
        DASH,
        RIFT,
        TRANSFORM
    }
}