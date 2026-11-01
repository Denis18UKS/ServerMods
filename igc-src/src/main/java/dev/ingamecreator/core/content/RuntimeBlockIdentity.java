package dev.ingamecreator.core.content;

import dev.ingamecreator.core.project.CreatorProject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Portable identity stored on the single Fabric runtime host block entity. */
public record RuntimeBlockIdentity(String projectId, String blockId) {
    public static final String PROJECT_KEY = "igc_project";
    public static final String BLOCK_KEY = "igc_block";

    public RuntimeBlockIdentity {
        projectId = CreatorProject.normalizeId(projectId);
        if (blockId == null) throw new IllegalArgumentException("block id is required");
        blockId = blockId.trim().toLowerCase(java.util.Locale.ROOT);
        if (!blockId.matches("[a-z][a-z0-9_-]{1,63}")) throw new IllegalArgumentException("invalid block id: " + blockId);
    }

    public Map<String, String> toTags() {
        Map<String, String> tags = new LinkedHashMap<>();
        tags.put(PROJECT_KEY, projectId);
        tags.put(BLOCK_KEY, blockId);
        return Map.copyOf(tags);
    }

    public static Optional<RuntimeBlockIdentity> fromTags(Map<String, String> tags) {
        if (tags == null) return Optional.empty();
        String project = tags.get(PROJECT_KEY);
        String block = tags.get(BLOCK_KEY);
        if (project == null || block == null) return Optional.empty();
        try {
            return Optional.of(new RuntimeBlockIdentity(project, block));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
