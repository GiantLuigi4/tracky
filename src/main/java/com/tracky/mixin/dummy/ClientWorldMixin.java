package com.tracky.mixin.dummy;

import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;

/**
 * this is intentionally empty
 * it is merely here so that {@link com.tracky.MixinPlugin} can process the shared constants class
 */
@Mixin(ClientLevel.class)
public class ClientWorldMixin {
}
