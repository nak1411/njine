package com.nak.engine.core;

import com.nak.engine.core.lifecycle.Cleanupable;
import com.nak.engine.core.lifecycle.Initializable;
import com.nak.engine.core.lifecycle.Updateable;
import com.nak.engine.events.EventBus;

import java.util.*;

public class ModuleManager implements Initializable, Updateable, Cleanupable {
    private final ServiceLocator serviceLocator;
    private final EventBus eventBus;
    private final List<Module> modules = new ArrayList<>();
    private final Map<Class<? extends Module>, Module> moduleMap = new HashMap<>();

    private boolean initialized = false;

    public ModuleManager(ServiceLocator serviceLocator, EventBus eventBus) {
        this.serviceLocator = serviceLocator;
        this.eventBus = eventBus;
    }

    public void addModule(Module module) {
        if (initialized) {
            throw new IllegalStateException("Cannot add modules after initialization");
        }

        modules.add(module);
        moduleMap.put(module.getClass(), module);

        // Inject dependencies
        module.injectDependencies(serviceLocator);
    }

    @Override
    public void initialize() {
        if (initialized) return;

        try {
            // Sort modules by priority
            modules.sort(Comparator.comparingInt(Module::getInitializationPriority));

            // Initialize modules in order
            for (Module module : modules) {
                try {
                    System.out.println("Initializing module: " + module.getName());
                    module.initialize();

                    // Register module in service locator
                    serviceLocator.register(module.getClass(), module);

                } catch (Exception e) {
                    System.err.println("Failed to initialize module: " + module.getName());
                    throw new RuntimeException("Module initialization failed", e);
                }
            }

            initialized = true;
            System.out.println("All modules initialized successfully");

        } catch (Exception e) {
            cleanup();
            throw e;
        }
    }

    @Override
    public void update(float deltaTime) {
        if (!initialized) return;

        for (Module module : modules) {
            if (module.isEnabled()) {
                try {
                    module.update(deltaTime);
                } catch (Exception e) {
                    System.err.println("Error updating module " + module.getName() + ": " + e.getMessage());
                    // Continue with other modules
                }
            }
        }
    }

    @Override
    public void cleanup() {
        // Cleanup in reverse order
        List<Module> reversedModules = new ArrayList<>(modules);
        Collections.reverse(reversedModules);

        for (Module module : reversedModules) {
            try {
                System.out.println("Cleaning up module: " + module.getName());
                module.cleanup();
            } catch (Exception e) {
                System.err.println("Error cleaning up module " + module.getName() + ": " + e.getMessage());
                // Continue cleanup
            }
        }

        modules.clear();
        moduleMap.clear();
        initialized = false;
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T getModule(Class<T> moduleClass) {
        return (T) moduleMap.get(moduleClass);
    }

    public boolean hasModule(Class<? extends Module> moduleClass) {
        return moduleMap.containsKey(moduleClass);
    }

    public List<Module> getModules() {
        return new ArrayList<>(modules);
    }

    public boolean isInitialized() {
        return initialized;
    }
}