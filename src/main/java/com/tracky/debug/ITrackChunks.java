package com.tracky.debug;

import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;

public interface ITrackChunks {
	ArrayList<SectionPos> trackedChunks();
	

	boolean setDoUpdate(boolean val);
}
