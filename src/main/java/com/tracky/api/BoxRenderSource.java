package com.tracky.api;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.SectionPos;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple implementation of a {@link RenderSource} which has a box shaped selection of sections to render
 * Box render sources are immutable by default
 */
public class BoxRenderSource extends RenderSource {
	protected SectionPos min;
	protected SectionPos max;
	
	protected static List<SectionPos> createList(SectionPos min, SectionPos max) {
		ArrayList<SectionPos> poses = new ArrayList<>();
		
		for (int x = min.getX(); x <= max.getX(); x++)
			for (int y = min.getY(); y <= max.getY(); y++)
				for (int z = min.getZ(); z <= max.getZ(); z++)
					poses.add(SectionPos.of(x, y, z));
		
		return ImmutableList.copyOf(poses);
	}
	
	public BoxRenderSource(SectionPos min, SectionPos max) {
		super(createList(min, max));
		this.min = min;
		this.max = max;
	}
	
	@Override
	public boolean containsSection(SectionPos pos) {
		return pos.getX() >= min.getX() &&
				pos.getY() >= min.getY() &&
				pos.getZ() >= min.getZ() &&
				pos.getX() <= max.getX() &&
				pos.getY() <= max.getY() &&
				pos.getZ() <= max.getZ();
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
