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
    private static final int MAX_VISIBLE_CHUNKS = 25; // Reduced for stability
    private static final int MAX_BUFFER_UPDATES_PER_FRAME = 3;

    // Terrain data structures
    private final Map<String, TerrainChunk> activeChunks;
    private final Set<TerrainChunk> visibleChunks;
    private final Queue<TerrainChunk> bufferUpdateQueue;

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
    private float viewDistance = 200.0f; // Reduced for better performance
    private float nearPlane = 0.1f;
    private float farPlane = 1000.0f;
    private float fov = 60.0f;

    // Safety flags
    private boolean initialized = false;
    private final Object updateLock = new Object();

    public TerrainManager() {
        this.activeChunks = new ConcurrentHashMap<>();
        this.visibleChunks = Collections.synchronizedSet(new HashSet<>());
        this.bufferUpdateQueue = new LinkedList<>();
        this.lastCameraPos = new Vector3f();
        this.currentCameraPos = new Vector3f();
        this.cameraVelocity = new Vector3f();

        // Initialize with some basic chunks around origin for immediate rendering
        initializeBasicTerrain();
        this.initialized = true;

        System.out.println("TerrainManager initialized with view distance: " + viewDistance);
    }

    private void initializeBasicTerrain() {
        // Create a larger initial grid to ensure terrain is visible immediately
        int gridSize = 5; // 5x5 grid for better coverage
        float chunkSize = BASE_CHUNK_SIZE;

        System.out.println("Initializing basic terrain grid: " + gridSize + "x" + gridSize);

        for (int x = -gridSize/2; x <= gridSize/2; x++) {
            for (int z = -gridSize/2; z <= gridSize/2; z++) {
                Vector3f position = new Vector3f(x * chunkSize, 0, z * chunkSize);
                String chunkKey = getChunkKey(position, 0);

                TerrainChunk chunk = new TerrainChunk(position, chunkSize, 0);
                activeChunks.put(chunkKey, chunk);

                // Generate synchronously for immediate availability
                try {
                    chunk.generate();
                    chunk.setVisible(true);
                    visibleChunks.add(chunk);
                    bufferUpdateQueue.offer(chunk);
                    chunksGenerated++;

                    System.out.println("Generated initial chunk at: " + position);
                } catch (Exception e) {
                    System.err.println("Failed to generate initial chunk at " + position + ": " + e.getMessage());
                }
            }
        }

        System.out.println("Initialized " + activeChunks.size() + " basic terrain chunks");
    }

    /**
     * FIXED: Update terrain based on camera position with better chunk management
     */
    public void update(Vector3f cameraPos, float deltaTime) {
        if (!initialized || cameraPos == null) {
            return;
        }

        synchronized (updateLock) {
            try {
                updateCameraTracking(cameraPos, deltaTime);

                // Only perform expensive updates if camera moved significantly or time elapsed
                if (shouldUpdateTerrain()) {
                    System.out.println("Updating terrain for camera at: " + cameraPos);
                    updateTerrainLOD();
                    updateChunkVisibility();
                    lastCameraPos.set(currentCameraPos);
                }

                // Always process buffer updates
                processBufferUpdates();
                updatePerformanceMetrics();

            } catch (Exception e) {
                System.err.println("Error in terrain update: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void updateCameraTracking(Vector3f cameraPos, float deltaTime) {
        currentCameraPos.set(cameraPos);

        // Calculate camera velocity for predictive loading
        if (deltaTime > 0 && deltaTime < 1.0f) { // Sanity check deltaTime
            cameraVelocity.set(currentCameraPos).sub(lastCameraPos).div(deltaTime);
            cameraSpeed = cameraVelocity.length();
        }
    }

    private boolean shouldUpdateTerrain() {
        float distance = currentCameraPos.distance(lastCameraPos);
        long timeSinceUpdate = System.currentTimeMillis() - lastUpdateTime;

        return distance > UPDATE_THRESHOLD || timeSinceUpdate > 3000; // Force update every 3 seconds
    }

    private void updateTerrainLOD() {
        try {
            // FIXED: Better chunk loading strategy
            float chunkSize = BASE_CHUNK_SIZE;
            int loadRadius = Math.min(4, (int) Math.ceil(viewDistance / chunkSize)); // Cap radius

            // Calculate camera chunk position more accurately
            int cameraChunkX = (int) Math.floor(currentCameraPos.x / chunkSize);
            int cameraChunkZ = (int) Math.floor(currentCameraPos.z / chunkSize);

            System.out.println("Camera chunk position: " + cameraChunkX + ", " + cameraChunkZ + " (load radius: " + loadRadius + ")");

            // Load chunks in a grid around camera
            int chunksRequested = 0;
            for (int x = cameraChunkX - loadRadius; x <= cameraChunkX + loadRadius; x++) {
                for (int z = cameraChunkZ - loadRadius; z <= cameraChunkZ + loadRadius; z++) {
                    Vector3f chunkPos = new Vector3f(x * chunkSize, 0, z * chunkSize);
                    Vector3f chunkCenter = new Vector3f(chunkPos.x + chunkSize/2, 0, chunkPos.z + chunkSize/2);
                    float distanceToCamera = currentCameraPos.distance(chunkCenter);

                    if (distanceToCamera < viewDistance) {
                        int lodLevel = calculateLODLevel(distanceToCamera);
                        onChunkNeeded(chunkPos, chunkSize, lodLevel);
                        chunksRequested++;
                    }
                }
            }

            System.out.println("Requested " + chunksRequested + " chunks around camera");

            // Cleanup distant chunks
            cleanupDistantChunks();

        } catch (Exception e) {
            System.err.println("Error in updateTerrainLOD: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private int calculateLODLevel(float distance) {
        if (distance < 80) return 0;
        if (distance < 150) return 1;
        if (distance < 250) return 2;
        return Math.min(3, MAX_LOD_LEVEL);
    }

    private void onChunkNeeded(Vector3f position, float size, int level) {
        try {
            String chunkKey = getChunkKey(position, level);

            TerrainChunk chunk = activeChunks.get(chunkKey);
            if (chunk == null) {
                // Limit total chunks to prevent memory issues
                if (activeChunks.size() >= MAX_VISIBLE_CHUNKS * 3) {
                    System.out.println("Chunk limit reached, skipping: " + position);
                    return;
                }

                // Create new chunk
                chunk = new TerrainChunk(position, size, level);
                activeChunks.put(chunkKey, chunk);

                System.out.println("Created new chunk at: " + position + " level: " + level);

                // Start generation asynchronously
                TerrainChunk finalChunk = chunk;
                chunk.generateAsync().thenRun(() -> {
                    if (finalChunk.isGenerated()) {
                        synchronized (bufferUpdateQueue) {
                            bufferUpdateQueue.offer(finalChunk);
                        }
                        System.out.println("Chunk generation completed: " + position);
                    }
                }).exceptionally(throwable -> {
                    System.err.println("Chunk generation failed for " + position + ": " + throwable.getMessage());
                    return null;
                });

                chunksGenerated++;
            }

            // Mark as visible if in range
            Vector3f chunkCenter = new Vector3f(position.x + size / 2, 0, position.z + size / 2);
            float distance = currentCameraPos.distance(chunkCenter);

            if (distance < viewDistance) {
                synchronized (visibleChunks) {
                    if (!visibleChunks.contains(chunk)) {
                        visibleChunks.add(chunk);
                        System.out.println("Added chunk to visible set: " + position);
                    }
                }
                chunk.setVisible(true);
            }

        } catch (Exception e) {
            System.err.println("Error in onChunkNeeded: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateChunkVisibility() {
        try {
            List<TerrainChunk> chunksToRemove = new ArrayList<>();

            synchronized (visibleChunks) {
                System.out.println("Updating visibility for " + visibleChunks.size() + " chunks");

                // Frustum culling for all visible chunks
                for (TerrainChunk chunk : visibleChunks) {
                    if (chunk.isGenerated()) {
                        Vector3f chunkCenter = new Vector3f(chunk.getPosition())
                                .add(chunk.getSize() / 2, 0, chunk.getSize() / 2);
                        float distance = currentCameraPos.distance(chunkCenter);

                        // Simple distance-based visibility
                        boolean shouldBeVisible = distance < viewDistance;

                        if (shouldBeVisible) {
                            chunk.setVisible(true);
                            if (chunk.needsBufferUpdate()) {
                                synchronized (bufferUpdateQueue) {
                                    bufferUpdateQueue.offer(chunk);
                                }
                            }
                        } else {
                            chunk.setVisible(false);
                            if (distance > viewDistance * 1.8f) {
                                chunksToRemove.add(chunk);
                            }
                        }
                    }
                }

                // Remove distant chunks from visible set
                visibleChunks.removeAll(chunksToRemove);
                if (!chunksToRemove.isEmpty()) {
                    System.out.println("Removed " + chunksToRemove.size() + " distant chunks from visible set");
                }
            }

            // Limit visible chunks to prevent performance issues
            synchronized (visibleChunks) {
                if (visibleChunks.size() > MAX_VISIBLE_CHUNKS) {
                    limitVisibleChunks();
                }
            }

        } catch (Exception e) {
            System.err.println("Error in updateChunkVisibility: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void limitVisibleChunks() {
        try {
            // Sort by distance and keep only the closest chunks
            List<TerrainChunk> sortedChunks;
            synchronized (visibleChunks) {
                sortedChunks = new ArrayList<>(visibleChunks);
            }

            sortedChunks.sort((a, b) -> {
                Vector3f centerA = new Vector3f(a.getPosition()).add(a.getSize() / 2, 0, a.getSize() / 2);
                Vector3f centerB = new Vector3f(b.getPosition()).add(b.getSize() / 2, 0, b.getSize() / 2);
                float distA = currentCameraPos.distance(centerA);
                float distB = currentCameraPos.distance(centerB);
                return Float.compare(distA, distB);
            });

            synchronized (visibleChunks) {
                visibleChunks.clear();
                for (int i = 0; i < Math.min(MAX_VISIBLE_CHUNKS, sortedChunks.size()); i++) {
                    visibleChunks.add(sortedChunks.get(i));
                }
            }

            // Hide chunks that were culled
            for (int i = MAX_VISIBLE_CHUNKS; i < sortedChunks.size(); i++) {
                sortedChunks.get(i).setVisible(false);
            }

            System.out.println("Limited visible chunks to " + Math.min(MAX_VISIBLE_CHUNKS, sortedChunks.size()));

        } catch (Exception e) {
            System.err.println("Error in limitVisibleChunks: " + e.getMessage());
        }
    }

    private void processBufferUpdates() {
        bufferUpdatesThisFrame = 0;

        try {
            synchronized (bufferUpdateQueue) {
                while (!bufferUpdateQueue.isEmpty() &&
                        bufferUpdatesThisFrame < MAX_BUFFER_UPDATES_PER_FRAME) {

                    TerrainChunk chunk = bufferUpdateQueue.poll();
                    if (chunk != null && chunk.isGenerated() && !chunk.areBuffersCreated()) {
                        try {
                            chunk.createBuffers();
                            bufferUpdatesThisFrame++;
                            System.out.println("Created buffers for chunk at: " + chunk.getPosition());
                        } catch (Exception e) {
                            System.err.println("Failed to create buffers for chunk: " + e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error in processBufferUpdates: " + e.getMessage());
        }
    }

    private void cleanupDistantChunks() {
        try {
            Iterator<Map.Entry<String, TerrainChunk>> iterator =
                    activeChunks.entrySet().iterator();

            int removed = 0;
            while (iterator.hasNext()) {
                Map.Entry<String, TerrainChunk> entry = iterator.next();
                TerrainChunk chunk = entry.getValue();

                Vector3f chunkCenter = new Vector3f(chunk.getPosition())
                        .add(chunk.getSize() / 2, 0, chunk.getSize() / 2);
                float distance = currentCameraPos.distance(chunkCenter);

                // Remove chunks that are very far away
                if (distance > viewDistance * 3.0f) {
                    chunk.cleanup();
                    iterator.remove();
                    synchronized (visibleChunks) {
                        visibleChunks.remove(chunk);
                    }
                    removed++;
                }
            }

            if (removed > 0) {
                System.out.println("Cleaned up " + removed + " distant chunks");
            }
        } catch (Exception e) {
            System.err.println("Error in cleanupDistantChunks: " + e.getMessage());
        }
    }

    /**
     * FIXED: Render all visible terrain chunks with debug output
     */
    public void render() {
        if (!initialized) {
            return;
        }

        chunksRendered = 0;

        try {
            List<TerrainChunk> chunksToRender;
            synchronized (visibleChunks) {
                chunksToRender = new ArrayList<>(visibleChunks);
            }

            if (chunksToRender.isEmpty()) {
                System.out.println("No terrain chunks ready to render");
                return;
            }

            // Sort chunks by distance for better depth sorting
            chunksToRender.sort((a, b) -> {
                Vector3f centerA = new Vector3f(a.getPosition()).add(a.getSize() / 2, 0, a.getSize() / 2);
                Vector3f centerB = new Vector3f(b.getPosition()).add(b.getSize() / 2, 0, b.getSize() / 2);
                float distA = currentCameraPos.distance(centerA);
                float distB = currentCameraPos.distance(centerB);
                return Float.compare(distA, distB);
            });

            // Render chunks front to back for better performance
            for (TerrainChunk chunk : chunksToRender) {
                if (chunk.isReadyToRender()) {
                    chunk.render();
                    chunksRendered++;
                }
            }

            if (chunksRendered > 0) {
                System.out.println("Rendered " + chunksRendered + " terrain chunks");
            }
        } catch (Exception e) {
            System.err.println("Error in terrain render: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * FIXED: Get height at specific world coordinates with better fallback
     */
    public float getHeightAt(float worldX, float worldZ) {
        try {
            // Find the chunk containing this position
            String chunkKey = findChunkKeyForPosition(worldX, worldZ);
            TerrainChunk chunk = activeChunks.get(chunkKey);

            if (chunk != null && chunk.isGenerated()) {
                return chunk.getHeightAt(worldX, worldZ);
            }

            // Fallback to procedural generation
            return generateHeightFallback(worldX, worldZ);

        } catch (Exception e) {
            System.err.println("Error getting height at " + worldX + ", " + worldZ + ": " + e.getMessage());
            return 0.0f;
        }
    }

    private String findChunkKeyForPosition(float worldX, float worldZ) {
        // Calculate which chunk this position belongs to
        int chunkX = (int) Math.floor(worldX / BASE_CHUNK_SIZE);
        int chunkZ = (int) Math.floor(worldZ / BASE_CHUNK_SIZE);
        return chunkX + "," + chunkZ + ",0"; // Assume level 0 for height queries
    }

    private float generateHeightFallback(float x, float z) {
        try {
            // FIXED: Better noise fallback that matches chunk generation
            float height = 0;
            float amplitude = 30.0f;
            float frequency = 0.005f;

            // Generate multi-octave noise similar to TerrainChunk
            for (int i = 0; i < 5; i++) {
                height += amplitude * (float)(Math.sin(x * frequency) * Math.cos(z * frequency));
                amplitude *= 0.55f;
                frequency *= 2.1f;
            }

            return height;
        } catch (Exception e) {
            return 0.0f;
        }
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
        this.viewDistance = Math.min(viewDistance, 400.0f); // Cap view distance for stability
    }

    /**
     * FIXED: Get performance information with more detail
     */
    public String getPerformanceInfo() {
        try {
            return String.format(
                    "Terrain: Active=%d, Visible=%d, Generated=%d, Rendered=%d, BufferUpdates=%d, ViewDist=%.0f",
                    activeChunks.size(),
                    visibleChunks.size(),
                    chunksGenerated,
                    chunksRendered,
                    bufferUpdatesThisFrame,
                    viewDistance
            );
        } catch (Exception e) {
            return "Terrain: Error getting performance info";
        }
    }

    /**
     * Cleanup all resources
     */
    public void cleanup() {
        try {
            System.out.println("Cleaning up terrain manager...");

            synchronized (updateLock) {
                initialized = false;

                for (TerrainChunk chunk : activeChunks.values()) {
                    chunk.cleanup();
                }
                activeChunks.clear();

                synchronized (visibleChunks) {
                    visibleChunks.clear();
                }

                synchronized (bufferUpdateQueue) {
                    bufferUpdateQueue.clear();
                }
            }

            // Shutdown the chunk generation thread pool
            TerrainChunk.shutdown();

            System.out.println("Terrain manager cleanup completed");

        } catch (Exception e) {
            System.err.println("Error during terrain manager cleanup: " + e.getMessage());
        }
    }

    /**
     * Force regeneration of chunks around a position (for terrain editing)
     */
    public void invalidateArea(Vector3f center, float radius) {
        try {
            synchronized (updateLock) {
                List<String> keysToRemove = new ArrayList<>();

                for (Map.Entry<String, TerrainChunk> entry : activeChunks.entrySet()) {
                    TerrainChunk chunk = entry.getValue();
                    Vector3f chunkCenter = new Vector3f(chunk.getPosition())
                            .add(chunk.getSize() / 2, 0, chunk.getSize() / 2);

                    if (center.distance(chunkCenter) < radius + chunk.getSize()) {
                        chunk.cleanup();
                        keysToRemove.add(entry.getKey());
                        synchronized (visibleChunks) {
                            visibleChunks.remove(chunk);
                        }
                    }
                }

                for (String key : keysToRemove) {
                    activeChunks.remove(key);
                }

                System.out.println("Invalidated " + keysToRemove.size() + " chunks around " + center);
            }
        } catch (Exception e) {
            System.err.println("Error invalidating terrain area: " + e.getMessage());
        }
    }

    // Getters
    public int getActiveChunkCount() {
        return activeChunks.size();
    }

    public int getVisibleChunkCount() {
        synchronized (visibleChunks) {
            return visibleChunks.size();
        }
    }

    public int getChunksRendered() {
        return chunksRendered;
    }

    public float getViewDistance() {
        return viewDistance;
    }

    public void setViewDistance(float viewDistance) {
        this.viewDistance = Math.max(50.0f, Math.min(viewDistance, 400.0f)); // Clamp for stability
    }

    public boolean isInitialized() {
        return initialized;
    }
}