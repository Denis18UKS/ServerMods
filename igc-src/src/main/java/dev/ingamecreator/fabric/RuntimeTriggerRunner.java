package dev.ingamecreator.fabric;

import dev.ingamecreator.InGameCreatorMod;
import dev.ingamecreator.core.content.TriggerBinding;
import dev.ingamecreator.core.runtime.ProjectRuntimeHooks;
import dev.ingamecreator.core.runtime.RuntimePlayer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.io.IOException;
import java.util.List;
import java.util.Map;

final class RuntimeTriggerRunner {
    private RuntimeTriggerRunner() {}

    static void run(ServerPlayerEntity player, String projectId, TriggerBinding.ElementKind kind,
                    String elementId, String eventId, Map<String, String> arguments) {
        try {
            var project = InGameCreatorMod.projects().load(projectId);
            TriggerBinding binding = project.triggers().stream()
                    .filter(t -> t.kind() == kind)
                    .filter(t -> t.elementId().equals(elementId))
                    .filter(t -> t.eventId().equals(eventId))
                    .findFirst().orElse(null);
            if (binding == null) return;
            RuntimePlayer runtimePlayer = new RuntimePlayer(
                    player.getUuid(), player.getName().getString(), player.getDisplayName().getString());
            ProjectRuntimeHooks hooks = ProjectRuntimeHooks.builder()
                    .commandListSupplier(() -> {
                        try { return InGameCreatorMod.runtime().commandNames(projectId); }
                        catch (IOException ignored) { return List.of(); }
                    })
                    .commandExecutor(command -> executeCommand(player, command))
                    .messageSink((target, text) -> player.sendMessage(Text.literal(text), false))
                    .build();
            var result = InGameCreatorMod.runtime().runBlueprint(
                    projectId, binding.blueprintId(), runtimePlayer, arguments, hooks);
            if (!result.success()) {
                player.sendMessage(Text.literal("[InGame Creator] Trigger failed: "
                        + String.join("; ", result.diagnostics())), false);
            }
        } catch (Exception failure) {
            player.sendMessage(Text.literal("[InGame Creator] Trigger error: " + safeMessage(failure)), false);
        }
    }

    private static int executeCommand(ServerPlayerEntity player, String value) {
        String command = value == null ? "" : value.trim();
        while (command.startsWith("/")) command = command.substring(1);
        if (command.isBlank()) return 0;
        try {
            return player.getServer().getCommandManager().getDispatcher().execute(command, player.getCommandSource());
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static String safeMessage(Throwable failure) {
        return failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
    }
}
