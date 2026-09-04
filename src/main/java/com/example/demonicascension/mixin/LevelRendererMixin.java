package com.example.demonicascension.mixin;

import com.example.demonicascension.client.EclipseClientState;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The one thing NeoForge doesn't expose an event for: cancelling vanilla's own sky
 * dome draw. Everything else about the eclipse's look (dark fog, the eclipse disc
 * itself) is ordinary {@code ViewportEvent}/{@code RenderLevelStageEvent} handling in
 * {@code client/EclipseSkyEvents} — this mixin's only job is to skip the vanilla sky
 * so that fog is what's actually visible instead of the normal blue dome peeking
 * through underneath it.
 */
@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Inject(method = "renderSky", at = @At("HEAD"), cancellable = true)
    private void demonicascension$skipSkyDuringEclipse(Matrix4f frustumMatrix, Matrix4f projectionMatrix,
                                                        float partialTick, Camera camera, boolean isFoggy,
                                                        Runnable skyFogSetup, CallbackInfo ci) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        if (EclipseClientState.isActive(Minecraft.getInstance().level.getGameTime())) {
            // Fog state still needs to be set up even though we're skipping the dome —
            // later render passes this frame expect it to have run.
            skyFogSetup.run();

            // Cancelling at HEAD means vanilla's own end-of-method cleanup — resetting
            // the shader color modulator and depth mask back to neutral — never runs
            // either. Every render type drawn later this frame (ours included — the
            // void sight flame's shader multiplies its output by this exact uniform)
            // silently inherits whatever ColorModulator was last left at instead of the
            // neutral (1,1,1,1) vanilla guarantees every frame. Restoring it here is
            // what vanilla's own renderSky() would have done on its way out.
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.depthMask(true);

            ci.cancel();
        }
    }
}
