package com.tracky.mixin.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.tracky.access.RenderChunkExtensions;
import com.tracky.api.RenderSource;
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
public abstract class RenderChunkMixin implements RenderChunkExtensions {

	@Shadow
	@Final
	ChunkRenderDispatcher this$0;

	@Shadow
	private AABB bb;

	@Shadow
	@Final
	private BlockPos.MutableBlockPos origin;
	@Unique
	private RenderSource tracky$renderSource;

	@Unique
	private Matrix4f tracky$getTransformation() {
		Vec3 cameraPosition = this.this$0.getCameraPosition();
		PoseStack stack = new PoseStack();
		stack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
		this.tracky$renderSource.transform(stack, cameraPosition.x, cameraPosition.y, cameraPosition.z);
		stack.translate(cameraPosition.x, cameraPosition.y, cameraPosition.z);
		return stack.last().pose();
	}

	@Inject(method = "getDistToPlayerSqr", at = @At("HEAD"), cancellable = true)
	protected void getDistToPlayerSqr(CallbackInfoReturnable<Double> cir) {
		if (this.tracky$renderSource != null) {
			Vec3 cameraPosition = this.this$0.getCameraPosition();
			Vector3f center = new Vector3f();
			center.set(this.bb.minX + 8.0D, this.bb.minY + 8.0D, this.bb.minZ + 8.0D);
			this.tracky$renderSource.calculateChunkOffset(center, cameraPosition.x, cameraPosition.y, cameraPosition.z);
			this.tracky$getTransformation().transformPosition(center);
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
	public @Nullable RenderSource tracky$getRenderSource() {
		return this.tracky$renderSource;
	}

	@Override
	public void tracky$setRenderSource(@Nullable RenderSource renderSource) {
		this.tracky$renderSource = renderSource;
	}

	@Override
	public VertexSorting tracky$getSorting() {
		Vec3 vec = this.this$0.getCameraPosition();
		Vector3f cameraPosition = new Vector3f();
		this.tracky$getTransformation().invert().transformPosition(cameraPosition);
		cameraPosition.add((float) vec.x, (float) vec.y, (float) vec.z);

		Vector3f center = new Vector3f();
		center.set(this.origin.getX(), this.origin.getY(), this.origin.getZ());
		this.tracky$renderSource.calculateChunkOffset(center, 0, 0, 0);
		return VertexSorting.byDistance(cameraPosition.sub(center));
//		return VertexSorting.byDistance(center.sub((float) cameraPosition.x, (float) cameraPosition.y, (float) cameraPosition.z));
	}
}