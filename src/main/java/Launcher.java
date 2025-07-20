import com.nak.engine.camera.CameraModule;
import com.nak.engine.config.*;
import com.nak.engine.core.Engine;
import com.nak.engine.input.InputModule;
import com.nak.engine.render.RenderModule;
import com.nak.engine.render.WindowModule;  // ← ADD THIS IMPORT
import com.nak.engine.shader.ShaderModule;
import com.nak.engine.terrain.TerrainModule;

public class Launcher {

    public static void main(String[] args) {
        try {
            // Load all configuration
            EngineConfig engineConfig = ConfigManager.loadDefault();
            RenderSettings renderSettings = ConfigManager.load("render.properties", RenderSettings.class);
            TerrainSettings terrainSettings = ConfigManager.load("terrain.properties", TerrainSettings.class);
            InputSettings inputSettings = ConfigManager.load("input.properties", InputSettings.class);

            // Validate configurations
            try {
                engineConfig.validate();
                renderSettings.validate();
                terrainSettings.validate();
                inputSettings.validate();
                System.out.println("All configurations validated successfully");
            } catch (ValidationException e) {
                System.err.println("Configuration validation failed: " + e.getMessage());
                System.err.println("Using default values for invalid settings");
            }

            // Create and configure engine - ADD WINDOWMODULE HERE!
            Engine engine = new Engine()
                    .withModule(new ConfigurationModule(engineConfig, renderSettings, terrainSettings, inputSettings))
                    .withModule(new WindowModule())  // ← ADD THIS LINE - MOST IMPORTANT FIX!
                    .withModule(new ShaderModule())
                    .withModule(new CameraModule())
                    .withModule(new InputModule())
                    .withModule(new RenderModule())
                    .withModule(new TerrainModule())
                    .configure(engineConfig);

            // Initialize and run
            engine.initialize();
            engine.run();

        } catch (Exception e) {
            System.err.println("Application failed to start: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Configuration module to register all configuration services
     */
    private static class ConfigurationModule extends com.nak.engine.core.Module {
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

            System.out.println("Configuration module initialized - registered all settings");
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
