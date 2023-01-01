package com.tracky.access;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.Vec3i;

import java.util.HashMap;

public interface RenderInfoMapExtensions {
	HashMap<Vec3i, LevelRenderer.RenderChunkInfo> getRenderChunkMap();
}