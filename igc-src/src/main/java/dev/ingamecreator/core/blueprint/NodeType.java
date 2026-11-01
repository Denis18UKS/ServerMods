package dev.ingamecreator.core.blueprint;

import java.util.List;
import java.util.Optional;

public record NodeType(String id, String title, String category, List<PortDefinition> ports, String description) {
    public NodeType {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("node id is required");
        if (title == null || title.isBlank()) throw new IllegalArgumentException("node title is required");
        category = category == null ? "Other" : category;
        ports = List.copyOf(ports == null ? List.of() : ports);
        description = description == null ? "" : description;
    }

    public Optional<PortDefinition> port(String portId) {
        return ports.stream().filter(p -> p.id().equals(portId)).findFirst();
    }
}
