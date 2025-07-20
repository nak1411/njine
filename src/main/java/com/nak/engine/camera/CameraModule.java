package com.nak.engine.camera;

import com.nak.engine.core.Module;
import com.nak.engine.events.EventBus;
import com.nak.engine.events.annotations.EventHandler;
import com.nak.engine.events.events.InputActionEvent;
import com.nak.engine.events.events.CameraMovedEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

public class CameraModule extends Module {
    private EventBus eventBus;

    // Camera management
    private final Map<String, Camera> cameras = new HashMap<>();
    private Camera activeCamera;
    private String activeCameraName = "main";

    // Camera controllers
    private FreeFlightController freeFlightController;
    private OrbitController orbitController;

    // State tracking
    private Vector3f lastPosition = new Vector3f();
    private boolean initialized = false;

    @Override
    public String getName() {
        return "Camera";
    }

    @Override
    public int getInitializationPriority() {
        return 120; // Initialize after core systems
    }

    @Override
    public void initialize() {
        eventBus = getService(EventBus.class);

        // Create default camera
        FreeCamera mainCamera = new FreeCamera();
        mainCamera.setPosition(new Vector3f(0, 100, 3));
        cameras.put("main", mainCamera);
        activeCamera = mainCamera;

        // Create camera controllers
        freeFlightController = new FreeFlightController(mainCamera);
        orbitController = new OrbitController();

        // Register services
        serviceLocator.register(CameraModule.class, this);
        serviceLocator.register(Camera.class, activeCamera);

        // Register for events
        eventBus.register(this);

        initialized = true;
        System.out.println("Camera module initialized");
    }

    @Override
    public void update(float deltaTime) {
        if (!initialized || activeCamera == null) return;

        try {
            // Update active camera
            activeCamera.update(deltaTime);

            // Update controllers
            if (activeCamera instanceof FreeCamera) {
                freeFlightController.update(deltaTime);
            }

            // Check for camera movement
            Vector3f currentPos = activeCamera.getPosition();
            if (!currentPos.equals(lastPosition, 0.1f)) {
                Vector3f velocity = new Vector3f(currentPos).sub(lastPosition).div(deltaTime);
                eventBus.post(new CameraMovedEvent(lastPosition, currentPos, velocity));
                lastPosition.set(currentPos);
            }

        } catch (Exception e) {
            System.err.println("Error updating camera module: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onInputAction(InputActionEvent event) {
        if (activeCamera == null) return;

        // Delegate input to appropriate controller
        if (activeCamera instanceof FreeCamera && freeFlightController != null) {
            freeFlightController.handleInput(event);
        }
    }

    public Camera getActiveCamera() {
        return activeCamera;
    }

    public void setActiveCamera(String name) {
        Camera camera = cameras.get(name);
        if (camera != null) {
            activeCamera = camera;
            activeCameraName = name;

            // Update service locator
            serviceLocator.register(Camera.class, activeCamera);

            System.out.println("Switched to camera: " + name);
        }
    }

    public void addCamera(String name, Camera camera) {
        cameras.put(name, camera);
    }

    public Camera getCamera(String name) {
        return cameras.get(name);
    }

    public Matrix4f getViewMatrix() {
        return activeCamera != null ? activeCamera.getViewMatrix() : new Matrix4f();
    }

    public Matrix4f getProjectionMatrix() {
        return activeCamera != null ? activeCamera.getProjectionMatrix() : new Matrix4f();
    }

    public Vector3f getPosition() {
        return activeCamera != null ? activeCamera.getPosition() : new Vector3f();
    }

    public Vector3f getDirection() {
        return activeCamera != null ? activeCamera.getDirection() : new Vector3f(0, 0, -1);
    }

    @Override
    public void cleanup() {
        cameras.clear();
        activeCamera = null;

        if (freeFlightController != null) {
            freeFlightController.cleanup();
        }

        initialized = false;
        System.out.println("Camera module cleaned up");
    }
}
