package dev.ingamecreator.core.roles;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface RoleRepository {
    Snapshot load() throws IOException;
    void save(Snapshot snapshot) throws IOException;

    record Snapshot(Set<String> roles, Map<UUID, Set<String>> assignments) {
        public static Snapshot empty() {
            return new Snapshot(Set.of(), Map.of());
        }
    }
}
