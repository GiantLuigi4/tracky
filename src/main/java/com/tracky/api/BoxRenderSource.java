package com.tracky.api;

import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A simple implementation of a {@link RenderSource} which has a box shaped selection of sections to render
 * Box render sources are immutable by default
 */
public class BoxRenderSource extends RenderSource {
    protected SectionPos min;
    protected SectionPos max;

    protected static List<SectionPos> createList(SectionPos min, SectionPos max) {
        Vec3i size = max.subtract(min);
        List<SectionPos> poses = new ArrayList<>(size.getX() * size.getY() * size.getZ());

        for (int z = min.getZ(); z <= max.getZ(); z++) {
            for (int x = min.getX(); x <= max.getX(); x++) {
                for (int y = min.getY(); y <= max.getY(); y++) {
                    poses.add(SectionPos.of(x, y, z));
                }
            }
        }

        return Collections.unmodifiableList(poses);
    }

    public BoxRenderSource(SectionPos min, SectionPos max) {
        super(createList(min, max));
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean containsSection(SectionPos pos) {
        return pos.getX() >= this.min.getX() &&
                pos.getY() >= this.min.getY() &&
                pos.getZ() >= this.min.getZ() &&
                pos.getX() <= this.max.getX() &&
                pos.getY() <= this.max.getY() &&
                pos.getZ() <= this.max.getZ();
    }

    @Override
    public boolean containsChunk(ChunkPos pos) {
        return pos.x >= this.min.getX() &&
                pos.z >= this.min.getZ() &&
                pos.x <= this.max.getX() &&
                pos.z <= this.max.getZ();
    }

    @Override
    public void addSection(SectionPos pos) {
        throw new RuntimeException("Cannot add a section to a cubic render source");
    }

    @Override
    public void removeSection(SectionPos pos) {
        throw new RuntimeException("Cannot remove a section from a cubic render source");
    }
}
