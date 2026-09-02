package com.example.demonicascension.client;

import com.example.demonicascension.DemonicAscension;
import com.example.demonicascension.network.UseAbilityPayload;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = DemonicAscension.MODID, value = Dist.CLIENT)
public class ModKeybinds {

    public static final String CATEGORY = "key.categories." + DemonicAscension.MODID;

    public static final KeyMapping BOLT_KEY = new KeyMapping(
            "key." + DemonicAscension.MODID + ".bolt",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);

    public static final KeyMapping DASH_KEY = new KeyMapping(
            "key." + DemonicAscension.MODID + ".dash",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY);

    public static final KeyMapping SKILL_TREE_KEY = new KeyMapping(
            "key." + DemonicAscension.MODID + ".skill_tree",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORY);

    public static final KeyMapping RIFT_KEY = new KeyMapping(
            "key." + DemonicAscension.MODID + ".rift",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY);

    @EventBusSubscriber(modid = DemonicAscension.MODID, value = Dist.CLIENT)
    public static class Registration {
        @SubscribeEvent
        public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
            event.register(BOLT_KEY);
            event.register(DASH_KEY);
            event.register(SKILL_TREE_KEY);
            event.register(RIFT_KEY);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        while (BOLT_KEY.consumeClick()) {
            PacketDistributor.sendToServer(
                    new UseAbilityPayload(UseAbilityPayload.Ability.BOLT.ordinal()));
        }
        while (DASH_KEY.consumeClick()) {
            PacketDistributor.sendToServer(
                    new UseAbilityPayload(UseAbilityPayload.Ability.DASH.ordinal()));
        }
        while (RIFT_KEY.consumeClick()) {
            PacketDistributor.sendToServer(
                    new UseAbilityPayload(UseAbilityPayload.Ability.RIFT.ordinal()));
        }
        while (SKILL_TREE_KEY.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            // Only open when no other screen is active.
            if (mc.screen == null) {
                mc.setScreen(new SkillTreeScreen());
            }
        }
    }
}