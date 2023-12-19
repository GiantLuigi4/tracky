package com.tracky.mixin.client;

import com.tracky.Tracky;
import net.minecraft.client.KeyboardHandler;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {

	@Shadow
	protected abstract boolean handleChunkDebugKeys(int pKeyCode);

	@Inject(method = "handleDebugKeys", at = @At("HEAD"), cancellable = true)
	public void addDebugKeys(int pKey, CallbackInfoReturnable<Boolean> cir) {
		if (!FMLEnvironment.production) {
			if (Tracky.ENABLE_TEST && this.handleChunkDebugKeys(pKey)) {
				cir.setReturnValue(true);
			}
		}
	}
}
