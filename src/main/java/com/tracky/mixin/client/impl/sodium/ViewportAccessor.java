package com.tracky.mixin.client.impl.sodium;

import me.jellysquid.mods.sodium.client.render.viewport.Viewport;
import me.jellysquid.mods.sodium.client.render.viewport.frustum.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Viewport.class)
public interface ViewportAccessor {

    @Accessor
    Frustum getFrustum();
}
