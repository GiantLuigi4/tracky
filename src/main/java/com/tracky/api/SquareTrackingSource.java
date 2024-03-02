package com.tracky.api;

import net.minecraft.world.level.ChunkPos;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SquareTrackingSource extends TrackingSource {

    protected final ChunkPos min;
    protected final ChunkPos max;

    @Override
    public boolean containsChunk(ChunkPos pos) {
        return pos.x >= this.min.x &&
                pos.z >= this.min.z &&
                pos.x <= this.max.x &&
                pos.z <= this.max.z;
    }

    public static Collection<ChunkPos> createList(ChunkPos min, ChunkPos max) {
        Set<ChunkPos> poses = new HashSet<>((max.x - min.x) * (max.z - min.z));

        for (int x = min.x; x <= max.x; x++) {
            for (int z = min.z; z <= max.z; z++) {
                poses.add(new ChunkPos(x, z));
            }
        }

        return Collections.unmodifiableSet(poses);
    }

    public SquareTrackingSource(ChunkPos min, ChunkPos max) {
        super(createList(min, max));
        this.min = min;
        this.max = max;
    }

    @Override
    public void addChunk(ChunkPos pos) {
        throw new RuntimeException("Cannot add a section to a square tracking source");
    }

    @Override
    public void removeChunk(ChunkPos pos) {
        throw new RuntimeException("Cannot remove a section from a square tracking source");
    }
}