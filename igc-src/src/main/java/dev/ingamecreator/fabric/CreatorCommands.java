package dev.ingamecreator.fabric;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.ingamecreator.core.project.CreatorProject;
import dev.ingamecreator.core.project.ProjectStore;
import dev.ingamecreator.core.roles.RoleService;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class CreatorCommands {
    private CreatorCommands() {}

    public static void register(ProjectStore projects, RoleService roles, GeneratedCommandRegistrar generated) {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("igc")
                    .then(literal("reload")
                            .requires(source -> canManage(source, roles))
                            .executes(ctx -> reload(ctx.getSource(), generated)))
                    .then(literal("projects")
                            .executes(ctx -> listProjects(ctx.getSource(), projects)))
                    .then(literal("give")
                            .then(argument("project", StringArgumentType.word())
                                    .suggests((ctx, builder) -> {
                                        try { return CommandSource.suggestMatching(projects.listProjectIds(), builder); }
                                        catch (Exception ignored) { return builder.buildFuture(); }
                                    })
                                    .then(argument("item", StringArgumentType.word())
                                            .suggests((ctx, builder) -> {
                                                try {
                                                    CreatorProject project = projects.load(StringArgumentType.getString(ctx, "project"));
                                                    return CommandSource.suggestMatching(project.runtimeItems().keySet(), builder);
                                                } catch (Exception ignored) {
                                                    return builder.buildFuture();
                                                }
                                            })
                                            .executes(ctx -> giveRuntimeItem(
                                                    ctx.getSource(), projects,
                                                    StringArgumentType.getString(ctx, "project"),
                                                    StringArgumentType.getString(ctx, "item"))))))
                    .then(literal("giveblock")
                            .then(argument("project", StringArgumentType.word())
                                    .suggests((ctx, builder) -> {
                                        try { return CommandSource.suggestMatching(projects.listProjectIds(), builder); }
                                        catch (Exception ignored) { return builder.buildFuture(); }
                                    })
                                    .then(argument("block", StringArgumentType.word())
                                            .suggests((ctx, builder) -> {
                                                try {
                                                    CreatorProject project = projects.load(StringArgumentType.getString(ctx, "project"));
                                                    return CommandSource.suggestMatching(project.runtimeBlocks().keySet(), builder);
                                                } catch (Exception ignored) {
                                                    return builder.buildFuture();
                                                }
                                            })
                                            .executes(ctx -> giveRuntimeBlock(
                                                    ctx.getSource(), projects,
                                                    StringArgumentType.getString(ctx, "project"),
                                                    StringArgumentType.getString(ctx, "block"))))))
                    .then(literal("run")
                            .then(argument("project", StringArgumentType.word())
                                    .suggests((ctx, builder) -> {
                                        try { return CommandSource.suggestMatching(projects.listProjectIds(), builder); }
                                        catch (Exception ignored) { return builder.buildFuture(); }
                                    })
                                    .then(argument("command", StringArgumentType.word())
                                            .executes(ctx -> generated.executeDirect(
                                                    ctx.getSource(),
                                                    StringArgumentType.getString(ctx, "project"),
                                                    StringArgumentType.getString(ctx, "command"),
                                                    ""))
                                            .then(argument("args", StringArgumentType.greedyString())
                                                    .executes(ctx -> generated.executeDirect(
                                                            ctx.getSource(),
                                                            StringArgumentType.getString(ctx, "project"),
                                                            StringArgumentType.getString(ctx, "command"),
                                                            StringArgumentType.getString(ctx, "args"))))))));
            GeneratedCommandRegistrar.RegisterReport report = generated.registerAll(dispatcher);
            if (!report.errors().isEmpty()) {
                System.err.println("[InGame Creator] Generated command registration: " + String.join(" | ", report.errors()));
            }
        });
    }

    private static int giveRuntimeItem(ServerCommandSource source, ProjectStore projects, String projectId, String itemId) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("[InGame Creator] /igc give requires a player source."));
            return 0;
        }
        try {
            CreatorProject project = projects.load(projectId);
            var definition = project.runtimeItem(itemId).orElse(null);
            if (definition == null) {
                source.sendError(Text.literal("[InGame Creator] Runtime item not found: " + itemId));
                return 0;
            }
            ItemStack stack = RuntimeItemStacks.create(project.id(), definition);
            if (!player.giveItemStack(stack)) {
                source.sendError(Text.literal("[InGame Creator] No inventory space for " + definition.displayName()));
                return 0;
            }
            source.sendFeedback(() -> Text.literal("[InGame Creator] Given " + definition.displayName()
                    + " (" + project.id() + ":" + definition.id() + ")"), false);
            return 1;
        } catch (Exception failure) {
            source.sendError(Text.literal("[InGame Creator] Failed to give runtime item: " + failure.getMessage()));
            return 0;
        }
    }

    private static int giveRuntimeBlock(ServerCommandSource source, ProjectStore projects, String projectId, String blockId) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("[InGame Creator] /igc giveblock requires a player source."));
            return 0;
        }
        try {
            CreatorProject project = projects.load(projectId);
            var definition = project.runtimeBlock(blockId).orElse(null);
            if (definition == null) {
                source.sendError(Text.literal("[InGame Creator] Runtime block not found: " + blockId));
                return 0;
            }
            ItemStack stack = RuntimeBlockStacks.create(project.id(), definition);
            if (!player.giveItemStack(stack)) {
                source.sendError(Text.literal("[InGame Creator] No inventory space for " + definition.displayName()));
                return 0;
            }
            source.sendFeedback(() -> Text.literal("[InGame Creator] Given block " + definition.displayName()
                    + " (" + project.id() + ":" + definition.id() + ")"), false);
            return 1;
        } catch (Exception failure) {
            source.sendError(Text.literal("[InGame Creator] Failed to give runtime block: " + failure.getMessage()));
            return 0;
        }
    }

    private static int reload(ServerCommandSource source, GeneratedCommandRegistrar generated) {
        GeneratedCommandRegistrar.RegisterReport report = generated.registerAll(
                source.getServer().getCommandManager().getDispatcher());
        source.sendFeedback(() -> Text.literal("[InGame Creator] Command reload: registered "
                + report.registered() + ", skipped " + report.skipped() + ", errors " + report.errors().size()), false);
        for (String error : report.errors()) source.sendError(Text.literal("[InGame Creator] " + error));
        return report.errors().isEmpty() ? 1 : 0;
    }

    private static int listProjects(ServerCommandSource source, ProjectStore projects) {
        try {
            String joined = String.join(", ", projects.listProjectIds());
            source.sendFeedback(() -> Text.literal("[InGame Creator] Projects: " + (joined.isBlank() ? "none" : joined)), false);
            return 1;
        } catch (Exception failure) {
            source.sendError(Text.literal("[InGame Creator] Failed to read projects: " + failure.getMessage()));
            return 0;
        }
    }

    private static boolean canManage(ServerCommandSource source, RoleService roles) {
        if (source.hasPermissionLevel(2)) return true;
        ServerPlayerEntity player = source.getPlayer();
        return player != null && roles.hasRole(player.getUuid(), "admin");
    }
}
