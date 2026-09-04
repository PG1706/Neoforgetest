package com.example.demonicascension.client;

import com.example.demonicascension.DemonicAscension;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.model.geom.EntityModelSet;

/**
 * Renders the Abyssal Sword using the same 3D {@link AltarSwordModel} the altar prop
 * uses, everywhere except the inventory/creative-tab GUI slot — there it falls back to
 * the original flat icon (registered separately as a standalone model, since the
 * item's own model now points at this renderer instead). Per-context positioning
 * (hand, ground, item frame) comes from the "display" block in
 * assets/demonicascension/models/item/abyssal_sword.json, exactly like vanilla's
 * shield — this class only corrects the model's own authored orientation.
 */
public class AbyssalSwordItemRenderer extends BlockEntityWithoutLevelRenderer {

    /** The original flat model, registered as an extra since it's no longer the item's own model. */
    public static final ResourceLocation GUI_ICON_MODEL_ID =
            ResourceLocation.fromNamespaceAndPath(DemonicAscension.MODID, "item/abyssal_sword_gui");
    public static final ModelResourceLocation GUI_ICON_MODEL =
            ModelResourceLocation.standalone(GUI_ICON_MODEL_ID);

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(DemonicAscension.MODID, "textures/entity/altar_sword.png");

    private final ModelPart root;

    public AbyssalSwordItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet modelSet) {
        super(dispatcher, modelSet);
        this.root = modelSet.bakeLayer(AltarSwordModel.LAYER);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (context == ItemDisplayContext.GUI) {
            ModelManager models = Minecraft.getInstance().getModelManager();
            BakedModel flatIcon = models.getModel(GUI_ICON_MODEL);

            // The caller already ran this same render() once for our own item model
            // (an identity transform, since abyssal_sword.json defines no "gui" entry)
            // before invoking us, including its internal -0.5/-0.5/-0.5 translate.
            // Calling render() again below repeats that translate for the flat icon's
            // own model — undo the first one so it isn't offset twice.
            poseStack.pushPose();
            poseStack.translate(0.5, 0.5, 0.5);
            Minecraft.getInstance().getItemRenderer()
                    .render(stack, context, false, poseStack, buffer, packedLight, packedOverlay, flatIcon);
            poseStack.popPose();
            return;
        }

        poseStack.pushPose();

        // All hand/ground/frame positioning is done here in one place, rather than
        // split between this code and the model json's "display" block — composing a
        // large multi-axis json rotation with vanilla's own fixed recenter offset (and
        // our flip below) got unpredictable for anything beyond a small tweak, so the
        // json only carries translation/scale now and every rotation happens here.
        boolean leftHand = context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
        boolean inHand = context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;

        // The model hangs blade-down by default (see AltarSwordRenderer) — flip it
        // blade-up for a held weapon, then angle it out to one side just enough to
        // read as held rather than swinging its ~1.6-block length out past the body.
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        // Roll around the blade's own length axis (local Y — see AltarSwordModel's own
        // "blade at positive Y" doc comment), which doesn't move the tip at all.
        poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
        if (inHand) {
            poseStack.mulPose(Axis.YP.rotationDegrees(leftHand ? -8.0F : 8.0F));
        }

        // Same flip AltarSwordRenderer applies: the model is authored with +Y as
        // "down" (blade at positive Y), so this maps that to world-down.
        poseStack.scale(-1.0F, -1.0F, 1.0F);

        root.render(poseStack, buffer.getBuffer(RenderType.entityCutout(TEXTURE)), packedLight, packedOverlay);

        poseStack.popPose();
    }
}
