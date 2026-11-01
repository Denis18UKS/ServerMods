package dev.ingamecreator.core.project;

import dev.ingamecreator.core.command.CommandArgument;
import dev.ingamecreator.core.command.CommandDefinition;
import dev.ingamecreator.core.content.RuntimeItemDefinition;
import dev.ingamecreator.core.content.RuntimeBlockDefinition;
import dev.ingamecreator.core.content.TextureAsset;
import dev.ingamecreator.core.content.CuboidModelAsset;
import dev.ingamecreator.core.content.TriggerBinding;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public final class ProjectStore {
    private final Path root;

    public ProjectStore(Path root) { this.root = root; }

    public void save(CreatorProject project) throws IOException {
        Files.createDirectories(root);
        Properties p = new Properties();
        p.setProperty("project.id", project.id());
        p.setProperty("project.name", project.name());
        p.setProperty("project.author", project.author());
        p.setProperty("project.version", project.version());
        p.setProperty("project.description", project.description());
        List<CommandDefinition> commands = new ArrayList<>(project.commands().values());
        p.setProperty("command.count", Integer.toString(commands.size()));
        for (int i = 0; i < commands.size(); i++) writeCommand(p, "command." + i + ".", commands.get(i));
        List<RuntimeItemDefinition> items = new ArrayList<>(project.runtimeItems().values());
        p.setProperty("item.count", Integer.toString(items.size()));
        for (int i = 0; i < items.size(); i++) writeItem(p, "item." + i + ".", items.get(i));
        List<RuntimeBlockDefinition> blocks = new ArrayList<>(project.runtimeBlocks().values());
        p.setProperty("block.count", Integer.toString(blocks.size()));
        for (int i = 0; i < blocks.size(); i++) writeBlock(p, "block." + i + ".", blocks.get(i));
        List<TextureAsset> textures = new ArrayList<>(project.textures().values());
        p.setProperty("texture.count", Integer.toString(textures.size()));
        for (int i = 0; i < textures.size(); i++) writeTexture(p, "texture." + i + ".", textures.get(i));
        List<CuboidModelAsset> models = new ArrayList<>(project.models().values());
        p.setProperty("model.count", Integer.toString(models.size()));
        for (int i = 0; i < models.size(); i++) writeModel(p, "model." + i + ".", models.get(i));
        p.setProperty("trigger.count", Integer.toString(project.triggers().size()));
        for (int i = 0; i < project.triggers().size(); i++) writeTrigger(p, "trigger." + i + ".", project.triggers().get(i));
        try (OutputStream out = Files.newOutputStream(file(project.id()))) {
            p.store(out, "InGame Creator project");
        }
    }

    public CreatorProject load(String id) throws IOException {
        String normalized = CreatorProject.normalizeId(id);
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(file(normalized))) { p.load(in); }
        try {
            CreatorProject project = CreatorProject.create(
                    p.getProperty("project.id", normalized),
                    p.getProperty("project.name", normalized));
            project.setAuthor(p.getProperty("project.author", ""));
            project.setVersion(p.getProperty("project.version", "1.0.0"));
            project.setDescription(p.getProperty("project.description", ""));
            int count = parseInt(p.getProperty("command.count"), 0);
            for (int i = 0; i < count; i++) project.addCommand(readCommand(p, "command." + i + "."));
            int itemCount = parseInt(p.getProperty("item.count"), 0);
            for (int i = 0; i < itemCount; i++) {
                try { project.addRuntimeItem(readItem(p, "item." + i + ".")); } catch (IllegalArgumentException ignored) {}
            }
            int blockCount = parseInt(p.getProperty("block.count"), 0);
            for (int i = 0; i < blockCount; i++) {
                try { project.addRuntimeBlock(readBlock(p, "block." + i + ".")); } catch (IllegalArgumentException ignored) {}
            }
            int textureCount = parseInt(p.getProperty("texture.count"), 0);
            for (int i = 0; i < textureCount; i++) {
                try { project.addTexture(readTexture(p, "texture." + i + ".")); } catch (IllegalArgumentException ignored) {}
            }
            int modelCount = parseInt(p.getProperty("model.count"), 0);
            for (int i = 0; i < modelCount; i++) {
                try { project.addModel(readModel(p, "model." + i + ".")); } catch (IllegalArgumentException ignored) {}
            }
            int triggerCount = parseInt(p.getProperty("trigger.count"), 0);
            for (int i = 0; i < triggerCount; i++) {
                try { project.addTrigger(readTrigger(p, "trigger." + i + ".")); } catch (IllegalArgumentException ignored) {}
            }
            return project;
        } catch (IllegalArgumentException corruptProject) {
            return CreatorProject.create(normalized, normalized);
        }
    }

    public Set<String> listProjectIds() throws IOException {
        if (!Files.isDirectory(root)) return Set.of();
        try (var stream = Files.list(root)) {
            return stream.filter(path -> path.getFileName().toString().endsWith(".igc.properties"))
                    .map(path -> path.getFileName().toString().replaceFirst("\\.igc\\.properties$", ""))
                    .sorted()
                    .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        }
    }

    private Path file(String id) { return root.resolve(id + ".igc.properties"); }


    private static void writeItem(Properties p, String prefix, RuntimeItemDefinition item) {
        p.setProperty(prefix + "id", item.id());
        p.setProperty(prefix + "name", item.displayName());
        p.setProperty(prefix + "description", item.description());
        p.setProperty(prefix + "maxStack", Integer.toString(item.maxStack()));
        p.setProperty(prefix + "glint", Boolean.toString(item.glint()));
        p.setProperty(prefix + "texture", item.textureId());
        p.setProperty(prefix + "model", item.modelId());
    }

    private static RuntimeItemDefinition readItem(Properties p, String prefix) {
        return new RuntimeItemDefinition(
                p.getProperty(prefix + "id"),
                p.getProperty(prefix + "name"),
                p.getProperty(prefix + "description", ""),
                parseInt(p.getProperty(prefix + "maxStack"), 64),
                Boolean.parseBoolean(p.getProperty(prefix + "glint", "false")),
                p.getProperty(prefix + "texture", ""),
                p.getProperty(prefix + "model", ""));
    }

    private static void writeBlock(Properties p, String prefix, RuntimeBlockDefinition block) {
        p.setProperty(prefix + "id", block.id());
        p.setProperty(prefix + "name", block.displayName());
        p.setProperty(prefix + "hardness", Float.toString(block.hardness()));
        p.setProperty(prefix + "resistance", Float.toString(block.resistance()));
        p.setProperty(prefix + "luminance", Integer.toString(block.luminance()));
        p.setProperty(prefix + "texture", block.textureId());
        p.setProperty(prefix + "model", block.modelId());
    }

    private static RuntimeBlockDefinition readBlock(Properties p, String prefix) {
        return new RuntimeBlockDefinition(p.getProperty(prefix + "id"), p.getProperty(prefix + "name"),
                parseFloat(p.getProperty(prefix + "hardness"), 1.5f), parseFloat(p.getProperty(prefix + "resistance"), 6.0f),
                parseInt(p.getProperty(prefix + "luminance"), 0), p.getProperty(prefix + "texture", ""), p.getProperty(prefix + "model", ""));
    }

    private static void writeTexture(Properties p, String prefix, TextureAsset texture) {
        p.setProperty(prefix + "id", texture.id());
        p.setProperty(prefix + "width", Integer.toString(texture.width()));
        p.setProperty(prefix + "height", Integer.toString(texture.height()));
        StringBuilder pixels = new StringBuilder();
        int[] values = texture.pixels();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) pixels.append(',');
            pixels.append(Integer.toUnsignedString(values[i], 16));
        }
        p.setProperty(prefix + "pixels", pixels.toString());
    }

    private static TextureAsset readTexture(Properties p, String prefix) {
        int width = parseInt(p.getProperty(prefix + "width"), 16);
        int height = parseInt(p.getProperty(prefix + "height"), 16);
        int[] pixels = new int[width * height];
        String[] parts = p.getProperty(prefix + "pixels", "").split(",", -1);
        for (int i = 0; i < pixels.length && i < parts.length; i++) {
            if (!parts[i].isBlank()) pixels[i] = (int) Long.parseUnsignedLong(parts[i], 16);
        }
        return new TextureAsset(p.getProperty(prefix + "id"), width, height, pixels);
    }

    private static void writeModel(Properties p, String prefix, CuboidModelAsset model) {
        p.setProperty(prefix + "id", model.id());
        p.setProperty(prefix + "cuboid.count", Integer.toString(model.cuboids().size()));
        for (int i = 0; i < model.cuboids().size(); i++) {
            CuboidModelAsset.Cuboid c = model.cuboids().get(i);
            String cp = prefix + "cuboid." + i + ".";
            p.setProperty(cp + "name", c.name()); p.setProperty(cp + "x", Double.toString(c.x())); p.setProperty(cp + "y", Double.toString(c.y())); p.setProperty(cp + "z", Double.toString(c.z()));
            p.setProperty(cp + "w", Double.toString(c.width())); p.setProperty(cp + "h", Double.toString(c.height())); p.setProperty(cp + "d", Double.toString(c.depth()));
            p.setProperty(cp + "rx", Double.toString(c.rotX())); p.setProperty(cp + "ry", Double.toString(c.rotY())); p.setProperty(cp + "rz", Double.toString(c.rotZ())); p.setProperty(cp + "texture", c.textureId());
        }
    }

    private static CuboidModelAsset readModel(Properties p, String prefix) {
        int count = parseInt(p.getProperty(prefix + "cuboid.count"), 0);
        List<CuboidModelAsset.Cuboid> cuboids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String cp = prefix + "cuboid." + i + ".";
            cuboids.add(new CuboidModelAsset.Cuboid(p.getProperty(cp + "name", "cube"), parseDouble(p.getProperty(cp + "x"), 0), parseDouble(p.getProperty(cp + "y"), 0), parseDouble(p.getProperty(cp + "z"), 0),
                    parseDouble(p.getProperty(cp + "w"), 1), parseDouble(p.getProperty(cp + "h"), 1), parseDouble(p.getProperty(cp + "d"), 1), parseDouble(p.getProperty(cp + "rx"), 0), parseDouble(p.getProperty(cp + "ry"), 0), parseDouble(p.getProperty(cp + "rz"), 0), p.getProperty(cp + "texture", "")));
        }
        return new CuboidModelAsset(p.getProperty(prefix + "id"), cuboids);
    }

    private static void writeTrigger(Properties p, String prefix, TriggerBinding trigger) {
        p.setProperty(prefix + "kind", trigger.kind().name()); p.setProperty(prefix + "element", trigger.elementId()); p.setProperty(prefix + "event", trigger.eventId()); p.setProperty(prefix + "blueprint", trigger.blueprintId());
    }

    private static TriggerBinding readTrigger(Properties p, String prefix) {
        return new TriggerBinding(TriggerBinding.ElementKind.valueOf(p.getProperty(prefix + "kind", "ITEM")), p.getProperty(prefix + "element"), p.getProperty(prefix + "event"), p.getProperty(prefix + "blueprint"));
    }

    private static void writeCommand(Properties p, String prefix, CommandDefinition command) {
        p.setProperty(prefix + "name", command.name());
        p.setProperty(prefix + "aliases", String.join(",", command.aliases()));
        p.setProperty(prefix + "roles", String.join(",", command.allowedRoles()));
        p.setProperty(prefix + "cooldown", Integer.toString(command.cooldownSeconds()));
        p.setProperty(prefix + "blueprint", command.blueprintHandlerId());
        p.setProperty(prefix + "arg.count", Integer.toString(command.arguments().size()));
        for (int i = 0; i < command.arguments().size(); i++) {
            CommandArgument a = command.arguments().get(i);
            String ap = prefix + "arg." + i + ".";
            p.setProperty(ap + "name", a.name());
            p.setProperty(ap + "type", a.type().name());
            p.setProperty(ap + "required", Boolean.toString(a.required()));
            p.setProperty(ap + "description", a.description());
        }
        p.setProperty(prefix + "sub.count", Integer.toString(command.subcommands().size()));
        for (int i = 0; i < command.subcommands().size(); i++) writeCommand(p, prefix + "sub." + i + ".", command.subcommands().get(i));
    }

    private static CommandDefinition readCommand(Properties p, String prefix) {
        CommandDefinition.Builder b = CommandDefinition.builder(p.getProperty(prefix + "name"));
        for (String alias : split(p.getProperty(prefix + "aliases", ""))) b.alias(alias);
        for (String role : split(p.getProperty(prefix + "roles", ""))) b.allowedRole(role);
        b.cooldownSeconds(parseInt(p.getProperty(prefix + "cooldown"), 0));
        b.blueprintHandler(p.getProperty(prefix + "blueprint", ""));
        int argCount = parseInt(p.getProperty(prefix + "arg.count"), 0);
        for (int i = 0; i < argCount; i++) {
            String ap = prefix + "arg." + i + ".";
            b.argument(new CommandArgument(
                    p.getProperty(ap + "name"),
                    CommandArgument.Type.valueOf(p.getProperty(ap + "type", "STRING")),
                    Boolean.parseBoolean(p.getProperty(ap + "required", "true")),
                    p.getProperty(ap + "description", "")));
        }
        int subCount = parseInt(p.getProperty(prefix + "sub.count"), 0);
        for (int i = 0; i < subCount; i++) b.subcommand(readCommand(p, prefix + "sub." + i + "."));
        return b.build();
    }

    private static List<String> split(String value) {
        if (value == null || value.isBlank()) return List.of();
        return java.util.Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private static int parseInt(String value, int fallback) {
        try { return Integer.parseInt(value); } catch (Exception ignored) { return fallback; }
    }
    private static float parseFloat(String value, float fallback) {
        try { return Float.parseFloat(value); } catch (Exception ignored) { return fallback; }
    }
    private static double parseDouble(String value, double fallback) {
        try { return Double.parseDouble(value); } catch (Exception ignored) { return fallback; }
    }
}
