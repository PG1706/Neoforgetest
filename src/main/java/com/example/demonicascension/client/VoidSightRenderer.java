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
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = DemonicAscension.MODID, value = Dist.CLIENT)
public class VoidSightRenderer {

    private static final int RESCAN_INTERVAL = 10; // ticks

    // Hostiles burn red, players glow violet, everything else alive burns green.
    private static final float[] HOSTILE_COLOUR = {1.00F, 0.25F, 0.20F};
    private static final float[] PLAYER_COLOUR = {0.72F, 0.40F, 1.00F};
    private static final float[] FRIENDLY_COLOUR = {0.35F, 1.00F, 0.45F};

    private static final float FLAME_WIDTH = 0.45F;
    private static final float FLAME_HEIGHT = 0.7F;

    /** The texture is a vertical strip of frames — see RiftRenderer for the same convention. */
    private static final int FRAMES = 8;
    private static final int TICKS_PER_FRAME = 2;

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
            if (living != self) {
                targets.add(living);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        // AFTER_WEATHER used to be enough, but a shaderpack's deferred/composite
        // passes (Iris and friends) run their own depth-aware recompositing between
        // AFTER_WEATHER and the end of LevelRenderer.renderLevel — which silently
        // reintroduces the exact occlusion this render type's NO_DEPTH_TEST is meant
        // to avoid (see ModRenderTypes.FORCE_MAIN_TARGET). AFTER_LEVEL fires from
        // GameRenderer only once LevelRenderer.renderLevel has fully returned, so
        // nothing legitimate redraws world geometry after it.
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Player self = mc.player;

        if (self == null || mc.level == null || targets.isEmpty() || !isActive(self)) {
            return;
        }

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float age = mc.level.getGameTime() + partialTick;

        // Unlike every other stage, AFTER_LEVEL is dispatched with no PoseStack of its
        // own (NeoForge defaults it to a fresh identity one) — the camera's view
        // rotation has to be applied explicitly instead of arriving pre-baked.
        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer consumer = null;

        poseStack.pushPose();
        poseStack.mulPose(event.getModelViewMatrix());
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        for (LivingEntity target : targets) {
            // A target can die or be removed (e.g. a dimension change through the
            // rift) between the periodic scan and this render call. Skipping it here
            // must not leave the batch started with zero vertices in it, or
            // endBatch below throws "BufferBuilder was empty".
            if (!target.isAlive() || target.isRemoved()) {
                continue;
            }

            // Interpolate so the flame doesn't lag behind a moving target.
            double x = Mth.lerp(partialTick, target.xOld, target.getX());
            double y = Mth.lerp(partialTick, target.yOld, target.getY());
            double z = Mth.lerp(partialTick, target.zOld, target.getZ());

            AABB box = target.getBoundingBox().move(
                    x - target.getX(), y - target.getY(), z - target.getZ());
            Vec3 anchor = box.getCenter();

            float[] colour = target instanceof Player ? PLAYER_COLOUR
                    : target instanceof Enemy ? HOSTILE_COLOUR
                    : FRIENDLY_COLOUR;

            // Fade with distance so a crowded cave doesn't become noise.
            double distance = self.distanceTo(target);
            float alpha = (float) Mth.clamp(1.0 - (distance / ModConfigs.VOID_SIGHT_RANGE.get()), 0.15, 0.85);

            if (consumer == null) {
                consumer = buffers.getBuffer(ModRenderTypes.VOID_SIGHT_FLAME);
            }
            renderFlame(poseStack, consumer, mc, anchor, colour, alpha, age, target.getId());
        }

        poseStack.popPose();

        if (consumer != null) {
            buffers.endBatch(ModRenderTypes.VOID_SIGHT_FLAME);
        }
    }

    /** A camera-facing flame billboard, gently bobbing and pulsing so it reads as alight rather than static. */
    private static void renderFlame(PoseStack poseStack, VertexConsumer consumer, Minecraft mc,
                                    Vec3 anchor, float[] colour, float alpha, float age, int seed) {
        // Offset the phase per-target so a room full of targets doesn't flicker in lockstep.
        float phase = age * 0.15F + seed;
        float bob = Mth.sin(phase) * 0.06F;
        float pulse = 1.0F + Mth.sin(phase * 1.7F) * 0.08F;

        // Same per-target offset applied to the frame clock, so targets don't all lick in lockstep either.
        int frame = (int) ((age + seed) / TICKS_PER_FRAME) % FRAMES;
        float v0 = frame / (float) FRAMES;
        float v1 = (frame + 1) / (float) FRAMES;

        poseStack.pushPose();
        poseStack.translate(anchor.x, anchor.y + bob, anchor.z);
        poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());

        Matrix4f matrix = poseStack.last().pose();

        float hw = (FLAME_WIDTH / 2.0F) * pulse;
        float hh = (FLAME_HEIGHT / 2.0F) * pulse;

        quad(consumer, matrix, -hw, -hh, 0.0F, v1, colour, alpha);
        quad(consumer, matrix, -hw, hh, 0.0F, v0, colour, alpha);
        quad(consumer, matrix, hw, hh, 1.0F, v0, colour, alpha);
        quad(consumer, matrix, hw, -hh, 1.0F, v1, colour, alpha);

        poseStack.popPose();
    }

    private static void quad(VertexConsumer consumer, Matrix4f matrix,
                             float x, float y, float u, float v, float[] colour, float alpha) {
        consumer.addVertex(matrix, x, y, 0.0F)
                .setColor(colour[0], colour[1], colour[2], alpha)
                .setUv(u, v);
    }

    private static boolean isActive(Player player) {
        DemonData data = player.getData(ModAttachments.DEMON_DATA);
        return data.isTransformed() && data.hasSkill(DemonSkill.VOID_SIGHT);
    }
}
