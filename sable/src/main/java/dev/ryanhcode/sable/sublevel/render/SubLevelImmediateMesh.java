package dev.ryanhcode.sable.sublevel.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3dc;
import org.joml.Vector3f;

import java.util.EnumMap;
import java.util.Map;

/**
 * A sub-level's tesselated block models in plot-local space, recorded once by Sodium's immediate
 * sub-level path and replayed each frame with the current transform.
 *
 * <p>Re-tesselating every block every frame, smooth lighting included, dominated frame time for
 * large sub-levels. The light baked here is the plot's own light; parent-world light is still
 * sampled per vertex on replay by {@link SubLevelLightVertexConsumerProvider}.</p>
 */
public final class SubLevelImmediateMesh {

    private final int minX, minY, minZ, maxX, maxY, maxZ;
    private final boolean ambientOcclusion;
    private final boolean cutoutLeaves;
    private final BlockStateModelSet blockModels;
    private final Map<ChunkSectionLayer, Recorder> layers = new EnumMap<>(ChunkSectionLayer.class);

    public SubLevelImmediateMesh(final BoundingBox3ic bounds, final boolean ambientOcclusion, final boolean cutoutLeaves, final BlockStateModelSet blockModels) {
        this.minX = bounds.minX();
        this.minY = bounds.minY();
        this.minZ = bounds.minZ();
        this.maxX = bounds.maxX();
        this.maxY = bounds.maxY();
        this.maxZ = bounds.maxZ();
        this.ambientOcclusion = ambientOcclusion;
        this.cutoutLeaves = cutoutLeaves;
        this.blockModels = blockModels;
    }

    /**
     * @return whether this mesh was built for the same plot bounds and render settings
     */
    public boolean matches(final BoundingBox3ic bounds, final boolean ambientOcclusion, final boolean cutoutLeaves, final BlockStateModelSet blockModels) {
        return this.minX == bounds.minX() && this.minY == bounds.minY() && this.minZ == bounds.minZ()
                && this.maxX == bounds.maxX() && this.maxY == bounds.maxY() && this.maxZ == bounds.maxZ()
                && this.ambientOcclusion == ambientOcclusion && this.cutoutLeaves == cutoutLeaves
                && this.blockModels == blockModels;
    }

    /**
     * @return the plot position that recorded vertices are relative to
     */
    public BlockPos origin() {
        return new BlockPos(this.minX, this.minY, this.minZ);
    }

    public VertexConsumer recorder(final ChunkSectionLayer layer) {
        return this.layers.computeIfAbsent(layer, l -> new Recorder());
    }

    /**
     * Emits the recorded vertices into {@code buffers}, placed with the sub-level's current
     * camera-relative {@code transformation} around {@code rotationPoint}.
     */
    public void replay(final Matrix4f transformation, final Vector3dc rotationPoint, final SubLevelLightVertexConsumerProvider buffers) {
        // Subtract in double precision first; plot coordinates are tens of millions of blocks out.
        final Matrix4f pose = new Matrix4f(transformation).translate(
                (float) (this.minX - rotationPoint.x()),
                (float) (this.minY - rotationPoint.y()),
                (float) (this.minZ - rotationPoint.z())
        );
        final Matrix3f normalMatrix = pose.normal(new Matrix3f());
        final Vector3f position = new Vector3f();
        final Vector3f normal = new Vector3f();

        for (final Map.Entry<ChunkSectionLayer, Recorder> entry : this.layers.entrySet()) {
            final Recorder recorder = entry.getValue();
            if (recorder.vertexCount == 0) {
                continue;
            }

            // Vertices sharing a corner sample the same parent-world light; sample each point once.
            final int sampleCount = recorder.sampleCount();
            if (physicalLightScratch.length < sampleCount) {
                physicalLightScratch = new int[sampleCount];
            }
            final int[] physicalLight = physicalLightScratch;
            final float[] samplePositions = recorder.samplePositions.elements();
            for (int s = 0; s < sampleCount; s++) {
                pose.transformPosition(samplePositions[s * 3], samplePositions[s * 3 + 1], samplePositions[s * 3 + 2], position);
                physicalLight[s] = buffers.samplePhysicalLightAt(position.x, position.y, position.z);
            }

            final VertexConsumer buffer = buffers.getUnlitBuffer(entry.getKey());
            final float[] positions = recorder.positions.elements();
            final float[] uvs = recorder.uvs.elements();
            final float[] normals = recorder.normals.elements();
            final int[] colors = recorder.colors.elements();
            final int[] overlays = recorder.overlays.elements();
            final int[] lights = recorder.lights.elements();
            final int[] sampleIndices = recorder.sampleIndices.elements();

            for (int i = 0; i < recorder.vertexCount; i++) {
                pose.transformPosition(positions[i * 3], positions[i * 3 + 1], positions[i * 3 + 2], position);
                normalMatrix.transform(normals[i * 3], normals[i * 3 + 1], normals[i * 3 + 2], normal).normalize();
                final int light = SubLevelLightVertexConsumerProvider.combineLight(
                        lights[i] & 0xFFFF, lights[i] >>> 16, physicalLight[sampleIndices[i]]);
                buffer.addVertex(position.x, position.y, position.z)
                        .setColor(colors[i])
                        .setUv(uvs[i * 2], uvs[i * 2 + 1])
                        .setUv1(overlays[i] & 0xFFFF, overlays[i] >>> 16)
                        .setUv2(light & 0xFFFF, light >>> 16)
                        .setNormal(normal.x, normal.y, normal.z);
            }
        }
    }

    // Render thread only; replays run one at a time.
    private static int[] physicalLightScratch = new int[0];

    /**
     * Captures vertices as the model renderer emits them. Each setter fills in the vertex most
     * recently started by {@link #addVertex(float, float, float)}.
     */
    private static final class Recorder implements VertexConsumer {
        private final FloatArrayList positions = new FloatArrayList();
        private final FloatArrayList uvs = new FloatArrayList();
        private final FloatArrayList normals = new FloatArrayList();
        private final IntArrayList colors = new IntArrayList();
        private final IntArrayList overlays = new IntArrayList();
        private final IntArrayList lights = new IntArrayList();
        private final IntArrayList sampleIndices = new IntArrayList();
        private final FloatArrayList samplePositions = new FloatArrayList();
        private final Long2IntOpenHashMap sampleLookup = new Long2IntOpenHashMap();
        private int vertexCount;

        private int sampleCount() {
            return this.samplePositions.size() / 3;
        }

        // Light sample points are merged at 1/256 block, far below anything visible in interpolated light.
        private static long sampleKey(final float x, final float y, final float z) {
            final long qx = Math.round(x * 256.0F) + (1 << 20) & 0x1FFFFF;
            final long qy = Math.round(y * 256.0F) + (1 << 20) & 0x1FFFFF;
            final long qz = Math.round(z * 256.0F) + (1 << 20) & 0x1FFFFF;
            return qx | qy << 21 | qz << 42;
        }

        @Override
        public VertexConsumer addVertex(final float x, final float y, final float z) {
            this.positions.add(x);
            this.positions.add(y);
            this.positions.add(z);
            final long key = sampleKey(x, y, z);
            int sample = this.sampleLookup.getOrDefault(key, -1);
            if (sample < 0) {
                sample = this.sampleCount();
                this.sampleLookup.put(key, sample);
                this.samplePositions.add(x);
                this.samplePositions.add(y);
                this.samplePositions.add(z);
            }
            this.sampleIndices.add(sample);
            this.uvs.add(0.0F);
            this.uvs.add(0.0F);
            this.normals.add(0.0F);
            this.normals.add(1.0F);
            this.normals.add(0.0F);
            this.colors.add(0xFFFFFFFF);
            this.overlays.add(0);
            this.lights.add(0);
            this.vertexCount++;
            return this;
        }

        @Override
        public VertexConsumer setColor(final int red, final int green, final int blue, final int alpha) {
            return this.setColor((alpha & 0xFF) << 24 | (red & 0xFF) << 16 | (green & 0xFF) << 8 | blue & 0xFF);
        }

        @Override
        public VertexConsumer setColor(final int color) {
            this.colors.set(this.vertexCount - 1, color);
            return this;
        }

        @Override
        public VertexConsumer setUv(final float u, final float v) {
            this.uvs.set((this.vertexCount - 1) * 2, u);
            this.uvs.set((this.vertexCount - 1) * 2 + 1, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(final int u, final int v) {
            this.overlays.set(this.vertexCount - 1, u & 0xFFFF | v << 16);
            return this;
        }

        @Override
        public VertexConsumer setUv2(final int u, final int v) {
            this.lights.set(this.vertexCount - 1, u & 0xFFFF | v << 16);
            return this;
        }

        @Override
        public VertexConsumer setNormal(final float x, final float y, final float z) {
            final int index = (this.vertexCount - 1) * 3;
            this.normals.set(index, x);
            this.normals.set(index + 1, y);
            this.normals.set(index + 2, z);
            return this;
        }

        @Override
        public VertexConsumer setLineWidth(final float width) {
            return this;
        }
    }
}
