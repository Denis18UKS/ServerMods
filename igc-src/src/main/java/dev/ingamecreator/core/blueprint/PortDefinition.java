package dev.ingamecreator.core.blueprint;

public record PortDefinition(String id, String name, PortType type, PortDirection direction, boolean multiple) {
    public PortDefinition {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("port id is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("port name is required");
        if (type == null) throw new IllegalArgumentException("port type is required");
        if (direction == null) throw new IllegalArgumentException("port direction is required");
    }
}
