package com.tracky.api;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.NativeResource;

/**
 * A rendering chunk tracked by tracky.
 *
 * @author Ocelot
 */
public interface TrackyRenderChunk extends NativeResource {

	/**
	 * @return Whether this chunk has transparency that needs to be sorted
	 */
	boolean needsSorting();

	/**
	 * Queues the specified layer to be resorted.
	 *
	 * @param layer The layer to sort
	 * @return Whether the layer was queued top be sorted
	 */
	boolean resort(RenderType layer);

	/**
	 * @return The block pos origin of this chunk
	 */
	BlockPos getChunkOrigin();

	/**
	 * @return The position of this section
	 */
	default SectionPos getSectionPos() {
		return SectionPos.of(this.getChunkOrigin());
	}

	AABB getAABB();

	/**
	 * @return The render source this chunk is part of
	 */
	@Nullable RenderSource getRenderSource();

	@ApiStatus.Internal
	void setRenderSource(@Nullable RenderSource renderSource);
}
