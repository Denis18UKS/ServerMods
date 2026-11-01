package dev.ingamecreator.core.roles;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public final class RoleService {
    public static final String DEFAULT_ROLE = "default";
    private static final Pattern ROLE_PATTERN = Pattern.compile("[a-z0-9_.-]{1,32}");

    private final RoleRepository repository;
    private final Set<String> roles = new LinkedHashSet<>();
    private final Map<UUID, Set<String>> assignments = new LinkedHashMap<>();

    public RoleService(RoleRepository repository) {
        this.repository = repository;
        roles.add(DEFAULT_ROLE);
        try {
            RoleRepository.Snapshot snapshot = repository.load();
            for (String role : snapshot.roles()) {
                String normalized = normalizeRoleName(role);
                roles.add(normalized);
            }
            for (Map.Entry<UUID, Set<String>> entry : snapshot.assignments().entrySet()) {
                LinkedHashSet<String> valid = new LinkedHashSet<>();
                for (String role : entry.getValue()) {
                    String normalized;
                    try {
                        normalized = normalizeRoleName(role);
                    } catch (IllegalArgumentException ignored) {
                        continue;
                    }
                    if (!DEFAULT_ROLE.equals(normalized) && roles.contains(normalized)) valid.add(normalized);
                }
                if (!valid.isEmpty()) assignments.put(entry.getKey(), valid);
            }
        } catch (IOException | IllegalArgumentException ignored) {
            // Invalid/corrupt persistence must not prevent server startup.
        }
    }

    public Set<String> roles() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(roles));
    }

    public Set<String> getRoles(UUID userId) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        result.add(DEFAULT_ROLE);
        result.addAll(assignments.getOrDefault(userId, Set.of()));
        return Collections.unmodifiableSet(result);
    }

    public boolean hasRole(UUID userId, String role) {
        String normalized = normalizeRoleName(role);
        return DEFAULT_ROLE.equals(normalized) || assignments.getOrDefault(userId, Set.of()).contains(normalized);
    }

    public Set<UUID> usersWithRole(String role) {
        String normalized = normalizeRoleName(role);
        if (!roles.contains(normalized)) throw new IllegalArgumentException("unknown role: " + normalized);
        if (DEFAULT_ROLE.equals(normalized)) return Set.copyOf(assignments.keySet());
        LinkedHashSet<UUID> result = new LinkedHashSet<>();
        assignments.forEach((id, values) -> { if (values.contains(normalized)) result.add(id); });
        return Collections.unmodifiableSet(result);
    }

    public void createRole(String role) {
        String normalized = normalizeRoleName(role);
        if (roles.add(normalized)) persist();
    }

    public void deleteRole(String role) {
        String normalized = normalizeRoleName(role);
        if (DEFAULT_ROLE.equals(normalized)) throw new IllegalArgumentException("default role cannot be deleted");
        if (!roles.remove(normalized)) throw new IllegalArgumentException("unknown role: " + normalized);
        assignments.values().forEach(values -> values.remove(normalized));
        assignments.entrySet().removeIf(e -> e.getValue().isEmpty());
        persist();
    }

    public void addRole(UUID userId, String role) {
        String normalized = requireKnownRole(role);
        if (DEFAULT_ROLE.equals(normalized)) return;
        if (assignments.computeIfAbsent(userId, ignored -> new LinkedHashSet<>()).add(normalized)) persist();
    }

    public void removeRole(UUID userId, String role) {
        String normalized = requireKnownRole(role);
        if (DEFAULT_ROLE.equals(normalized)) return;
        Set<String> userRoles = assignments.get(userId);
        if (userRoles != null && userRoles.remove(normalized)) {
            if (userRoles.isEmpty()) assignments.remove(userId);
            persist();
        }
    }

    public void setRole(UUID userId, String role) {
        String normalized = requireKnownRole(role);
        if (DEFAULT_ROLE.equals(normalized)) {
            assignments.remove(userId);
        } else {
            LinkedHashSet<String> values = new LinkedHashSet<>();
            values.add(normalized);
            assignments.put(userId, values);
        }
        persist();
    }

    public void clearRoles(UUID userId) {
        if (assignments.remove(userId) != null) persist();
    }

    public static String normalizeRoleName(String role) {
        if (role == null) throw new IllegalArgumentException("role is required");
        String normalized = role.trim().toLowerCase(Locale.ROOT);
        if (!ROLE_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("invalid role: " + role);
        }
        return normalized;
    }

    private String requireKnownRole(String role) {
        String normalized = normalizeRoleName(role);
        if (!roles.contains(normalized)) throw new IllegalArgumentException("unknown role: " + normalized);
        return normalized;
    }

    private void persist() {
        Map<UUID, Set<String>> copy = new LinkedHashMap<>();
        assignments.forEach((key, value) -> copy.put(key, Set.copyOf(value)));
        try {
            repository.save(new RoleRepository.Snapshot(Set.copyOf(roles), copy));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not persist roles", e);
        }
    }
}
