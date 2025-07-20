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
    private boolean initialized = false;

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
        try {
            // Get configuration with fallback to defaults
            settings = getOptionalService(InputSettings.class);
            if (settings == null) {
                System.out.println("InputSettings not found, creating default settings");
                settings = createDefaultInputSettings();
                serviceLocator.register(InputSettings.class, settings);
            }

            eventBus = getService(EventBus.class);

            // Get window handle from engine or window service (for now we'll skip this)
            // TODO: Get actual window handle when Window service is available
            // windowHandle = getService(WindowService.class).getWindowHandle();

            // Initialize input components
            actionMap = new ActionMap(settings.getKeyBindings());
            currentContext = new InputContext("default");
            keyboardHandler = new KeyboardHandler(this);
            mouseHandler = new MouseHandler(this);

            // Setup GLFW callbacks (only if window handle is available)
            if (windowHandle != 0) {
                setupCallbacks();
            } else {
                System.out.println("Window handle not available, input callbacks will be set up later");
            }

            // Register services
            serviceLocator.register(ActionMap.class, actionMap);
            serviceLocator.register(InputContext.class, currentContext);

            initialized = true;
            System.out.println("Input module initialized with settings: " + getInputInfo());

        } catch (Exception e) {
            System.err.println("Failed to initialize input module: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Input module initialization failed", e);
        }
    }

    private InputSettings createDefaultInputSettings() {
        InputSettings defaultSettings = new InputSettings();

        try {
            defaultSettings.validate();
            System.out.println("Created and validated default input settings");
        } catch (Exception e) {
            System.err.println("Warning: Default input settings validation failed: " + e.getMessage());
        }

        return defaultSettings;
    }

    private String getInputInfo() {
        return String.format("MouseSens=%.2f MovementSpeed=%.1f RawMouse=%s",
                settings.getMouseSensitivity(),
                settings.getMovementSpeed(),
                settings.isRawMouseInput() ? "ON" : "OFF");
    }

    public void setWindowHandle(long windowHandle) {
        this.windowHandle = windowHandle;
        if (initialized && windowHandle != 0) {
            setupCallbacks();
        }
    }

    private void setupCallbacks() {
        try {
            // Key callback
            GLFW.glfwSetKeyCallback(windowHandle, (window, key, scancode, action, mods) -> {
                if (!inputEnabled) return;
                if (keyboardHandler != null) {
                    keyboardHandler.handleKey(key, action, mods);
                }
            });

            // Mouse button callback
            GLFW.glfwSetMouseButtonCallback(windowHandle, (window, button, action, mods) -> {
                if (!inputEnabled) return;
                if (mouseHandler != null) {
                    mouseHandler.handleMouseButton(button, action, mods);
                }
            });

            // Cursor position callback
            GLFW.glfwSetCursorPosCallback(windowHandle, (window, xpos, ypos) -> {
                if (!inputEnabled) return;
                if (mouseHandler != null) {
                    mouseHandler.handleMouseMove(xpos, ypos);
                }
            });

            // Scroll callback
            GLFW.glfwSetScrollCallback(windowHandle, (window, xoffset, yoffset) -> {
                if (!inputEnabled) return;
                if (mouseHandler != null) {
                    mouseHandler.handleScroll(xoffset, yoffset);
                }
            });

            System.out.println("Input callbacks configured successfully");

        } catch (Exception e) {
            System.err.println("Warning: Failed to configure some input callbacks: " + e.getMessage());
        }
    }

    @Override
    public void update(float deltaTime) {
        if (!initialized || !inputEnabled) return;

        try {
            // Store previous action states
            previousActionStates.clear();
            previousActionStates.putAll(actionStates);

            // Update current action states
            updateActionStates();

            // Process input events
            processActionEvents();

            // Update input components
            if (keyboardHandler != null) {
                keyboardHandler.update(deltaTime);
            }
            if (mouseHandler != null) {
                mouseHandler.update(deltaTime);
            }

        } catch (Exception e) {
            System.err.println("Error updating input module: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateActionStates() {
        if (windowHandle == 0 || actionMap == null) return;

        try {
            for (Map.Entry<String, Integer> binding : actionMap.getKeyBindings().entrySet()) {
                String action = binding.getKey();
                int keyCode = binding.getValue();

                boolean pressed = GLFW.glfwGetKey(windowHandle, keyCode) == GLFW.GLFW_PRESS;
                actionStates.put(action, pressed);
            }
        } catch (Exception e) {
            System.err.println("Error updating action states: " + e.getMessage());
        }
    }

    private void processActionEvents() {
        if (eventBus == null) return;

        try {
            for (String action : actionStates.keySet()) {
                boolean current = actionStates.getOrDefault(action, false);
                boolean previous = previousActionStates.getOrDefault(action, false);

                InputAction inputAction = getInputActionFromString(action);
                if (inputAction == null) continue;

                if (current && !previous) {
                    // Action pressed
                    eventBus.post(new InputActionEvent(
                            inputAction,
                            InputActionEvent.Type.PRESSED,
                            0
                    ));
                } else if (!current && previous) {
                    // Action released
                    eventBus.post(new InputActionEvent(
                            inputAction,
                            InputActionEvent.Type.RELEASED,
                            0
                    ));
                } else if (current) {
                    // Action held
                    eventBus.post(new InputActionEvent(
                            inputAction,
                            InputActionEvent.Type.HELD,
                            0
                    ));
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing action events: " + e.getMessage());
        }
    }

    private InputAction getInputActionFromString(String actionString) {
        try {
            return InputAction.valueOf(actionString.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Action string doesn't match any InputAction enum
            return null;
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
            try {
                GLFW.glfwSetInputMode(windowHandle, GLFW.GLFW_CURSOR,
                        locked ? GLFW.GLFW_CURSOR_DISABLED : GLFW.GLFW_CURSOR_NORMAL);
            } catch (Exception e) {
                System.err.println("Error setting mouse lock state: " + e.getMessage());
            }
        }
    }

    public boolean isMouseLocked() {
        return mouseLocked;
    }

    @Override
    public void cleanup() {
        try {
            initialized = false;

            // Clear GLFW callbacks
            if (windowHandle != 0) {
                GLFW.glfwSetKeyCallback(windowHandle, null);
                GLFW.glfwSetMouseButtonCallback(windowHandle, null);
                GLFW.glfwSetCursorPosCallback(windowHandle, null);
                GLFW.glfwSetScrollCallback(windowHandle, null);
            }

            actionStates.clear();
            previousActionStates.clear();

            keyboardHandler = null;
            mouseHandler = null;
            actionMap = null;
            currentContext = null;

            System.out.println("Input module cleaned up");

        } catch (Exception e) {
            System.err.println("Error during input module cleanup: " + e.getMessage());
        }
    }

    // Getters
    public boolean isInitialized() {
        return initialized;
    }

    public InputSettings getSettings() {
        return settings;
    }

    public long getWindowHandle() {
        return windowHandle;
    }
}