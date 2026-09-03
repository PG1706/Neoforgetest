package com.example.demonicascension.item;

import com.example.demonicascension.DemonicAscension;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
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

    // 18 total attack damage: the player's own base 1.0, plus this 13.0 modifier, plus
    // NETHERITE's own +4.0 tier bonus (the same arithmetic vanilla's netherite sword
    // uses to reach 8: 1.0 base + 3.0 modifier + 4.0 tier bonus).
    public static final Supplier<Item> ABYSSAL_SWORD = ITEMS.register("abyssal_sword",
            () -> new AbyssalSwordItem(Tiers.NETHERITE, new Item.Properties()
                    .fireResistant()
                    .rarity(Rarity.EPIC)
                    .attributes(SwordItem.createAttributes(Tiers.NETHERITE, 13, -2.4F))));

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}