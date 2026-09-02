package com.example.demonicascension.client;

import com.example.demonicascension.DemonicAscension;
import com.example.demonicascension.entity.SoulBoltEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class SoulBoltRenderer extends EntityRenderer<SoulBoltEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(DemonicAscension.MODID, "textures/entity/soul_bolt.png");

    private static final float SIZE = 0.5F;

    public SoulBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(SoulBoltEntity entity, float yaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {

        poseStack.pushPose();

        // Face the camera, and spin slowly for a bit of life.
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        float spin = (entity.tickCount + partialTick) * 12.0F;
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(spin));

        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));

        // A single camera-facing quad. Full brightness so it glows in the dark.
        quad(consumer, matrix, normal, -SIZE, -SIZE, 0.0F, 1.0F);
        quad(consumer, matrix, normal, -SIZE, SIZE, 0.0F, 0.0F);
        quad(consumer, matrix, normal, SIZE, SIZE, 1.0F, 0.0F);
        quad(consumer, matrix, normal, SIZE, -SIZE, 1.0F, 1.0F);

        poseStack.popPose();

        super.render(entity, yaw, partialTick, poseStack, buffer, packedLight);
    }

    private void quad(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal,
                      float x, float y, float u, float v) {
        consumer.addVertex(matrix, x, y, 0.0F)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(Mth.clamp(15728880, 0, 15728880))
                .setNormal(0.0F, 1.0F, 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(SoulBoltEntity entity) {
        return TEXTURE;
    }
}