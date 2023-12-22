package com.tracky.access.sodium;

import me.jellysquid.mods.sodium.client.render.viewport.CameraTransform;
import org.jetbrains.annotations.Nullable;

public interface ExtendedDefaultChunkRenderer {

	void tracky$setCameraTransform(@Nullable CameraTransform transform);
}
