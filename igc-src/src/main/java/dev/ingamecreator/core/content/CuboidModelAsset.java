package dev.ingamecreator.core.content;

import java.util.List;
import java.util.Locale;

public record CuboidModelAsset(String id, List<Cuboid> cuboids) {
    public CuboidModelAsset {
        if (id == null) throw new IllegalArgumentException("model id is required");
        id = id.trim().toLowerCase(Locale.ROOT);
        if (!id.matches("[a-z][a-z0-9_-]{1,63}")) throw new IllegalArgumentException("invalid model id: " + id);
        cuboids = List.copyOf(cuboids == null ? List.of() : cuboids);
    }

    public record Cuboid(String name, double x, double y, double z, double width, double height, double depth,
                         double rotX, double rotY, double rotZ, String textureId) {
        public Cuboid {
            if (name == null || name.isBlank()) throw new IllegalArgumentException("cuboid name is required");
            if (width <= 0 || height <= 0 || depth <= 0) throw new IllegalArgumentException("cuboid size must be positive");
            textureId = textureId == null ? "" : textureId.trim();
        }
    }
}
