package com.example.demonicascension.item;

import com.example.demonicascension.DemonicAscension;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, DemonicAscension.MODID);

    public static final Supplier<Item> ABYSSAL_SOUL = ITEMS.register("abyssal_soul",
            () -> new AbyssalSoulItem(new Item.Properties()
                    .stacksTo(1)
                    .rarity(Rarity.EPIC)
                    .fireResistant()));

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}