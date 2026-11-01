package dev.ingamecreator.core.runtime;

import dev.ingamecreator.core.blueprint.BlueprintGraph;
import dev.ingamecreator.core.blueprint.BlueprintStore;
import dev.ingamecreator.core.command.CommandDefinition;
import dev.ingamecreator.core.project.CreatorProject;
import dev.ingamecreator.core.project.ProjectStore;
import dev.ingamecreator.core.roles.RoleService;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.LongSupplier;

public final class ProjectRuntimeService {
    private final ProjectStore projects;
    private final BlueprintStore blueprints;
    private final RoleService roles;
    private final LongSupplier clock;
    private final BlueprintExecutor executor;
    private final Map<CooldownKey, Long> lastSuccessAt = new LinkedHashMap<>();
    private final ThreadLocal<Integer> callDepth = ThreadLocal.withInitial(() -> 0);

    public ProjectRuntimeService(ProjectStore projects, BlueprintStore blueprints, RoleService roles, LongSupplier clock) {
        this(projects, blueprints, roles, clock, new BlueprintExecutor());
    }

    public ProjectRuntimeService(ProjectStore projects, BlueprintStore blueprints, RoleService roles,
                                 LongSupplier clock, BlueprintExecutor executor) {
        this.projects = Objects.requireNonNull(projects, "projects");
        this.blueprints = Objects.requireNonNull(blueprints, "blueprints");
        this.roles = Objects.requireNonNull(roles, "roles");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    public RunResult run(String projectId, String commandName, RuntimePlayer player,
                         Map<String, String> arguments, ProjectRuntimeHooks hooks) {
        Objects.requireNonNull(player, "player");
        int depth = callDepth.get();
        if (depth >= 16) return RunResult.of(Status.FAILED, "command recursion limit exceeded");
        callDepth.set(depth + 1);
        try {
            hooks = hooks == null ? ProjectRuntimeHooks.none() : hooks;
        CreatorProject project;
        try {
            project = projects.load(projectId);
        } catch (NoSuchFileException missing) {
            return RunResult.of(Status.NOT_FOUND, "project not found: " + projectId);
        } catch (IOException | IllegalArgumentException failure) {
            return RunResult.of(Status.FAILED, "could not load project: " + safeMessage(failure));
        }

        CommandDefinition command = resolveCommand(project, commandName);
        if (command == null) return RunResult.of(Status.NOT_FOUND, "command not found: " + commandName);

        if (!command.allowedRoles().isEmpty()
                && command.allowedRoles().stream().noneMatch(role -> roles.hasRole(player.id(), role))) {
            return RunResult.of(Status.DENIED, "required role: " + String.join(" or ", command.allowedRoles()));
        }

        long now = clock.getAsLong();
        CooldownKey cooldownKey = new CooldownKey(project.id(), command.name(), player.id());
        if (command.cooldownSeconds() > 0) {
            long duration = command.cooldownSeconds() * 1000L;
            Long last = lastSuccessAt.get(cooldownKey);
            if (last != null) {
                long remaining = duration - (now - last);
                if (remaining > 0) return RunResult.cooldown(remaining);
            }
        }

        if (command.blueprintHandlerId() == null || command.blueprintHandlerId().isBlank()) {
            return RunResult.of(Status.MISSING_BLUEPRINT, "command has no Blueprint handler");
        }

        BlueprintGraph graph;
        try {
            graph = blueprints.load(project.id(), command.blueprintHandlerId());
        } catch (NoSuchFileException missing) {
            return RunResult.of(Status.MISSING_BLUEPRINT, "Blueprint not found: " + command.blueprintHandlerId());
        } catch (IOException | IllegalArgumentException failure) {
            return RunResult.of(Status.MISSING_BLUEPRINT, "could not load Blueprint: " + safeMessage(failure));
        }

        ProjectRuntimeHooks finalHooks = hooks;
        List<String> defaultCommandList = commandNames(project);
        BlueprintExecutionContext context = BlueprintExecutionContext.builder(player, roles)
                .arguments(arguments == null ? Map.of() : arguments)
                .commandListSupplier(finalHooks.commandListSupplier() == null ? () -> defaultCommandList : finalHooks.commandListSupplier())
                .commandExecutor(finalHooks.commandExecutor())
                .messageSink(finalHooks.messageSink())
                .functionInvoker(finalHooks.functionInvoker())
                .maxSteps(finalHooks.maxSteps())
                .build();
        BlueprintExecutionResult execution = executor.execute(graph, context);
        if (!execution.success()) {
            return new RunResult(Status.FAILED,
                    execution.diagnostics().isEmpty() ? "Blueprint execution failed" : String.join("; ", execution.diagnostics()),
                    0L, execution);
        }
            if (command.cooldownSeconds() > 0) lastSuccessAt.put(cooldownKey, now);
            return new RunResult(Status.SUCCESS, "ok", 0L, execution);
        } finally {
            if (depth == 0) callDepth.remove();
            else callDepth.set(depth);
        }
    }

    public BlueprintExecutionResult runBlueprint(String projectId, String blueprintId, RuntimePlayer player,
                                                 Map<String, String> arguments, ProjectRuntimeHooks hooks) {
        Objects.requireNonNull(player, "player");
        hooks = hooks == null ? ProjectRuntimeHooks.none() : hooks;
        BlueprintGraph graph;
        try {
            graph = blueprints.load(projectId, blueprintId);
        } catch (IOException | IllegalArgumentException failure) {
            return BlueprintExecutionResult.failure(0, List.of("could not load Blueprint: " + safeMessage(failure)));
        }
        ProjectRuntimeHooks finalHooks = hooks;
        BlueprintExecutionContext context = BlueprintExecutionContext.builder(player, roles)
                .arguments(arguments == null ? Map.of() : arguments)
                .commandListSupplier(finalHooks.commandListSupplier() == null ? List::of : finalHooks.commandListSupplier())
                .commandExecutor(finalHooks.commandExecutor())
                .messageSink(finalHooks.messageSink())
                .functionInvoker(finalHooks.functionInvoker())
                .maxSteps(finalHooks.maxSteps())
                .build();
        return executor.execute(graph, context);
    }

    public List<String> commandNames(String projectId) throws IOException {
        return commandNames(projects.load(projectId));
    }

    private static CommandDefinition resolveCommand(CreatorProject project, String requested) {
        String normalized;
        try {
            normalized = CommandDefinition.normalizeName(requested);
        } catch (IllegalArgumentException invalid) {
            return null;
        }
        CommandDefinition direct = project.commands().get(normalized);
        if (direct != null) return direct;
        for (CommandDefinition command : project.commands().values()) {
            if (command.aliases().contains(normalized)) return command;
        }
        return null;
    }

    private static List<String> commandNames(CreatorProject project) {
        ArrayList<String> names = new ArrayList<>();
        for (CommandDefinition command : project.commands().values()) {
            names.add(command.name());
            names.addAll(command.aliases());
        }
        return List.copyOf(names);
    }

    private static String safeMessage(Throwable throwable) {
        return throwable.getMessage() == null ? throwable.getClass().getSimpleName() : throwable.getMessage();
    }

    public enum Status { SUCCESS, NOT_FOUND, DENIED, COOLDOWN, MISSING_BLUEPRINT, FAILED }

    public record RunResult(Status status, String message, long retryAfterMillis, BlueprintExecutionResult execution) {
        public RunResult {
            if (status == null) throw new IllegalArgumentException("status is required");
            message = message == null ? "" : message;
        }
        public static RunResult of(Status status, String message) { return new RunResult(status, message, 0L, null); }
        public static RunResult cooldown(long retryAfterMillis) {
            return new RunResult(Status.COOLDOWN, "cooldown active", Math.max(1L, retryAfterMillis), null);
        }
    }

    private record CooldownKey(String projectId, String commandName, UUID playerId) {}
}
