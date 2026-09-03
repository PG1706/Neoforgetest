package com.example.demonicascension.client;

import com.example.demonicascension.DemonicAscension;
import com.example.demonicascension.demon.ModAttachments;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = DemonicAscension.MODID, value = Dist.CLIENT)
public class DemonParticleHandler {

    private static final int FADE_DURATION = 40; // 2 seconds

    // Each transformed player fades independently, since any of them can be seen dropping form at any time.
    private static final Map<UUID, Integer> fadeTicksByPlayer = new HashMap<>();

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.isPaused()) {
            return;
        }

        boolean localFirstPerson = mc.options.getCameraType().isFirstPerson();

        for (AbstractClientPlayer player : mc.level.players()) {
            boolean transformed = player.getData(ModAttachments.DEMON_DATA).isTransformed();
            UUID id = player.getUUID();
            int fadeTicks = fadeTicksByPlayer.getOrDefault(id, 0);

            int particlesThisTick;
            if (transformed) {
                fadeTicks = FADE_DURATION;
                particlesThisTick = 2;
            } else if (fadeTicks > 0) {
                fadeTicks--;
                particlesThisTick = (fadeTicks * 2) / FADE_DURATION;
            } else {
                fadeTicksByPlayer.remove(id);
                continue;
            }
            fadeTicksByPlayer.put(id, fadeTicks);

            if (particlesThisTick <= 0) {
                continue;
            }

            boolean isLocal = player == mc.player;
            if (isLocal && localFirstPerson) {
                // The aura sits right in front of the camera in first person; it just obscures the view.
                continue;
            }

            // Keep particles off the local player's own head so they don't obscure vision in third person either.
            double yMax = isLocal ? Math.max(0.2, player.getEyeHeight() - 0.3) : 1.8;

            var random = player.getRandom();
            for (int i = 0; i < particlesThisTick; i++) {
                double x = player.getX() + (random.nextDouble() - 0.5) * 0.8;
                double y = player.getY() + random.nextDouble() * yMax;
                double z = player.getZ() + (random.nextDouble() - 0.5) * 0.8;

                mc.level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 0.0, 0.02, 0.0);
            }
        }
    }
}
