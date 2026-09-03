package com.example.demonicascension.client;

import com.example.demonicascension.DemonicAscension;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * A faithful 3D translation of the Tainted Phoenix Blade artwork: a long jagged
 * tooth-edged blade, a swept multi-segment wing guard around a glowing gem, a
 * ribbed grip, and a layered spike pommel — built tip-down (blade at positive Y,
 * the standard "down" direction for model parts) so it renders point-first once
 * the renderer applies the usual entity-model Y flip.
 */
public class AltarSwordModel {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(DemonicAscension.MODID, "altar_sword"), "main");

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // --- Pommel: a layered spike crest, narrow at the very top. ---
        root.addOrReplaceChild("pommel_tip",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -7.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.ZERO);
        root.addOrReplaceChild("pommel_crown",
                CubeListBuilder.create().texOffs(0, 6).addBox(-2.0F, -4.2F, -1.4F, 4.0F, 3.2F, 2.8F),
                PartPose.ZERO);
        for (int side = -1; side <= 1; side += 2) {
            // The box itself extends only in +X from its origin, so it has to be
            // mirrored explicitly per side — offset/rotation alone just folds the
            // -X side's box back toward the centre instead of sweeping outward.
            float ox = side < 0 ? -2.4F : 0.0F;
            root.addOrReplaceChild("pommel_wing" + side,
                    CubeListBuilder.create().texOffs(20, 0).addBox(ox, -0.6F, -0.8F, 2.4F, 1.2F, 1.6F),
                    PartPose.offsetAndRotation(side * 1.6F, -2.2F, 0.0F, 0.0F, 0.0F, side * -0.3F));
        }

        // --- Grip: ribbed vertebrae segments — the ribbing reads through the
        // texture's banding, not through physical gaps, so each segment overlaps
        // the previous one slightly and the whole grip stays solidly connected. ---
        for (int i = 0; i < 3; i++) {
            float y = -1.2F + i * 1.8F;
            root.addOrReplaceChild("grip_seg" + i,
                    CubeListBuilder.create().texOffs(0, 12).addBox(-1.4F, y, -1.4F, 2.8F, 1.8F, 2.8F),
                    PartPose.ZERO);
        }

        // --- Guard: a glowing gem flanked by swept, multi-segment wings. Each
        // segment's rotation is modest and only compounds a little through the
        // chain, so the sweep stays a smooth fan rather than folding into a twist.
        root.addOrReplaceChild("guard_core",
                CubeListBuilder.create().texOffs(20, 6).addBox(-1.6F, 4.0F, -1.6F, 3.2F, 3.2F, 3.2F),
                PartPose.ZERO);
        root.addOrReplaceChild("guard_gem",
                CubeListBuilder.create().texOffs(0, 20).addBox(-1.1F, 4.6F, -1.9F, 2.2F, 2.2F, 0.6F),
                PartPose.ZERO);

        for (int side = -1; side <= 1; side += 2) {
            // Every box here extends only in +X from its own origin, and every
            // child's offset likewise only advances in +X — so both need mirroring
            // for the -1 side, not just the rotation, or the geometry folds back
            // toward the centre instead of sweeping outward.
            float m = side; // +1 or -1, multiplies every X-extent/offset below
            PartDefinition wingBase = root.addOrReplaceChild("wing_base" + side,
                    CubeListBuilder.create().texOffs(0, 24).addBox(Math.min(0, m * 3.6F), -1.1F, -1.0F, 3.6F, 2.2F, 2.0F),
                    PartPose.offsetAndRotation(side * 1.6F, 5.6F, 0.0F, 0.0F, 0.0F, side * -0.20F));
            PartDefinition wingMid = wingBase.addOrReplaceChild("wing_mid" + side,
                    CubeListBuilder.create().texOffs(0, 29).addBox(Math.min(0, m * 3.2F), -0.9F, -0.8F, 3.2F, 1.8F, 1.6F),
                    PartPose.offsetAndRotation(m * 3.6F, 0.0F, 0.0F, 0.0F, 0.0F, side * -0.25F));
            wingMid.addOrReplaceChild("wing_tip" + side,
                    CubeListBuilder.create().texOffs(16, 29).addBox(Math.min(0, m * 2.6F), -0.6F, -0.6F, 2.6F, 1.2F, 1.2F),
                    PartPose.offsetAndRotation(m * 3.2F, 0.0F, 0.0F, 0.0F, 0.0F, side * -0.30F));
            root.addOrReplaceChild("hook" + side,
                    CubeListBuilder.create().texOffs(20, 20).addBox(Math.min(0, m * 1.6F), -0.5F, -0.5F, 1.6F, 1.0F, 1.0F),
                    PartPose.offsetAndRotation(side * 2.0F, 3.9F, 0.0F, 0.0F, 0.0F, side * 0.5F));
        }

        // --- Blade: much longer than the first pass, still a wide upper section
        // with jagged tooth notches tapering through to a narrower point. Each
        // segment overlaps the one before it by 0.2 so there's no exact shared
        // edge for seam-cracking to appear at when viewed edge-on from below. ---
        root.addOrReplaceChild("blade_upper",
                CubeListBuilder.create().texOffs(0, 37).addBox(-2.2F, 7.0F, -1.0F, 4.4F, 14.0F, 2.0F),
                PartPose.ZERO);
        for (float y : new float[]{10.0F, 17.0F}) {
            for (int side = -1; side <= 1; side += 2) {
                root.addOrReplaceChild("tooth" + y + "_" + side,
                        CubeListBuilder.create().texOffs(0, 33).addBox(0.0F, 0.0F, -0.9F, 1.1F, 1.4F, 1.8F),
                        PartPose.offset(side < 0 ? -3.3F : 2.2F, y, 0.0F));
            }
        }
        root.addOrReplaceChild("blade_lower",
                CubeListBuilder.create().texOffs(0, 53).addBox(-1.5F, 20.8F, -0.7F, 3.0F, 16.0F, 1.4F),
                PartPose.ZERO);
        root.addOrReplaceChild("blade_tip",
                CubeListBuilder.create().texOffs(14, 53).addBox(-0.7F, 36.6F, -0.4F, 1.4F, 7.0F, 0.8F),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 40, 72);
    }
}
