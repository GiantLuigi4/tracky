package com.tracky.mixin.client.impl.sodium;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.tracky.access.ExtendedRenderSection;
import com.tracky.api.RenderSource;
import com.tracky.api.TrackyRenderChunk;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderSection.class, remap = false)
public abstract class RenderSectionMixin implements TrackyRenderChunk, ExtendedRenderSection {

	@Unique
	private RenderSource tracky$renderSource;

	@Unique
	private RenderSectionManager tracky$renderSectionManager;

	@Unique
	private BlockPos tracky$origin;

	@Shadow
	public abstract void delete();

	@Shadow
	@Final
	private int chunkX;

	@Shadow
	@Final
	private int chunkY;

	@Shadow
	@Final
	private int chunkZ;

	@Inject(method = "<init>", at = @At("TAIL"))
	public void init(RenderRegion region, int chunkX, int chunkY, int chunkZ, CallbackInfo ci) {
		this.tracky$origin = new BlockPos(chunkX, chunkY, chunkZ);
	}

	@Override
	public boolean needsSorting() {
		return false; // TODO sorting?
	}

	@Override
	public boolean resort(RenderType layer) {
		return false;
	}

	@Override
	public void markDirty(boolean reRenderOnMainThread) {
		this.tracky$renderSectionManager.scheduleRebuild(this.chunkX >> SectionPos.SECTION_BITS, this.chunkY >> SectionPos.SECTION_BITS, this.chunkZ >> SectionPos.SECTION_BITS, reRenderOnMainThread);
	}

	@Override
	public Matrix4f getTransformation() {
		Vec3 cameraPosition = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
		PoseStack stack = new PoseStack();
		stack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
		this.tracky$renderSource.transform(stack, cameraPosition.x, cameraPosition.y, cameraPosition.z);
		stack.translate(cameraPosition.x, cameraPosition.y, cameraPosition.z);
		return stack.last().pose();
	}

	@Override
	public BlockPos getChunkOrigin() {
		return this.tracky$origin;
	}

	@Override
	public @Nullable RenderSource getRenderSource() {
		return this.tracky$renderSource;
	}

	@Override
	public VertexSorting createVertexSorting() {
		return null;
	}

	@Override
	public void setRenderSource(@Nullable RenderSource renderSource) {
		this.tracky$renderSource = renderSource;
	}

	@Override
	public void veil$setRenderSectionManager(RenderSectionManager renderSectionManager) {
		this.tracky$renderSectionManager = renderSectionManager;
	}

	@Override
	public void free() {
		this.delete();
	}
}
