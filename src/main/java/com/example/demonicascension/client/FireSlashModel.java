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
 * A curved 3D blade of fire: a short chain of tapering segments, each a child of the
 * last and offset to its tip with a small extra rotation — the same chaining
 * {@link AltarSwordModel} uses to sweep its guard wings — so the cumulative rotation
 * reads as one continuous crescent instead of a flat billboard.
 *
 * <p>Built extending along local +X, curving in the X/Y plane via cumulative Z
 * rotation — the same axis convention vanilla's {@code AbstractArrow}/{@code
 * ArrowRenderer} use, so {@code FireSlashRenderer} can reuse their exact proven
 * orientation formula instead of a new one. Symmetric taper — thin at both ends,
 * thickest in the middle — since a one-sided point read as a flat, blunt cut-off on
 * the untapered end, especially viewed nearly head-on when flying away.
 */
public class FireSlashModel {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(DemonicAscension.MODID, "fire_slash"), "main");

    private static final int SEGMENTS = 7;
    private static final float SWEEP_DEGREES = 110.0F;
    private static final float SEGMENT_LENGTH = 6.0F;

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        float stepDeg = SWEEP_DEGREES / (SEGMENTS - 1);
        PartDefinition parent = root;

        for (int i = 0; i < SEGMENTS; i++) {
            float t = i / (float) (SEGMENTS - 1);
            // Thin at both ends (t=0 and t=1), thickest around the middle.
            float half = (float) (Math.pow(Math.sin(Math.PI * t), 0.7) * 3.8) + 0.1F;
            // A blade's own thin profile — noticeably thinner than before, but still
            // floored well above the near-zero sliver that went edge-on invisible.
            float thickness = 0.5F + half * 0.08F;

            // Swapped from Y/Z as first written: the renderer's two roll rotations mean
            // local Y and Z don't map to "thin" and "wide" on screen the way authoring
            // them naively would suggest — this order is what actually reads correctly.
            CubeListBuilder box = CubeListBuilder.create()
                    .texOffs(0, 0)
                    .addBox(0.0F, -half, -thickness * 0.5F, SEGMENT_LENGTH, half * 2.0F, thickness);

            PartPose pose = i == 0
                    // Center the sweep: the first segment starts rotated back by half
                    // the total arc, so the whole chain curves evenly around its middle.
                    ? PartPose.rotation(0.0F, 0.0F, (float) Math.toRadians(-stepDeg * (SEGMENTS - 1) / 2.0))
                    : PartPose.offsetAndRotation(SEGMENT_LENGTH, 0.0F, 0.0F,
                            0.0F, 0.0F, (float) Math.toRadians(stepDeg));

            parent = parent.addOrReplaceChild("segment" + i, box, pose);
        }

        return LayerDefinition.create(mesh, 64, 16);
    }
}
