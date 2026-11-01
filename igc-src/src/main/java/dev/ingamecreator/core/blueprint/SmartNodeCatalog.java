package dev.ingamecreator.core.blueprint;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class SmartNodeCatalog {
    private final Map<String, NodeType> types;

    public SmartNodeCatalog(List<NodeType> types) {
        LinkedHashMap<String, NodeType> map = new LinkedHashMap<>();
        for (NodeType type : types) {
            if (map.putIfAbsent(type.id(), type) != null) throw new IllegalArgumentException("duplicate node type: " + type.id());
        }
        this.types = Map.copyOf(map);
    }

    public Optional<NodeType> find(String id) { return Optional.ofNullable(types.get(id)); }
    public List<NodeType> all() { return List.copyOf(types.values()); }

    public List<NodeType> search(String query) {
        String q = query == null ? "" : query.toLowerCase(Locale.ROOT).trim();
        if (q.isEmpty()) return all();
        String singular = q.endsWith("s") && q.length() > 3 ? q.substring(0, q.length() - 1) : q;
        return types.values().stream()
                .filter(type -> {
                    String haystack = (type.id() + " " + type.title() + " " + type.category() + " " + type.description())
                            .toLowerCase(Locale.ROOT);
                    return haystack.contains(q) || (!singular.equals(q) && haystack.contains(singular));
                })
                .toList();
    }

    public static SmartNodeCatalog standard() {
        List<NodeType> nodes = new ArrayList<>();
        nodes.add(node("event.command", "When Command Executes", "Events", "Entry point for a generated command",
                out("exec", "Exec", PortType.EXEC), out("context", "Command Context", PortType.COMMAND_CONTEXT)));
        nodes.add(node("event.item.right_click", "When Item Right Clicked", "Events", "Runs when the runtime item is used",
                out("exec", "Exec", PortType.EXEC), out("player", "Player", PortType.PLAYER)));
        nodes.add(node("event.item.inventory_tick", "When Item Ticks", "Events", "Runs for an item inventory tick",
                out("exec", "Exec", PortType.EXEC), out("player", "Player", PortType.PLAYER)));
        nodes.add(node("event.block.placed", "When Block Placed", "Events", "Runs when a runtime block is placed",
                out("exec", "Exec", PortType.EXEC), out("player", "Player", PortType.PLAYER)));
        nodes.add(node("event.block.broken", "When Block Broken", "Events", "Runs when a runtime block is broken",
                out("exec", "Exec", PortType.EXEC), out("player", "Player", PortType.PLAYER)));
        nodes.add(node("event.block.right_click", "When Block Right Clicked", "Events", "Runs when a runtime block is used",
                out("exec", "Exec", PortType.EXEC), out("player", "Player", PortType.PLAYER)));
        nodes.add(node("command.user", "Get Command User", "Commands", "Gets the player who executed the command",
                in("context", "Context", PortType.COMMAND_CONTEXT), out("player", "Player", PortType.PLAYER)));
        nodes.add(node("player.name", "Get Player Name", "Player", "Returns the account username for a player",
                in("player", "Player", PortType.PLAYER), out("name", "Name", PortType.TEXT)));
        nodes.add(node("player.display_name", "Get Player Display Name", "Player", "Returns the formatted display name for a player",
                in("player", "Player", PortType.PLAYER), out("name", "Display Name", PortType.TEXT)));
        nodes.add(node("command.arguments", "Get Command Arguments", "Commands", "Returns the current command argument values",
                in("context", "Context", PortType.COMMAND_CONTEXT), out("arguments", "Arguments", PortType.ANY)));
        nodes.add(node("command.list", "Get Command List", "Commands", "Gets the available command ids for a user/context",
                in("context", "Context", PortType.COMMAND_CONTEXT), out("commands", "Commands", PortType.COMMANDS)));
        nodes.add(node("command.execute", "Execute Command", "Commands", "Executes a command string",
                in("exec", "Exec", PortType.EXEC), in("command", "Command", PortType.TEXT), out("then", "Then", PortType.EXEC)));
        nodes.add(node("role.get", "Get User Role", "Roles & Permissions", "Gets one effective role for a player (explicit role first, otherwise default)",
                in("player", "Player", PortType.PLAYER), out("role", "Role", PortType.ROLE)));
        nodes.add(node("role.user_roles", "Get User Roles", "Roles & Permissions", "Gets every role assigned to a player",
                in("player", "Player", PortType.PLAYER), out("roles", "Roles", PortType.ROLES)));
        nodes.add(node("role.has", "Has Role", "Roles & Permissions", "Checks whether a player has a role",
                in("player", "Player", PortType.PLAYER), in("role", "Role", PortType.ROLE), out("result", "Has Role", PortType.BOOLEAN)));
        nodes.add(node("role.add", "Add Role", "Roles & Permissions", "Adds a role to a player",
                in("exec", "Exec", PortType.EXEC), in("player", "Player", PortType.PLAYER), in("role", "Role", PortType.ROLE), out("then", "Then", PortType.EXEC)));
        nodes.add(node("role.remove", "Remove Role", "Roles & Permissions", "Removes a role from a player",
                in("exec", "Exec", PortType.EXEC), in("player", "Player", PortType.PLAYER), in("role", "Role", PortType.ROLE), out("then", "Then", PortType.EXEC)));
        nodes.add(node("role.list", "Get All Roles", "Roles & Permissions", "Gets all registered roles",
                out("roles", "Roles", PortType.ROLES)));
        nodes.add(node("flow.branch", "Branch", "Flow Control", "If/Else execution branch",
                in("exec", "Exec", PortType.EXEC), in("condition", "Condition", PortType.BOOLEAN), out("true", "True", PortType.EXEC), out("false", "False", PortType.EXEC)));
        nodes.add(node("chat.send", "Send Message", "Player", "Sends text to a player",
                in("exec", "Exec", PortType.EXEC), in("player", "Player", PortType.PLAYER), in("text", "Text", PortType.TEXT), out("then", "Then", PortType.EXEC)));
        nodes.add(node("value.text", "Text", "Text", "Text literal", out("value", "Value", PortType.TEXT)));
        nodes.add(node("value.role", "Role", "Roles & Permissions", "Role literal", out("value", "Role", PortType.ROLE)));
        nodes.add(node("function.call", "Call Function", "Functions", "Calls a user-defined Blueprint function",
                in("exec", "Exec", PortType.EXEC), out("then", "Then", PortType.EXEC), out("result", "Result", PortType.ANY)));
        return new SmartNodeCatalog(nodes);
    }

    private static NodeType node(String id, String title, String category, String description, PortDefinition... ports) {
        return new NodeType(id, title, category, List.of(ports), description);
    }

    private static PortDefinition in(String id, String name, PortType type) {
        return new PortDefinition(id, name, type, PortDirection.INPUT, false);
    }

    private static PortDefinition out(String id, String name, PortType type) {
        return new PortDefinition(id, name, type, PortDirection.OUTPUT, true);
    }
}
