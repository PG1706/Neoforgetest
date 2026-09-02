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

public class DemonHornLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(DemonicAscension.MODID, "textures/entity/demon_horns.png");

    private final ModelPart horns;

    public DemonHornLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
                          EntityModelSet modelSet) {
        super(parent);
        this.horns = modelSet.bakeLayer(DemonHornModel.HORN_LAYER);
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

        poseStack.pushPose();

        // Move into the head's coordinate space so horns follow head rotation.
        this.getParentModel().head.translateAndRotate(poseStack);

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        this.horns.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);

        poseStack.popPose();
    }
}