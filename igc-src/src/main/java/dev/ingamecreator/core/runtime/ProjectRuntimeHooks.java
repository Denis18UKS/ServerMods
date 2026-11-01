package dev.ingamecreator.core.runtime;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ProjectRuntimeHooks {
    private final Supplier<List<String>> commandListSupplier;
    private final Function<String, Integer> commandExecutor;
    private final BiConsumer<RuntimePlayer, String> messageSink;
    private final Function<String, Object> functionInvoker;
    private final int maxSteps;

    private ProjectRuntimeHooks(Builder builder) {
        this.commandListSupplier = builder.commandListSupplier;
        this.commandExecutor = builder.commandExecutor;
        this.messageSink = builder.messageSink;
        this.functionInvoker = builder.functionInvoker;
        this.maxSteps = builder.maxSteps;
    }

    public static Builder builder() { return new Builder(); }
    public static ProjectRuntimeHooks none() { return builder().build(); }

    public Supplier<List<String>> commandListSupplier() { return commandListSupplier; }
    public Function<String, Integer> commandExecutor() { return commandExecutor; }
    public BiConsumer<RuntimePlayer, String> messageSink() { return messageSink; }
    public Function<String, Object> functionInvoker() { return functionInvoker; }
    public int maxSteps() { return maxSteps; }

    public static final class Builder {
        private Supplier<List<String>> commandListSupplier;
        private Function<String, Integer> commandExecutor = ignored -> 0;
        private BiConsumer<RuntimePlayer, String> messageSink = (ignoredPlayer, ignoredText) -> {};
        private Function<String, Object> functionInvoker = ignored -> null;
        private int maxSteps = 1000;

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

        public Builder maxSteps(int value) {
            if (value < 1) throw new IllegalArgumentException("maxSteps must be positive");
            this.maxSteps = value;
            return this;
        }

        public ProjectRuntimeHooks build() { return new ProjectRuntimeHooks(this); }
    }
}
