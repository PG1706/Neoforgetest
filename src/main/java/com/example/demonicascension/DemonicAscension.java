package com.example.demonicascension;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.example.demonicascension.item.ModItems;
import com.example.demonicascension.demon.ModAttachments;
import com.example.demonicascension.entity.ModEntities;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(DemonicAscension.MODID)
public class DemonicAscension {
    public static final String MODID = "demonicascension";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> DEMONIC_TAB =
            CREATIVE_MODE_TABS.register("demonic_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + MODID))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.ABYSSAL_SOUL.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.ABYSSAL_SOUL.get());
                    }).build());

    public DemonicAscension(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        ModItems.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        ModAttachments.register(modEventBus);
        ModEntities.register(modEventBus);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Demonic Ascension: common setup complete.");
    }

    @EventBusSubscriber(modid = DemonicAscension.MODID, value = Dist.CLIENT)
    static class ClientModEvents {
        @SubscribeEvent
        static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("Demonic Ascension: client setup complete.");
        }
    }
}