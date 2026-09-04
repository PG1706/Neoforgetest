package com.example.demonicascension.client;

import com.example.demonicascension.DemonicAscension;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Matrix4f;

/**
 * The eclipse's visual: dark, close-in fog (so the world visibly shrouds regardless of
 * whether the sky-cancelling mixin is active) and a giant soulfire disc drawn where the
 * sun would normally sit, using the exact same celestial rotation vanilla's own sun
 * quad uses (see {@code LevelRenderer.renderSky}) so it tracks correctly through the
 * sky rather than sitting fixed on screen.
 */
@EventBusSubscriber(modid = DemonicAscension.MODID, value = Dist.CLIENT)
public class EclipseSkyEvents {

    private static final ResourceLocation DISC_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(DemonicAscension.MODID, "textures/gui/eclipse_disc.png");

    private static boolean active() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && EclipseClientState.isActive(mc.level.getGameTime());
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (!active()) {
            return;
        }
        event.setRed(0.02F);
        event.setGreen(0.015F);
        event.setBlue(0.03F);
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (!active()) {
            return;
        }
        event.scaleNearPlaneDistance(0.3F);
        event.scaleFarPlaneDistance(0.4F);
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY || !active()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        // AFTER_SKY hands us a brand new, untransformed PoseStack (NeoForge passes
        // null through here and it defaults to identity) — unlike every other stage,
        // there's no camera view rotation baked in yet, so it has to be applied
        // explicitly, exactly as vanilla's own renderSky() applies its frustumMatrix
        // parameter first. Skipping this was why the disc didn't track the view.
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.mulPose(event.getModelViewMatrix());

        // Same celestial rotation vanilla's own sun quad uses, so the disc tracks
        // through the sky like a real celestial body instead of sitting fixed.
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(mc.level.getTimeOfDay(event.getPartialTick().getGameTimeDeltaPartialTick(false)) * 360.0F));

        Matrix4f matrix = poseStack.last().pose();
        float half = 60.0F; // twice the vanilla sun's half-size — this is meant to loom

        // Sky elements never interact with the depth buffer — vanilla's own sky/sun
        // draw disables depth writing for exactly this reason. Without it, later
        // geometry this same frame could occlude a quad that's meant to always read
        // as "infinitely far away".
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, DISC_TEXTURE);

        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.addVertex(matrix, -half, 100.0F, -half).setUv(0.0F, 0.0F);
        buffer.addVertex(matrix, half, 100.0F, -half).setUv(1.0F, 0.0F);
        buffer.addVertex(matrix, half, 100.0F, half).setUv(1.0F, 1.0F);
        buffer.addVertex(matrix, -half, 100.0F, half).setUv(0.0F, 1.0F);
        BufferUploader.drawWithShader(buffer.buildOrThrow());

        RenderSystem.depthMask(true);

        poseStack.popPose();
    }
}
