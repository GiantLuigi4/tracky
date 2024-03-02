package com.tracky.access.sodium;

import com.tracky.api.RenderSource;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3ic;

public interface ExtendedOcclusionCuller {

    void tracky$setRenderSource(@Nullable RenderSource source);

    void tracky$setBounds(Vector3ic minSection, Vector3ic maxSection);
}
