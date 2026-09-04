package com.example.demonicascension.entity;

import com.example.demonicascension.DemonicAscension;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, DemonicAscension.MODID);

    public static final Supplier<EntityType<SoulBoltEntity>> SOUL_BOLT =
            ENTITIES.register("soul_bolt", () -> EntityType.Builder
                    .<SoulBoltEntity>of(SoulBoltEntity::new, MobCategory.MISC)
                    .sized(0.4F, 0.4F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("soul_bolt"));

    public static final Supplier<EntityType<RiftEntity>> RIFT =
            ENTITIES.register("rift", () -> EntityType.Builder
                    .<RiftEntity>of(RiftEntity::new, MobCategory.MISC)
                    .sized(1.6F, 2.4F)
                    .clientTrackingRange(16)
                    .updateInterval(2)
                    .fireImmune()
                    .build("rift"));

    public static final Supplier<EntityType<FireSlashEntity>> FIRE_SLASH =
            ENTITIES.register("fire_slash", () -> EntityType.Builder
                    .<FireSlashEntity>of(FireSlashEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.3F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("fire_slash"));

    public static final Supplier<EntityType<AltarSwordEntity>> ALTAR_SWORD =
            ENTITIES.register("altar_sword", () -> EntityType.Builder
                    .<AltarSwordEntity>of(AltarSwordEntity::new, MobCategory.MISC)
                    .sized(0.6F, 3.4F)
                    .clientTrackingRange(8)
                    .updateInterval(20)
                    .fireImmune()
                    .build("altar_sword"));

    public static void register(IEventBus modEventBus) {
        ENTITIES.register(modEventBus);
    }
}