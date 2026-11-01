package dev.ingamecreator.core.blueprint;

import dev.ingamecreator.core.project.CreatorProject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

public final class BlueprintStore {
    private final Path root;
    private final SmartNodeCatalog catalog;

    public BlueprintStore(Path root, SmartNodeCatalog catalog) {
        this.root = root;
        this.catalog = catalog;
    }

    public void save(String projectId, String blueprintId, BlueprintGraph graph) throws IOException {
        String pid = CreatorProject.normalizeId(projectId);
        String bid = normalizeBlueprintId(blueprintId);
        Path file = file(pid, bid);
        Files.createDirectories(file.getParent());
        Properties p = new Properties();

        var nodes = graph.nodes().values().stream().toList();
        p.setProperty("node.count", Integer.toString(nodes.size()));
        for (int i = 0; i < nodes.size(); i++) {
            BlueprintNode n = nodes.get(i);
            String prefix = "node." + i + ".";
            p.setProperty(prefix + "id", n.id().toString());
            p.setProperty(prefix + "type", n.typeId());
            p.setProperty(prefix + "x", Double.toString(n.x()));
            p.setProperty(prefix + "y", Double.toString(n.y()));
            p.setProperty(prefix + "prop.count", Integer.toString(n.properties().size()));
            int pi = 0;
            for (Map.Entry<String, String> entry : n.properties().entrySet()) {
                p.setProperty(prefix + "prop." + pi + ".key", entry.getKey());
                p.setProperty(prefix + "prop." + pi + ".value", entry.getValue());
                pi++;
            }
        }

        p.setProperty("edge.count", Integer.toString(graph.edges().size()));
        for (int i = 0; i < graph.edges().size(); i++) {
            BlueprintEdge e = graph.edges().get(i);
            String prefix = "edge." + i + ".";
            p.setProperty(prefix + "fromNode", e.fromNode().toString());
            p.setProperty(prefix + "fromPort", e.fromPort());
            p.setProperty(prefix + "toNode", e.toNode().toString());
            p.setProperty(prefix + "toPort", e.toPort());
        }

        p.setProperty("var.count", Integer.toString(graph.variables().size()));
        for (int i = 0; i < graph.variables().size(); i++) {
            BlueprintVariable v = graph.variables().get(i);
            String prefix = "var." + i + ".";
            p.setProperty(prefix + "name", v.name());
            p.setProperty(prefix + "type", v.type().name());
            p.setProperty(prefix + "default", v.defaultValue());
        }

        p.setProperty("function.count", Integer.toString(graph.functions().size()));
        for (int i = 0; i < graph.functions().size(); i++) {
            BlueprintFunction f = graph.functions().get(i);
            String prefix = "function." + i + ".";
            p.setProperty(prefix + "name", f.name());
            p.setProperty(prefix + "return", f.returnType().name());
            p.setProperty(prefix + "param.count", Integer.toString(f.parameters().size()));
            for (int j = 0; j < f.parameters().size(); j++) {
                PortDefinition pd = f.parameters().get(j);
                String pp = prefix + "param." + j + ".";
                p.setProperty(pp + "id", pd.id());
                p.setProperty(pp + "name", pd.name());
                p.setProperty(pp + "type", pd.type().name());
                p.setProperty(pp + "direction", pd.direction().name());
                p.setProperty(pp + "multiple", Boolean.toString(pd.multiple()));
            }
        }

        try (OutputStream out = Files.newOutputStream(file)) {
            p.store(out, "InGame Creator blueprint");
        }
    }

    public BlueprintGraph load(String projectId, String blueprintId) throws IOException {
        String pid = CreatorProject.normalizeId(projectId);
        String bid = normalizeBlueprintId(blueprintId);
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(file(pid, bid))) {
            p.load(in);
        }
        BlueprintGraph graph = new BlueprintGraph(catalog);

        int nodeCount = parseInt(p.getProperty("node.count"), 0);
        for (int i = 0; i < nodeCount; i++) {
            String prefix = "node." + i + ".";
            Map<String, String> props = new LinkedHashMap<>();
            int propCount = parseInt(p.getProperty(prefix + "prop.count"), 0);
            for (int j = 0; j < propCount; j++) {
                String pp = prefix + "prop." + j + ".";
                String key = p.getProperty(pp + "key");
                if (key != null) props.put(key, p.getProperty(pp + "value", ""));
            }
            graph.addNode(new BlueprintNode(
                    UUID.fromString(p.getProperty(prefix + "id")),
                    p.getProperty(prefix + "type"),
                    parseDouble(p.getProperty(prefix + "x"), 0),
                    parseDouble(p.getProperty(prefix + "y"), 0),
                    props));
        }

        int edgeCount = parseInt(p.getProperty("edge.count"), 0);
        for (int i = 0; i < edgeCount; i++) {
            String prefix = "edge." + i + ".";
            graph.connect(
                    UUID.fromString(p.getProperty(prefix + "fromNode")),
                    p.getProperty(prefix + "fromPort"),
                    UUID.fromString(p.getProperty(prefix + "toNode")),
                    p.getProperty(prefix + "toPort"));
        }

        int varCount = parseInt(p.getProperty("var.count"), 0);
        for (int i = 0; i < varCount; i++) {
            String prefix = "var." + i + ".";
            graph.addVariable(new BlueprintVariable(
                    p.getProperty(prefix + "name"),
                    PortType.valueOf(p.getProperty(prefix + "type", "ANY")),
                    p.getProperty(prefix + "default", "")));
        }

        int functionCount = parseInt(p.getProperty("function.count"), 0);
        for (int i = 0; i < functionCount; i++) {
            String prefix = "function." + i + ".";
            int paramCount = parseInt(p.getProperty(prefix + "param.count"), 0);
            java.util.ArrayList<PortDefinition> params = new java.util.ArrayList<>();
            for (int j = 0; j < paramCount; j++) {
                String pp = prefix + "param." + j + ".";
                params.add(new PortDefinition(
                        p.getProperty(pp + "id"),
                        p.getProperty(pp + "name"),
                        PortType.valueOf(p.getProperty(pp + "type", "ANY")),
                        PortDirection.valueOf(p.getProperty(pp + "direction", "INPUT")),
                        Boolean.parseBoolean(p.getProperty(pp + "multiple", "false"))));
            }
            graph.addFunction(new BlueprintFunction(
                    p.getProperty(prefix + "name"),
                    params,
                    PortType.valueOf(p.getProperty(prefix + "return", "ANY"))));
        }
        return graph;
    }

    public java.util.Set<String> listBlueprintIds(String projectId) throws IOException {
        String pid = CreatorProject.normalizeId(projectId);
        Path dir = root.resolve(pid).resolve("blueprints");
        if (!Files.isDirectory(dir)) return java.util.Set.of();
        try (var stream = Files.list(dir)) {
            return stream.filter(path -> path.getFileName().toString().endsWith(".igcbp.properties"))
                    .map(path -> path.getFileName().toString().replaceFirst("\\.igcbp\\.properties$", ""))
                    .sorted()
                    .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        }
    }

    private Path file(String projectId, String blueprintId) {
        return root.resolve(projectId).resolve("blueprints").resolve(blueprintId + ".igcbp.properties");
    }

    private static String normalizeBlueprintId(String value) {
        if (value == null) throw new IllegalArgumentException("blueprint id is required");
        String v = value.trim().toLowerCase(java.util.Locale.ROOT);
        if (!v.matches("[a-z][a-z0-9_-]{1,63}")) throw new IllegalArgumentException("invalid blueprint id: " + value);
        return v;
    }

    private static int parseInt(String value, int fallback) {
        try { return Integer.parseInt(value); } catch (Exception ignored) { return fallback; }
    }

    private static double parseDouble(String value, double fallback) {
        try { return Double.parseDouble(value); } catch (Exception ignored) { return fallback; }
    }
}
