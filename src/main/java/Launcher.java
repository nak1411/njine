import com.nak.engine.camera.CameraModule;
import com.nak.engine.config.ConfigManager;
import com.nak.engine.config.EngineConfig;
import com.nak.engine.core.Engine;
import com.nak.engine.input.InputModule;
import com.nak.engine.render.RenderModule;
import com.nak.engine.terrain.TerrainModule;

public class Launcher {

    public static void main(String[] args) {
        try {
            // Load configuration
            EngineConfig config = ConfigManager.loadDefault();

            // Create and configure engine
            Engine engine = new Engine()
                    .withModule(new CameraModule())
                    .withModule(new InputModule())
                    .withModule(new RenderModule())
                    .withModule(new TerrainModule())
                    .configure(config);

            // Initialize and run
            engine.initialize();
            engine.run();

        } catch (Exception e) {
            System.err.println("Application failed to start: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
