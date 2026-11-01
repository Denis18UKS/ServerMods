package dev.ingamecreator.core.content;

import java.util.Locale;

public record RuntimeBlockDefinition(String id, String displayName, float hardness, float resistance, int luminance,
                                     String textureId, String modelId) {
    public RuntimeBlockDefinition {
        if (id == null) throw new IllegalArgumentException("block id is required");
        id = id.trim().toLowerCase(Locale.ROOT);
        if (!id.matches("[a-z][a-z0-9_-]{1,63}")) throw new IllegalArgumentException("invalid block id: " + id);
        if (displayName == null || displayName.isBlank()) throw new IllegalArgumentException("display name is required");
        if (hardness < 0 || resistance < 0) throw new IllegalArgumentException("hardness/resistance must be >= 0");
        if (luminance < 0 || luminance > 15) throw new IllegalArgumentException("luminance must be 0..15");
        textureId = textureId == null ? "" : textureId.trim();
        modelId = modelId == null ? "" : modelId.trim();
    }
}
