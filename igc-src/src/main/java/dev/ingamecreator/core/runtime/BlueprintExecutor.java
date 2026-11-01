package dev.ingamecreator.core.runtime;

import dev.ingamecreator.core.blueprint.BlueprintEdge;
import dev.ingamecreator.core.blueprint.BlueprintGraph;
import dev.ingamecreator.core.blueprint.BlueprintNode;
import dev.ingamecreator.core.roles.RoleService;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class BlueprintExecutor {
    public BlueprintExecutionResult execute(BlueprintGraph graph, BlueprintExecutionContext context) {
        if (graph == null) throw new IllegalArgumentException("graph is required");
        if (context == null) throw new IllegalArgumentException("context is required");
        Session session = new Session(graph, context);
        List<BlueprintNode> events = graph.nodes().values().stream()
                .filter(node -> node.typeId().startsWith("event."))
                .toList();
        if (events.isEmpty()) return BlueprintExecutionResult.failure(0, List.of("missing event node"));
        try {
            for (BlueprintNode event : events) session.followExec(event.id(), "exec");
        } catch (ExecutionFailure failure) {
            session.diagnostics.add(failure.getMessage());
        } catch (RuntimeException failure) {
            session.diagnostics.add("runtime error: " + safeMessage(failure));
        }
        return session.diagnostics.isEmpty()
                ? BlueprintExecutionResult.success(session.steps)
                : BlueprintExecutionResult.failure(session.steps, session.diagnostics);
    }

    public Object evaluateOutput(BlueprintGraph graph, UUID nodeId, String portId, BlueprintExecutionContext context) {
        if (graph == null || context == null) throw new IllegalArgumentException("graph/context are required");
        Session session = new Session(graph, context);
        return session.evaluateOutput(nodeId, portId);
    }

    private static String safeMessage(Throwable throwable) {
        return throwable.getMessage() == null ? throwable.getClass().getSimpleName() : throwable.getMessage();
    }

    private static final class Session {
        private final BlueprintGraph graph;
        private final BlueprintExecutionContext context;
        private final Map<ValueKey, Object> memo = new HashMap<>();
        private final Set<ValueKey> evaluating = new HashSet<>();
        private final List<String> diagnostics = new ArrayList<>();
        private int steps;

        private Session(BlueprintGraph graph, BlueprintExecutionContext context) {
            this.graph = graph;
            this.context = context;
        }

        private void followExec(UUID fromNode, String fromPort) {
            for (BlueprintEdge edge : outgoing(fromNode, fromPort)) {
                executeNode(requireNode(edge.toNode()));
            }
        }

        private void executeNode(BlueprintNode node) {
            steps++;
            if (steps > context.maxSteps()) throw new ExecutionFailure("step limit exceeded (" + context.maxSteps() + ")");
            switch (node.typeId()) {
                case "flow.branch" -> {
                    boolean condition = requireBoolean(input(node, "condition"), node, "condition");
                    followExec(node.id(), condition ? "true" : "false");
                }
                case "chat.send" -> {
                    RuntimePlayer player = optionalPlayer(inputOrNull(node, "player"), context.commandUser());
                    String text = optionalText(inputOrNull(node, "text"), node.properties().getOrDefault("text", ""));
                    context.sendMessage(player, text);
                    followExec(node.id(), "then");
                }
                case "role.add" -> {
                    RuntimePlayer player = requirePlayer(input(node, "player"), node, "player");
                    String role = requireText(input(node, "role"), node, "role");
                    context.roles().addRole(player.id(), role);
                    memo.clear();
                    followExec(node.id(), "then");
                }
                case "role.remove" -> {
                    RuntimePlayer player = requirePlayer(input(node, "player"), node, "player");
                    String role = requireText(input(node, "role"), node, "role");
                    context.roles().removeRole(player.id(), role);
                    memo.clear();
                    followExec(node.id(), "then");
                }
                case "command.execute" -> {
                    String command = requireText(input(node, "command"), node, "command");
                    int result = context.executeCommand(command);
                    if (result <= 0) throw new ExecutionFailure("command failed: " + command);
                    followExec(node.id(), "then");
                }
                case "function.call" -> {
                    String function = node.properties().getOrDefault("function", node.properties().getOrDefault("name", ""));
                    if (function.isBlank()) throw new ExecutionFailure("function.call missing function property");
                    context.invokeFunction(function);
                    followExec(node.id(), "then");
                }
                default -> throw new ExecutionFailure("node is not executable: " + node.typeId());
            }
        }

        private Object evaluateOutput(UUID nodeId, String portId) {
            ValueKey key = new ValueKey(nodeId, portId);
            if (memo.containsKey(key)) return memo.get(key);
            if (!evaluating.add(key)) throw new ExecutionFailure("data cycle at " + nodeId + "." + portId);
            try {
                BlueprintNode node = requireNode(nodeId);
                Object result = switch (node.typeId()) {
                    case "event.command" -> {
                        if (!"context".equals(portId)) throw invalidOutput(node, portId);
                        yield context;
                    }
                    case "event.item.right_click", "event.item.inventory_tick", "event.item.picked_up",
                         "event.block.placed", "event.block.broken", "event.block.right_click" -> {
                        if (!"player".equals(portId)) throw invalidOutput(node, portId);
                        yield context.commandUser();
                    }
                    case "command.user" -> {
                        if (!"player".equals(portId)) throw invalidOutput(node, portId);
                        yield context.commandUser();
                    }
                    case "player.name" -> {
                        if (!"name".equals(portId)) throw invalidOutput(node, portId);
                        RuntimePlayer player = requirePlayer(input(node, "player"), node, "player");
                        yield player.name();
                    }
                    case "player.display_name" -> {
                        if (!"name".equals(portId)) throw invalidOutput(node, portId);
                        RuntimePlayer player = requirePlayer(input(node, "player"), node, "player");
                        yield player.displayName();
                    }
                    case "command.arguments" -> {
                        if (!"arguments".equals(portId)) throw invalidOutput(node, portId);
                        yield context.arguments();
                    }
                    case "command.list" -> {
                        if (!"commands".equals(portId)) throw invalidOutput(node, portId);
                        yield context.commandList();
                    }
                    case "role.get" -> {
                        if (!"role".equals(portId)) throw invalidOutput(node, portId);
                        RuntimePlayer player = requirePlayer(input(node, "player"), node, "player");
                        yield primaryRole(context.roles(), player.id());
                    }
                    case "role.user_roles" -> {
                        if (!"roles".equals(portId)) throw invalidOutput(node, portId);
                        RuntimePlayer player = requirePlayer(input(node, "player"), node, "player");
                        yield context.roles().getRoles(player.id());
                    }
                    case "role.has" -> {
                        if (!"result".equals(portId)) throw invalidOutput(node, portId);
                        RuntimePlayer player = requirePlayer(input(node, "player"), node, "player");
                        String role = requireText(input(node, "role"), node, "role");
                        yield context.roles().hasRole(player.id(), role);
                    }
                    case "role.list" -> {
                        if (!"roles".equals(portId)) throw invalidOutput(node, portId);
                        yield context.roles().roles();
                    }
                    case "value.text", "value.role" -> {
                        if (!"value".equals(portId)) throw invalidOutput(node, portId);
                        yield node.properties().getOrDefault("value", "");
                    }
                    case "function.call" -> {
                        if (!"result".equals(portId)) throw invalidOutput(node, portId);
                        String function = node.properties().getOrDefault("function", node.properties().getOrDefault("name", ""));
                        if (function.isBlank()) throw new ExecutionFailure("function.call missing function property");
                        yield context.invokeFunction(function);
                    }
                    default -> throw new ExecutionFailure("node has no runtime evaluator: " + node.typeId());
                };
                memo.put(key, result);
                return result;
            } finally {
                evaluating.remove(key);
            }
        }

        private Object input(BlueprintNode node, String port) {
            Object value = inputOrNull(node, port);
            if (value == null) throw new ExecutionFailure("missing input " + node.typeId() + "." + port);
            return value;
        }

        private Object inputOrNull(BlueprintNode node, String port) {
            List<BlueprintEdge> incoming = graph.edges().stream()
                    .filter(edge -> edge.toNode().equals(node.id()) && edge.toPort().equals(port))
                    .toList();
            if (incoming.isEmpty()) return null;
            if (incoming.size() > 1) throw new ExecutionFailure("multiple data inputs for " + node.typeId() + "." + port);
            BlueprintEdge edge = incoming.get(0);
            return evaluateOutput(edge.fromNode(), edge.fromPort());
        }

        private List<BlueprintEdge> outgoing(UUID nodeId, String portId) {
            return graph.edges().stream()
                    .filter(edge -> edge.fromNode().equals(nodeId) && edge.fromPort().equals(portId))
                    .toList();
        }

        private BlueprintNode requireNode(UUID nodeId) {
            BlueprintNode node = graph.nodes().get(nodeId);
            if (node == null) throw new ExecutionFailure("missing node: " + nodeId);
            return node;
        }
    }

    private static String primaryRole(RoleService roles, UUID playerId) {
        LinkedHashSet<String> values = new LinkedHashSet<>(roles.getRoles(playerId));
        values.remove(RoleService.DEFAULT_ROLE);
        return values.stream().findFirst().orElse(RoleService.DEFAULT_ROLE);
    }

    private static RuntimePlayer requirePlayer(Object value, BlueprintNode node, String port) {
        if (value instanceof RuntimePlayer player) return player;
        throw new ExecutionFailure("expected player at " + node.typeId() + "." + port);
    }

    private static RuntimePlayer optionalPlayer(Object value, RuntimePlayer fallback) {
        return value == null ? fallback : (RuntimePlayer) value;
    }

    private static String requireText(Object value, BlueprintNode node, String port) {
        if (value instanceof String text) return text;
        throw new ExecutionFailure("expected text at " + node.typeId() + "." + port);
    }

    private static String optionalText(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }

    private static boolean requireBoolean(Object value, BlueprintNode node, String port) {
        if (value instanceof Boolean bool) return bool;
        throw new ExecutionFailure("expected boolean at " + node.typeId() + "." + port);
    }

    private static ExecutionFailure invalidOutput(BlueprintNode node, String portId) {
        return new ExecutionFailure("invalid output " + node.typeId() + "." + portId);
    }

    private record ValueKey(UUID nodeId, String portId) {}

    private static final class ExecutionFailure extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private ExecutionFailure(String message) { super(message); }
    }
}
