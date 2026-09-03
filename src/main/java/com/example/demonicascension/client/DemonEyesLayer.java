package com.example.demonicascension.client;

import com.example.demonicascension.DemonicAscension;
import com.example.demonicascension.demon.ModAttachments;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * A flat glow painted directly onto the face, unlike the horns which are protruding
 * geometry. Built the same way as {@link DemonHornLayer} — a baked model part
 * rendered with a full-bright emissive texture. Brightness breathes slowly via an
 * alpha tint on the model part rather than a frame-strip texture, since a baked
 * part's UV is fixed at bake time.
 */
public class DemonEyesLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(DemonicAscension.MODID, "textures/entity/demon_eyes.png");

    /** One full breathe every ~3 seconds, never fully dark. */
    private static final float PULSE_PERIOD_TICKS = 60.0F;
    private static final int ALPHA_MIN = 130;
    private static final int ALPHA_MAX = 255;

    private final ModelPart eyes;

    public DemonEyesLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
                          EntityModelSet modelSet) {
        super(parent);
        this.eyes = modelSet.bakeLayer(DemonEyesModel.EYES_LAYER);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {

        if (player.isInvisible() || player.isSpectator()) {
            return;
        }
        if (!player.getData(ModAttachments.DEMON_DATA).isTransformed()) {
            return;
        }

        float age = player.tickCount + partialTick;
        float phase = 0.5F - 0.5F * Mth.cos(2.0F * Mth.PI * age / PULSE_PERIOD_TICKS);
        int alpha = ALPHA_MIN + Math.round((ALPHA_MAX - ALPHA_MIN) * phase);
        int color = (alpha << 24) | 0xFFFFFF;

        poseStack.pushPose();
        this.getParentModel().head.translateAndRotate(poseStack);

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));
        this.eyes.render(poseStack, consumer, 15728880, OverlayTexture.NO_OVERLAY, color);

        poseStack.popPose();
    }
}
