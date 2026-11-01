package dev.ingamecreator.core.content;

import java.util.Locale;

public record TriggerBinding(ElementKind kind, String elementId, String eventId, String blueprintId) {
    public TriggerBinding {
        if (kind == null) throw new IllegalArgumentException("element kind is required");
        elementId = normalize(elementId, "element id");
        eventId = normalize(eventId, "event id");
        blueprintId = normalize(blueprintId, "blueprint id");
    }

    private static String normalize(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.trim().toLowerCase(Locale.ROOT);
    }

    public enum ElementKind { ITEM, BLOCK }
}
