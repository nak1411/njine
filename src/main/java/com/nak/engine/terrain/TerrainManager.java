package com.nak.engine.terrain;

import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TerrainManager {
    // Configuration
    private static final float WORLD_SIZE = 2048.0f;
    private static final float BASE_CHUNK_SIZE = 64.0f;
    private static final int MAX_LOD_LEVEL = 4;
    private static final float UPDATE_THRESHOLD = 8.0f;
    private static final int MAX_VISIBLE_CHUNKS = 100;
    private static final int MAX_BUFFER_UPDATES_PER_FRAME = 3;

    // Terrain data structures
    private final Map<String, TerrainChunk> activeChunks;
    private final Set<TerrainChunk> visibleChunks;
    private final Queue<TerrainChunk> bufferUpdateQueue;
    private final TerrainQuadTree quadTree;

    // Camera tracking
    private Vector3f lastCameraPos;
    private Vector3f currentCameraPos;
    private Vector3f cameraVelocity;
    private float cameraSpeed;

    // Performance metrics
    private int chunksGenerated = 0;
    private int chunksRendered = 0;
    private int bufferUpdatesThisFrame = 0;
    private long lastUpdateTime = 0;

    // Culling parameters
    private float viewDistance = 400.0f;
    private float nearPlane = 0.1f;
    private float farPlane = 1000.0f;
    private float fov = 60.0f;

    public TerrainManager() {
        this.activeChunks = new ConcurrentHashMap<>();
        this.visibleChunks = new HashSet<>();
        this.bufferUpdateQueue = new LinkedList<>();
        this.quadTree = new TerrainQuadTree(new Vector3f(0, 0, 0), WORLD_SIZE);
        this.lastCameraPos = new Vector3f();
        this.currentCameraPos = new Vector3f();
        this.cameraVelocity = new Vector3f();
    }

    /**
     * Update terrain based on camera position and movement
     */
    public void update(Vector3f cameraPos, float deltaTime) {
        updateCameraTracking(cameraPos, deltaTime);

        // Only perform expensive updates if camera moved significantly
        if (shouldUpdateTerrain()) {
            updateTerrainLOD();
            updateChunkVisibility();
            processBufferUpdates();
            lastCameraPos.set(currentCameraPos);
        } else {
            // Still process buffer updates even if camera didn't move much
            processBufferUpdates();
        }

        updatePerformanceMetrics();
    }

    private void updateCameraTracking(Vector3f cameraPos, float deltaTime) {
        currentCameraPos.set(cameraPos);

        // Calculate camera velocity for predictive loading
        if (deltaTime > 0) {
            cameraVelocity.set(currentCameraPos).sub(lastCameraPos).div(deltaTime);
            cameraSpeed = cameraVelocity.length();
        }
    }

    private boolean shouldUpdateTerrain() {
        return currentCameraPos.distance(lastCameraPos) > UPDATE_THRESHOLD ||
                System.currentTimeMillis() - lastUpdateTime > 1000; // Force update every second
    }

    private void updateTerrainLOD() {
        // Clear visibility for fresh calculation
        visibleChunks.clear();

        // Update quadtree with current camera position
        quadTree.update(currentCameraPos, viewDistance, this::onChunkNeeded);

        // Predictive loading based on camera movement
        if (cameraSpeed > 5.0f) {
            predictiveChunkLoading();
        }

        // Cleanup distant chunks
        cleanupDistantChunks();
    }

    private void predictiveChunkLoading() {
        // Load chunks in the direction of camera movement
        Vector3f predictedPos = new Vector3f(currentCameraPos)
                .add(new Vector3f(cameraVelocity).mul(2.0f)); // Predict 2 seconds ahead

        quadTree.updatePredictive(predictedPos, viewDistance * 0.7f, this::onChunkNeeded);
    }

    private void onChunkNeeded(Vector3f position, float size, int level) {
        String chunkKey = getChunkKey(position, level);

        TerrainChunk chunk = activeChunks.get(chunkKey);
        if (chunk == null) {
            // Create new chunk
            chunk = new TerrainChunk(position, size, level);
            activeChunks.put(chunkKey, chunk);

            // Start generation asynchronously
            TerrainChunk finalChunk = chunk;
            chunk.generateAsync().thenRun(() -> {
                if (finalChunk.isGenerated()) {
                    bufferUpdateQueue.offer(finalChunk);
                }
            });

            chunksGenerated++;
        }

        // Mark as visible if in range
        float distance = currentCameraPos.distance(
                new Vector3f(position.x + size / 2, 0, position.z + size / 2)
        );

        if (distance < viewDistance) {
            visibleChunks.add(chunk);
            chunk.setVisible(true);
        }
    }

    private void updateChunkVisibility() {
        // Frustum culling for all active chunks
        for (TerrainChunk chunk : activeChunks.values()) {
            if (chunk.isGenerated()) {
                boolean inFrustum = chunk.isInFrustum(
                        currentCameraPos,
                        getCameraFront(),
                        fov,
                        farPlane,
                        nearPlane
                );

                if (inFrustum && visibleChunks.contains(chunk)) {
                    chunk.setVisible(true);
                    if (chunk.needsBufferUpdate()) {
                        bufferUpdateQueue.offer(chunk);
                    }
                } else {
                    chunk.setVisible(false);
                }
            }
        }

        // Limit visible chunks to prevent performance issues
        if (visibleChunks.size() > MAX_VISIBLE_CHUNKS) {
            limitVisibleChunks();
        }
    }

    private void limitVisibleChunks() {
        // Sort by distance and keep only the closest chunks
        List<TerrainChunk> sortedChunks = new ArrayList<>(visibleChunks);
        sortedChunks.sort((a, b) -> {
            float distA = currentCameraPos.distance(a.getPosition());
            float distB = currentCameraPos.distance(b.getPosition());
            return Float.compare(distA, distB);
        });

        visibleChunks.clear();
        for (int i = 0; i < Math.min(MAX_VISIBLE_CHUNKS, sortedChunks.size()); i++) {
            visibleChunks.add(sortedChunks.get(i));
        }

        // Hide chunks that were culled
        for (int i = MAX_VISIBLE_CHUNKS; i < sortedChunks.size(); i++) {
            sortedChunks.get(i).setVisible(false);
        }
    }

    private void processBufferUpdates() {
        bufferUpdatesThisFrame = 0;

        while (!bufferUpdateQueue.isEmpty() &&
                bufferUpdatesThisFrame < MAX_BUFFER_UPDATES_PER_FRAME) {

            TerrainChunk chunk = bufferUpdateQueue.poll();
            if (chunk != null && chunk.isGenerated() && !chunk.areBuffersCreated()) {
                chunk.createBuffers();
                bufferUpdatesThisFrame++;
            }
        }
    }

    private void cleanupDistantChunks() {
        Iterator<Map.Entry<String, TerrainChunk>> iterator =
                activeChunks.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, TerrainChunk> entry = iterator.next();
            TerrainChunk chunk = entry.getValue();

            float distance = currentCameraPos.distance(chunk.getPosition());

            // Remove chunks that are very far away
            if (distance > viewDistance * 2.0f) {
                chunk.cleanup();
                iterator.remove();
                visibleChunks.remove(chunk);
            }
        }
    }

    /**
     * Render all visible terrain chunks
     */
    public void render() {
        chunksRendered = 0;

        // Sort chunks by distance for better depth sorting
        List<TerrainChunk> sortedChunks = new ArrayList<>(visibleChunks);
        sortedChunks.sort((a, b) -> {
            float distA = currentCameraPos.distance(a.getPosition());
            float distB = currentCameraPos.distance(b.getPosition());
            return Float.compare(distA, distB);
        });

        // Render chunks front to back for better performance
        for (TerrainChunk chunk : sortedChunks) {
            if (chunk.isReadyToRender()) {
                chunk.render();
                chunksRendered++;
            }
        }
    }

    /**
     * Get height at specific world coordinates
     */
    public float getHeightAt(float worldX, float worldZ) {
        // Find the chunk containing this position
        String chunkKey = findChunkKeyForPosition(worldX, worldZ);
        TerrainChunk chunk = activeChunks.get(chunkKey);

        if (chunk != null && chunk.isGenerated()) {
            return chunk.getHeightAt(worldX, worldZ);
        }

        // Fallback to procedural generation
        return generateHeightFallback(worldX, worldZ);
    }

    private String findChunkKeyForPosition(float worldX, float worldZ) {
        // Calculate which chunk this position belongs to
        int chunkX = (int) Math.floor(worldX / BASE_CHUNK_SIZE);
        int chunkZ = (int) Math.floor(worldZ / BASE_CHUNK_SIZE);
        return chunkX + "," + chunkZ + ",0"; // Assume level 0 for height queries
    }

    private float generateHeightFallback(float x, float z) {
        // Simple noise fallback for positions without chunks
        return (float) (Math.sin(x * 0.005f) * Math.cos(z * 0.005f) * 20.0f +
                Math.sin(x * 0.01f) * Math.cos(z * 0.01f) * 10.0f);
    }

    private Vector3f getCameraFront() {
        // This should be provided by the camera system
        // For now, return a default forward vector
        return new Vector3f(0, 0, -1);
    }

    private String getChunkKey(Vector3f position, int level) {
        return (int) position.x + "," + (int) position.z + "," + level;
    }

    private void updatePerformanceMetrics() {
        lastUpdateTime = System.currentTimeMillis();
    }

    /**
     * Set camera parameters for frustum culling
     */
    public void setCameraParameters(float fov, float nearPlane, float farPlane, float viewDistance) {
        this.fov = fov;
        this.nearPlane = nearPlane;
        this.farPlane = farPlane;
        this.viewDistance = viewDistance;
    }

    /**
     * Get performance information
     */
    public String getPerformanceInfo() {
        return String.format(
                "Terrain: Chunks Active=%d, Visible=%d, Generated=%d, Rendered=%d, Buffer Updates=%d",
                activeChunks.size(),
                visibleChunks.size(),
                chunksGenerated,
                chunksRendered,
                bufferUpdatesThisFrame
        );
    }

    /**
     * Cleanup all resources
     */
    public void cleanup() {
        for (TerrainChunk chunk : activeChunks.values()) {
            chunk.cleanup();
        }
        activeChunks.clear();
        visibleChunks.clear();
        bufferUpdateQueue.clear();

        // Shutdown the chunk generation thread pool
        TerrainChunk.shutdown();
    }

    /**
     * Force regeneration of chunks around a position (for terrain editing)
     */
    public void invalidateArea(Vector3f center, float radius) {
        for (TerrainChunk chunk : activeChunks.values()) {
            Vector3f chunkCenter = new Vector3f(chunk.getPosition())
                    .add(chunk.getSize() / 2, 0, chunk.getSize() / 2);

            if (center.distance(chunkCenter) < radius + chunk.getSize()) {
                chunk.cleanup();
                String key = getChunkKey(chunk.getPosition(), chunk.getLevel());
                activeChunks.remove(key);
                visibleChunks.remove(chunk);
            }
        }
    }

    // Getters
    public int getActiveChunkCount() {
        return activeChunks.size();
    }

    public int getVisibleChunkCount() {
        return visibleChunks.size();
    }

    public int getChunksRendered() {
        return chunksRendered;
    }

    public float getViewDistance() {
        return viewDistance;
    }

    public void setViewDistance(float viewDistance) {
        this.viewDistance = viewDistance;
    }

    /**
     * Spatial data structure for efficient terrain chunk management
     */
    private static class TerrainQuadTree {
        private final Vector3f center;
        private final float size;
        private final int maxDepth;
        private TerrainQuadTree[] children;
        private boolean isLeaf;

        public TerrainQuadTree(Vector3f center, float size) {
            this(center, size, 0, MAX_LOD_LEVEL);
        }

        private TerrainQuadTree(Vector3f center, float size, int depth, int maxDepth) {
            this.center = new Vector3f(center);
            this.size = size;
            this.maxDepth = maxDepth;
            this.isLeaf = depth >= maxDepth || size <= BASE_CHUNK_SIZE;

            if (!isLeaf) {
                children = new TerrainQuadTree[4];
                float childSize = size / 2;
                float offset = childSize / 2;

                children[0] = new TerrainQuadTree(
                        new Vector3f(center.x - offset, center.y, center.z - offset),
                        childSize, depth + 1, maxDepth
                );
                children[1] = new TerrainQuadTree(
                        new Vector3f(center.x + offset, center.y, center.z - offset),
                        childSize, depth + 1, maxDepth
                );
                children[2] = new TerrainQuadTree(
                        new Vector3f(center.x - offset, center.y, center.z + offset),
                        childSize, depth + 1, maxDepth
                );
                children[3] = new TerrainQuadTree(
                        new Vector3f(center.x + offset, center.y, center.z + offset),
                        childSize, depth + 1, maxDepth
                );
            }
        }

        public void update(Vector3f cameraPos, float viewDistance, ChunkCallback callback) {
            float distance = cameraPos.distance(center);

            if (distance - (size * 0.7f) > viewDistance) {
                return; // Too far away
            }

            if (isLeaf) {
                // Calculate LOD level based on distance
                int lodLevel = calculateLODLevel(distance);
                Vector3f chunkPos = new Vector3f(center.x - size / 2, 0, center.z - size / 2);
                callback.onChunkNeeded(chunkPos, size, lodLevel);
            } else {
                // Recursively update children
                for (TerrainQuadTree child : children) {
                    child.update(cameraPos, viewDistance, callback);
                }
            }
        }

        public void updatePredictive(Vector3f predictedPos, float viewDistance, ChunkCallback callback) {
            float distance = predictedPos.distance(center);

            if (distance - (size * 0.7f) > viewDistance) {
                return;
            }

            if (isLeaf) {
                int lodLevel = calculateLODLevel(distance) + 1; // Lower detail for predictive
                Vector3f chunkPos = new Vector3f(center.x - size / 2, 0, center.z - size / 2);
                callback.onChunkNeeded(chunkPos, size, Math.min(lodLevel, MAX_LOD_LEVEL));
            } else {
                for (TerrainQuadTree child : children) {
                    child.updatePredictive(predictedPos, viewDistance, callback);
                }
            }
        }

        private int calculateLODLevel(float distance) {
            if (distance < 50) return 0;
            if (distance < 100) return 1;
            if (distance < 200) return 2;
            if (distance < 300) return 3;
            return MAX_LOD_LEVEL;
        }
    }

    @FunctionalInterface
    private interface ChunkCallback {
        void onChunkNeeded(Vector3f position, float size, int level);
    }
}