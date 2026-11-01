package dev.ingamecreator.core.project;

import dev.ingamecreator.core.command.CommandDefinition;
import dev.ingamecreator.core.content.RuntimeItemDefinition;
import dev.ingamecreator.core.content.RuntimeBlockDefinition;
import dev.ingamecreator.core.content.TextureAsset;
import dev.ingamecreator.core.content.CuboidModelAsset;
import dev.ingamecreator.core.content.TriggerBinding;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.regex.Pattern;

public final class CreatorProject {
    private static final Pattern ID_PATTERN = Pattern.compile("[a-z][a-z0-9_-]{1,63}");

    private final String id;
    private String name;
    private String author = "";
    private String version = "1.0.0";
    private String description = "";
    private final Map<String, CommandDefinition> commands = new LinkedHashMap<>();
    private final Map<String, RuntimeItemDefinition> runtimeItems = new LinkedHashMap<>();
    private final Map<String, RuntimeBlockDefinition> runtimeBlocks = new LinkedHashMap<>();
    private final Map<String, TextureAsset> textures = new LinkedHashMap<>();
    private final Map<String, CuboidModelAsset> models = new LinkedHashMap<>();
    private final List<TriggerBinding> triggers = new ArrayList<>();

    private CreatorProject(String id, String name) {
        this.id = normalizeId(id);
        this.name = requireText(name, "name");
    }

    public static CreatorProject create(String id, String name) { return new CreatorProject(id, name); }

    public String id() { return id; }
    public String name() { return name; }
    public String author() { return author; }
    public String version() { return version; }
    public String description() { return description; }
    public Map<String, CommandDefinition> commands() { return Collections.unmodifiableMap(commands); }
    public Optional<CommandDefinition> command(String name) { return Optional.ofNullable(commands.get(CommandDefinition.normalizeName(name))); }
    public Map<String, RuntimeItemDefinition> runtimeItems() { return Collections.unmodifiableMap(runtimeItems); }
    public Optional<RuntimeItemDefinition> runtimeItem(String id) { return Optional.ofNullable(runtimeItems.get(RuntimeItemDefinition.normalizeId(id))); }
    public Map<String, RuntimeBlockDefinition> runtimeBlocks() { return Collections.unmodifiableMap(runtimeBlocks); }
    public Optional<RuntimeBlockDefinition> runtimeBlock(String id) { return Optional.ofNullable(runtimeBlocks.get(id == null ? "" : id.trim().toLowerCase(Locale.ROOT))); }
    public Map<String, TextureAsset> textures() { return Collections.unmodifiableMap(textures); }
    public Optional<TextureAsset> texture(String id) { return Optional.ofNullable(textures.get(id == null ? "" : id.trim().toLowerCase(Locale.ROOT))); }
    public Map<String, CuboidModelAsset> models() { return Collections.unmodifiableMap(models); }
    public Optional<CuboidModelAsset> model(String id) { return Optional.ofNullable(models.get(id == null ? "" : id.trim().toLowerCase(Locale.ROOT))); }
    public List<TriggerBinding> triggers() { return List.copyOf(triggers); }

    public void setName(String name) { this.name = requireText(name, "name"); }
    public void setAuthor(String author) { this.author = author == null ? "" : author.trim(); }
    public void setVersion(String version) { this.version = requireText(version, "version"); }
    public void setDescription(String description) { this.description = description == null ? "" : description; }

    public void addCommand(CommandDefinition command) {
        if (command == null) throw new IllegalArgumentException("command is required");
        if (commands.putIfAbsent(command.name(), command) != null) {
            throw new IllegalArgumentException("duplicate command: " + command.name());
        }
    }

    public void addRuntimeItem(RuntimeItemDefinition item) {
        if (item == null) throw new IllegalArgumentException("item is required");
        if (runtimeItems.putIfAbsent(item.id(), item) != null) throw new IllegalArgumentException("duplicate runtime item: " + item.id());
    }

    public void addRuntimeBlock(RuntimeBlockDefinition block) {
        if (block == null) throw new IllegalArgumentException("block is required");
        if (runtimeBlocks.putIfAbsent(block.id(), block) != null) throw new IllegalArgumentException("duplicate runtime block: " + block.id());
    }

    public void addTexture(TextureAsset texture) {
        if (texture == null) throw new IllegalArgumentException("texture is required");
        if (textures.putIfAbsent(texture.id(), texture) != null) throw new IllegalArgumentException("duplicate texture: " + texture.id());
    }

    public void addModel(CuboidModelAsset model) {
        if (model == null) throw new IllegalArgumentException("model is required");
        if (models.putIfAbsent(model.id(), model) != null) throw new IllegalArgumentException("duplicate model: " + model.id());
    }

    public void addTrigger(TriggerBinding trigger) {
        if (trigger == null) throw new IllegalArgumentException("trigger is required");
        triggers.removeIf(t -> t.kind() == trigger.kind() && t.elementId().equals(trigger.elementId()) && t.eventId().equals(trigger.eventId()));
        triggers.add(trigger);
    }

    public void putTexture(TextureAsset texture) {
        if (texture == null) throw new IllegalArgumentException("texture is required");
        textures.put(texture.id(), texture);
    }
    public void putModel(CuboidModelAsset model) {
        if (model == null) throw new IllegalArgumentException("model is required");
        models.put(model.id(), model);
    }
    public void putRuntimeBlock(RuntimeBlockDefinition block) {
        if (block == null) throw new IllegalArgumentException("block is required");
        runtimeBlocks.put(block.id(), block);
    }

    public static String normalizeId(String value) {
        if (value == null) throw new IllegalArgumentException("project id is required");
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (!ID_PATTERN.matcher(normalized).matches()) throw new IllegalArgumentException("invalid project id: " + value);
        return normalized;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.trim();
    }
}
