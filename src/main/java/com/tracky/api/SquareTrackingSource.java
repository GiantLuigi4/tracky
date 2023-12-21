package com.tracky.api;

import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SquareTrackingSource extends TrackingSource {
    protected ChunkPos min;
    protected ChunkPos max;

    @Override
    public boolean containsChunk(ChunkPos pos) {
        return pos.x >= min.x &&
                pos.z >= min.z &&
                pos.x <= max.x &&
                pos.z <= max.z;
    }

    protected static List<ChunkPos> createList(ChunkPos min, ChunkPos max) {
        List<ChunkPos> poses = new ArrayList<>((max.x - min.x) * (max.z - min.z));

        for (int x = min.x; x <= max.x; x++) {
            for (int z = min.z; z <= max.z; z++) {
                poses.add(new ChunkPos(x, z));
            }
        }

        return Collections.unmodifiableList(poses);
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