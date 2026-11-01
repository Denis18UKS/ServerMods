package dev.ingamecreator.core.roles;

import java.util.stream.Collectors;
import java.util.UUID;

public final class RoleCommandFacade {
    private final RoleService roles;

    public RoleCommandFacade(RoleService roles) {
        this.roles = roles;
    }

    public Result status(UUID userId, String userName) {
        return list(userId, userName);
    }

    public Result list(UUID userId, String userName) {
        String joined = roles.getRoles(userId).stream().sorted().collect(Collectors.joining(", "));
        return Result.ok(userName + ": " + joined);
    }

    public Result add(UUID userId, String userName, String role) {
        return run(() -> roles.addRole(userId, role), "Добавлена роль " + safeRole(role) + " игроку " + userName);
    }

    public Result remove(UUID userId, String userName, String role) {
        return run(() -> roles.removeRole(userId, role), "Удалена роль " + safeRole(role) + " у игрока " + userName);
    }

    public Result set(UUID userId, String userName, String role) {
        return run(() -> roles.setRole(userId, role), "Основной набор ролей игрока " + userName + " заменён на " + safeRole(role));
    }

    public Result clear(UUID userId, String userName) {
        return run(() -> roles.clearRoles(userId), "Роли игрока " + userName + " очищены до default");
    }

    public Result create(String role) {
        return run(() -> roles.createRole(role), "Создана роль " + safeRole(role));
    }

    public Result delete(String role) {
        return run(() -> roles.deleteRole(role), "Удалена роль " + safeRole(role));
    }

    public Result info(String role) {
        try {
            String normalized = RoleService.normalizeRoleName(role);
            if (!roles.roles().contains(normalized)) return Result.error("Роль не найдена: " + normalized);
            return Result.ok("Роль " + normalized + " существует");
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    private Result run(Runnable action, String success) {
        try {
            action.run();
            return Result.ok(success);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }

    private static String safeRole(String role) {
        try {
            return RoleService.normalizeRoleName(role);
        } catch (IllegalArgumentException ignored) {
            return String.valueOf(role);
        }
    }

    public record Result(boolean success, String message) {
        public static Result ok(String message) { return new Result(true, message); }
        public static Result error(String message) { return new Result(false, message); }
    }
}
