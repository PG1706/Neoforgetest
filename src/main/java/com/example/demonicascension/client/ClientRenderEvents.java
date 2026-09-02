package com.example.demonicascension.client;

import com.example.demonicascension.DemonicAscension;
import com.example.demonicascension.entity.ModEntities;

import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = DemonicAscension.MODID, value = Dist.CLIENT)
public class ClientRenderEvents {

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(DemonHornModel.HORN_LAYER, DemonHornModel::createLayer);
    }

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        // Both the "default" (Steve) and "slim" (Alex) player models need the layer.
        for (var skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof PlayerRenderer renderer) {
                renderer.addLayer(new DemonHornLayer(renderer, event.getEntityModels()));
            }
        }
    }
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SOUL_BOLT.get(), SoulBoltRenderer::new);
        event.registerEntityRenderer(ModEntities.RIFT.get(), RiftRenderer::new);
    }
}