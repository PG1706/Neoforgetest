package com.example.demonicascension.demon;

import com.example.demonicascension.DemonicAscension;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, DemonicAscension.MODID);

    public static final Supplier<AttachmentType<DemonData>> DEMON_DATA =
            ATTACHMENT_TYPES.register("demon_data", () -> AttachmentType
                    .builder(DemonData::new)
                    .serialize(DemonData.CODEC)
                    .copyOnDeath()
                    .build());

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}