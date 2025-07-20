package com.nak.engine.input;

import java.util.HashMap;
import java.util.Map;

public class InputContext {
    private final String name;
    private final Map<String, ActionMap> actionMaps;
    private ActionMap currentActionMap;

    public InputContext(String name) {
        this.name = name;
        this.actionMaps = new HashMap<>();
        this.currentActionMap = new ActionMap();
    }

    public void addActionMap(String name, ActionMap actionMap) {
        actionMaps.put(name, actionMap);
    }

    public void setActiveActionMap(String name) {
        ActionMap map = actionMaps.get(name);
        if (map != null) {
            currentActionMap = map;
        }
    }

    public ActionMap getCurrentActionMap() {
        return currentActionMap;
    }

    public String getName() {
        return name;
    }
}