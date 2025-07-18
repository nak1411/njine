package com.nak.engine.terrain;

import org.joml.Vector3f;

class TerrainOctreeNode {
    private Vector3f center;
    private float size;
    private int level;
    private TerrainOctreeNode[] children;
    private TerrainChunk chunk;
    private boolean isLeaf;

    public TerrainOctreeNode(Vector3f center, float size, int level) {
        this.center = new Vector3f(center);
        this.size = size;
        this.level = level;
        this.isLeaf = level >= 4; // Reduced max depth for better performance

        if (isLeaf) {
            chunk = new TerrainChunk(new Vector3f(center.x - size/2, 0, center.z - size/2), size, level);
        } else {
            children = new TerrainOctreeNode[4]; // Quadtree for terrain
            float childSize = size / 2;
            float offset = childSize / 2;

            children[0] = new TerrainOctreeNode(new Vector3f(center.x - offset, center.y, center.z - offset), childSize, level + 1);
            children[1] = new TerrainOctreeNode(new Vector3f(center.x + offset, center.y, center.z - offset), childSize, level + 1);
            children[2] = new TerrainOctreeNode(new Vector3f(center.x - offset, center.y, center.z + offset), childSize, level + 1);
            children[3] = new TerrainOctreeNode(new Vector3f(center.x + offset, center.y, center.z + offset), childSize, level + 1);
        }
    }

    public void update(Vector3f cameraPos) {
        float distance = cameraPos.distance(center);
        float lodDistance = size * 1.5f; // Adjusted LOD distance

        if (isLeaf) {
            // Generate and set visibility for leaf nodes
            if (distance < lodDistance * 2) {
                chunk.generate();
                chunk.setVisible(distance < lodDistance * 4);
            } else {
                chunk.setVisible(false);
            }
        } else {
            // Recursively update children
            for (TerrainOctreeNode child : children) {
                child.update(cameraPos);
            }
        }
    }

    public void render() {
        if (isLeaf) {
            chunk.render();
        } else {
            for (TerrainOctreeNode child : children) {
                child.render();
            }
        }
    }

    public void cleanup() {
        // Cleanup resources if needed
    }
}
