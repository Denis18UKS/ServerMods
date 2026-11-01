package dev.ingamecreator.core.blueprint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BlueprintGraph {
    private final SmartNodeCatalog catalog;
    private final Map<UUID, BlueprintNode> nodes = new LinkedHashMap<>();
    private final List<BlueprintEdge> edges = new ArrayList<>();
    private final List<BlueprintVariable> variables = new ArrayList<>();
    private final List<BlueprintFunction> functions = new ArrayList<>();

    public BlueprintGraph(SmartNodeCatalog catalog) {
        this.catalog = catalog;
    }

    public BlueprintNode addNode(String typeId, double x, double y) {
        BlueprintNode node = new BlueprintNode(UUID.randomUUID(), typeId, x, y, Map.of());
        addNode(node);
        return node;
    }

    public void addNode(BlueprintNode node) {
        if (node == null) throw new IllegalArgumentException("node is required");
        catalog.find(node.typeId()).orElseThrow(() -> new IllegalArgumentException("unknown node type: " + node.typeId()));
        if (nodes.putIfAbsent(node.id(), node) != null) throw new IllegalArgumentException("duplicate node id: " + node.id());
    }

    public void removeNode(UUID id) {
        if (nodes.remove(id) == null) return;
        edges.removeIf(edge -> edge.fromNode().equals(id) || edge.toNode().equals(id));
    }

    public void moveNode(UUID id, double x, double y) {
        BlueprintNode node = requireNode(id);
        nodes.put(id, new BlueprintNode(node.id(), node.typeId(), x, y, node.properties()));
    }

    public void setNodeProperty(UUID id, String key, String value) {
        BlueprintNode node = requireNode(id);
        if (key == null || key.isBlank()) throw new IllegalArgumentException("property key is required");
        LinkedHashMap<String, String> properties = new LinkedHashMap<>(node.properties());
        properties.put(key.trim(), value == null ? "" : value);
        nodes.put(id, new BlueprintNode(node.id(), node.typeId(), node.x(), node.y(), properties));
    }

    public BlueprintEdge connect(UUID fromNodeId, String fromPortId, UUID toNodeId, String toPortId) {
        BlueprintNode fromNode = requireNode(fromNodeId);
        BlueprintNode toNode = requireNode(toNodeId);
        PortDefinition from = requirePort(fromNode, fromPortId);
        PortDefinition to = requirePort(toNode, toPortId);
        if (from.direction() != PortDirection.OUTPUT || to.direction() != PortDirection.INPUT) {
            throw new IllegalArgumentException("connections must go OUTPUT -> INPUT");
        }
        if (!from.type().compatibleWith(to.type())) {
            throw new IllegalArgumentException("incompatible ports: " + from.type() + " -> " + to.type());
        }
        if (!to.multiple()) {
            edges.removeIf(edge -> edge.toNode().equals(toNodeId) && edge.toPort().equals(toPortId));
        }
        BlueprintEdge edge = new BlueprintEdge(fromNodeId, fromPortId, toNodeId, toPortId);
        edges.add(edge);
        return edge;
    }

    public void addVariable(BlueprintVariable variable) { variables.add(variable); }
    public void addFunction(BlueprintFunction function) { functions.add(function); }

    public Map<UUID, BlueprintNode> nodes() { return Collections.unmodifiableMap(nodes); }
    public List<BlueprintEdge> edges() { return Collections.unmodifiableList(edges); }
    public List<BlueprintVariable> variables() { return Collections.unmodifiableList(variables); }
    public List<BlueprintFunction> functions() { return Collections.unmodifiableList(functions); }

    private BlueprintNode requireNode(UUID id) {
        BlueprintNode node = nodes.get(id);
        if (node == null) throw new IllegalArgumentException("missing node: " + id);
        return node;
    }

    private PortDefinition requirePort(BlueprintNode node, String portId) {
        NodeType type = catalog.find(node.typeId()).orElseThrow();
        return type.port(portId).orElseThrow(() -> new IllegalArgumentException("missing port: " + portId));
    }
}
