package dev.ingamecreator.core.command;

import dev.ingamecreator.core.roles.RoleService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class CommandDefinition {
    private static final Pattern NAME_PATTERN = Pattern.compile("[a-z][a-z0-9_-]{0,31}");

    private final String name;
    private final List<String> aliases;
    private final List<CommandArgument> arguments;
    private final Set<String> allowedRoles;
    private final int cooldownSeconds;
    private final String blueprintHandlerId;
    private final List<CommandDefinition> subcommands;

    private CommandDefinition(Builder builder) {
        this.name = normalizeName(builder.name);
        this.aliases = List.copyOf(builder.aliases);
        this.arguments = List.copyOf(builder.arguments);
        this.allowedRoles = Collections.unmodifiableSet(new LinkedHashSet<>(builder.allowedRoles));
        this.cooldownSeconds = builder.cooldownSeconds;
        this.blueprintHandlerId = builder.blueprintHandlerId;
        this.subcommands = List.copyOf(builder.subcommands);
    }

    public static Builder builder(String name) { return new Builder(name); }

    public String name() { return name; }
    public List<String> aliases() { return aliases; }
    public List<CommandArgument> arguments() { return arguments; }
    public Set<String> allowedRoles() { return allowedRoles; }
    public int cooldownSeconds() { return cooldownSeconds; }
    public String blueprintHandlerId() { return blueprintHandlerId; }
    public List<CommandDefinition> subcommands() { return subcommands; }

    public static String normalizeName(String value) {
        if (value == null) throw new IllegalArgumentException("command name is required");
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        while (normalized.startsWith("/")) normalized = normalized.substring(1);
        if (!NAME_PATTERN.matcher(normalized).matches()) throw new IllegalArgumentException("invalid command name: " + value);
        return normalized;
    }

    public static final class Builder {
        private final String name;
        private final List<String> aliases = new ArrayList<>();
        private final List<CommandArgument> arguments = new ArrayList<>();
        private final Set<String> allowedRoles = new LinkedHashSet<>();
        private int cooldownSeconds;
        private String blueprintHandlerId = "";
        private final List<CommandDefinition> subcommands = new ArrayList<>();

        private Builder(String name) { this.name = name; }

        public Builder alias(String alias) {
            aliases.add(normalizeName(alias));
            return this;
        }

        public Builder argument(CommandArgument argument) {
            if (argument == null) throw new IllegalArgumentException("argument is required");
            arguments.add(argument);
            return this;
        }

        public Builder allowedRole(String role) {
            allowedRoles.add(RoleService.normalizeRoleName(role));
            return this;
        }

        public Builder cooldownSeconds(int seconds) {
            if (seconds < 0) throw new IllegalArgumentException("cooldown cannot be negative");
            this.cooldownSeconds = seconds;
            return this;
        }

        public Builder blueprintHandler(String id) {
            this.blueprintHandlerId = id == null ? "" : id.trim();
            return this;
        }

        public Builder subcommand(CommandDefinition subcommand) {
            if (subcommand == null) throw new IllegalArgumentException("subcommand is required");
            subcommands.add(subcommand);
            return this;
        }

        public CommandDefinition build() { return new CommandDefinition(this); }
    }
}
