package com.example.demonicascension.client;

import com.example.demonicascension.DemonicAscension;
import com.example.demonicascension.config.ModConfigs;
import com.example.demonicascension.demon.DemonData;
import com.example.demonicascension.demon.DemonSkill;
import com.example.demonicascension.demon.ModAttachments;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.entity.monster.Enemy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = DemonicAscension.MODID, value = Dist.CLIENT)
public class VoidSightRenderer {

    private static final int RESCAN_INTERVAL = 10; // ticks

    // Hostiles burn red; players glow violet.
    private static final float[] HOSTILE_COLOUR = {1.00F, 0.25F, 0.20F};
    private static final float[] PLAYER_COLOUR = {0.72F, 0.40F, 1.00F};

    /** Rebuilt periodically rather than every frame, to keep the cost down. */
    private static final List<LivingEntity> targets = new ArrayList<>();
    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onClientTick(PlayerTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player self = mc.player;

        if (self == null || mc.level == null || event.getEntity() != self) {
            return;
        }

        if (++tickCounter < RESCAN_INTERVAL) {
            return;
        }
        tickCounter = 0;

        targets.clear();

        if (!isActive(self)) {
            return;
        }

        AABB search = self.getBoundingBox().inflate(ModConfigs.VOID_SIGHT_RANGE.get());

        for (Entity entity : mc.level.getEntities(self, search)) {
            if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
                continue;
            }
            if (living instanceof Enemy || living instanceof Player) {
                targets.add(living);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Player self = mc.player;

        if (self == null || mc.level == null || targets.isEmpty() || !isActive(self)) {
            return;
        }

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);

        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(ModRenderTypes.VOID_SIGHT_LINES);

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        for (LivingEntity target : targets) {
            if (!target.isAlive()) {
                continue;
            }

            // Interpolate so the outline doesn't lag behind a moving mob.
            double x = Mth.lerp(partialTick, target.xOld, target.getX());
            double y = Mth.lerp(partialTick, target.yOld, target.getY());
            double z = Mth.lerp(partialTick, target.zOld, target.getZ());

            AABB box = target.getBoundingBox().move(
                    x - target.getX(), y - target.getY(), z - target.getZ());

            float[] colour = target instanceof Player ? PLAYER_COLOUR : HOSTILE_COLOUR;

            // Fade with distance so a crowded cave doesn't become noise.
            double distance = self.distanceTo(target);
            float alpha = (float) Mth.clamp(1.0 - (distance / ModConfigs.VOID_SIGHT_RANGE.get()), 0.15, 0.85);

            LevelRenderer.renderLineBox(poseStack, consumer, box,
                    colour[0], colour[1], colour[2], alpha);
        }

        poseStack.popPose();

        buffers.endBatch(ModRenderTypes.VOID_SIGHT_LINES);
    }

    private static boolean isActive(Player player) {
        DemonData data = player.getData(ModAttachments.DEMON_DATA);
        return data.isTransformed() && data.hasSkill(DemonSkill.VOID_SIGHT);
    }
}