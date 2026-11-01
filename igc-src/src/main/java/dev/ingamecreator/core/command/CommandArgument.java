package dev.ingamecreator.core.command;

import java.util.Locale;
import java.util.regex.Pattern;

public record CommandArgument(String name, Type type, boolean required, String description) {
    private static final Pattern NAME_PATTERN = Pattern.compile("[a-z][a-z0-9_]{0,31}");

    public CommandArgument {
        if (name == null) throw new IllegalArgumentException("argument name is required");
        name = name.trim().toLowerCase(Locale.ROOT);
        if (!NAME_PATTERN.matcher(name).matches()) throw new IllegalArgumentException("invalid argument name: " + name);
        if (type == null) throw new IllegalArgumentException("argument type is required");
        description = description == null ? "" : description;
    }

    public enum Type {
        STRING, INTEGER, DOUBLE, BOOLEAN, PLAYER, ENTITY, BLOCK_POS, ITEM, BLOCK, DIMENSION, ROLE, ENUM, TEXT
    }
}
