package com.tracky.mixin.client.impl.vanilla;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.tracky.access.ExtendedRenderChunk;
import com.tracky.api.RenderSource;
import com.tracky.api.TrackyRenderChunk;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkRenderDispatcher.RenderChunk.class)
public abstract class RenderChunkMixin implements TrackyRenderChunk, ExtendedRenderChunk {

	@Shadow
	@Final
	ChunkRenderDispatcher this$0;

	@Shadow
	private AABB bb;

	@Shadow
	@Final
	BlockPos.MutableBlockPos origin;

	@Shadow
	public abstract void releaseBuffers();

	@Shadow
	public abstract boolean resortTransparency(RenderType pType, ChunkRenderDispatcher pDispatcher);

	@Shadow
	public abstract void setDirty(boolean pReRenderOnMainThread);

	@Shadow
	public abstract ChunkRenderDispatcher.CompiledChunk getCompiledChunk();

	@Unique
	private RenderSource tracky$renderSource;

	@Inject(method = "getDistToPlayerSqr", at = @At("HEAD"), cancellable = true)
	protected void getDistToPlayerSqr(CallbackInfoReturnable<Double> cir) {
		if (this.tracky$renderSource != null) {
			Vec3 cameraPosition = this.this$0.getCameraPosition();
			Vector3f center = new Vector3f();
			center.set(this.bb.minX + 8.0D - cameraPosition.x, this.bb.minY + 8.0D - cameraPosition.y, this.bb.minZ + 8.0D - cameraPosition.z);
			this.tracky$renderSource.getTransformation(cameraPosition.x, cameraPosition.y, cameraPosition.z).transformPosition(center);
			cir.setReturnValue((double) center.lengthSquared());
		}
	}

	@Inject(method = "hasAllNeighbors", at = @At("HEAD"), cancellable = true)
	public void hasAllNeighbors(CallbackInfoReturnable<Boolean> cir) {
		if (this.tracky$renderSource != null) {
			// This makes sure the chunks are built even if vanilla doesn't think they should be
			cir.setReturnValue(true);
		}
	}

	@Override
	public @Nullable RenderSource getRenderSource() {
		return this.tracky$renderSource;
	}

	@Override
	public void setRenderSource(@Nullable RenderSource renderSource) {
		this.tracky$renderSource = renderSource;
	}

	@Override
	public VertexSorting createVertexSorting() {
		Vec3 vec = this.this$0.getCameraPosition();
		Vector3f cameraPosition = new Vector3f();
		this.tracky$renderSource.getTransformation(vec.x, vec.y, vec.z).invert().transformPosition(cameraPosition);
		cameraPosition.add((float) vec.x, (float) vec.y, (float) vec.z);

		Vector3f center = new Vector3f();
		center.set(this.origin.getX(), this.origin.getY(), this.origin.getZ());
		return VertexSorting.byDistance(cameraPosition.sub(center));
	}

	@Override
	public boolean resort(RenderType layer) {
		return this.resortTransparency(layer, this.this$0);
	}

	@Override
	public boolean needsSorting() {
		return !this.getCompiledChunk().isEmpty(RenderType.translucent());
	}

	@Override
	public void markDirty(boolean reRenderOnMainThread) {
		this.setDirty(reRenderOnMainThread);
	}

	@Override
	public BlockPos getChunkOrigin() {
		return this.origin;
	}

	@Override
	public void free() {
		this.releaseBuffers();
	}
}