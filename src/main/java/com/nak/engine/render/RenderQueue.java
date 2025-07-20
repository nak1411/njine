package com.nak.engine.render;

import java.util.ArrayList;
import java.util.List;

public class RenderQueue {
    private final List<RenderCommand> commands = new ArrayList<>();

    public void addCommand(RenderCommand command) {
        commands.add(command);
    }

    public void execute() {
        for (RenderCommand command : commands) {
            command.execute();
        }
    }

    public void clear() {
        commands.clear();
    }

    public int size() {
        return commands.size();
    }

    @FunctionalInterface
    public interface RenderCommand {
        void execute();
    }
}
