package com.tracky.api;

import com.mojang.blaze3d.shaders.FogShape;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4fc;

import java.util.Collection;

/**
 * Abstract chunk renderer for drawing tracky render chunks.
 *
 * @author Ocelot
 */
public interface TrackyChunkRenderer {

    /**
     * Sets the model view matrix used when rendering chunks with {@link #render(Collection, RenderType)}.
     *
     * @param matrix The modelview matrix value
     */
    void setModelViewMatrix(Matrix4fc matrix);

    /**
     * Sets the projection matrix used when rendering chunks with {@link #render(Collection, RenderType)}.
     *
     * @param matrix The projection matrix value
     */
    void setProjectionMatrix(Matrix4fc matrix);

    /**
     * Sets the fog start for chunks rendered with {@link #render(Collection, RenderType)}.
     *
     * @param fogStart The fog starting distance
     */
    void setFogStart(float fogStart);

    /**
     * Sets the fog end for chunks rendered with {@link #render(Collection, RenderType)}.
     *
     * @param fogEnd The fog ending distance
     */
    void setFogEnd(float fogEnd);

    /**
     * Sets the fog shape for chunks rendered with {@link #render(Collection, RenderType)}.
     *
     * @param shape The shape of the fog
     */
    void setFogShape(FogShape shape);

    /**
     * Sets the fog color for chunks rendered with {@link #render(Collection, RenderType)}.
     *
     * @param red   The fog red
     * @param green The fog green
     * @param blue  The fog blue
     * @param alpha The fog alpha
     */
    void setFogColor(float red, float green, float blue, float alpha);

    /**
     * Array version of {@link #setFogColor(float, float, float, float)}.
     *
     * @param colors An array with the 4 color components
     */
    void setFogColor(float[] colors);

    /**
     * Renders the chunks in the render source.
     *
     * @param chunks The chunks to render. This is only used in Vanilla
     * @param layer  The layer to render
     */
    void render(Collection<TrackyRenderChunk> chunks, RenderType layer);
}
