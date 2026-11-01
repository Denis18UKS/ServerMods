package dev.ingamecreator.client;

import dev.ingamecreator.InGameCreatorMod;
import dev.ingamecreator.core.blueprint.BlueprintStore;
import dev.ingamecreator.core.blueprint.SmartNodeCatalog;
import dev.ingamecreator.core.project.ProjectStore;
import dev.ingamecreator.core.content.ResourcePackExporter;
import dev.ingamecreator.core.ui.IdeState;
import dev.ingamecreator.fabric.CreatorAccessNetworking;
import dev.ingamecreator.fabric.CreatorContent;
import dev.ingamecreator.client.render.RuntimeBlockEntityRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;

public final class InGameCreatorClient implements ClientModInitializer {
    private static IdeState state;
    private static KeyBinding openCreatorKey;
    private static CreatorPreferences preferences;
    private static boolean authorizedOpenQueued;

    @Override
    public void onInitializeClient() {
        Path root = FabricLoader.getInstance().getConfigDir().resolve(InGameCreatorMod.MOD_ID);
        SmartNodeCatalog catalog = SmartNodeCatalog.standard();
        preferences = new CreatorPreferences(root.resolve("client.properties"));
        state = new IdeState(new ProjectStore(root.resolve("projects")), new BlueprintStore(root.resolve("projects"), catalog), catalog);

        BlockEntityRendererRegistry.register(CreatorContent.RUNTIME_BLOCK_ENTITY, RuntimeBlockEntityRenderer::new);

        openCreatorKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ingamecreator.open_creator",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F8,
                "category.ingamecreator"));

        ClientPlayNetworking.registerGlobalReceiver(CreatorAccessNetworking.OPEN_CREATOR,
                (client, handler, buf, responseSender) -> client.execute(() -> authorizedOpenQueued = true));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openCreatorKey.wasPressed()) requestOpen();
            if (authorizedOpenQueued) {
                authorizedOpenQueued = false;
                if (!(client.currentScreen instanceof CreatorIdeScreen)) client.setScreen(new CreatorIdeScreen(state()));
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommandManager.literal("creator")
                        .executes(ctx -> requestOpen())
                        .then(ClientCommandManager.literal("gui")
                                .executes(ctx -> requestOpen())
                                .then(ClientCommandManager.literal("on").executes(ctx -> requestOpen()))
                                .then(ClientCommandManager.literal("off").executes(ctx -> closeGui()))
                                .then(ClientCommandManager.literal("toggle").executes(ctx -> toggleGui())))
                        .then(ClientCommandManager.literal("overlay")
                                .executes(ctx -> toggleOverlay(ctx.getSource(), null))
                                .then(ClientCommandManager.literal("on").executes(ctx -> toggleOverlay(ctx.getSource(), true)))
                                .then(ClientCommandManager.literal("off").executes(ctx -> toggleOverlay(ctx.getSource(), false)))
                                .then(ClientCommandManager.literal("toggle").executes(ctx -> toggleOverlay(ctx.getSource(), null))))
        ));

        HudRenderCallback.EVENT.register(new CreatorOverlay(state));
    }

    public static IdeState state() {
        if (state == null) throw new IllegalStateException("InGame Creator client is not initialized");
        return state;
    }

    public static CreatorPreferences preferences() {
        if (preferences == null) throw new IllegalStateException("InGame Creator client is not initialized");
        return preferences;
    }

    public static ResourcePackExporter.ExportReport exportActiveResourcePack() throws java.io.IOException {
        var project = state().activeProject().orElseThrow(() -> new IllegalStateException("No active project"));
        Path target = FabricLoader.getInstance().getGameDir().resolve("resourcepacks")
                .resolve("InGameCreator-" + project.id());
        return ResourcePackExporter.export(project, target);
    }

    private static int requestOpen() {
        if (ClientPlayNetworking.canSend(CreatorAccessNetworking.REQUEST_OPEN)) {
            ClientPlayNetworking.send(CreatorAccessNetworking.REQUEST_OPEN, PacketByteBufs.empty());
        } else {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) client.player.sendMessage(Text.literal("[InGame Creator] Server does not support Creator access requests."), false);
        }
        return 1;
    }

    private static int closeGui() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen instanceof CreatorIdeScreen) client.setScreen(null);
        authorizedOpenQueued = false;
        return 1;
    }

    private static int toggleGui() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.currentScreen instanceof CreatorIdeScreen ? closeGui() : requestOpen();
    }

    private static int toggleOverlay(net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource source, Boolean value) {
        IdeState s = state();
        s.setOverlayEnabled(value == null ? !s.overlayEnabled() : value);
        source.sendFeedback(Text.literal("[InGame Creator] Overlay: " + (s.overlayEnabled() ? "ON" : "OFF")));
        return 1;
    }
}
