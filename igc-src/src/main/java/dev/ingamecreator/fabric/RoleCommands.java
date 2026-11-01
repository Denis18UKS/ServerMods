package dev.ingamecreator.fabric;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.ingamecreator.core.roles.RoleCommandFacade;
import dev.ingamecreator.core.roles.RoleService;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class RoleCommands {
    private RoleCommands() {}

    public static void register(RoleService roles) {
        RoleCommandFacade facade = new RoleCommandFacade(roles);
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                literal("role")
                        .then(literal("status")
                                .executes(ctx -> selfStatus(ctx.getSource(), facade))
                                .then(argument("player", EntityArgumentType.player())
                                        .requires(source -> canManage(source, roles))
                                        .executes(ctx -> playerStatus(ctx.getSource(), EntityArgumentType.getPlayer(ctx, "player"), facade))))
                        .then(literal("list")
                                .executes(ctx -> selfStatus(ctx.getSource(), facade))
                                .then(argument("player", EntityArgumentType.player())
                                        .requires(source -> canManage(source, roles))
                                        .executes(ctx -> playerStatus(ctx.getSource(), EntityArgumentType.getPlayer(ctx, "player"), facade))))
                        .then(literal("add")
                                .requires(source -> canManage(source, roles))
                                .then(argument("player", EntityArgumentType.player())
                                        .then(argument("role", StringArgumentType.word())
                                                .suggests((ctx, builder) -> CommandSource.suggestMatching(roles.roles(), builder))
                                                .executes(ctx -> mutate(ctx.getSource(), facade.add(
                                                        EntityArgumentType.getPlayer(ctx, "player").getUuid(),
                                                        EntityArgumentType.getPlayer(ctx, "player").getName().getString(),
                                                        StringArgumentType.getString(ctx, "role")))))))
                        .then(literal("remove")
                                .requires(source -> canManage(source, roles))
                                .then(argument("player", EntityArgumentType.player())
                                        .then(argument("role", StringArgumentType.word())
                                                .suggests((ctx, builder) -> CommandSource.suggestMatching(roles.roles(), builder))
                                                .executes(ctx -> mutate(ctx.getSource(), facade.remove(
                                                        EntityArgumentType.getPlayer(ctx, "player").getUuid(),
                                                        EntityArgumentType.getPlayer(ctx, "player").getName().getString(),
                                                        StringArgumentType.getString(ctx, "role")))))))
                        .then(literal("set")
                                .requires(source -> canManage(source, roles))
                                .then(argument("player", EntityArgumentType.player())
                                        .then(argument("role", StringArgumentType.word())
                                                .suggests((ctx, builder) -> CommandSource.suggestMatching(roles.roles(), builder))
                                                .executes(ctx -> mutate(ctx.getSource(), facade.set(
                                                        EntityArgumentType.getPlayer(ctx, "player").getUuid(),
                                                        EntityArgumentType.getPlayer(ctx, "player").getName().getString(),
                                                        StringArgumentType.getString(ctx, "role")))))))
                        .then(literal("clear")
                                .requires(source -> canManage(source, roles))
                                .then(argument("player", EntityArgumentType.player())
                                        .executes(ctx -> {
                                            ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "player");
                                            return mutate(ctx.getSource(), facade.clear(player.getUuid(), player.getName().getString()));
                                        })))
                        .then(literal("create")
                                .requires(source -> canManage(source, roles))
                                .then(argument("role", StringArgumentType.word())
                                        .executes(ctx -> mutate(ctx.getSource(), facade.create(StringArgumentType.getString(ctx, "role"))))))
                        .then(literal("delete")
                                .requires(source -> canManage(source, roles))
                                .then(argument("role", StringArgumentType.word())
                                        .suggests((ctx, builder) -> CommandSource.suggestMatching(roles.roles(), builder))
                                        .executes(ctx -> mutate(ctx.getSource(), facade.delete(StringArgumentType.getString(ctx, "role"))))))
                        .then(literal("info")
                                .then(argument("role", StringArgumentType.word())
                                        .suggests((ctx, builder) -> CommandSource.suggestMatching(roles.roles(), builder))
                                        .executes(ctx -> mutate(ctx.getSource(), facade.info(StringArgumentType.getString(ctx, "role"))))))
        ));
    }

    private static int selfStatus(ServerCommandSource source, RoleCommandFacade facade) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Эту форму команды должен выполнить игрок. Используй /role status <player>."));
            return 0;
        }
        return mutate(source, facade.status(player.getUuid(), player.getName().getString()));
    }

    private static int playerStatus(ServerCommandSource source, ServerPlayerEntity player, RoleCommandFacade facade) {
        return mutate(source, facade.status(player.getUuid(), player.getName().getString()));
    }

    private static int mutate(ServerCommandSource source, RoleCommandFacade.Result result) {
        if (result.success()) {
            source.sendFeedback(() -> Text.literal("[InGame Creator] " + result.message()), false);
            return 1;
        }
        source.sendError(Text.literal("[InGame Creator] " + result.message()));
        return 0;
    }

    private static boolean canManage(ServerCommandSource source, RoleService roles) {
        if (source.hasPermissionLevel(2)) return true;
        ServerPlayerEntity player = source.getPlayer();
        return player != null && roles.hasRole(player.getUuid(), "admin");
    }
}
