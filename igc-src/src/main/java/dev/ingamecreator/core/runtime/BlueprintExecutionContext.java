package dev.ingamecreator.core.runtime;

import dev.ingamecreator.core.roles.RoleService;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class BlueprintExecutionContext {
    private final RuntimePlayer commandUser;
    private final RoleService roles;
    private final Map<String, String> arguments;
    private final Supplier<List<String>> commandListSupplier;
    private final Function<String, Integer> commandExecutor;
    private final BiConsumer<RuntimePlayer, String> messageSink;
    private final Function<String, Object> functionInvoker;
    private final int maxSteps;

    private BlueprintExecutionContext(Builder builder) {
        this.commandUser = builder.commandUser;
        this.roles = builder.roles;
        this.arguments = Map.copyOf(builder.arguments);
        this.commandListSupplier = builder.commandListSupplier;
        this.commandExecutor = builder.commandExecutor;
        this.messageSink = builder.messageSink;
        this.functionInvoker = builder.functionInvoker;
        this.maxSteps = builder.maxSteps;
    }

    public static Builder builder(RuntimePlayer commandUser, RoleService roles) {
        return new Builder(commandUser, roles);
    }

    public RuntimePlayer commandUser() { return commandUser; }
    public RoleService roles() { return roles; }
    public Map<String, String> arguments() { return arguments; }
    public List<String> commandList() { return List.copyOf(commandListSupplier.get()); }
    public int executeCommand(String command) { return commandExecutor.apply(command); }
    public void sendMessage(RuntimePlayer target, String text) { messageSink.accept(target, text); }
    public Object invokeFunction(String name) { return functionInvoker.apply(name); }
    public int maxSteps() { return maxSteps; }

    public static final class Builder {
        private final RuntimePlayer commandUser;
        private final RoleService roles;
        private Map<String, String> arguments = Map.of();
        private Supplier<List<String>> commandListSupplier = List::of;
        private Function<String, Integer> commandExecutor = ignored -> 0;
        private BiConsumer<RuntimePlayer, String> messageSink = (ignoredPlayer, ignoredText) -> {};
        private Function<String, Object> functionInvoker = ignored -> null;
        private int maxSteps = 1000;

        private Builder(RuntimePlayer commandUser, RoleService roles) {
            this.commandUser = Objects.requireNonNull(commandUser, "commandUser");
            this.roles = Objects.requireNonNull(roles, "roles");
        }

        public Builder arguments(Map<String, String> arguments) {
            this.arguments = arguments == null ? Map.of() : Map.copyOf(arguments);
            return this;
        }

        public Builder commandListSupplier(Supplier<List<String>> supplier) {
            this.commandListSupplier = Objects.requireNonNull(supplier, "supplier");
            return this;
        }

        public Builder commandExecutor(Function<String, Integer> executor) {
            this.commandExecutor = Objects.requireNonNull(executor, "executor");
            return this;
        }

        public Builder messageSink(BiConsumer<RuntimePlayer, String> sink) {
            this.messageSink = Objects.requireNonNull(sink, "sink");
            return this;
        }

        public Builder functionInvoker(Function<String, Object> invoker) {
            this.functionInvoker = Objects.requireNonNull(invoker, "invoker");
            return this;
        }

        public Builder maxSteps(int maxSteps) {
            if (maxSteps < 1) throw new IllegalArgumentException("maxSteps must be positive");
            this.maxSteps = maxSteps;
            return this;
        }

        public BlueprintExecutionContext build() { return new BlueprintExecutionContext(this); }
    }
}
