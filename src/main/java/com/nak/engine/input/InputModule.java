package com.nak.engine.input;

import com.nak.engine.core.Module;
import com.nak.engine.config.InputSettings;
import com.nak.engine.events.EventBus;
import com.nak.engine.events.events.InputActionEvent;
import com.nak.engine.input.handlers.KeyboardHandler;
import com.nak.engine.input.handlers.MouseHandler;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

public class InputModule extends Module {
    private InputSettings settings;
    private EventBus eventBus;
    private long windowHandle;

    // Input components
    private ActionMap actionMap;
    private InputContext currentContext;
    private KeyboardHandler keyboardHandler;
    private MouseHandler mouseHandler;

    // Input state
    private final Map<String, Boolean> actionStates = new HashMap<>();
    private final Map<String, Boolean> previousActionStates = new HashMap<>();

    private boolean inputEnabled = true;
    private boolean mouseLocked = true;

    @Override
    public String getName() {
        return "Input";
    }

    @Override
    public int getInitializationPriority() {
        return 150; // Initialize after window but before game systems
    }

    @Override
    public void initialize() {
        settings = getService(InputSettings.class);
        eventBus = getService(EventBus.class);

        // Get window handle from engine or window service
        // windowHandle = getService(WindowService.class).getWindowHandle();

        // Initialize input components
        actionMap = new ActionMap(settings.getKeyBindings());
        currentContext = new InputContext("default");
        keyboardHandler = new KeyboardHandler(this);
        mouseHandler = new MouseHandler(this);

        // Setup GLFW callbacks
        setupCallbacks();

        // Register services
        serviceLocator.register(ActionMap.class, actionMap);
        serviceLocator.register(InputContext.class, currentContext);

        System.out.println("Input module initialized");
    }

    private void setupCallbacks() {
        // Key callback
        GLFW.glfwSetKeyCallback(windowHandle, (window, key, scancode, action, mods) -> {
            if (!inputEnabled) return;
            keyboardHandler.handleKey(key, action, mods);
        });

        // Mouse button callback
        GLFW.glfwSetMouseButtonCallback(windowHandle, (window, button, action, mods) -> {
            if (!inputEnabled) return;
            mouseHandler.handleMouseButton(button, action, mods);
        });

        // Cursor position callback
        GLFW.glfwSetCursorPosCallback(windowHandle, (window, xpos, ypos) -> {
            if (!inputEnabled) return;
            mouseHandler.handleMouseMove(xpos, ypos);
        });

        // Scroll callback
        GLFW.glfwSetScrollCallback(windowHandle, (window, xoffset, yoffset) -> {
            if (!inputEnabled) return;
            mouseHandler.handleScroll(xoffset, yoffset);
        });
    }

    @Override
    public void update(float deltaTime) {
        if (!inputEnabled) return;

        try {
            // Store previous action states
            previousActionStates.clear();
            previousActionStates.putAll(actionStates);

            // Update current action states
            updateActionStates();

            // Process input events
            processActionEvents();

            // Update input components
            keyboardHandler.update(deltaTime);
            mouseHandler.update(deltaTime);

        } catch (Exception e) {
            System.err.println("Error updating input module: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateActionStates() {
        for (Map.Entry<String, Integer> binding : actionMap.getKeyBindings().entrySet()) {
            String action = binding.getKey();
            int keyCode = binding.getValue();

            boolean pressed = GLFW.glfwGetKey(windowHandle, keyCode) == GLFW.GLFW_PRESS;
            actionStates.put(action, pressed);
        }
    }

    private void processActionEvents() {
        for (String action : actionStates.keySet()) {
            boolean current = actionStates.getOrDefault(action, false);
            boolean previous = previousActionStates.getOrDefault(action, false);

            if (current && !previous) {
                // Action pressed
                eventBus.post(new InputActionEvent(
                        InputAction.valueOf(action.toUpperCase()),
                        InputActionEvent.Type.PRESSED,
                        0
                ));
            } else if (!current && previous) {
                // Action released
                eventBus.post(new InputActionEvent(
                        InputAction.valueOf(action.toUpperCase()),
                        InputActionEvent.Type.RELEASED,
                        0
                ));
            } else if (current) {
                // Action held
                eventBus.post(new InputActionEvent(
                        InputAction.valueOf(action.toUpperCase()),
                        InputActionEvent.Type.HELD,
                        0
                ));
            }
        }
    }

    public boolean isActionPressed(String action) {
        return actionStates.getOrDefault(action, false);
    }

    public boolean wasActionJustPressed(String action) {
        boolean current = actionStates.getOrDefault(action, false);
        boolean previous = previousActionStates.getOrDefault(action, false);
        return current && !previous;
    }

    public boolean wasActionJustReleased(String action) {
        boolean current = actionStates.getOrDefault(action, false);
        boolean previous = previousActionStates.getOrDefault(action, false);
        return !current && previous;
    }

    public void setInputEnabled(boolean enabled) {
        this.inputEnabled = enabled;
    }

    public boolean isInputEnabled() {
        return inputEnabled;
    }

    public void setMouseLocked(boolean locked) {
        this.mouseLocked = locked;
        if (windowHandle != 0) {
            GLFW.glfwSetInputMode(windowHandle, GLFW.GLFW_CURSOR,
                    locked ? GLFW.GLFW_CURSOR_DISABLED : GLFW.GLFW_CURSOR_NORMAL);
        }
    }

    public boolean isMouseLocked() {
        return mouseLocked;
    }

    @Override
    public void cleanup() {
        // Clear GLFW callbacks
        if (windowHandle != 0) {
            GLFW.glfwSetKeyCallback(windowHandle, null);
            GLFW.glfwSetMouseButtonCallback(windowHandle, null);
            GLFW.glfwSetCursorPosCallback(windowHandle, null);
            GLFW.glfwSetScrollCallback(windowHandle, null);
        }

        actionStates.clear();
        previousActionStates.clear();

        System.out.println("Input module cleaned up");
    }
}
