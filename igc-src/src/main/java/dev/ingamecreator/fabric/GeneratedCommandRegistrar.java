package dev.ingamecreator.fabric;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ingamecreator.core.command.CommandDefinition;
import dev.ingamecreator.core.project.CreatorProject;
import dev.ingamecreator.core.project.ProjectStore;
import dev.ingamecreator.core.runtime.CommandInvocationParser;
import dev.ingamecreator.core.runtime.CommandRootOwnership;
import dev.ingamecreator.core.runtime.ProjectRuntimeHooks;
import dev.ingamecreator.core.runtime.ProjectRuntimeService;
import dev.ingamecreator.core.runtime.RuntimePlayer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class GeneratedCommandRegistrar {
    private final ProjectStore projects;
    private final ProjectRuntimeService runtime;
    private final CommandRootOwnership rootOwnership = new CommandRootOwnership();
    private CommandDispatcher<ServerCommandSource> lastDispatcher;

    public GeneratedCommandRegistrar(ProjectStore projects, ProjectRuntimeService runtime) {
        this.projects = projects;
        this.runtime = runtime;
    }

    public RegisterReport registerAll(CommandDispatcher<ServerCommandSource> dispatcher) {
        if (lastDispatcher != dispatcher) {
            rootOwnership.reset();
            lastDispatcher = dispatcher;
        }
        int registered = 0;
        int skipped = 0;
        ArrayList<String> errors = new ArrayList<>();
        try {
            for (String projectId : projects.listProjectIds()) {
                CreatorProject project;
                try {
                    project = projects.load(projectId);
                } catch (Exception failure) {
                    errors.add(projectId + ": " + safeMessage(failure));
                    continue;
                }
                for (CommandDefinition command : project.commands().values()) {
                    RegisterOne result = registerOne(dispatcher, project.id(), command, command.name());
                    registered += result.registered ? 1 : 0;
                    skipped += result.skipped ? 1 : 0;
                    if (result.error != null) errors.add(result.error);
                    for (String alias : command.aliases()) {
                        RegisterOne aliasResult = registerOne(dispatcher, project.id(), command, alias);
                        registered += aliasResult.registered ? 1 : 0;
                        skipped += aliasResult.skipped ? 1 : 0;
                        if (aliasResult.error != null) errors.add(aliasResult.error);
                    }
                }
            }
        } catch (IOException failure) {
            errors.add("project list: " + safeMessage(failure));
        }
        return new RegisterReport(registered, skipped, List.copyOf(errors));
    }

    public int executeDirect(ServerCommandSource source, String projectId, String commandName, String rawArguments) {
        CreatorProject project;
        try {
            project = projects.load(projectId);
        } catch (Exception failure) {
            source.sendError(Text.literal("[InGame Creator] Failed to open project: " + safeMessage(failure)));
            return 0;
        }
        CommandDefinition command = findCommand(project, commandName);
        if (command == null) {
            source.sendError(Text.literal("[InGame Creator] Command not found: " + commandName));
            return 0;
        }
        return execute(source, project.id(), command, rawArguments);
    }

    private RegisterOne registerOne(CommandDispatcher<ServerCommandSource> dispatcher, String projectId,
                                    CommandDefinition command, String rootName) {
        String ownerKey = projectId + ":" + command.name();
        String existingOwner = rootOwnership.owner(rootName).orElse(null);
        if (dispatcher.getRoot().getChild(rootName) != null && existingOwner == null) {
            return new RegisterOne(false, true,
                    "Skipped /" + rootName + " from " + projectId + ": name is already used by Minecraft/another mod");
        }
        if (!rootOwnership.claim(rootName, ownerKey)) {
            return new RegisterOne(false, true,
                    "Skipped /" + rootName + " from " + projectId + ": root already belongs to "
                            + rootOwnership.owner(rootName).orElse("another project command"));
        }
        LiteralArgumentBuilder<ServerCommandSource> root = literal(rootName)
                .executes(ctx -> execute(ctx.getSource(), projectId, command, ""))
                .then(argument("__igc_args", StringArgumentType.greedyString())
                        .executes(ctx -> execute(ctx.getSource(), projectId, command,
                                StringArgumentType.getString(ctx, "__igc_args"))));
        dispatcher.register(root);
        return new RegisterOne(true, false, null);
    }

    private int execute(ServerCommandSource source, String projectId, CommandDefinition command, String rawArguments) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("[InGame Creator] Runtime commands currently require a player source."));
            return 0;
        }

        Map<String, String> arguments;
        try {
            arguments = CommandInvocationParser.parse(command, rawArguments);
        } catch (IllegalArgumentException invalid) {
            source.sendError(Text.literal("[InGame Creator] " + invalid.getMessage()));
            return 0;
        }

        RuntimePlayer runtimePlayer = new RuntimePlayer(player.getUuid(), player.getName().getString(), player.getDisplayName().getString());
        ProjectRuntimeHooks hooks = ProjectRuntimeHooks.builder()
                .commandListSupplier(() -> {
                    try { return runtime.commandNames(projectId); }
                    catch (IOException ignored) { return List.of(); }
                })
                .commandExecutor(commandText -> executeNested(source, commandText))
                .messageSink((target, text) -> source.sendFeedback(() -> Text.literal(text), false))
                .build();
        ProjectRuntimeService.RunResult result = runtime.run(projectId, command.name(), runtimePlayer, arguments, hooks);
        return report(source, result);
    }

    private int executeNested(ServerCommandSource source, String commandText) {
        String command = commandText == null ? "" : commandText.trim();
        while (command.startsWith("/")) command = command.substring(1);
        if (command.isBlank()) return 0;
        try {
            return source.getServer().getCommandManager().getDispatcher().execute(command, source);
        } catch (Exception failure) {
            source.sendError(Text.literal("[InGame Creator] Nested command failed: " + safeMessage(failure)));
            return 0;
        }
    }

    private static int report(ServerCommandSource source, ProjectRuntimeService.RunResult result) {
        return switch (result.status()) {
            case SUCCESS -> 1;
            case COOLDOWN -> {
                long millis = result.retryAfterMillis();
                long tenths = Math.max(1L, (millis + 99L) / 100L);
                source.sendError(Text.literal("[InGame Creator] Wait another " + (tenths / 10.0) + " s."));
                yield 0;
            }
            case DENIED -> {
                source.sendError(Text.literal("[InGame Creator] Access denied: " + result.message()));
                yield 0;
            }
            default -> {
                source.sendError(Text.literal("[InGame Creator] " + result.message()));
                yield 0;
            }
        };
    }

    private static CommandDefinition findCommand(CreatorProject project, String requested) {
        String normalized;
        try { normalized = CommandDefinition.normalizeName(requested); }
        catch (IllegalArgumentException invalid) { return null; }
        CommandDefinition direct = project.commands().get(normalized);
        if (direct != null) return direct;
        for (CommandDefinition command : project.commands().values()) {
            if (command.aliases().contains(normalized)) return command;
        }
        return null;
    }

    private static String safeMessage(Throwable failure) {
        return failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
    }

    public record RegisterReport(int registered, int skipped, List<String> errors) {}
    private record RegisterOne(boolean registered, boolean skipped, String error) {}
}
