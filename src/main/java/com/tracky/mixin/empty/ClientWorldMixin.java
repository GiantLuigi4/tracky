package com.tracky.mixin.empty;

import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;

/**
 * this is intentionally empty
 * it is merely here so that {@link com.tracky.MixinPlugin} can process the shared constants class
 */
@Mixin(ClientWorld.class)
public class ClientWorldMixin {
}
