package com.example.demonicascension.client;

import com.example.demonicascension.DemonicAscension;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * Extends RenderType purely to reach its protected RenderStateShard constants.
 * The constructor is never called — this class exists for the static field below.
 */
public class ModRenderTypes extends RenderType {

    private ModRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode,
                           int bufferSize, boolean affectsCrumbling, boolean sortOnUpload,
                           Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
        throw new UnsupportedOperationException("Not instantiable");
    }

    private static final ResourceLocation VOID_SIGHT_FLAME_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            DemonicAscension.MODID, "textures/entity/soul_flame_marker.png");

    /**
     * Two unrelated vanilla quirks land in this one shard because neither has its own
     * builder slot we can override with a custom Runnable pair:
     *
     * <p>1. {@code MAIN_TARGET} is a no-op — it assumes the main framebuffer is
     * already bound. That's false while a shaderpack (Iris) or Fabulous graphics has
     * an offscreen target (e.g. the weather target) bound at the point our stage
     * runs, and content drawn there gets composited back with a depth test, silently
     * reintroducing the occlusion this render type exists to avoid.
     *
     * <p>2. {@code NO_DEPTH_TEST} is <em>also</em> a no-op, for a much stranger
     * reason: {@code DepthTestStateShard}'s constructor skips issuing any GL call
     * whenever the requested function is {@code GL_ALWAYS} (519), on the assumption
     * that depth testing is already off between draws by default. That assumption
     * holds — except vanilla's own rain renderer
     * ({@code LevelRenderer#renderSnowAndRain}) calls {@code enableDepthTest()} once
     * actual precipitation is falling and never disables it again. The Abyssal
     * Eclipse is the only thing in this mod that forces real rain, which is why
     * Void Sight's flames only ever went behind walls and mobs during it, and why it
     * didn't matter which render stage this fired on. There's no public way to make
     * {@code DepthTestStateShard} issue a real {@code glDisable} for the ALWAYS
     * case, so it's forced here instead, unconditionally, every engage.
     */
    private static final RenderStateShard.OutputStateShard FORCE_VISIBLE_ON_TOP = new RenderStateShard.OutputStateShard(
            "demonicascension_void_sight_force_visible",
            () -> {
                Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
                RenderSystem.disableDepthTest();
            },
            () -> {
                Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
                RenderSystem.disableDepthTest();
            });

    /**
     * A textured, vertex-tinted quad, straight to the main target with depth testing
     * off, so the soul flame billboard reads through walls exactly like the hitbox
     * wireframe it replaced.
     */
    public static final RenderType VOID_SIGHT_FLAME = RenderType.create(
            "void_sight_flame",
            DefaultVertexFormat.POSITION_TEX_COLOR,
            VertexFormat.Mode.QUADS,
            256,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionTexColorShader))
                    .setTextureState(new RenderStateShard.TextureStateShard(VOID_SIGHT_FLAME_TEXTURE, false, false))
                    .setLayeringState(NO_LAYERING)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setOutputState(FORCE_VISIBLE_ON_TOP)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .setDepthTestState(NO_DEPTH_TEST)
                    .createCompositeState(false));
}
