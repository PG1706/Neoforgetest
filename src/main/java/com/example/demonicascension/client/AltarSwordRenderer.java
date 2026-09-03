package com.example.demonicascension.client;

import com.example.demonicascension.DemonicAscension;
import com.example.demonicascension.entity.AltarSwordEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Renders the 3D altar sword prop, slowly spinning and bobbing tip-down over the altar. */
public class AltarSwordRenderer extends EntityRenderer<AltarSwordEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(DemonicAscension.MODID, "textures/entity/altar_sword.png");

    private final ModelPart root;

    public AltarSwordRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.root = context.bakeLayer(AltarSwordModel.LAYER);
    }

    @Override
    public void render(AltarSwordEntity entity, float yaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        float age = entity.tickCount + partialTick;
        poseStack.translate(0.0, 0.65 + Math.sin(age * 0.05F) * 0.08F, 0.0);
        poseStack.mulPose(Axis.YP.rotationDegrees(age * 1.5F));
        // Standard entity-model flip: parts are authored with +Y as "down" (matching
        // every other model in this codebase), so this maps that to world-down —
        // meaning the blade, placed at positive Y, ends up pointing down as intended.
        poseStack.scale(-1.0F, -1.0F, 1.0F);

        root.render(poseStack, buffer.getBuffer(RenderType.entityCutout(TEXTURE)),
                packedLight, OverlayTexture.NO_OVERLAY);

        poseStack.popPose();

        super.render(entity, yaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(AltarSwordEntity entity) {
        return TEXTURE;
    }
}
