package com.tracky.mixin.client.render;

import com.tracky.access.RenderInfoMapExtensions;
import com.tracky.util.VecMap;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;

@Mixin(LevelRenderer.RenderInfoMap.class)
public class RenderInfoMapMixin implements RenderInfoMapExtensions {
//	VecMap<LevelRenderer.RenderChunkInfo> map = new VecMap<>(2);
	HashMap<Vec3i,  LevelRenderer.RenderChunkInfo> map = new HashMap<>();

	@Inject(at = @At("HEAD"), method = "put", cancellable = true)
	public void prePut(ChunkRenderDispatcher.RenderChunk pRenderChunk, LevelRenderer.RenderChunkInfo pInfo, CallbackInfo ci) {
		Vec3i vec = pRenderChunk.getOrigin();

		int x = Mth.intFloorDiv((int) vec.getX(), 16);
		int y = Mth.intFloorDiv((int) vec.getY(), 16);
		int z = Mth.intFloorDiv((int) vec.getZ(), 16);

		vec = new Vec3i(x, y, z);
		map.put(vec, pInfo);
		ci.cancel();
	}

	@Inject(at = @At("HEAD"), method = "get", cancellable = true)
	public void preGet(ChunkRenderDispatcher.RenderChunk pRenderChunk, CallbackInfoReturnable<LevelRenderer.RenderChunkInfo> cir) {
		Vec3i vec = pRenderChunk.getOrigin();

		int x = Mth.intFloorDiv((int) vec.getX(), 16);
		int y = Mth.intFloorDiv((int) vec.getY(), 16);
		int z = Mth.intFloorDiv((int) vec.getZ(), 16);

		vec = new Vec3i(x, y, z);
		cir.setReturnValue(map.get(vec));
	}
	
	@Override
	public HashMap<Vec3i, LevelRenderer.RenderChunkInfo> getRenderChunkMap() {
		return map;
	}
}
