import com.nak.engine.camera.CameraModule;
import com.nak.engine.config.*;
import com.nak.engine.core.Engine;
import com.nak.engine.core.Module;
import com.nak.engine.core.ServiceLocator;
import com.nak.engine.input.InputModule;
import com.nak.engine.render.RenderModule;
import com.nak.engine.render.WindowModule;
import com.nak.engine.shader.ShaderModule;
import com.nak.engine.terrain.TerrainModule;

public class Launcher {

    public static void main(String[] args) {
        System.out.println("Starting NAK Engine...");

        try {
            // Load configurations with error handling
            EngineConfig engineConfig = loadEngineConfig();
            RenderSettings renderSettings = loadRenderSettings();
            TerrainSettings terrainSettings = loadTerrainSettings();
            InputSettings inputSettings = loadInputSettings();

            // Create and configure engine with proper module order
            Engine engine = new Engine()
                    .withModule(new ConfigurationModule(engineConfig, renderSettings, terrainSettings, inputSettings))
                    .withModule(new WindowModule())      // MUST be first for OpenGL context
                    .withModule(new ShaderModule())      // After window for OpenGL calls
                    .withModule(new RenderModule())      // After shaders
                    .withModule(new CameraModule())      // After render for context
                    .withModule(new InputModule())       // After window for input handling
                    .withModule(new TerrainModule())     // Last - depends on all others
                    .configure(engineConfig);

            // Initialize and run
            System.out.println("Initializing engine...");
            engine.initialize();

            System.out.println("Starting main loop...");
            engine.run();

        } catch (Exception e) {
            System.err.println("Application failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static EngineConfig loadEngineConfig() {
        try {
            EngineConfig config = ConfigManager.loadDefault();
            config.validate();
            System.out.println("✓ Engine config loaded");
            return config;
        } catch (Exception e) {
            System.err.println("Failed to load engine config, using defaults: " + e.getMessage());
            return createDefaultEngineConfig();
        }
    }

    private static RenderSettings loadRenderSettings() {
        try {
            RenderSettings settings = ConfigManager.load("render.properties", RenderSettings.class);
            settings.validate();
            System.out.println("✓ Render settings loaded");
            return settings;
        } catch (Exception e) {
            System.err.println("Failed to load render settings, using defaults: " + e.getMessage());
            return createDefaultRenderSettings();
        }
    }

    private static TerrainSettings loadTerrainSettings() {
        try {
            TerrainSettings settings = ConfigManager.load("terrain.properties", TerrainSettings.class);
            settings.validate();
            System.out.println("✓ Terrain settings loaded");
            return settings;
        } catch (Exception e) {
            System.err.println("Failed to load terrain settings, using defaults: " + e.getMessage());
            return createDefaultTerrainSettings();
        }
    }

    private static InputSettings loadInputSettings() {
        try {
            InputSettings settings = ConfigManager.load("input.properties", InputSettings.class);
            settings.validate();
            System.out.println("✓ Input settings loaded");
            return settings;
        } catch (Exception e) {
            System.err.println("Failed to load input settings, using defaults: " + e.getMessage());
            return createDefaultInputSettings();
        }
    }

    // Default configuration creators
    private static EngineConfig createDefaultEngineConfig() {
        EngineConfig config = new EngineConfig();
        // Set safe defaults
        config.setWindowWidth(1024);
        config.setWindowHeight(768);
        //config.setWindowTitle("NAK Engine - Terrain Demo");
        config.setFullscreen(false);
        config.setVsync(true);
        config.setMsaaSamples(4);
        config.setDebugMode(true);
        return config;
    }

    private static RenderSettings createDefaultRenderSettings() {
        RenderSettings settings = new RenderSettings();
        // Set safe render defaults
        return settings;
    }

    private static TerrainSettings createDefaultTerrainSettings() {
        TerrainSettings settings = new TerrainSettings();
        // Set safe terrain defaults
        return settings;
    }

    private static InputSettings createDefaultInputSettings() {
        InputSettings settings = new InputSettings();
        settings.setMouseSensitivity(1.0f);
        settings.setMovementSpeed(10.0f);
        settings.setRawMouseInput(false);
        return settings;
    }

    /**
     * Configuration module to register all configuration services
     */
    private static class ConfigurationModule extends Module {
        private final EngineConfig engineConfig;
        private final RenderSettings renderSettings;
        private final TerrainSettings terrainSettings;
        private final InputSettings inputSettings;

        public ConfigurationModule(EngineConfig engineConfig, RenderSettings renderSettings,
                                   TerrainSettings terrainSettings, InputSettings inputSettings) {
            this.engineConfig = engineConfig;
            this.renderSettings = renderSettings;
            this.terrainSettings = terrainSettings;
            this.inputSettings = inputSettings;
        }

        @Override
        public String getName() {
            return "Configuration";
        }

        @Override
        public int getInitializationPriority() {
            return 10; // Initialize very early
        }

        @Override
        public void initialize() {
            // Register all configuration services
            serviceLocator.register(EngineConfig.class, engineConfig);
            serviceLocator.register(RenderSettings.class, renderSettings);
            serviceLocator.register(TerrainSettings.class, terrainSettings);
            serviceLocator.register(InputSettings.class, inputSettings);

            System.out.println("✓ Configuration module initialized - registered all settings");
        }

        @Override
        public void update(float deltaTime) {
            // Configuration module doesn't need updates
        }

        @Override
        public void cleanup() {
            // No cleanup needed for configuration
        }
    }
}
