package com.example.demonicascension.client;

import com.example.demonicascension.DemonicAscension;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * Coordinates are relative to the player's head part (see {@link DemonHornModel}): the
 * head occupies x -4..4, y -8..0, z -4..4, so z=-4 is the face plane. Each eye is a
 * thin flat box sitting just proud of that plane — an overlay, not a horn.
 * <p>
 * A box's UV footprint is normally tied to its own physical size, which is far too
 * small here to show painted detail (a lesson learned building the flame halo) — so
 * this uses {@code addBox(..., CubeDeformation.NONE, texScaleU, texScaleV)} with a
 * scale computed to map each eye's front face across the <em>entire</em> bound
 * texture, regardless of how small the box itself is.
 */
public class DemonEyesModel {

    public static final ModelLayerLocation EYES_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(DemonicAscension.MODID, "demon_eyes"), "main");

    private static final float WIDTH = 2.6F;
    private static final float HEIGHT = 1.4F;
    private static final float DEPTH = 1.0F; // must be a whole number — see addEye

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        addEye(root, "right_eye", -1.6F);
        addEye(root, "left_eye", 1.6F);

        // texScale is a ratio against the declared texture size, not a pixel count,
        // so a trivial 1x1 declaration keeps the per-box scale math simple below.
        return LayerDefinition.create(mesh, 1, 1);
    }

    private static void addEye(PartDefinition root, String name, float x) {
        // The front face's raw UV footprint is [depth, depth+width] x [depth, depth+height]
        // (see the box-UV unwrap math), not [0,width] x [0,height] — offsetting texOffs by
        // -depth cancels that leading "+depth" out, so with texScale = width/height and a
        // 1x1 declared texture size, the front face maps exactly [0,1] on both axes.
        CubeListBuilder cube = CubeListBuilder.create()
                .texOffs(-(int) DEPTH, -(int) DEPTH)
                .addBox(-WIDTH / 2.0F, -HEIGHT / 2.0F, -DEPTH / 2.0F, WIDTH, HEIGHT, DEPTH,
                        CubeDeformation.NONE, WIDTH, HEIGHT);

        root.addOrReplaceChild(name, cube, PartPose.offset(x, -3.5F, -4.0F));
    }
}
