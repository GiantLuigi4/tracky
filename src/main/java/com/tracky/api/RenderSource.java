package com.tracky.api;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.math.Vector3f;
import com.tracky.access.ExtendedViewArea;
import com.tracky.mixin.client.render.RenderChunkInfoMixin;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectRBTreeSet;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;

import java.util.*;

public class RenderSource {
	protected final Collection<SectionPos> sections;
	protected List<ChunkRenderDispatcher.RenderChunk> chunksInSource = new ArrayList<>();
	protected final List<ChunkRenderDispatcher.RenderChunk> chunksInFrustum = new ObjectArrayList<>();
	
	protected Queue<SectionPos> newSections = new ArrayDeque<>();
	
	public RenderSource(Collection<SectionPos> sections) {
		this.sections = sections;
		newSections.addAll(sections);
	}
	
	public void addSection(SectionPos pos) {
		if (sections.add(pos))
			newSections.add(pos);
	}
	
	public void removeSection(SectionPos pos) {
		if (sections.remove(pos)) {
			if (!newSections.remove(pos)) {
				ChunkRenderDispatcher.RenderChunk target = null;
				for (ChunkRenderDispatcher.RenderChunk renderChunk : chunksInSource) {
					if (
							renderChunk.getOrigin().getX() == pos.minBlockX() &&
									renderChunk.getOrigin().getY() == pos.minBlockY() &&
									renderChunk.getOrigin().getZ() == pos.minBlockZ()
					) {
						target = renderChunk;
						break;
					}
				}
				chunksInSource.remove(target);
				chunksInFrustum.remove(target);
			}
		}
	}
	
	/**
	 * used by tracky to figure out what chunks to force redrawing of
	 *
	 * @return the list of render chunks which are currently in the frustum, or a list of only dirty render chunks if you have a way to keep track of that
	 */
	public List<ChunkRenderDispatcher.RenderChunk> getChunksInFrustum() {
		return chunksInFrustum;
	}
	
	/**
	 * Tests if the render source contains a section
	 * This is used by various hooks to vanilla code to know if tracky should use it's own caches or if it should use vanilla's ones
	 *
	 * @param pos the position of the section
	 * @return if the render source has it in its list of sections
	 */
	public boolean containsSection(SectionPos pos) {
		return sections.contains(pos);
	}
	
	/**
	 * called when vanilla updates the frustum, if the render source is visible
	 * ideally, mods would do some reprojected frustum checking on each of the sections
	 *
	 * @param camera  the game camera
	 * @param frustum the frustum to use
	 */
	public void updateFrustum(Camera camera, Frustum frustum) {
		chunksInFrustum.clear();
		chunksInFrustum.addAll(chunksInSource);
	}
	
	/**
	 * defaults to true
	 * generally mods will want to do a frustum cull on the AABB of whatever is creating the render source
	 *
	 * @param camera  the game camera
	 * @param frustum the frustum to use
	 * @return if the render source is currently visible, and thus should render
	 */
	public boolean canDraw(Camera camera, Frustum frustum) {
		return true;
	}
	
	/**
	 * used for transparency sorting
	 *
	 * @param camX   the camera's X position
	 * @param camY   the camera's Y position
	 * @param camZ   the camera's Z position
	 * @param center the center of the chunk
	 * @return the distance between the chunk and the player
	 */
	public double calculateDistance(double camX, double camY, double camZ, Vector3f center) {
		// I believe manhattan distance should work here
		return
				(center.x() - camX) * (center.x() - camX) +
						(center.y() - camY) * (center.y() - camY) +
						(center.z() - camZ) * (center.z() - camZ)
				;
	}
	
	protected int lx = Integer.MIN_VALUE, ly = Integer.MIN_VALUE, lz = Integer.MIN_VALUE;
	protected Collection<ChunkRenderDispatcher.RenderChunk> sorted = chunksInFrustum;
	
	/**
	 * does transparency sorting
	 *
	 * @param camX the camera's X position
	 * @param camY the camera's Y position
	 * @param camZ the camera's Z position
	 */
	public void resort(double camX, double camY, double camZ) {
		Vector3f vec = new Vector3f();
		
		// TODO: heavy optimization is needed here
		sorted = new ArrayList<>(new ObjectRBTreeSet<>(
				chunksInFrustum.toArray(new ChunkRenderDispatcher.RenderChunk[0]),
				Comparator.comparingDouble(left -> {
					vec.set(left.getOrigin().getX() + 8, left.getOrigin().getY() + 8, left.getOrigin().getZ() + 8);
					// TODO: caching?
					return calculateDistance(camX, camY, camZ, vec);
				})
		));
	}
	
	/**
	 * @param viewArea   the view area which contains RenderChunks
	 * @param chunkInfos unused, unsure what this does or if it's necessary at all
	 * @param infoMap    the map of existing chunk infos
	 * @param sectionPos the position of the section being added
	 * @return if the render chunk got added
	 */
	protected boolean handleAdd(Collection<ChunkRenderDispatcher.RenderChunk> dst, HashSet<ChunkRenderDispatcher.RenderChunk> existing, ViewArea viewArea, LinkedHashSet<LevelRenderer.RenderChunkInfo> chunkInfos, LevelRenderer.RenderInfoMap infoMap, SectionPos sectionPos) {
		if (sectionPos == null) return true;
		
		// directly accessing the extended map, as the chunks are guaranteed to be within it if they have been created
		ExtendedViewArea extendedArea = (ExtendedViewArea) viewArea;
		HashMap<ChunkPos, ChunkRenderDispatcher.RenderChunk[]> map = extendedArea.getTracky$renderChunkCache();
		
		ChunkPos ckPos = new ChunkPos(sectionPos.getX(), sectionPos.getZ());
		ChunkRenderDispatcher.RenderChunk[] renderChunks = map.get(ckPos);
		
		if (renderChunks == null) {
			viewArea.setDirty(
					sectionPos.getX(), sectionPos.getY(), sectionPos.getZ(),
					false
			);
			renderChunks = map.get(ckPos);
		}
		
		if (renderChunks == null) return false;
		
		// calculate y-index
		int y = Math.floorMod(sectionPos.getY() - Minecraft.getInstance().level.getMinSection(), Minecraft.getInstance().level.getSectionsCount());
		ChunkRenderDispatcher.RenderChunk renderChunk = renderChunks[y];
		
		if (renderChunk != null) {
			// without this, the render sections can get messed up if this gets called before the chunks are synced
			if (
					!renderChunk.getOrigin().equals(sectionPos.origin())
			) {
				viewArea.setDirty(
						sectionPos.getX(), sectionPos.getY(), sectionPos.getZ(),
						false
				);
				renderChunk = renderChunks[y];
				
				if (renderChunk == null || renderChunk.getOrigin().equals(sectionPos.origin()))
					return false;
			}
			
			if (infoMap.get(renderChunk) == null) {
				LevelRenderer.RenderChunkInfo info = RenderChunkInfoMixin.invokeInit(renderChunk, null, 0);
				infoMap.put(renderChunk, info);
			}
			
			if (!existing.contains(renderChunk))
				dst.add(renderChunk);
			
			return true;
		} else {
			viewArea.setDirty(
					sectionPos.getX(), sectionPos.getY(), sectionPos.getZ(),
					false
			);
		}
		
		return false;
	}
	
	/**
	 * clears out the RenderChunk caches and marks them to be updated
	 * used to account for F3+A
	 */
	public void refresh() {
		chunksInSource.clear();
		chunksInFrustum.clear();
		
		newSections.addAll(sections);
	}
	
	/**
	 * updates render chunks for when new sections are added to the render source
	 *
	 * @param viewArea   the view area which contains the RenderChunks
	 * @param chunkInfos the list of chunk infos, unsure what this actually does
	 * @param infoMap    the map of existing chunk infos
	 */
	public void updateChunks(ViewArea viewArea, LinkedHashSet<LevelRenderer.RenderChunkInfo> chunkInfos, LevelRenderer.RenderInfoMap infoMap) {
		if (!newSections.isEmpty()) {
			List<SectionPos> toKeep = new ObjectArrayList<>();
			// update chunks is called on the main thread, while everything else that might use it is called from the render thread
			// so if I don't copy this list, the game will crash
			List<ChunkRenderDispatcher.RenderChunk> tmp = new ArrayList<>(chunksInSource);
			HashSet<ChunkRenderDispatcher.RenderChunk> known = new HashSet<>(tmp);
			int i = 0;
			// run updates
			while (!newSections.isEmpty() && i < 1000) {
				SectionPos newSection = newSections.poll();
				if (!handleAdd(tmp, known, viewArea, chunkInfos, infoMap, newSection))
					toKeep.add(newSection);
				i++;
			}
			
			// move data
			chunksInSource = tmp;
			newSections.addAll(toKeep);
			
			// force resort
			lx = Integer.MIN_VALUE;
			ly = Integer.MIN_VALUE;
			lz = Integer.MIN_VALUE;
		}
		
		for (ChunkRenderDispatcher.RenderChunk renderChunk : chunksInFrustum) {
			LevelRenderer.RenderChunkInfo info = infoMap.get(renderChunk);
			if (info == null) {
				info = RenderChunkInfoMixin.invokeInit(renderChunk, null, 0);
				infoMap.put(renderChunk, info);
			}
		}
	}
	
	/**
	 * calculates the render offset of a chunk relative to the render origin
	 *
	 * @param vec  the origin of the chunk in global space
	 * @param camX the camera's X position
	 * @param camY the camera's Y position
	 * @param camZ the camera's Z position
	 */
	public void calculateChunkOffset(Vector3f vec, double camX, double camY, double camZ) {
		vec.set(
				(float) (vec.x() - camX),
				(float) (vec.y() - camY),
				(float) (vec.z() - camZ)
		);
	}
	
	// TODO: probably shouldn't be giving direct access to the shader instance
	
	/**
	 * draws the chunks in the render source
	 * if you want to apply transformations to the rendering, see {@link com.tracky.debug.TestSource}
	 * most mods will want to override this, do some setup before calling super, and then do some teardown afterwards
	 *
	 * @param stack    the active pose stack
	 * @param area     the view area
	 * @param instance the shader being used
	 * @param type     the render type being drawn
	 * @param camX     the camera's X position
	 * @param camY     the camera's Y position
	 * @param camZ     the camera's Z position
	 */
	public void draw(PoseStack stack, ViewArea area, ShaderInstance instance, RenderType type, double camX, double camY, double camZ) {
		Uniform uniform = instance.CHUNK_OFFSET;
		
		if (
				type == RenderType.translucent() &&
						lx != camX ||
						ly != camY ||
						lz != camZ
		) {
			resort(camX, camY, camZ);
			lx = (int) camX;
			ly = (int) camY;
			lz = (int) camZ;
		}
		
		Vector3f vec = new Vector3f();
		
		for (
				ChunkRenderDispatcher.RenderChunk renderChunk :
				(type == RenderType.translucent() ? sorted : chunksInFrustum)
		) {
			if (renderChunk.getCompiledChunk().isEmpty(type))
				continue;
			
			VertexBuffer buffer = renderChunk.getBuffer(type);
			BlockPos blockpos = renderChunk.getOrigin();
			if (uniform != null) {
				vec.set(blockpos.getX(), blockpos.getY(), blockpos.getZ());
				calculateChunkOffset(vec, camX, camY, camZ);
				uniform.set(vec.x(), vec.y(), vec.z());
				uniform.upload();
			}
			
			buffer.drawChunkLayer();
		}
		
		if (uniform != null) uniform.set(0f, 0, 0);
	}
}
