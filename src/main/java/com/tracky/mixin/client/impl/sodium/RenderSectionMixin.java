package com.tracky.mixin.client.impl.sodium;

import com.tracky.api.RenderSource;
import com.tracky.api.TrackyRenderChunk;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionInfo;
import me.jellysquid.mods.sodium.client.render.chunk.data.SectionRenderDataStorage;
import me.jellysquid.mods.sodium.client.render.chunk.data.SectionRenderDataUnsafe;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RenderSection.class, remap = false)
public abstract class RenderSectionMixin implements TrackyRenderChunk {

	@Unique
	private RenderSource tracky$renderSource;
	@Unique
	private BlockPos tracky$origin;
	@Unique
	private AABB tracky$aabb;
	@Unique
	private boolean tracky$needsSorting;

	@Shadow
	public abstract void delete();

	@Shadow
	public abstract SectionPos getPosition();

	@Shadow
	public abstract int getOriginX();

	@Shadow
	public abstract int getOriginY();

	@Shadow
	public abstract int getOriginZ();

	@Shadow
	@Final
	private RenderRegion region;

	@Shadow
	@Final
	private int sectionIndex;

	@Inject(method = "<init>", at = @At("TAIL"))
	public void init(RenderRegion region, int chunkX, int chunkY, int chunkZ, CallbackInfo ci) {
		this.tracky$origin = new BlockPos(this.getOriginX(), this.getOriginY(), this.getOriginZ());
		this.tracky$aabb = new AABB(this.tracky$origin, this.tracky$origin.offset(SectionPos.SECTION_SIZE, SectionPos.SECTION_SIZE, SectionPos.SECTION_SIZE));
	}

	@Inject(method = "getSquaredDistance(FFF)F", at = @At("HEAD"), cancellable = true)
	public void getSquaredDistance(float x, float y, float z, CallbackInfoReturnable<Float> cir) {
		if (this.tracky$renderSource != null) {
			Vector3f center = new Vector3f();
			Vector3dc chunkOffset = this.tracky$renderSource.getChunkOffset();
			center.set(this.tracky$origin.getX() + chunkOffset.x() + 8.0D - x, this.tracky$origin.getY() + chunkOffset.y() + 8.0D - y, this.tracky$origin.getZ() + chunkOffset.z() + 8.0D - z);
			this.tracky$renderSource.getTransformation(x, y, z).transformPosition(center);
			cir.setReturnValue(center.lengthSquared());
		}
	}

	@Inject(method = "setRenderState", at = @At("TAIL"))
	private void setRenderState(BuiltSectionInfo info, CallbackInfo ci) {
		this.tracky$needsSorting = false;

		SectionRenderDataStorage storage = this.region.getStorage(DefaultTerrainRenderPasses.TRANSLUCENT);
		if (storage == null) {
			return;
		}

		long data = storage.getDataPointer(this.sectionIndex);
		for (int facing = 0; facing < ModelQuadFacing.COUNT; facing++) {
			int count = SectionRenderDataUnsafe.getElementCount(data, facing);
			if (count > 0) {
				this.tracky$needsSorting = true;
				break;
			}
		}
	}

	@Inject(method = "clearRenderState", at = @At("TAIL"))
	private void clearRenderState(CallbackInfo ci) {
		this.tracky$needsSorting = false;
	}

	@Override
	public boolean needsSorting() {
		return this.tracky$needsSorting;
	}

	@Override
	public boolean resort(RenderType layer) {
		return false;
	}

	@Override
	public BlockPos getChunkOrigin() {
		return this.tracky$origin;
	}

	@Override
	public SectionPos getSectionPos() {
		return this.getPosition();
	}

	@Override
	public AABB getAABB() {
		return this.tracky$aabb;
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
	public void free() {
		this.delete();
	}
}
