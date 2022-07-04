package com.tracky.mixin;

import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

/**
 * this is intentionally empty
 * it is merely here so that {@link com.tracky.MixinPlugin} can process the world class
 */
@Mixin(World.class)
public class WorldMixin {
}
