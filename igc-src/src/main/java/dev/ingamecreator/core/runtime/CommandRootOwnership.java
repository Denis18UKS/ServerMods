package dev.ingamecreator.core.runtime;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Tracks which generated project command owns a Brigadier root across reloads. */
public final class CommandRootOwnership {
    private final Map<String, String> owners = new LinkedHashMap<>();

    public synchronized boolean claim(String root, String owner) {
        if (root == null || root.isBlank()) throw new IllegalArgumentException("root is required");
        if (owner == null || owner.isBlank()) throw new IllegalArgumentException("owner is required");
        String existing = owners.get(root);
        if (existing == null) {
            owners.put(root, owner);
            return true;
        }
        return existing.equals(owner);
    }

    public synchronized void reset() {
        owners.clear();
    }

    public synchronized Optional<String> owner(String root) {
        return Optional.ofNullable(owners.get(root));
    }
}
