package dev.ingamecreator.core.content;

import java.util.Locale;
import java.util.regex.Pattern;

public record RuntimeItemDefinition(String id, String displayName, String description, int maxStack, boolean glint,
                                    String textureId, String modelId) {
    private static final Pattern ID_PATTERN = Pattern.compile("[a-z][a-z0-9_-]{1,63}");

    public RuntimeItemDefinition {
        if (id == null) throw new IllegalArgumentException("item id is required");
        id = id.trim().toLowerCase(Locale.ROOT);
        if (!ID_PATTERN.matcher(id).matches()) throw new IllegalArgumentException("invalid item id: " + id);
        if (displayName == null || displayName.isBlank()) throw new IllegalArgumentException("display name is required");
        displayName = displayName.trim();
        description = description == null ? "" : description.trim();
        if (maxStack < 1 || maxStack > 64) throw new IllegalArgumentException("maxStack must be 1..64");
        textureId = textureId == null ? "" : textureId.trim();
        modelId = modelId == null ? "" : modelId.trim();
    }

    public RuntimeItemDefinition(String id, String displayName, String description, int maxStack, boolean glint) {
        this(id, displayName, description, maxStack, glint, "", "");
    }

    public static String normalizeId(String value) {
        if (value == null) throw new IllegalArgumentException("item id is required");
        String id = value.trim().toLowerCase(Locale.ROOT);
        if (!ID_PATTERN.matcher(id).matches()) throw new IllegalArgumentException("invalid item id: " + value);
        return id;
    }
}
