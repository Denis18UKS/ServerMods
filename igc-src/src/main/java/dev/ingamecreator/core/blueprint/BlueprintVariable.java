package dev.ingamecreator.core.blueprint;

public record BlueprintVariable(String name, PortType type, String defaultValue) {
    public BlueprintVariable {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("variable name is required");
        if (type == null) throw new IllegalArgumentException("variable type is required");
        defaultValue = defaultValue == null ? "" : defaultValue;
    }
}
