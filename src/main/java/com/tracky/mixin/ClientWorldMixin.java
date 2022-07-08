package com.tracky.mixin;

import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;

/**
 * this is intentionally empty
 * it is merely here so that {@link com.tracky.MixinPlugin} can process the client world class
 */
@Mixin(ClientWorld.class)
public class ClientWorldMixin {
}
