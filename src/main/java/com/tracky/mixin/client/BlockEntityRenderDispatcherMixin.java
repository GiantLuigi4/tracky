package com.tracky.mixin.client;

import com.tracky.access.ExtendedBlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin implements ExtendedBlockEntityRenderDispatcher {

	@Unique
	private Vec3 tracky$cameraPos;

	@ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;shouldRender(Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/phys/Vec3;)Z"), index = 1)
	public Vec3 modifyCameraPos(Vec3 pCameraPos) {
		return this.tracky$cameraPos != null ? this.tracky$cameraPos : pCameraPos;
	}

	@Override
	public void tracky$setCameraPosition(@Nullable Vec3 pos) {
		this.tracky$cameraPos = pos;
	}
}
