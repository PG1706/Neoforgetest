package com.example.demonicascension.client;

import com.example.demonicascension.DemonicAscension;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

public class DemonHornModel {

    public static final ModelLayerLocation HORN_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(DemonicAscension.MODID, "demon_horns"), "main");

    /**
     * Coordinates are relative to the player's head part.
     * The head occupies x -4..4, y -8..0, z -4..4, so z=-4 is the face plane
     * and x=±4 are the side edges. Bases sit flush against the skull edge to
     * avoid clipping through it. Negative X rotation sweeps the horns backward.
     */
    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // --- Right horn (player's right, negative X) ---
        PartDefinition rightBase = root.addOrReplaceChild("right_horn_base",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-1.5F, -3.0F, -1.5F, 3.0F, 3.0F, 3.0F),
                PartPose.offsetAndRotation(-4.0F, -5.5F, -3.5F, -0.30F, 0.0F, -0.30F));

        PartDefinition rightMid = rightBase.addOrReplaceChild("right_horn_mid",
                CubeListBuilder.create()
                        .texOffs(0, 6)
                        .addBox(-1.25F, -3.0F, -1.25F, 2.5F, 3.0F, 2.5F),
                PartPose.offsetAndRotation(0.0F, -2.75F, 0.0F, -0.35F, 0.0F, -0.25F));

        rightMid.addOrReplaceChild("right_horn_tip",
                CubeListBuilder.create()
                        .texOffs(0, 12)
                        .addBox(-1.0F, -2.5F, -1.0F, 2.0F, 2.5F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -2.75F, 0.0F, -0.40F, 0.0F, -0.20F));

        // --- Left horn (mirrored: Z rotations flip sign) ---
        PartDefinition leftBase = root.addOrReplaceChild("left_horn_base",
                CubeListBuilder.create()
                        .texOffs(16, 0)
                        .addBox(-1.5F, -3.0F, -1.5F, 3.0F, 3.0F, 3.0F),
                PartPose.offsetAndRotation(4.0F, -5.5F, -3.5F, -0.30F, 0.0F, 0.30F));

        PartDefinition leftMid = leftBase.addOrReplaceChild("left_horn_mid",
                CubeListBuilder.create()
                        .texOffs(16, 6)
                        .addBox(-1.25F, -3.0F, -1.25F, 2.5F, 3.0F, 2.5F),
                PartPose.offsetAndRotation(0.0F, -2.75F, 0.0F, -0.35F, 0.0F, 0.25F));

        leftMid.addOrReplaceChild("left_horn_tip",
                CubeListBuilder.create()
                        .texOffs(16, 12)
                        .addBox(-1.0F, -2.5F, -1.0F, 2.0F, 2.5F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -2.75F, 0.0F, -0.40F, 0.0F, 0.20F));

        return LayerDefinition.create(mesh, 32, 32);
    }
}