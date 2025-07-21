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

            System.out.println("🔧 Module initialization order:");
            for (Module module : modules) {
                System.out.println("  " + module.getInitializationPriority() + ": " + module.getName());
            }

            // FIX: Register ModuleManager itself in ServiceLocator for module cross-references
            serviceLocator.register("ModuleManager", this);
            serviceLocator.register(ModuleManager.class, this);

            // Initialize modules in order
            for (Module module : modules) {
                try {
                    System.out.println("🔧 Initializing module: " + module.getName());

                    // FIX: Pre-register module before initialization
                    // Register with the exact class type to avoid generics issues
                    Class<?> moduleClass = module.getClass();
                    registerModuleInServiceLocator(moduleClass, module);
                    serviceLocator.register(module.getClass().getName(), module);

                    System.out.println("  📝 Pre-registered " + module.getName() + " in ServiceLocator");

                    // Now initialize the module
                    module.initialize();

                    // FIX: Verify registration was successful
                    if (!serviceLocator.hasService(moduleClass)) {
                        System.err.println("⚠️ Warning: " + module.getName() + " not found in ServiceLocator after registration");
                        // Re-register if somehow lost
                        registerModuleInServiceLocator(moduleClass, module);
                        serviceLocator.register(module.getClass().getName(), module);
                    }

                    // CRITICAL: Process events after each module initialization
                    // This ensures events fired during init are handled immediately
                    if (eventBus != null) {
                        eventBus.processEvents();
                    }

                    System.out.println("✅ " + module.getName() + " initialized and registered successfully");

                } catch (Exception e) {
                    System.err.println("❌ Failed to initialize module: " + module.getName());
                    System.err.println("    Error: " + e.getMessage());
                    e.printStackTrace();
                    throw new RuntimeException("Module initialization failed", e);
                }
            }

            initialized = true;
            System.out.println("✅ All modules initialized successfully");

            // FIX: Final verification pass - ensure all modules are properly registered
            System.out.println("🔍 Final registration verification:");
            for (Module module : modules) {
                boolean isRegistered = serviceLocator.hasService(module.getClass());
                System.out.println("  " + module.getName() + ": " + (isRegistered ? "✓ REGISTERED" : "❌ MISSING"));

                if (!isRegistered) {
                    // Emergency re-registration
                    registerModuleInServiceLocator(module.getClass(), module);
                    System.out.println("    🚨 Emergency re-registration completed");
                }
            }

            // Final event processing pass
            if (eventBus != null) {
                System.out.println("🔧 Final event processing pass...");
                eventBus.processEvents();
            }

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

    public void refreshAllRegistrations() {
        System.out.println("🔄 Refreshing all module registrations...");

        for (Module module : modules) {
            try {
                @SuppressWarnings("unchecked")
                Class<Module> moduleClass = (Class<Module>) module.getClass();

                // Re-register in ServiceLocator
                serviceLocator.register(moduleClass, module);
                serviceLocator.register(module.getClass().getName(), module);

                // Update moduleMap
                moduleMap.put(moduleClass, module);

                System.out.println("  ✓ Refreshed: " + module.getName());

            } catch (Exception e) {
                System.err.println("  ❌ Failed to refresh: " + module.getName() + " - " + e.getMessage());
            }
        }

        System.out.println("🔄 Registration refresh completed");
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
        T module = (T) moduleMap.get(moduleClass);

        if (module == null) {
            System.err.println("❌ Module not found in moduleMap: " + moduleClass.getSimpleName());
            System.err.println("🔍 Available modules in moduleMap:");
            for (Class<? extends Module> key : moduleMap.keySet()) {
                System.err.println("  - " + key.getSimpleName());
            }

            // Try to find by scanning all modules
            for (Module m : modules) {
                if (moduleClass.isInstance(m)) {
                    System.out.println("🔧 Found " + moduleClass.getSimpleName() + " via scan, updating moduleMap");
                    moduleMap.put(moduleClass, m);
                    return moduleClass.cast(m);
                }
            }
        }

        return module;
    }

    @SuppressWarnings("unchecked")
    private void registerModuleInServiceLocator(Class<?> moduleClass, Module module) {
        try {
            // Cast to the raw type to satisfy ServiceLocator's generic requirements
            serviceLocator.register((Class<Object>) moduleClass, module);
        } catch (ClassCastException e) {
            System.err.println("⚠️ Type casting issue with " + moduleClass.getSimpleName() + ", trying alternative registration");
            // Alternative: register by name only if type registration fails
            serviceLocator.register(moduleClass.getName(), module);
        }
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