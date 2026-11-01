package dev.ingamecreator.core.blueprint;

import java.util.UUID;

public record BlueprintEdge(UUID fromNode, String fromPort, UUID toNode, String toPort) {}
