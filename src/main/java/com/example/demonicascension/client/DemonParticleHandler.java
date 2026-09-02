package com.example.demonicascension.client;

import com.example.demonicascension.DemonicAscension;
import com.example.demonicascension.demon.ModAttachments;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = DemonicAscension.MODID, value = Dist.CLIENT)
public class DemonParticleHandler {

    private static int fadeTicks = 0;
    private static final int FADE_DURATION = 40; // 2 seconds

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null || mc.isPaused()) {
            return;
        }

        boolean transformed = player.getData(ModAttachments.DEMON_DATA).isTransformed();

        int particlesThisTick;
        if (transformed) {
            fadeTicks = FADE_DURATION;
            particlesThisTick = 2;
        } else if (fadeTicks > 0) {
            fadeTicks--;
            particlesThisTick = (fadeTicks * 2) / FADE_DURATION;
        } else {
            return;
        }

        var random = player.getRandom();
        for (int i = 0; i < particlesThisTick; i++) {
            double x = player.getX() + (random.nextDouble() - 0.5) * 0.8;
            double y = player.getY() + random.nextDouble() * 1.8;
            double z = player.getZ() + (random.nextDouble() - 0.5) * 0.8;

            mc.level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 0.0, 0.02, 0.0);
        }
    }
}