package com.nak.engine.core;

import com.nak.engine.core.lifecycle.Cleanupable;
import com.nak.engine.core.lifecycle.Initializable;
import com.nak.engine.core.lifecycle.Updateable;

public abstract class Module implements Initializable, Updateable, Cleanupable {
    private boolean enabled = true;
    protected ServiceLocator serviceLocator;

    public abstract String getName();

    public abstract int getInitializationPriority();

    public void injectDependencies(ServiceLocator serviceLocator) {
        this.serviceLocator = serviceLocator;
    }

    @Override
    public void initialize() {
        // Default implementation - override if needed
    }

    @Override
    public void update(float deltaTime) {
        // Default implementation - override if needed
    }

    @Override
    public void cleanup() {
        // Default implementation - override if needed
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    protected <T> T getService(Class<T> serviceClass) {
        return serviceLocator.get(serviceClass);
    }

    protected <T> T getOptionalService(Class<T> serviceClass) {
        return serviceLocator.getOptional(serviceClass);
    }

    protected boolean hasService(Class<?> serviceClass) {
        return serviceLocator.hasService(serviceClass);
    }
}