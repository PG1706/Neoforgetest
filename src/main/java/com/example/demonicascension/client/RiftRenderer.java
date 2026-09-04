package com.example.demonicascension.client;

import com.example.demonicascension.DemonicAscension;
import com.example.demonicascension.entity.RiftEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public class RiftRenderer extends EntityRenderer<RiftEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(DemonicAscension.MODID, "textures/entity/rift.png");

    /** The texture is a vertical strip of frames. */
    private static final int FRAMES = 8;
    private static final int TICKS_PER_FRAME = 2;

    private static final float WIDTH = 1.6F;
    private static final float HEIGHT = 2.4F;

    public RiftRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(RiftEntity entity, float yaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {

        float age = entity.tickCount + partialTick;

        // Open fast, hold, then snap shut at the end of its life.
        float openProgress = Mth.clamp(age / 8.0F, 0.0F, 1.0F);
        float remaining = RiftEntity.LIFETIME - age;
        float closeProgress = Mth.clamp(remaining / 8.0F, 0.0F, 1.0F);
        float scale = openProgress * closeProgress;

        if (scale <= 0.01F) {
            return;
        }

        // Gentle breathing so it never sits perfectly still.
        float pulse = 1.0F + Mth.sin(age * 0.25F) * 0.05F;

        int frame = ((int) (age / TICKS_PER_FRAME)) % FRAMES;
        float v0 = frame / (float) FRAMES;
        float v1 = (frame + 1) / (float) FRAMES;

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));

        poseStack.pushPose();
        poseStack.translate(0.0, HEIGHT / 2.0, 0.0);

        // Two crossed planes so the rift reads from any angle.
        for (int plane = 0; plane < 2; plane++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(plane * 90.0F + entity.getYRot()));

            Matrix4f matrix = poseStack.last().pose();

            float hw = (WIDTH / 2.0F) * scale * pulse;
            float hh = (HEIGHT / 2.0F) * scale;

            vertex(consumer, poseStack, matrix, -hw, -hh, 0.0F, v1);
            vertex(consumer, poseStack, matrix, hw, -hh, 1.0F, v1);
            vertex(consumer, poseStack, matrix, hw, hh, 1.0F, v0);
            vertex(consumer, poseStack, matrix, -hw, hh, 0.0F, v0);

            // Back face, so it isn't invisible from behind.
            vertex(consumer, poseStack, matrix, -hw, hh, 0.0F, v0);
            vertex(consumer, poseStack, matrix, hw, hh, 1.0F, v0);
            vertex(consumer, poseStack, matrix, hw, -hh, 1.0F, v1);
            vertex(consumer, poseStack, matrix, -hw, -hh, 0.0F, v1);

            poseStack.popPose();
        }

        poseStack.popPose();

        super.render(entity, yaw, partialTick, poseStack, buffer, packedLight);
    }

    private void vertex(VertexConsumer consumer, PoseStack poseStack, Matrix4f matrix,
                        float x, float y, float u, float v) {
        consumer.addVertex(matrix, x, y, 0.0F)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(15728880) // full brightness — it glows
                .setNormal(poseStack.last(), 0.0F, 0.0F, 1.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(RiftEntity entity) {
        return TEXTURE;
    }
}