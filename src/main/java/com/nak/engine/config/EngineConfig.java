package com.nak.engine.config;

public class EngineConfig implements Validatable {
    // Display settings
    private int windowWidth = 1920;
    private int windowHeight = 1080;
    private boolean fullscreen = false;
    private boolean vsync = true;
    private int msaaSamples = 4;

    // Performance settings
    private int targetFPS = 60;
    private int targetUPS = 60;
    private boolean limitFrameRate = true;
    private int maxThreads = Runtime.getRuntime().availableProcessors();

    // Debug settings
    private boolean debugMode = false;
    private boolean showPerformanceOverlay = false;
    private boolean enableHotReload = false;
    private String logLevel = "INFO";

    // Engine settings
    private String title = "Terrain Engine";
    private String version = "1.0.0";
    private boolean exitOnWindowClose = true;

    @Override
    public void validate() throws ValidationException {
        if (windowWidth <= 0 || windowHeight <= 0) {
            throw new ValidationException("Window dimensions must be positive");
        }

        if (targetFPS <= 0 || targetUPS <= 0) {
            throw new ValidationException("Target FPS and UPS must be positive");
        }

        if (maxThreads <= 0) {
            throw new ValidationException("Max threads must be positive");
        }

        if (msaaSamples < 0 || msaaSamples > 16) {
            throw new ValidationException("MSAA samples must be between 0 and 16");
        }
    }

    // Getters and setters
    public int getWindowWidth() { return windowWidth; }
    public void setWindowWidth(int windowWidth) { this.windowWidth = windowWidth; }

    public int getWindowHeight() { return windowHeight; }
    public void setWindowHeight(int windowHeight) { this.windowHeight = windowHeight; }

    public boolean isFullscreen() { return fullscreen; }
    public void setFullscreen(boolean fullscreen) { this.fullscreen = fullscreen; }

    public boolean isVsync() { return vsync; }
    public void setVsync(boolean vsync) { this.vsync = vsync; }

    public int getMsaaSamples() { return msaaSamples; }
    public void setMsaaSamples(int msaaSamples) { this.msaaSamples = msaaSamples; }

    public int getTargetFPS() { return targetFPS; }
    public void setTargetFPS(int targetFPS) { this.targetFPS = targetFPS; }

    public int getTargetUPS() { return targetUPS; }
    public void setTargetUPS(int targetUPS) { this.targetUPS = targetUPS; }

    public boolean isLimitFrameRate() { return limitFrameRate; }
    public void setLimitFrameRate(boolean limitFrameRate) { this.limitFrameRate = limitFrameRate; }

    public int getMaxThreads() { return maxThreads; }
    public void setMaxThreads(int maxThreads) { this.maxThreads = maxThreads; }

    public boolean isDebugMode() { return debugMode; }
    public void setDebugMode(boolean debugMode) { this.debugMode = debugMode; }

    public boolean isShowPerformanceOverlay() { return showPerformanceOverlay; }
    public void setShowPerformanceOverlay(boolean showPerformanceOverlay) { this.showPerformanceOverlay = showPerformanceOverlay; }

    public boolean isEnableHotReload() { return enableHotReload; }
    public void setEnableHotReload(boolean enableHotReload) { this.enableHotReload = enableHotReload; }

    public String getLogLevel() { return logLevel; }
    public void setLogLevel(String logLevel) { this.logLevel = logLevel; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public boolean isExitOnWindowClose() { return exitOnWindowClose; }
    public void setExitOnWindowClose(boolean exitOnWindowClose) { this.exitOnWindowClose = exitOnWindowClose; }
}