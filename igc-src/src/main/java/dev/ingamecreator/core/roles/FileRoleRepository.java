package dev.ingamecreator.core.roles;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class FileRoleRepository implements RoleRepository {
    private static final String ROLES_KEY = "roles";
    private static final String USER_PREFIX = "user.";
    private final Path file;

    public FileRoleRepository(Path file) {
        this.file = file;
    }

    @Override
    public Snapshot load() throws IOException {
        if (!Files.exists(file)) return Snapshot.empty();
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
        }

        Set<String> roles = split(properties.getProperty(ROLES_KEY, ""));
        Map<UUID, Set<String>> assignments = new LinkedHashMap<>();
        for (String key : properties.stringPropertyNames()) {
            if (!key.startsWith(USER_PREFIX)) continue;
            try {
                UUID user = UUID.fromString(key.substring(USER_PREFIX.length()));
                assignments.put(user, split(properties.getProperty(key, "")));
            } catch (IllegalArgumentException ignored) {
                // Ignore corrupt user keys rather than breaking world/server startup.
            }
        }
        return new Snapshot(roles, assignments);
    }

    @Override
    public void save(Snapshot snapshot) throws IOException {
        Path parent = file.getParent();
        if (parent != null) Files.createDirectories(parent);
        Properties properties = new Properties();
        properties.setProperty(ROLES_KEY, join(snapshot.roles()));
        for (Map.Entry<UUID, Set<String>> entry : snapshot.assignments().entrySet()) {
            if (!entry.getValue().isEmpty()) {
                properties.setProperty(USER_PREFIX + entry.getKey(), join(entry.getValue()));
            }
        }
        try (OutputStream output = Files.newOutputStream(file)) {
            properties.store(output, "InGame Creator roles");
        }
    }

    private static Set<String> split(String value) {
        if (value == null || value.isBlank()) return new LinkedHashSet<>();
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static String join(Set<String> values) {
        return values.stream().sorted().collect(Collectors.joining(","));
    }
}
