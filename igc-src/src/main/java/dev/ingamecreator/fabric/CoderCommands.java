package dev.ingamecreator.fabric;

import dev.ingamecreator.core.roles.RoleService;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class CoderCommands {
    public static final String CODER_ROLE = "coder";

    private CoderCommands() {}

    public static void register(RoleService roles) {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                literal("coder")
                        .then(literal("add")
                                .requires(source -> canManage(source, roles))
                                .then(argument("player", EntityArgumentType.player())
                                        .executes(ctx -> add(ctx.getSource(), EntityArgumentType.getPlayer(ctx, "player"), roles))))
                        .then(literal("remove")
                                .requires(source -> canManage(source, roles))
                                .then(argument("player", EntityArgumentType.player())
                                        .executes(ctx -> remove(ctx.getSource(), EntityArgumentType.getPlayer(ctx, "player"), roles))))
                        .then(literal("status")
                                .executes(ctx -> selfStatus(ctx.getSource(), roles))
                                .then(argument("player", EntityArgumentType.player())
                                        .requires(source -> canView(source, roles))
                                        .executes(ctx -> status(ctx.getSource(), EntityArgumentType.getPlayer(ctx, "player"), roles))))
                        .then(literal("list")
                                .requires(source -> canView(source, roles))
                                .executes(ctx -> list(ctx.getSource(), roles)))
        ));
    }

    private static int add(ServerCommandSource source, ServerPlayerEntity player, RoleService roles) {
        roles.addRole(player.getUuid(), CODER_ROLE);
        source.sendFeedback(() -> Text.literal("[InGame Creator] coder granted to " + player.getName().getString()), true);
        return 1;
    }

    private static int remove(ServerCommandSource source, ServerPlayerEntity player, RoleService roles) {
        roles.removeRole(player.getUuid(), CODER_ROLE);
        source.sendFeedback(() -> Text.literal("[InGame Creator] coder removed from " + player.getName().getString()), true);
        return 1;
    }

    private static int selfStatus(ServerCommandSource source, RoleService roles) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Use /coder status <player> from console."));
            return 0;
        }
        return status(source, player, roles);
    }

    private static int status(ServerCommandSource source, ServerPlayerEntity player, RoleService roles) {
        boolean coder = roles.hasRole(player.getUuid(), CODER_ROLE);
        source.sendFeedback(() -> Text.literal("[InGame Creator] " + player.getName().getString() + ": coder=" + coder), false);
        return coder ? 1 : 0;
    }

    private static int list(ServerCommandSource source, RoleService roles) {
        Set<UUID> ids = roles.usersWithRole(CODER_ROLE);
        List<String> entries = new ArrayList<>();
        java.util.LinkedHashSet<UUID> offline = new java.util.LinkedHashSet<>(ids);
        for (ServerPlayerEntity player : source.getServer().getPlayerManager().getPlayerList()) {
            if (ids.contains(player.getUuid())) {
                entries.add(player.getName().getString() + " (online)");
                offline.remove(player.getUuid());
            }
        }
        offline.stream().map(UUID::toString).sorted().forEach(id -> entries.add(id + " (offline)"));
        entries.sort(Comparator.naturalOrder());
        String text = entries.isEmpty() ? "none" : String.join(", ", entries);
        source.sendFeedback(() -> Text.literal("[InGame Creator] coders: " + text), false);
        return ids.size();
    }

    private static boolean canView(ServerCommandSource source, RoleService roles) {
        if (canManage(source, roles)) return true;
        ServerPlayerEntity player = source.getPlayer();
        return player != null && roles.hasRole(player.getUuid(), CODER_ROLE);
    }

    private static boolean canManage(ServerCommandSource source, RoleService roles) {
        if (source.hasPermissionLevel(2)) return true;
        ServerPlayerEntity player = source.getPlayer();
        return player != null && roles.hasRole(player.getUuid(), "admin");
    }
}
