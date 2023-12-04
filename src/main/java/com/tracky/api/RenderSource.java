package com.tracky.api;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.*;
import java.util.function.Consumer;

public class RenderSource {

	protected final Collection<SectionPos> sections;
	protected Set<TrackyRenderChunk> chunksInSource = new HashSet<>();
	protected final List<TrackyRenderChunk> chunksInFrustum = new ObjectArrayList<>();

	protected boolean forceCulling = false;

	protected Queue<SectionPos> newSections = new ArrayDeque<>();

	protected final Vector3i lastSortPos = new Vector3i(Integer.MIN_VALUE);
	protected List<TrackyRenderChunk> sorted = new LinkedList<>();

	public RenderSource(Collection<SectionPos> sections) {
		this.sections = sections;
	}

	public void addSection(SectionPos pos) {
		if (this.sections.add(pos)) {
			this.newSections.add(pos);
		}
	}

	public void removeSection(SectionPos pos) {
		if (this.sections.remove(pos) && !this.newSections.remove(pos)) {
			for (TrackyRenderChunk renderChunk : this.chunksInSource) {
				BlockPos origin = renderChunk.getChunkOrigin();
				if (origin.getX() == pos.minBlockX() && origin.getY() == pos.minBlockY() && origin.getZ() == pos.minBlockZ()) {
					this.chunksInSource.remove(renderChunk);
					return;
				}
			}
		}
	}

	/**
	 * used by tracky to figure out what chunks to force redrawing of
	 *
	 * @return the list of render chunks which are currently in the frustum, or a list of only dirty render chunks if you have a way to keep track of that
	 */
	public List<TrackyRenderChunk> getChunksInFrustum() {
		return this.chunksInFrustum;
	}

	/**
	 * Tests if the render source contains a section
	 * This is used by various hooks to vanilla code to know if tracky should use it's own caches or if it should use vanilla's ones
	 *
	 * @param pos the position of the section
	 * @return if the render source has it in its list of sections
	 */
	public boolean containsSection(SectionPos pos) {
		return this.sections.contains(pos);
	}

	@ApiStatus.Internal
	public final void doFrustumUpdate(Camera camera, Frustum frustum) {
		this.forceCulling = false;
		this.updateFrustum(camera, frustum);
	}

	/**
	 * called when vanilla updates the frustum, if the render source is visible
	 * ideally, mods would do some reprojected frustum checking on each of the sections
	 *
	 * @param camera  the game camera
	 * @param frustum the frustum to use
	 */
	protected void updateFrustum(Camera camera, Frustum frustum) {
		this.chunksInFrustum.clear();
		this.chunksInFrustum.addAll(this.chunksInSource);
	}

	public boolean needsFrustumUpdate() {
		return this.forceCulling;
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
	 * does transparency sorting
	 *
	 * @param camX the camera's X position
	 * @param camY the camera's Y position
	 * @param camZ the camera's Z position
	 */
	public void sort(double camX, double camY, double camZ) {
		Vector3f center = new Vector3f();

		PoseStack stack = new PoseStack();
		stack.translate(-camX, -camY, -camZ);
		this.transform(stack, camX, camY, camZ);
		stack.translate(camX, camY, camZ);
		Matrix4f matrix = stack.last().pose();

		this.sorted.clear();
		this.sorted.addAll(this.chunksInFrustum);
		this.sorted.sort(Comparator.<TrackyRenderChunk>comparingDouble(left -> {
			BlockPos origin = left.getChunkOrigin();
			center.set(origin.getX() + 8 - camX, origin.getY() + 8 - camY, origin.getZ() + 8 - camZ);
			matrix.transformPosition(center);
			return center.lengthSquared();
		}).reversed());

		for (TrackyRenderChunk chunk : this.chunksInFrustum) {
			// We don't bother adding in vanilla "improvements" because we want these chunks to ACTUALLY be resorted when they should be
			chunk.resort(RenderType.translucent());
		}
	}

	/**
	 * @param viewArea   the view area which contains RenderChunks
	 * @param sectionPos the position of the section being added
	 * @return if the render chunk got added
	 */
	protected boolean handleAdd(Consumer<TrackyRenderChunk> dst, TrackyViewArea viewArea, SectionPos sectionPos) {
		if (sectionPos == null) return true;

		// directly accessing the extended map, as the chunks are guaranteed to be within it if they have been created
		TrackyRenderChunk renderChunk = viewArea.tryCreateRenderChunk(sectionPos);
		if (renderChunk == null) {
			viewArea.setDirty(sectionPos, false);
			return false;
		}

		// without this, the render sections can get messed up if this gets called before the chunks are synced
		if (!renderChunk.getChunkOrigin().equals(sectionPos.origin())) {
			viewArea.setDirty(sectionPos, false);
			renderChunk = viewArea.getRenderChunk(sectionPos);

			if (renderChunk == null || !renderChunk.getChunkOrigin().equals(sectionPos.origin())) {
				return false;
			}
		}

		dst.accept(renderChunk);
		return true;
	}

	/**
	 * Forces the renderer to resort translucent chunks next frame.
	 */
	public void sort() {
		this.sorted.clear();
		this.lastSortPos.set(Integer.MIN_VALUE);
	}

	/**
	 * clears out the RenderChunk caches and marks them to be updated
	 * used to account for F3+A
	 */
	public void refresh() {
		// We can't draw the chunks that were in the frustum
		this.chunksInFrustum.clear();
		this.forceCulling = true;
		this.sort();

		this.chunksInSource.clear();
		this.newSections.clear();
		this.newSections.addAll(this.sections);
	}

	/**
	 * Deletes any extra resources associated with this source. This is called every time the level is changed.
	 */
	public void free() {
	}

	/**
	 * Updates the render status of the specified render chunk. This is called after the chunk has been compiled and can now be rendered.
	 *
	 * @param chunk The chunk that finished compiling
	 */
	public void updateCompiledChunk(TrackyRenderChunk chunk) {
		if (!this.chunksInSource.add(chunk)) {
			return;
		}

		// The new chunk should be checked to see if it's in the frustum
		this.forceCulling = true;

		if (chunk.needsSorting()) {
			this.sort();
		}
	}

	/**
	 * updates render chunks for when new sections are added to the render source
	 *
	 * @param viewArea the view area which contains the RenderChunks
	 */
	public void updateChunks(TrackyViewArea viewArea, Consumer<TrackyRenderChunk> toCompile) {
		if (this.newSections.isEmpty()) {
			return;
		}

		List<SectionPos> toKeep = new ObjectArrayList<>();

		int i = 0;
		// run updates
		while (!this.newSections.isEmpty() && i < 1000) {
			SectionPos newSection = this.newSections.poll();
			if (!this.handleAdd(toCompile, viewArea, newSection)) {
				toKeep.add(newSection);
			}
			i++;
		}

		// try adding failed sections to next iteration
		this.newSections.addAll(toKeep);

		// force resort
		this.sort();

		// force a culling check
		this.forceCulling = true;
	}

	public final Matrix4f getTransformation(double camX, double camY, double camZ) {
		PoseStack stack = new PoseStack();
		stack.translate(-camX, -camY, -camZ);
		this.transform(stack, camX, camY, camZ);
		stack.translate(camX, camY, camZ);
		return stack.last().pose();
	}

	/**
	 * Transforms the whole render source to a new position. The individual chunks in the source are not allowed to be transformed.
	 *
	 * @param matrix the matrix to transform the space
	 */
	public void transform(PoseStack matrix, double camX, double camY, double camZ) {
	}

	/**
	 * draws the chunks in the render source
	 * if you want to apply transformations to the rendering, see {@link com.tracky.debug.TestSource}
	 * most mods will want to override this, do some setup before calling super, and then do some teardown afterwards
	 *
	 * @param matrix the active pose stack
	 * @param area   the view area
	 * @param layer  the render type being drawn
	 * @param camX   the camera's X position
	 * @param camY   the camera's Y position
	 * @param camZ   the camera's Z position
	 */
	public void draw(TrackyChunkRenderer chunkRenderer, PoseStack matrix, TrackyViewArea area, RenderType layer, double camX, double camY, double camZ) {
		matrix.pushPose();
		matrix.translate(-camX, -camY, -camZ);
		this.transform(matrix, camX, camY, camZ);
		matrix.translate(camX, camY, camZ);
		chunkRenderer.setModelViewMatrix(matrix.last().pose());
		matrix.popPose();

		// Copy logic from LevelRenderer to determine if the chunk should be resorted
		if (layer == RenderType.translucent() && this.lastSortPos.distanceSquared((int) camX, (int) camY, (int) camZ) > 1.0) {
			this.sort(camX, camY, camZ);
			this.lastSortPos.set((int) camX, (int) camY, (int) camZ);
		}

		chunkRenderer.render(layer == RenderType.translucent() ? this.sorted : this.chunksInFrustum, layer);
	}
}
