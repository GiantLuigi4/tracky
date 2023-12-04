package com.tracky.access;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fc;

public interface ExtendedBlockEntityRenderDispatcher {

	void tracky$setCameraPosition(@Nullable Vec3 pos);
}
