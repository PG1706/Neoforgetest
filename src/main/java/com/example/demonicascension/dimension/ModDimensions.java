package com.example.demonicascension.dimension;

import com.example.demonicascension.DemonicAscension;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public class ModDimensions {

    public static final ResourceKey<Level> ABYSS = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(DemonicAscension.MODID, "abyss"));
}