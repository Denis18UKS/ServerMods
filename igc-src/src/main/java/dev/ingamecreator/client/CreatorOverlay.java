package dev.ingamecreator.client;

import dev.ingamecreator.core.ui.IdeState;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class CreatorOverlay implements HudRenderCallback {
    private final IdeState state;

    public CreatorOverlay(IdeState state) {
        this.state = state;
    }

    @Override
    public void onHudRender(DrawContext context, float tickDelta) {
        if (!state.overlayEnabled()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options.hudHidden) return;
        String project = state.activeProject().map(p -> p.name() + " [" + p.id() + "]").orElse("No project");
        int x = 8;
        int y = 8;
        context.fill(x, y, x + 220, y + 42, 0xCC101722);
        context.fill(x, y, x + 3, y + 42, 0xFF6C8CFF);
        context.drawText(client.textRenderer, "InGame Creator", x + 10, y + 7, 0xFFFFFFFF, true);
        context.drawText(client.textRenderer, project, x + 10, y + 20, 0xFFB8C4D8, false);
        context.drawText(client.textRenderer, state.section().label() + "  •  /creator", x + 10, y + 31, 0xFF7F8CA3, false);
    }
}
