package com.nak.engine.input;

import java.util.HashMap;
import java.util.Map;

public class ActionMap {
    private final Map<String, Integer> keyBindings;

    public ActionMap() {
        this.keyBindings = new HashMap<>();
    }

    public ActionMap(Map<String, Integer> bindings) {
        this.keyBindings = new HashMap<>(bindings);
    }

    public void bindKey(String action, int keyCode) {
        keyBindings.put(action, keyCode);
    }

    public Integer getKeyForAction(String action) {
        return keyBindings.get(action);
    }

    public Map<String, Integer> getKeyBindings() {
        return new HashMap<>(keyBindings);
    }

    public void setKeyBindings(Map<String, Integer> bindings) {
        keyBindings.clear();
        keyBindings.putAll(bindings);
    }
}