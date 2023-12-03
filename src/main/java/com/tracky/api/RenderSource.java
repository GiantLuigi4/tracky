package com.tracky.api;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.tracky.util.TrackyViewArea;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.*;
import java.util.function.Consumer;

public class RenderSource {

	protected final Collection<SectionPos> sections;
	protected List<ChunkRenderDispatcher.RenderChunk> chunksInSource = new ArrayList<>();
	protected final List<ChunkRenderDispatcher.RenderChunk> chunksInFrustum = new ObjectArrayList<>();

	protected boolean forceCulling = false;

	protected Queue<SectionPos> newSections = new ArrayDeque<>();

	protected final Vector3i lastSortPos = new Vector3i(Integer.MIN_VALUE);
	protected List<ChunkRenderDispatcher.RenderChunk> sorted = new LinkedList<>();

	public RenderSource(Collection<SectionPos> sections) {
		this.sections = new HashSet<>(sections);
	}

	public void addSection(SectionPos pos) {
		if (this.sections.add(pos)) {
			this.newSections.add(pos);
		}
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
			}
		}
	}

	/**
	 * used by tracky to figure out what chunks to force redrawing of
	 *
	 * @return the list of render chunks which are currently in the frustum, or a list of only dirty render chunks if you have a way to keep track of that
	 */
	public List<ChunkRenderDispatcher.RenderChunk> getChunksInFrustum() {
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

	public boolean needsCulling() {
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
		this.sorted.sort(Comparator.<ChunkRenderDispatcher.RenderChunk>comparingDouble(left -> {
			center.set(left.getOrigin().getX() + 8, left.getOrigin().getY() + 8, left.getOrigin().getZ() + 8);
			this.calculateChunkOffset(center, camX, camY, camZ);
			matrix.transformPosition(center);
			return center.lengthSquared();
		}).reversed());

		ChunkRenderDispatcher dispatcher = Minecraft.getInstance().levelRenderer.getChunkRenderDispatcher();
		for (ChunkRenderDispatcher.RenderChunk chunk : this.chunksInFrustum) {
			// We don't bother adding in vanilla "improvements" because we want these chunks to ACTUALLY be resorted when they should be
			chunk.resortTransparency(RenderType.translucent(), dispatcher);
		}
	}

	/**
	 * @param viewArea   the view area which contains RenderChunks
	 * @param sectionPos the position of the section being added
	 * @return if the render chunk got added
	 */
	protected boolean handleAdd(Consumer<ChunkRenderDispatcher.RenderChunk> dst, TrackyViewArea viewArea, SectionPos sectionPos) {
		if (sectionPos == null) return true;

		// directly accessing the extended map, as the chunks are guaranteed to be within it if they have been created
		ChunkPos ckPos = new ChunkPos(sectionPos.getX(), sectionPos.getZ());
		ChunkRenderDispatcher.RenderChunk[] renderChunks = viewArea.getRenderChunkColumn(ckPos);

		if (renderChunks == null) {
			viewArea.setDirty(sectionPos.getX(), sectionPos.getY(), sectionPos.getZ(), false);
			renderChunks = viewArea.getRenderChunkColumn(ckPos);
		}

		if (renderChunks == null) return false;

		// calculate y-index
		int y = Math.floorMod(sectionPos.getY() - Minecraft.getInstance().level.getMinSection(), Minecraft.getInstance().level.getSectionsCount());
		ChunkRenderDispatcher.RenderChunk renderChunk = renderChunks[y];

		if (renderChunk == null) {
			viewArea.setDirty(sectionPos.getX(), sectionPos.getY(), sectionPos.getZ(), false);
			return false;
		}

		// without this, the render sections can get messed up if this gets called before the chunks are synced
		if (!renderChunk.getOrigin().equals(sectionPos.origin())) {
			viewArea.setDirty(sectionPos.getX(), sectionPos.getY(), sectionPos.getZ(), false);
			renderChunk = renderChunks[y];

			if (renderChunk == null || renderChunk.getOrigin().equals(sectionPos.origin())) {
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
	public void updateCompiledChunk(ChunkRenderDispatcher.RenderChunk chunk) {
		// Don't bother trying to render the chunk at all if it can't be rendered
		if (chunk.getCompiledChunk().hasNoRenderableLayers()) {
			return;
		}

		this.chunksInSource.add(chunk);

		// The new chunk should be checked to see if it's in the frustum
		this.forceCulling = true;

		if (!chunk.getCompiledChunk().isEmpty(RenderType.translucent())) {
			this.sort();
		}
	}

	/**
	 * updates render chunks for when new sections are added to the render source
	 *
	 * @param viewArea the view area which contains the RenderChunks
	 */
	public void updateChunks(TrackyViewArea viewArea, Consumer<ChunkRenderDispatcher.RenderChunk> toCompile) {
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

	/**
	 * calculates the render offset of a chunk relative to the render origin
	 *
	 * @param vec  the origin of the chunk in global space
	 * @param camX the camera's X position
	 * @param camY the camera's Y position
	 * @param camZ the camera's Z position
	 */
	public void calculateChunkOffset(Vector3f vec, double camX, double camY, double camZ) {
		vec.sub((float) camX, (float) camY, (float) camZ);
	}

	/**
	 * Transforms the specified stack. The transformation should be in local chunk space. {@link #calculateChunkOffset(Vector3f, double, double, double)} transforms the chunk from the player position to the camera.
	 *
	 * @param matrix the matrix to transform the space
	 */
	public void transform(PoseStack matrix, double camX, double camY, double camZ) {
	}

	// TODO: probably shouldn't be giving direct access to the shader instance

	/**
	 * draws the chunks in the render source
	 * if you want to apply transformations to the rendering, see {@link com.tracky.debug.TestSource}
	 * most mods will want to override this, do some setup before calling super, and then do some teardown afterwards
	 *
	 * @param matrix   the active pose stack
	 * @param area     the view area
	 * @param instance the shader being used
	 * @param type     the render type being drawn
	 * @param camX     the camera's X position
	 * @param camY     the camera's Y position
	 * @param camZ     the camera's Z position
	 */
	public void draw(PoseStack matrix, TrackyViewArea area, ShaderInstance instance, RenderType type, double camX, double camY, double camZ) {
		if (instance.MODEL_VIEW_MATRIX != null) {
			matrix.pushPose();
			matrix.translate(-camX, -camY, -camZ);
			this.transform(matrix, camX, camY, camZ);
			matrix.translate(camX, camY, camZ);
			instance.MODEL_VIEW_MATRIX.set(matrix.last().pose());
			instance.MODEL_VIEW_MATRIX.upload();
			matrix.popPose();
		}

		Uniform uniform = instance.CHUNK_OFFSET;

		// Copy logic from LevelRenderer to determine if the chunk should be resorted
		if (type == RenderType.translucent() && this.lastSortPos.distanceSquared((int) camX, (int) camY, (int) camZ) > 1.0) {
			this.sort(camX, camY, camZ);
			this.lastSortPos.set((int) camX, (int) camY, (int) camZ);
		}

		Vector3f chunkOffset = new Vector3f();

		Collection<ChunkRenderDispatcher.RenderChunk> chunks = type == RenderType.translucent() ? this.sorted : this.chunksInFrustum;
		for (ChunkRenderDispatcher.RenderChunk renderChunk : chunks) {
			if (renderChunk.getCompiledChunk().isEmpty(type)) {
				continue;
			}

			VertexBuffer buffer = renderChunk.getBuffer(type);
			BlockPos blockpos = renderChunk.getOrigin();
			if (uniform != null) {
				chunkOffset.set(blockpos.getX(), blockpos.getY(), blockpos.getZ());
				this.calculateChunkOffset(chunkOffset, camX, camY, camZ);
				uniform.set(chunkOffset.x(), chunkOffset.y(), chunkOffset.z());
				uniform.upload();
			}

			buffer.bind();
			buffer.draw();
		}

		if (uniform != null) {
			uniform.set(0f, 0f, 0f);
		}

		if (instance.MODEL_VIEW_MATRIX != null) {
			instance.MODEL_VIEW_MATRIX.set(matrix.last().pose());
		}

		// VertexBuffer#unbind is called after this inject
	}
}
