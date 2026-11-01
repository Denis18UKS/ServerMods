package dev.ingamecreator.core.blueprint;

import java.util.List;

public record BlueprintFunction(String name, List<PortDefinition> parameters, PortType returnType) {
    public BlueprintFunction {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("function name is required");
        parameters = List.copyOf(parameters == null ? List.of() : parameters);
        returnType = returnType == null ? PortType.ANY : returnType;
    }
}
