package com.tracky.mixin.client.impl.sodium;

import com.tracky.api.RenderSource;
import com.tracky.api.TrackyRenderChunk;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderSection.class, remap = false)
public abstract class RenderSectionMixin implements TrackyRenderChunk {

	@Unique
	private RenderSource tracky$renderSource;

	@Unique
	private BlockPos tracky$origin;
	@Unique
	private AABB tracky$aabb;

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

	@Inject(method = "<init>", at = @At("TAIL"))
	public void init(RenderRegion region, int chunkX, int chunkY, int chunkZ, CallbackInfo ci) {
		this.tracky$origin = new BlockPos(this.getOriginX(), this.getOriginY(), this.getOriginZ());
		this.tracky$aabb = new AABB(this.tracky$origin, this.tracky$origin.offset(SectionPos.SECTION_SIZE, SectionPos.SECTION_SIZE, SectionPos.SECTION_SIZE));
	}

	@Override
	public boolean needsSorting() {
		return false;
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
