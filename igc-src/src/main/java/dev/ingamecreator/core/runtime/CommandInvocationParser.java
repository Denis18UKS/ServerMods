package dev.ingamecreator.core.runtime;

import dev.ingamecreator.core.command.CommandArgument;
import dev.ingamecreator.core.command.CommandDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CommandInvocationParser {
    private CommandInvocationParser() {}

    public static Map<String, String> parse(CommandDefinition command, String rawArguments) {
        if (command == null) throw new IllegalArgumentException("command is required");
        List<String> tokens = tokenize(rawArguments == null ? "" : rawArguments);
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        int tokenIndex = 0;
        List<CommandArgument> declared = command.arguments();
        for (int i = 0; i < declared.size(); i++) {
            CommandArgument argument = declared.get(i);
            boolean last = i == declared.size() - 1;
            if (tokenIndex >= tokens.size()) {
                if (argument.required()) throw new IllegalArgumentException("missing required argument: " + argument.name());
                continue;
            }
            if (last && argument.type() == CommandArgument.Type.TEXT) {
                String value = String.join(" ", tokens.subList(tokenIndex, tokens.size()));
                validate(argument, value);
                values.put(argument.name(), value);
                tokenIndex = tokens.size();
            } else {
                String value = tokens.get(tokenIndex++);
                validate(argument, value);
                values.put(argument.name(), value);
            }
        }
        if (tokenIndex < tokens.size()) throw new IllegalArgumentException("too many arguments");
        return Map.copyOf(values);
    }

    private static void validate(CommandArgument argument, String value) {
        try {
            switch (argument.type()) {
                case INTEGER -> Integer.parseInt(value);
                case DOUBLE -> Double.parseDouble(value);
                case BOOLEAN -> {
                    if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                        throw new IllegalArgumentException("invalid boolean");
                    }
                }
                default -> { }
            }
        } catch (NumberFormatException invalid) {
            throw new IllegalArgumentException("invalid " + argument.type().name().toLowerCase() + " for " + argument.name() + ": " + value);
        }
    }

    static List<String> tokenize(String input) {
        ArrayList<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        char quote = 0;
        boolean escaping = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (escaping) {
                current.append(c);
                escaping = false;
                continue;
            }
            if (c == '\\') {
                escaping = true;
                continue;
            }
            if (quote != 0) {
                if (c == quote) quote = 0;
                else current.append(c);
                continue;
            }
            if (c == '"' || c == '\'') {
                quote = c;
                continue;
            }
            if (Character.isWhitespace(c)) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }
        if (escaping) current.append('\\');
        if (quote != 0) throw new IllegalArgumentException("unterminated quote");
        if (current.length() > 0) tokens.add(current.toString());
        return List.copyOf(tokens);
    }
}
