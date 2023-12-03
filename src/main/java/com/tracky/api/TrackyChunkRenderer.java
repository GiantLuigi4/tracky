package com.tracky.api;

import com.mojang.blaze3d.shaders.FogShape;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4fc;

import java.util.Collection;

public interface TrackyChunkRenderer {

	void setModelViewMatrix(Matrix4fc matrix);

	void setProjectionMatrix(Matrix4fc matrix);

	void setFogStart(float fogStart);

	void setFogEnd(float fogEnd);

	void setFogShape(FogShape shape);

	void setFogColor(float red, float green, float blue, float alpha);

	default void setFogColor(float[] colors) {
		this.setFogColor(colors[0], colors[1], colors[2], colors[3]);
	}

	void render(Collection<TrackyRenderChunk> chunks, RenderType layer);
}
