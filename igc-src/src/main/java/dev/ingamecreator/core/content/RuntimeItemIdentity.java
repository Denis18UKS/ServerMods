package dev.ingamecreator.core.content;

import dev.ingamecreator.core.project.CreatorProject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Portable identity stored on the single Fabric host item. */
public record RuntimeItemIdentity(String projectId, String itemId) {
    public static final String PROJECT_KEY = "igc_project";
    public static final String ITEM_KEY = "igc_item";

    public RuntimeItemIdentity {
        projectId = CreatorProject.normalizeId(projectId);
        itemId = RuntimeItemDefinition.normalizeId(itemId);
    }

    public Map<String, String> toTags() {
        Map<String, String> tags = new LinkedHashMap<>();
        tags.put(PROJECT_KEY, projectId);
        tags.put(ITEM_KEY, itemId);
        return Map.copyOf(tags);
    }

    public static Optional<RuntimeItemIdentity> fromTags(Map<String, String> tags) {
        if (tags == null) return Optional.empty();
        String project = tags.get(PROJECT_KEY);
        String item = tags.get(ITEM_KEY);
        if (project == null || item == null) return Optional.empty();
        try {
            return Optional.of(new RuntimeItemIdentity(project, item));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
