package com.example.demonicascension.client;

import com.example.demonicascension.DemonicAscension;
import com.example.demonicascension.entity.ModEntities;
import com.example.demonicascension.item.ModItems;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@EventBusSubscriber(modid = DemonicAscension.MODID, value = Dist.CLIENT)
public class ClientRenderEvents {

    /**
     * By default NeoForge always shows every active effect in the inventory and HUD,
     * ignoring {@link MobEffectInstance#showIcon()}. We apply fire resistance, regeneration
     * and night vision silently (showIcon = false) as part of the demon form and the ascended
     * passive, so this restores that flag's effect for those three — a real potion of the same
     * effect still sets showIcon = true and displays normally.
     */
    private static final IClientMobEffectExtensions RESPECT_SHOW_ICON = new IClientMobEffectExtensions() {
        @Override
        public boolean isVisibleInInventory(MobEffectInstance instance) {
            return instance.showIcon();
        }

        @Override
        public boolean isVisibleInGui(MobEffectInstance instance) {
            return instance.showIcon();
        }
    };

    /**
     * Renders the Abyssal Sword as the 3D altar-sword model in hand — see
     * AbyssalSwordItemRenderer. Built lazily, on the first actual render call, rather
     * than eagerly here: constructing it touches {@code EntityModelSet.bakeLayer},
     * which needs {@link AltarSwordModel#LAYER} to already be registered — merely
     * referencing another static field on AbyssalSwordItemRenderer (as
     * registerAdditionalModels below does) triggers that class's full static
     * initialization, and that can happen before RegisterLayerDefinitions has fired.
     */
    private static final IClientItemExtensions ABYSSAL_SWORD_RENDERER = new IClientItemExtensions() {
        private AbyssalSwordItemRenderer renderer;

        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            if (renderer == null) {
                renderer = new AbyssalSwordItemRenderer(
                        Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                        Minecraft.getInstance().getEntityModels());
            }
            return renderer;
        }
    };

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerMobEffect(RESPECT_SHOW_ICON,
                MobEffects.FIRE_RESISTANCE, MobEffects.REGENERATION, MobEffects.NIGHT_VISION);
        event.registerItem(ABYSSAL_SWORD_RENDERER, ModItems.ABYSSAL_SWORD.get());
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(DemonHornModel.HORN_LAYER, DemonHornModel::createLayer);
        event.registerLayerDefinition(DemonEyesModel.EYES_LAYER, DemonEyesModel::createLayer);
        event.registerLayerDefinition(AltarSwordModel.LAYER, AltarSwordModel::createLayer);
        event.registerLayerDefinition(FireSlashModel.LAYER, FireSlashModel::createLayer);
    }

    /**
     * The Abyssal Sword's own item model now points at {@code builtin/entity} (a 3D
     * render, see AbyssalSwordItemRenderer), so its original flat icon lives in a
     * second, otherwise-unreferenced model that has to be registered explicitly to get
     * baked at all — this is that registration.
     */
    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(AbyssalSwordItemRenderer.GUI_ICON_MODEL);
    }

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        // Both the "default" (Steve) and "slim" (Alex) player models need the layer.
        for (var skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof PlayerRenderer renderer) {
                renderer.addLayer(new DemonHornLayer(renderer, event.getEntityModels()));
                renderer.addLayer(new DemonEyesLayer(renderer, event.getEntityModels()));
            }
        }
    }
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SOUL_BOLT.get(), SoulBoltRenderer::new);
        event.registerEntityRenderer(ModEntities.RIFT.get(), RiftRenderer::new);
        event.registerEntityRenderer(ModEntities.ALTAR_SWORD.get(), AltarSwordRenderer::new);
        event.registerEntityRenderer(ModEntities.FIRE_SLASH.get(), FireSlashRenderer::new);
    }
}