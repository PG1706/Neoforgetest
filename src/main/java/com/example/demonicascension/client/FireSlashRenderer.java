package com.example.demonicascension.client;

import com.example.demonicascension.DemonicAscension;
import com.example.demonicascension.entity.FireSlashEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Renders the curved 3D {@link FireSlashModel}, oriented each frame to the entity's
 * own yaw/pitch — which {@link FireSlashEntity} sets from its flight direction — so
 * the blade visibly points and curves along its actual path, unlike a camera-facing
 * billboard.
 */
public class FireSlashRenderer extends EntityRenderer<FireSlashEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(DemonicAscension.MODID, "textures/entity/fire_slash.png");

    private final ModelPart root;

    public FireSlashRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.root = context.bakeLayer(FireSlashModel.LAYER);
    }

    @Override
    public void render(FireSlashEntity entity, float yaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        // Same formula vanilla's ArrowRenderer uses to orient a shaft authored along
        // local +X to an entity's yRot/xRot — FireSlashModel is authored along the
        // same axis specifically so this proven formula applies unchanged.
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, entity.yRotO, entity.getYRot()) - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, entity.xRotO, entity.getXRot())));

        // Roll around the now-forward-facing local X axis.
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));

        // A further fixed 90° on the Z axis — distinct from the dynamic pitch rotation
        // above, which has to stay tied to the entity's actual aim.
        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));

        // Opaque cutout, not translucent: the texture has no real transparency to
        // lose, and a chain of overlapping translucent boxes isn't depth-sorted
        // correctly by Minecraft's simple entity renderer — parts of it were
        // disappearing depending on draw order. Cutout has no such sorting problem.
        root.render(poseStack, buffer.getBuffer(RenderType.entityCutout(TEXTURE)),
                packedLight, OverlayTexture.NO_OVERLAY);

        poseStack.popPose();

        super.render(entity, yaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(FireSlashEntity entity) {
        return TEXTURE;
    }
}
