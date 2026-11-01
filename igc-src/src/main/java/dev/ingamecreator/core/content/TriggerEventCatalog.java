package dev.ingamecreator.core.content;

import java.util.List;

/** Events currently dispatched by the runtime host item/block implementations. */
public final class TriggerEventCatalog {
    private static final List<String> ITEM = List.of("right_click", "inventory_tick");
    private static final List<String> BLOCK = List.of("placed", "broken", "right_click");

    private TriggerEventCatalog() {}

    public static List<String> events(TriggerBinding.ElementKind kind) {
        if (kind == null) throw new IllegalArgumentException("trigger kind is required");
        return kind == TriggerBinding.ElementKind.ITEM ? ITEM : BLOCK;
    }

    public static boolean supports(TriggerBinding.ElementKind kind, String eventId) {
        if (eventId == null) return false;
        return events(kind).contains(eventId.trim().toLowerCase(java.util.Locale.ROOT));
    }
}
