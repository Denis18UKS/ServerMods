package dev.ingamecreator.core.runtime;

import java.util.UUID;

public record RuntimePlayer(UUID id, String name, String displayName) {
    public RuntimePlayer {
        if (id == null) throw new IllegalArgumentException("player id is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("player name is required");
        if (displayName == null || displayName.isBlank()) displayName = name;
    }

    public RuntimePlayer(UUID id, String name) {
        this(id, name, name);
    }
}
