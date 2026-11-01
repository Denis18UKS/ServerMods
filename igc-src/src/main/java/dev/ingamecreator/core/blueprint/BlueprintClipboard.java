package dev.ingamecreator.core.blueprint;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

public final class BlueprintClipboard {
    private BlueprintClipboard() {}

    public record Selection(List<BlueprintNode> nodes, List<BlueprintEdge> edges) {
        public Selection {
            nodes = List.copyOf(nodes == null ? List.of() : nodes);
            edges = List.copyOf(edges == null ? List.of() : edges);
        }
    }

    public static Selection capture(BlueprintGraph graph, Set<UUID> selected) {
        if (graph == null) throw new IllegalArgumentException("graph is required");
        Set<UUID> ids = new LinkedHashSet<>(selected == null ? Set.of() : selected);
        List<BlueprintNode> nodes = graph.nodes().values().stream().filter(n -> ids.contains(n.id())).toList();
        List<BlueprintEdge> edges = graph.edges().stream()
                .filter(e -> ids.contains(e.fromNode()) && ids.contains(e.toNode()))
                .toList();
        return new Selection(nodes, edges);
    }

    public static Set<UUID> paste(BlueprintGraph graph, Selection selection, double offsetX, double offsetY) {
        if (graph == null || selection == null) throw new IllegalArgumentException("graph/selection are required");
        Map<UUID, UUID> ids = new LinkedHashMap<>();
        LinkedHashSet<UUID> created = new LinkedHashSet<>();
        for (BlueprintNode source : selection.nodes()) {
            UUID fresh = UUID.randomUUID();
            ids.put(source.id(), fresh);
            created.add(fresh);
            graph.addNode(new BlueprintNode(fresh, source.typeId(), source.x() + offsetX, source.y() + offsetY, source.properties()));
        }
        for (BlueprintEdge edge : selection.edges()) {
            UUID from = ids.get(edge.fromNode());
            UUID to = ids.get(edge.toNode());
            if (from != null && to != null) graph.connect(from, edge.fromPort(), to, edge.toPort());
        }
        return Set.copyOf(created);
    }

    public static String exportSelection(Selection selection) {
        Properties p = new Properties();
        p.setProperty("format", "igc-blueprint-selection-1");
        p.setProperty("node.count", Integer.toString(selection.nodes().size()));
        for (int i = 0; i < selection.nodes().size(); i++) {
            BlueprintNode n = selection.nodes().get(i);
            String pre = "node." + i + ".";
            p.setProperty(pre + "id", n.id().toString());
            p.setProperty(pre + "type", n.typeId());
            p.setProperty(pre + "x", Double.toString(n.x()));
            p.setProperty(pre + "y", Double.toString(n.y()));
            p.setProperty(pre + "prop.count", Integer.toString(n.properties().size()));
            int pi = 0;
            for (var entry : n.properties().entrySet()) {
                p.setProperty(pre + "prop." + pi + ".key", entry.getKey());
                p.setProperty(pre + "prop." + pi + ".value", entry.getValue());
                pi++;
            }
        }
        p.setProperty("edge.count", Integer.toString(selection.edges().size()));
        for (int i = 0; i < selection.edges().size(); i++) {
            BlueprintEdge e = selection.edges().get(i);
            String pre = "edge." + i + ".";
            p.setProperty(pre + "fromNode", e.fromNode().toString());
            p.setProperty(pre + "fromPort", e.fromPort());
            p.setProperty(pre + "toNode", e.toNode().toString());
            p.setProperty(pre + "toPort", e.toPort());
        }
        try {
            StringWriter out = new StringWriter();
            p.store(out, "InGame Creator Blueprint selection");
            return out.toString();
        } catch (IOException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    public static Selection importSelection(String encoded) {
        if (encoded == null || encoded.isBlank()) throw new IllegalArgumentException("blueprint data is empty");
        Properties p = new Properties();
        try { p.load(new StringReader(encoded)); }
        catch (IOException e) { throw new IllegalArgumentException("invalid blueprint data", e); }
        if (!"igc-blueprint-selection-1".equals(p.getProperty("format"))) throw new IllegalArgumentException("unsupported blueprint format");
        List<BlueprintNode> nodes = new ArrayList<>();
        int nodeCount = integer(p.getProperty("node.count"));
        for (int i = 0; i < nodeCount; i++) {
            String pre = "node." + i + ".";
            LinkedHashMap<String, String> props = new LinkedHashMap<>();
            int propCount = integer(p.getProperty(pre + "prop.count"));
            for (int j = 0; j < propCount; j++) {
                String pp = pre + "prop." + j + ".";
                props.put(p.getProperty(pp + "key"), p.getProperty(pp + "value", ""));
            }
            nodes.add(new BlueprintNode(UUID.fromString(p.getProperty(pre + "id")), p.getProperty(pre + "type"),
                    decimal(p.getProperty(pre + "x")), decimal(p.getProperty(pre + "y")), props));
        }
        List<BlueprintEdge> edges = new ArrayList<>();
        int edgeCount = integer(p.getProperty("edge.count"));
        for (int i = 0; i < edgeCount; i++) {
            String pre = "edge." + i + ".";
            edges.add(new BlueprintEdge(UUID.fromString(p.getProperty(pre + "fromNode")), p.getProperty(pre + "fromPort"),
                    UUID.fromString(p.getProperty(pre + "toNode")), p.getProperty(pre + "toPort")));
        }
        return new Selection(nodes, edges);
    }

    private static int integer(String value) {
        try { return Integer.parseInt(value); } catch (Exception e) { throw new IllegalArgumentException("invalid integer: " + value); }
    }

    private static double decimal(String value) {
        try { return Double.parseDouble(value); } catch (Exception e) { throw new IllegalArgumentException("invalid number: " + value); }
    }
}
