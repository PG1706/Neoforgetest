package com.example.demonicascension.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

import java.util.OptionalDouble;

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

    /**
     * Lines drawn straight to the main target with depth testing off.
     */
    public static final RenderType VOID_SIGHT_LINES = RenderType.create(
            "void_sight_lines",
            DefaultVertexFormat.POSITION_COLOR_NORMAL,
            VertexFormat.Mode.LINES,
            1536,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_LINES_SHADER)
                    .setLineState(new LineStateShard(OptionalDouble.of(3.0)))
                    .setLayeringState(NO_LAYERING)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setOutputState(MAIN_TARGET)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .setDepthTestState(NO_DEPTH_TEST)
                    .createCompositeState(false));
}