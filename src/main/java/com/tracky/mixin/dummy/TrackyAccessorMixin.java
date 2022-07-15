package com.tracky.mixin.dummy;

import com.tracky.TrackyAccessor;
import org.spongepowered.asm.mixin.Mixin;

/**
 * this is intentionally empty
 * it is merely here so that {@link com.tracky.MixinPlugin} can process the Tracky accessor class
 */
@Mixin(TrackyAccessor.class)
public class TrackyAccessorMixin {
}
