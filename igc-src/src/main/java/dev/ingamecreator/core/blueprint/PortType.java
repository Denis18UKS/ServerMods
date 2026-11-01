package dev.ingamecreator.core.blueprint;

public enum PortType {
    EXEC, BOOLEAN, NUMBER, TEXT, PLAYER, ENTITY, ITEM, BLOCK, WORLD, POSITION,
    COMMAND_CONTEXT, ROLE, ROLES, COMMANDS, ANY;

    public boolean compatibleWith(PortType other) {
        return this == ANY || other == ANY || this == other;
    }
}
