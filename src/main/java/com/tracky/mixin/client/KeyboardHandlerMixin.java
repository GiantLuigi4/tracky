package com.tracky.mixin.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
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

    @Shadow
    protected abstract void debugFeedback(String pMessage, Object... pArgs);

    @Inject(method = "handleDebugKeys", at = @At("HEAD"), cancellable = true)
    public void addDebugKeys(int pKey, CallbackInfoReturnable<Boolean> cir) {
        if (!FMLEnvironment.production) {
//			if (
//					InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 292) &&
//							pKey == GLFW.GLFW_KEY_0
//			) {
//				this.debugFeedback("debug.tracky_enabld");
//				Tracky.ENABLE_TEST = true;
//				cir.setReturnValue(true);
//			}
            if (
                    InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 292) &&
                            this.handleChunkDebugKeys(pKey)
            ) {
                cir.setReturnValue(true);
            }
        }
    }
}
