package com.tracky.api;

import net.minecraft.core.SectionPos;
import org.jetbrains.annotations.Nullable;

/**
 * Tracks tracky chunks in a region.
 *
 * @author Ocelot
 */
public interface TrackyViewArea {

    /**
     * Retrieves the render chunk at the specified section position.
     *
     * @param pos The position to get a chunk for
     * @return The chunk at that position or <code>null</code> if one could not be found
     */
    default @Nullable TrackyRenderChunk getRenderChunk(SectionPos pos) {
        return this.getRenderChunk(pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * Retrieves the render chunk at the specified section position.
     *
     * @param sectionX The x position to get a chunk for
     * @param sectionY The y position to get a chunk for
     * @param sectionZ The z position to get a chunk for
     * @return The chunk at that position or <code>null</code> if one could not be found
     */
    @Nullable TrackyRenderChunk getRenderChunk(int sectionX, int sectionY, int sectionZ);

    /**
     * Retrieves the render chunk at the specified section position and tries to build one if it couldn't be found.
     *
     * @param pos The position to get a chunk for
     * @return The chunk at that position or <code>null</code> if one could not be found
     */
    default @Nullable TrackyRenderChunk tryCreateRenderChunk(SectionPos pos) {
        return this.tryCreateRenderChunk(pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * Retrieves the render chunk at the specified section position and tries to build one if it couldn't be found.
     *
     * @param sectionX The x position to get a chunk for
     * @param sectionY The y position to get a chunk for
     * @param sectionZ The z position to get a chunk for
     * @return The chunk at that position or <code>null</code> if one could not be found
     */
    default @Nullable TrackyRenderChunk tryCreateRenderChunk(int sectionX, int sectionY, int sectionZ) {
        TrackyRenderChunk chunk = this.getRenderChunk(sectionX, sectionY, sectionZ);
        if (chunk != null) {
            return chunk;
        }

        this.setDirty(sectionX, sectionY, sectionZ, false);
        return this.getRenderChunk(sectionX, sectionY, sectionZ);
    }

    /**
     * Marks the render section at the specified position to be rebuilt.
     *
     * @param pos      The position of the section
     * @param priority Whether the chunk should be rebuilt as soon as possible
     */
    default void setDirty(SectionPos pos, boolean priority) {
        this.setDirty(pos.getX(), pos.getY(), pos.getZ(), priority);
    }

    /**
     * Marks the render section at the specified position to be rebuilt.
     *
     * @param sectionX The x position of the section
     * @param sectionY The y position of the section
     * @param sectionZ The z position of the section
     * @param priority Whether the chunk should be rebuilt as soon as possible
     */
    void setDirty(int sectionX, int sectionY, int sectionZ, boolean priority);
}
