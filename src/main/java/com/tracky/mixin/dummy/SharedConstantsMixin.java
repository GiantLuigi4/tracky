package com.tracky.mixin.dummy;

import net.minecraft.SharedConstants;
import org.spongepowered.asm.mixin.Mixin;

/**
 * this is intentionally empty
 * it is merely here so that {@link com.tracky.MixinPlugin} can process the Tracky accessor class
 */
@Mixin(SharedConstants.class)
public class SharedConstantsMixin {
}
