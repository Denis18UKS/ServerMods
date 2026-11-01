package dev.ingamecreator.core.blueprint;

import java.util.Map;
import java.util.UUID;

public record BlueprintNode(UUID id, String typeId, double x, double y, Map<String, String> properties) {
    public BlueprintNode {
        if (id == null) throw new IllegalArgumentException("node id is required");
        if (typeId == null || typeId.isBlank()) throw new IllegalArgumentException("type id is required");
        properties = Map.copyOf(properties == null ? Map.of() : properties);
    }
}
