package com.tracky.mixin.dummy;

import org.spongepowered.asm.mixin.Mixin;

import java.util.logging.Level;

/**
 * this is intentionally empty
 * it is merely here so that {@link com.tracky.MixinPlugin} can process the world class
 */
@Mixin(Level.class)
public class WorldMixin {
}
