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

        if (title == null || title.trim().isEmpty()) {
            throw new ValidationException("Title cannot be empty");
        }

        if (version == null || version.trim().isEmpty()) {
            throw new ValidationException("Version cannot be empty");
        }
    }

    // ==========  ALL GETTERS ==========

    // Display settings
    public int getWindowWidth() { return windowWidth; }
    public int getWindowHeight() { return windowHeight; }
    public boolean isFullscreen() { return fullscreen; }
    public boolean isVsync() { return vsync; }
    public int getMsaaSamples() { return msaaSamples; }

    // Performance settings - BOTH NAMING CONVENTIONS SUPPORTED
    public int getTargetFPS() { return targetFPS; }  // Original method
    public int getTargetFps() { return targetFPS; }  // ← THIS FIXES THE COMPILATION ERROR
    public int getTargetUPS() { return targetUPS; }
    public int getTargetUps() { return targetUPS; }  // Also add this for consistency
    public boolean isLimitFrameRate() { return limitFrameRate; }
    public int getMaxThreads() { return maxThreads; }

    // Debug settings
    public boolean isDebugMode() { return debugMode; }
    public boolean isShowPerformanceOverlay() { return showPerformanceOverlay; }
    public boolean isEnableHotReload() { return enableHotReload; }
    public String getLogLevel() { return logLevel; }

    // Engine settings
    public String getTitle() { return title; }
    public String getVersion() { return version; }
    public boolean isExitOnWindowClose() { return exitOnWindowClose; }

    // ==========  ALL SETTERS ==========

    // Display settings
    public void setWindowWidth(int windowWidth) { this.windowWidth = windowWidth; }
    public void setWindowHeight(int windowHeight) { this.windowHeight = windowHeight; }
    public void setFullscreen(boolean fullscreen) { this.fullscreen = fullscreen; }
    public void setVsync(boolean vsync) { this.vsync = vsync; }
    public void setMsaaSamples(int msaaSamples) { this.msaaSamples = msaaSamples; }

    // Performance settings - BOTH NAMING CONVENTIONS SUPPORTED
    public void setTargetFPS(int targetFPS) { this.targetFPS = targetFPS; }  // Original method
    public void setTargetFps(int targetFps) { this.targetFPS = targetFps; }  // New method
    public void setTargetUPS(int targetUPS) { this.targetUPS = targetUPS; }
    public void setTargetUps(int targetUps) { this.targetUPS = targetUps; }  // For consistency
    public void setLimitFrameRate(boolean limitFrameRate) { this.limitFrameRate = limitFrameRate; }
    public void setMaxThreads(int maxThreads) { this.maxThreads = maxThreads; }

    // Debug settings
    public void setDebugMode(boolean debugMode) { this.debugMode = debugMode; }
    public void setShowPerformanceOverlay(boolean showPerformanceOverlay) { this.showPerformanceOverlay = showPerformanceOverlay; }
    public void setEnableHotReload(boolean enableHotReload) { this.enableHotReload = enableHotReload; }
    public void setLogLevel(String logLevel) { this.logLevel = logLevel; }

    // Engine settings
    public void setTitle(String title) { this.title = title; }
    public void setVersion(String version) { this.version = version; }
    public void setExitOnWindowClose(boolean exitOnWindowClose) { this.exitOnWindowClose = exitOnWindowClose; }
}