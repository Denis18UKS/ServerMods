package dev.ingamecreator.client;

import dev.ingamecreator.core.blueprint.BlueprintEdge;
import dev.ingamecreator.core.blueprint.BlueprintNode;
import dev.ingamecreator.core.blueprint.NodeType;
import dev.ingamecreator.core.blueprint.PortDefinition;
import dev.ingamecreator.core.blueprint.PortDirection;
import dev.ingamecreator.core.command.CommandDefinition;
import dev.ingamecreator.core.content.CuboidModelAsset;
import dev.ingamecreator.core.content.RuntimeBlockDefinition;
import dev.ingamecreator.core.content.RuntimeItemDefinition;
import dev.ingamecreator.core.content.TextureAsset;
import dev.ingamecreator.core.content.TriggerBinding;
import dev.ingamecreator.core.project.CreatorProject;
import dev.ingamecreator.core.ui.CreatorLayout;
import dev.ingamecreator.core.ui.IdeSection;
import dev.ingamecreator.core.ui.IdeState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class CreatorIdeScreen extends Screen {
    private static final int BG = 0xFF0C1017;
    private static final int TOP = 0xFF151B26;
    private static final int SIDE = 0xFF101620;
    private static final int PANEL = 0xFF182231;
    private static final int PANEL_2 = 0xFF202C3D;
    private static final int BORDER = 0xFF2D3B50;
    private static final int ACCENT = 0xFF6C8CFF;
    private static final int ACCENT_2 = 0xFF39D3A2;
    private static final int DANGER = 0xFFFF6B78;
    private static final int TEXT = 0xFFF2F6FF;
    private static final int MUTED = 0xFF98A7BC;

    private static String blueprintTransferBuffer = "";

    private final IdeState state;
    private ActiveField activeField = ActiveField.NONE;
    private ElementMode elementMode = ElementMode.ITEMS;

    private String projectId = "my_mod";
    private String projectName = "My Mod";
    private String commandName = "hello";
    private String commandRole = "default";
    private String blueprintName = "main_logic";
    private String nodeSearch = "";
    private String nodePropertyKey = "value";
    private String nodePropertyValue = "";

    private String itemId = "crystal";
    private String itemName = "Crystal";
    private String itemDescription = "Runtime item";
    private String itemMaxStack = "64";
    private String itemTexture = "";
    private String itemModel = "";
    private boolean itemGlint;

    private String blockId = "crystal_block";
    private String blockName = "Crystal Block";
    private String blockHardness = "3.0";
    private String blockResistance = "6.0";
    private String blockLuminance = "0";
    private String blockTexture = "";
    private String blockModel = "";

    private String textureId = "crystal_tex";
    private String textureSize = "16";
    private String textureHex = "#FFFFFFFF";
    private TextureAsset editingTexture;

    private String modelId = "crystal_model";
    private String cubeName = "cube";
    private String cubeX = "0", cubeY = "0", cubeZ = "0";
    private String cubeW = "16", cubeH = "16", cubeD = "16";
    private String cubeRotX = "0", cubeRotY = "0", cubeRotZ = "0";
    private String cubeTexture = "";
    private final List<CuboidModelAsset.Cuboid> modelCuboids = new ArrayList<>();

    private TriggerBinding.ElementKind triggerKind = TriggerBinding.ElementKind.ITEM;
    private String triggerElement = "crystal";
    private String triggerEvent = "right_click";
    private String triggerBlueprint = "bp_crystal_click";

    private String status = "Ready. F8 or /creator requests access to this IDE.";

    private double panX;
    private double panY;
    private double zoom = 0.85;
    private UUID draggingNode;
    private UUID primarySelectedNode;
    private final LinkedHashSet<UUID> selectedNodes = new LinkedHashSet<>();
    private boolean panning;
    private double lastMouseX;
    private double lastMouseY;
    private PendingPort pendingPort;
    private boolean selectingBox;
    private double boxStartX, boxStartY, boxEndX, boxEndY;
    private final Deque<String> undo = new ArrayDeque<>();
    private final Deque<String> redo = new ArrayDeque<>();

    public CreatorIdeScreen(IdeState state) {
        super(Text.literal("InGame Creator"));
        this.state = state;
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        CreatorLayout l = layout();
        context.fill(0, 0, width, height, BG);
        renderTop(context, l);
        renderSidebar(context, l, mouseX, mouseY);
        renderStatus(context, l);
        switch (state.section()) {
            case DASHBOARD -> renderDashboard(context, l);
            case PROJECTS -> renderProjects(context, l);
            case COMMANDS -> renderCommands(context, l);
            case ROLES -> renderRoles(context, l);
            case BLUEPRINTS -> renderBlueprints(context, l, mouseX, mouseY);
            case ELEMENTS -> renderElements(context, l, mouseX, mouseY);
            case TEST -> renderTest(context, l);
            case SETTINGS -> renderSettings(context, l);
        }
    }

    private void renderTop(DrawContext c, CreatorLayout l) {
        c.fill(0, 0, width, l.topHeight(), TOP);
        c.fill(0, l.topHeight() - 1, width, l.topHeight(), BORDER);
        c.drawText(textRenderer, "InGame Creator", 12, 10, TEXT, true);
        if (width >= 620) c.drawText(textRenderer, "Fabric 1.20.1  •  Visual Mod IDE", Math.min(190, l.sidebarWidth() + 20), 11, MUTED, false);
        if (width >= 900) {
            String project = state.activeProject().map(p -> p.name() + "  [" + p.id() + "]").orElse("No active project");
            int px = Math.max(l.sidebarWidth() + 360, width - textRenderer.getWidth(project) - 18);
            c.drawText(textRenderer, project, px, 11, ACCENT_2, false);
        }
    }

    private void renderSidebar(DrawContext c, CreatorLayout l, int mouseX, int mouseY) {
        c.fill(0, l.topHeight(), l.sidebarWidth(), height - l.statusHeight(), SIDE);
        IdeSection[] values = IdeSection.values();
        int y = l.topHeight() + 18;
        int rowH = Math.max(26, Math.min(34, (height - l.topHeight() - l.statusHeight() - 50) / values.length));
        for (IdeSection section : values) {
            boolean active = state.section() == section;
            if (active) {
                c.fill(4, y - 4, l.sidebarWidth() - 8, y + rowH - 5, 0xFF2B4164);
                c.fill(4, y - 4, 8, y + rowH - 5, ACCENT);
            }
            c.drawText(textRenderer, sectionLabel(section), 14, y + 4, active ? TEXT : MUTED, false);
            y += rowH;
        }
        if (height >= 360) c.drawText(textRenderer, t("close"), 14, height - l.statusHeight() - 18, 0xFF66758B, false);
    }

    private void renderStatus(DrawContext c, CreatorLayout l) {
        int y = height - l.statusHeight();
        c.fill(0, y, width, height, TOP);
        c.fill(0, y, width, y + 1, BORDER);
        c.drawText(textRenderer, state.overlayEnabled() ? "HUD ON" : "HUD OFF", 6, y + 6, state.overlayEnabled() ? ACCENT_2 : MUTED, false);
        int sx = Math.min(l.sidebarWidth() + 12, width / 3);
        String visible = fit(status, Math.max(80, width - sx - 8));
        c.drawText(textRenderer, visible, sx, y + 6, MUTED, false);
    }

    private void renderDashboard(DrawContext c, CreatorLayout l) {
        int x = l.contentX(), y = l.contentTop() + 10;
        title(c, t("dashboard"), x, y);
        y += 34;
        int available = width - x - 12;
        int cardW = Math.max(150, Math.min(250, (available - 20) / 3));
        card(c, x, y, cardW, 76, "Active project", state.activeProject().map(p -> p.name() + "\n" + p.id()).orElse("No active project"));
        if (available >= 500) card(c, x + cardW + 10, y, cardW, 76, "Blueprint engine", state.searchNodes("").size() + " Smart Nodes\nTyped ports + runtime");
        if (available >= 760) card(c, x + (cardW + 10) * 2, y, cardW, 76, "Access", "coder role required\nF8 / /creator");
        y += 102;
        c.drawText(textRenderer, "Quick start", x, y, TEXT, true);
        String[] lines = {
                "1. Projects → create or open a project",
                "2. Elements → create items, blocks, textures, models and triggers",
                "3. Commands → create a command and starter Blueprint",
                "4. Blueprints → connect nodes, copy/paste and transfer graphs",
                "5. /coder → manage Creator access"
        };
        for (int i = 0; i < lines.length; i++) c.drawText(textRenderer, fit(lines[i], available), x, y + 18 + i * 15, MUTED, false);
        button(c, x, y + 100, 165, 24, state.overlayEnabled() ? "Disable HUD" : "Enable HUD", ACCENT);
    }

    private void renderProjects(DrawContext c, CreatorLayout l) {
        int x = l.contentX(), y = l.contentTop() + 10;
        title(c, t("projects"), x, y);
        int available = width - x - 12;
        int half = Math.max(130, (available - 16) / 2);
        drawInput(c, ActiveField.PROJECT_ID, "Project ID", projectId, x, y + 44, half);
        drawInput(c, ActiveField.PROJECT_NAME, "Project name", projectName, x + half + 12, y + 44, half);
        button(c, x, y + 82, Math.min(190, available), 24, t("create_project"), ACCENT);
        c.drawText(textRenderer, "Saved projects", x, y + 126, TEXT, true);
        int rowY = y + 146;
        try {
            for (String id : state.projectIds()) {
                if (rowY + 26 > l.contentBottom()) break;
                boolean active = state.activeProject().map(p -> p.id().equals(id)).orElse(false);
                c.fill(x, rowY, x + Math.min(580, available), rowY + 24, active ? 0xFF263754 : PANEL);
                c.drawText(textRenderer, id, x + 10, rowY + 8, active ? TEXT : MUTED, false);
                if (available > 330) c.drawText(textRenderer, active ? "OPEN" : "click to open", x + Math.min(450, available - 100), rowY + 8, active ? ACCENT_2 : 0xFF65758C, false);
                rowY += 29;
            }
        } catch (IOException e) { c.drawText(textRenderer, "Project list error: " + shortMessage(e), x, rowY, DANGER, false); }
    }

    private void renderCommands(DrawContext c, CreatorLayout l) {
        int x = l.contentX(), y = l.contentTop() + 10;
        title(c, "Command Builder", x, y);
        if (state.activeProject().isEmpty()) { c.drawText(textRenderer, "Create or open a project first.", x, y + 38, DANGER, false); return; }
        int available = width - x - 12;
        int half = Math.max(130, (available - 16) / 2);
        drawInput(c, ActiveField.COMMAND_NAME, "Command", commandName, x, y + 44, half);
        drawInput(c, ActiveField.COMMAND_ROLE, "Allowed role", commandRole, x + half + 12, y + 44, half);
        button(c, x, y + 82, Math.min(200, available), 24, "+ Basic command", ACCENT);
        if (available > 430) button(c, x + 212, y + 82, 230, 24, "+ Role-aware command", ACCENT_2);
        c.drawText(textRenderer, "Project commands", x, y + 126, TEXT, true);
        int rowY = y + 146;
        List<CommandDefinition> commands = state.activeProject().orElseThrow().commands().values().stream().sorted(Comparator.comparing(CommandDefinition::name)).toList();
        if (commands.isEmpty()) c.drawText(textRenderer, "No commands yet.", x, rowY, MUTED, false);
        for (CommandDefinition command : commands) {
            if (rowY + 30 > l.contentBottom()) break;
            c.fill(x, rowY, x + Math.min(700, available), rowY + 30, PANEL);
            c.drawText(textRenderer, "/" + command.name(), x + 10, rowY + 8, TEXT, true);
            if (available > 300) c.drawText(textRenderer, "roles: " + (command.allowedRoles().isEmpty() ? "public" : String.join(", ", command.allowedRoles())), x + 145, rowY + 8, MUTED, false);
            if (available > 520) c.drawText(textRenderer, "Blueprint: " + command.blueprintHandlerId(), x + 330, rowY + 8, ACCENT, false);
            rowY += 35;
        }
    }

    private void renderRoles(DrawContext c, CreatorLayout l) {
        int x = l.contentX(), y = l.contentTop() + 10;
        title(c, "Roles & Creator Access", x, y);
        String[] lines = {
                "/coder add <player>       — grant Creator access",
                "/coder remove <player>    — revoke Creator access",
                "/coder status [player]    — check Creator access",
                "/coder list               — list coders",
                "Player arguments use online-player TAB completion.",
                "Zunserz automatically receives Minecraft OP + coder on join.",
                "Creator GUI always requires the coder role. OP alone is not enough.",
                "",
                "/role add/remove/set/clear/create/delete/info — general role management"
        };
        int ly = y + 42;
        int max = width - x - 18;
        for (String line : lines) { c.drawText(textRenderer, fit(line, max), x, ly, line.startsWith("Zunserz") ? ACCENT_2 : MUTED, false); ly += 17; }
    }

    private void renderBlueprints(DrawContext c, CreatorLayout l, int mouseX, int mouseY) {
        int x = l.paletteX(), y = l.contentTop();
        if (state.activeProject().isEmpty()) { title(c, "Blueprints", x, y + 10); c.drawText(textRenderer, "Create or open a project first.", x, y + 44, DANGER, false); return; }
        c.fill(x, y, x + l.paletteWidth(), l.contentBottom(), PANEL);
        c.drawText(textRenderer, "NODE PALETTE", x + 10, y + 10, TEXT, true);
        drawInput(c, ActiveField.NODE_SEARCH, "Search", nodeSearch, x + 10, y + 40, l.paletteWidth() - 20);

        if (state.activeBlueprintId().isBlank()) {
            drawInput(c, ActiveField.BLUEPRINT_NAME, "New Blueprint ID", blueprintName, x + 10, y + 84, l.paletteWidth() - 20);
            button(c, x + 10, y + 122, l.paletteWidth() - 20, 24, "+ Create Blueprint", ACCENT);
            c.drawText(textRenderer, "Existing Blueprints", x + 10, y + 164, TEXT, true);
            int by = y + 182;
            try {
                for (String id : state.blueprintIds()) {
                    if (by + 22 > l.contentBottom()) break;
                    c.fill(x + 8, by, x + l.paletteWidth() - 8, by + 22, PANEL_2);
                    c.drawText(textRenderer, id, x + 14, by + 7, MUTED, false);
                    by += 26;
                }
            } catch (IOException e) { c.drawText(textRenderer, shortMessage(e), x + 10, by, DANGER, false); }
            return;
        }

        c.drawText(textRenderer, "Graph: " + state.activeBlueprintId(), x + 10, y + 80, ACCENT_2, false);
        int actionY = y + 96;
        int bw = Math.max(58, (l.paletteWidth() - 26) / 2);
        button(c, x + 8, actionY, bw, 20, "Copy", ACCENT);
        button(c, x + 12 + bw, actionY, bw, 20, "Paste", ACCENT_2);
        button(c, x + 8, actionY + 24, bw, 20, "Export graph", PANEL_2);
        button(c, x + 12 + bw, actionY + 24, bw, 20, "Import", PANEL_2);

        List<NodeType> palette = state.searchNodes(nodeSearch);
        int py = actionY + 52;
        int maxRows = Math.max(1, (l.contentBottom() - py - 4) / 30);
        for (int i = 0; i < Math.min(maxRows, palette.size()); i++) {
            NodeType type = palette.get(i);
            c.fill(x + 8, py, x + l.paletteWidth() - 8, py + 26, PANEL_2);
            c.fill(x + 8, py, x + 11, py + 26, categoryColor(type.category()));
            c.drawText(textRenderer, fit(type.title(), l.paletteWidth() - 28), x + 15, py + 5, TEXT, false);
            c.drawText(textRenderer, fit(type.category(), l.paletteWidth() - 28), x + 15, py + 16, 0xFF71829A, false);
            py += 30;
        }

        BlueprintGeometry g = blueprintGeometry(l);
        c.fill(g.x(), g.y(), g.x() + g.w(), g.y() + g.h(), 0xFF0B0F15);
        drawGrid(c, g);
        c.enableScissor(g.x(), g.y(), g.x() + g.w(), g.y() + g.h());
        renderEdges(c, g);
        renderNodes(c, g, mouseX, mouseY);
        if (selectingBox) drawSelectionBox(c);
        c.disableScissor();
        renderNodeInspector(c, l, g);
        c.drawText(textRenderer, fit("Ctrl+A/C/V/X/Z/Y/D/S/F • Delete • Shift+click • drag empty space to box-select • MMB pan • Wheel zoom " + String.format(Locale.ROOT, "%.0f%%", zoom * 100), g.w() - 12), g.x() + 6, g.y() + g.h() - 13, 0xFF637187, false);
        if (pendingPort != null) c.drawText(textRenderer, "Connecting " + pendingPort.portId + " → click a compatible port", g.x() + 8, g.y() + 8, ACCENT_2, true);
    }

    private void renderNodeInspector(DrawContext c, CreatorLayout l, BlueprintGeometry g) {
        if (primarySelectedNode == null || state.activeGraph().nodes().get(primarySelectedNode) == null || g.inspectorW() <= 0) return;
        BlueprintNode node = state.activeGraph().nodes().get(primarySelectedNode);
        NodeType type = type(node.typeId());
        int x = g.x() + g.w() + 8, y = g.y();
        int w = g.inspectorW();
        c.fill(x, y, x + w, Math.min(l.contentBottom(), y + 146), 0xF21A2432);
        c.fill(x, y, x + 3, Math.min(l.contentBottom(), y + 146), ACCENT_2);
        c.drawText(textRenderer, "NODE INSPECTOR", x + 10, y + 9, TEXT, true);
        c.drawText(textRenderer, fit(type == null ? node.typeId() : type.title(), w - 20), x + 10, y + 24, ACCENT, false);
        drawInput(c, ActiveField.NODE_PROPERTY_KEY, "Property key", nodePropertyKey, x + 10, y + 50, w - 20);
        drawInput(c, ActiveField.NODE_PROPERTY_VALUE, "Value", nodePropertyValue, x + 10, y + 88, Math.max(70, w - 100));
        button(c, x + w - 82, y + 88, 72, 22, "Apply", ACCENT_2);
        c.drawText(textRenderer, selectedNodes.size() + " selected", x + 10, y + 124, MUTED, false);
    }

    private void renderElements(DrawContext c, CreatorLayout l, int mouseX, int mouseY) {
        int x = l.contentX(), y = l.contentTop() + 6;
        title(c, "Elements", x, y);
        if (state.activeProject().isEmpty()) { c.drawText(textRenderer, "Create or open a project first.", x, y + 38, DANGER, false); return; }
        int available = width - x - 12;
        int tabW = Math.max(64, Math.min(100, (available - 16) / ElementMode.values().length));
        int tx = x;
        for (ElementMode mode : ElementMode.values()) {
            button(c, tx, y + 30, tabW, 22, mode.label, mode == elementMode ? ACCENT : PANEL_2);
            tx += tabW + 4;
        }
        button(c, x, y + 60, Math.min(220, available), 24, "Export resource pack", ACCENT_2);
        if (available > 330) c.drawText(textRenderer, "Exports PNG/models for Minecraft 1.20.1; enable the pack once in Resource Packs.", x + Math.min(230, available), y + 68, MUTED, false);
        int bodyY = y + 98;
        switch (elementMode) {
            case ITEMS -> renderItems(c, l, x, bodyY, available);
            case BLOCKS -> renderBlocks(c, l, x, bodyY, available);
            case TEXTURES -> renderTextures(c, l, x, bodyY, available, mouseX, mouseY);
            case MODELS -> renderModels(c, l, x, bodyY, available);
            case TRIGGERS -> renderTriggers(c, l, x, bodyY, available);
        }
    }

    private void renderItems(DrawContext c, CreatorLayout l, int x, int y, int available) {
        c.drawText(textRenderer, "Runtime Item Builder", x, y, TEXT, true);
        int col = Math.max(110, (available - 24) / 3);
        drawInput(c, ActiveField.ITEM_ID, "Item ID", itemId, x, y + 28, col);
        drawInput(c, ActiveField.ITEM_NAME, "Display name", itemName, x + col + 8, y + 28, col);
        drawInput(c, ActiveField.ITEM_MAX_STACK, "Max stack", itemMaxStack, x + (col + 8) * 2, y + 28, Math.min(col, 120));
        drawInput(c, ActiveField.ITEM_DESCRIPTION, "Description", itemDescription, x, y + 68, Math.min(available, col * 2 + 8));
        drawInput(c, ActiveField.ITEM_TEXTURE, "Texture ID", itemTexture, x, y + 108, col);
        drawInput(c, ActiveField.ITEM_MODEL, "Model ID (optional)", itemModel, x + col + 8, y + 108, col);
        button(c, x + (col + 8) * 2, y + 108, Math.min(120, col), 22, itemGlint ? "Glint: ON" : "Glint: OFF", PANEL_2);
        button(c, x, y + 146, Math.min(220, available), 24, "+ Create runtime item", ACCENT);
        int rowY = y + 190;
        c.drawText(textRenderer, "Project items", x, rowY - 20, TEXT, true);
        for (RuntimeItemDefinition item : state.activeProject().orElseThrow().runtimeItems().values()) {
            if (rowY + 28 > l.contentBottom()) break;
            c.fill(x, rowY, x + Math.min(760, available), rowY + 28, PANEL);
            c.drawText(textRenderer, fit(item.displayName(), 180), x + 10, rowY + 8, TEXT, false);
            if (available > 300) c.drawText(textRenderer, item.id(), x + 200, rowY + 8, ACCENT, false);
            if (available > 500) c.drawText(textRenderer, "texture: " + blankDash(item.textureId()) + "  model: " + blankDash(item.modelId()), x + 340, rowY + 8, MUTED, false);
            rowY += 32;
        }
    }

    private void renderBlocks(DrawContext c, CreatorLayout l, int x, int y, int available) {
        c.drawText(textRenderer, "Runtime Block Builder", x, y, TEXT, true);
        int col = Math.max(105, (available - 24) / 3);
        drawInput(c, ActiveField.BLOCK_ID, "Block ID", blockId, x, y + 28, col);
        drawInput(c, ActiveField.BLOCK_NAME, "Display name", blockName, x + col + 8, y + 28, col);
        drawInput(c, ActiveField.BLOCK_LUMINANCE, "Light 0..15", blockLuminance, x + (col + 8) * 2, y + 28, Math.min(col, 120));
        drawInput(c, ActiveField.BLOCK_HARDNESS, "Hardness", blockHardness, x, y + 68, col);
        drawInput(c, ActiveField.BLOCK_RESISTANCE, "Resistance", blockResistance, x + col + 8, y + 68, col);
        drawInput(c, ActiveField.BLOCK_TEXTURE, "Texture ID", blockTexture, x, y + 108, col);
        drawInput(c, ActiveField.BLOCK_MODEL, "Model ID", blockModel, x + col + 8, y + 108, col);
        button(c, x, y + 146, Math.min(220, available), 24, "+ Create runtime block", ACCENT);
        int rowY = y + 190;
        c.drawText(textRenderer, "Project blocks", x, rowY - 20, TEXT, true);
        for (RuntimeBlockDefinition block : state.activeProject().orElseThrow().runtimeBlocks().values()) {
            if (rowY + 28 > l.contentBottom()) break;
            c.fill(x, rowY, x + Math.min(760, available), rowY + 28, PANEL);
            c.drawText(textRenderer, fit(block.displayName(), 180), x + 10, rowY + 8, TEXT, false);
            if (available > 300) c.drawText(textRenderer, block.id(), x + 200, rowY + 8, ACCENT, false);
            if (available > 500) c.drawText(textRenderer, "H " + block.hardness() + "  R " + block.resistance() + "  L " + block.luminance(), x + 340, rowY + 8, MUTED, false);
            rowY += 32;
        }
    }

    private void renderTextures(DrawContext c, CreatorLayout l, int x, int y, int available, int mouseX, int mouseY) {
        c.drawText(textRenderer, "2D Pixel Texture Editor", x, y, TEXT, true);
        int controlW = Math.min(200, Math.max(150, available / 4));
        drawInput(c, ActiveField.TEXTURE_ID, "Texture ID", textureId, x, y + 28, controlW);
        drawInput(c, ActiveField.TEXTURE_SIZE, "Size (8..64)", textureSize, x, y + 68, controlW);
        drawInput(c, ActiveField.TEXTURE_HEX, "Paint color #AARRGGBB", textureHex, x, y + 108, controlW);
        button(c, x, y + 146, controlW, 22, "+ New texture", ACCENT);
        button(c, x, y + 172, controlW, 22, "Save texture", ACCENT_2);
        c.drawText(textRenderer, "LMB paint • RMB erase", x, y + 204, MUTED, false);
        int listY = y + 226;
        for (TextureAsset tex : state.activeProject().orElseThrow().textures().values()) {
            if (listY + 22 > l.contentBottom()) break;
            c.fill(x, listY, x + controlW, listY + 22, editingTexture != null && editingTexture.id().equals(tex.id()) ? 0xFF2B4164 : PANEL);
            c.drawText(textRenderer, tex.id() + "  " + tex.width() + "×" + tex.height(), x + 7, listY + 7, MUTED, false);
            listY += 25;
        }
        if (editingTexture == null) return;
        int gx = x + controlW + 20;
        int gy = y + 28;
        int maxPixels = Math.max(96, Math.min(available - controlW - 30, l.contentBottom() - gy - 8));
        int cell = Math.max(3, Math.min(18, maxPixels / Math.max(editingTexture.width(), editingTexture.height())));
        c.drawText(textRenderer, "Editing: " + editingTexture.id(), gx, y, ACCENT_2, false);
        for (int py = 0; py < editingTexture.height(); py++) {
            for (int px = 0; px < editingTexture.width(); px++) {
                int color = editingTexture.pixel(px, py);
                int sx = gx + px * cell, sy = gy + py * cell;
                c.fill(sx, sy, sx + cell, sy + cell, color == 0 ? 0xFF1A1F29 : color);
                if (cell >= 6) {
                    c.fill(sx, sy, sx + cell, sy + 1, 0x552D3B50);
                    c.fill(sx, sy, sx + 1, sy + cell, 0x552D3B50);
                }
            }
        }
        if (inside(mouseX, mouseY, gx, gy, cell * editingTexture.width(), cell * editingTexture.height())) {
            int px = (mouseX - gx) / cell, py = (mouseY - gy) / cell;
            c.drawText(textRenderer, px + ", " + py, gx, gy + cell * editingTexture.height() + 6, MUTED, false);
        }
    }

    private void renderModels(DrawContext c, CreatorLayout l, int x, int y, int available) {
        c.drawText(textRenderer, "Cuboid 3D Model Editor", x, y, TEXT, true);
        int controlW = Math.min(340, Math.max(250, available / 2));
        drawInput(c, ActiveField.MODEL_ID, "Model ID", modelId, x, y + 28, controlW);
        int small = Math.max(70, (controlW - 16) / 3);
        drawInput(c, ActiveField.CUBE_NAME, "Cube name", cubeName, x, y + 68, controlW);
        drawInput(c, ActiveField.CUBE_X, "X", cubeX, x, y + 108, small);
        drawInput(c, ActiveField.CUBE_Y, "Y", cubeY, x + small + 8, y + 108, small);
        drawInput(c, ActiveField.CUBE_Z, "Z", cubeZ, x + (small + 8) * 2, y + 108, small);
        drawInput(c, ActiveField.CUBE_W, "Width", cubeW, x, y + 148, small);
        drawInput(c, ActiveField.CUBE_H, "Height", cubeH, x + small + 8, y + 148, small);
        drawInput(c, ActiveField.CUBE_D, "Depth", cubeD, x + (small + 8) * 2, y + 148, small);
        drawInput(c, ActiveField.CUBE_RX, "Rot X", cubeRotX, x, y + 188, small);
        drawInput(c, ActiveField.CUBE_RY, "Rot Y", cubeRotY, x + small + 8, y + 188, small);
        drawInput(c, ActiveField.CUBE_RZ, "Rot Z", cubeRotZ, x + (small + 8) * 2, y + 188, small);
        drawInput(c, ActiveField.CUBE_TEXTURE, "Texture ID", cubeTexture, x, y + 228, controlW);
        button(c, x, y + 266, Math.min(150, controlW), 22, "+ Add cuboid", ACCENT);
        button(c, x + Math.min(158, controlW / 2), y + 266, Math.min(150, controlW), 22, "Save model", ACCENT_2);
        c.drawText(textRenderer, modelCuboids.size() + " cuboid(s) in working model", x, y + 298, MUTED, false);

        int px = x + controlW + 20, py = y + 28;
        int pw = Math.max(120, available - controlW - 24), ph = Math.max(120, Math.min(300, l.contentBottom() - py - 8));
        c.fill(px, py, px + pw, py + ph, 0xFF0B0F15);
        c.drawText(textRenderer, "3D preview (isometric)", px + 8, py + 8, MUTED, false);
        int cx = px + pw / 2, cy = py + ph / 2 + 20;
        int index = 0;
        for (CuboidModelAsset.Cuboid cube : modelCuboids) {
            int ox = (int) Math.round((cube.x() - cube.z()) * 2.0);
            int oy = (int) Math.round((cube.x() + cube.z()) - cube.y() * 1.5);
            int rw = Math.max(4, (int) Math.round(cube.width() * 2.0));
            int rh = Math.max(4, (int) Math.round(cube.height() * 2.0));
            int color = 0xFF4D678F + ((index++ * 0x00050505) & 0x00202020);
            c.fill(cx + ox - rw / 2, cy + oy - rh, cx + ox + rw / 2, cy + oy, color);
        }
        c.drawText(textRenderer, "Cuboids store XYZ, size, rotation and texture bindings.", px + 8, py + ph - 18, MUTED, false);
    }

    private void renderTriggers(DrawContext c, CreatorLayout l, int x, int y, int available) {
        c.drawText(textRenderer, "MCreator-like Trigger Bindings", x, y, TEXT, true);
        int w = Math.min(360, available);
        button(c, x, y + 28, 140, 22, "Kind: " + triggerKind.name(), PANEL_2);
        drawInput(c, ActiveField.TRIGGER_ELEMENT, "Element ID", triggerElement, x, y + 66, w);
        drawInput(c, ActiveField.TRIGGER_EVENT, "Trigger event", triggerEvent, x, y + 106, w);
        drawInput(c, ActiveField.TRIGGER_BLUEPRINT, "Blueprint ID", triggerBlueprint, x, y + 146, w);
        button(c, x, y + 184, Math.min(220, w), 24, "Bind trigger → Blueprint", ACCENT);
        String hint = triggerKind == TriggerBinding.ElementKind.ITEM
                ? "Item events: right_click, inventory_tick"
                : "Block events: placed, broken, right_click";
        c.drawText(textRenderer, fit(hint, available), x, y + 220, MUTED, false);
        int rowY = y + 250;
        for (TriggerBinding trigger : state.activeProject().orElseThrow().triggers()) {
            if (rowY + 26 > l.contentBottom()) break;
            c.fill(x, rowY, x + Math.min(760, available), rowY + 26, PANEL);
            c.drawText(textRenderer, trigger.kind() + "  " + trigger.elementId() + " / " + trigger.eventId() + " → " + trigger.blueprintId(), x + 8, rowY + 8, MUTED, false);
            rowY += 30;
        }
    }

    private void renderTest(DrawContext c, CreatorLayout l) {
        int x = l.contentX(), y = l.contentTop() + 10;
        title(c, "Test & Runtime", x, y);
        CreatorProject p = state.activeProject().orElse(null);
        int commands = p == null ? 0 : p.commands().size();
        int items = p == null ? 0 : p.runtimeItems().size();
        int blocks = p == null ? 0 : p.runtimeBlocks().size();
        int textures = p == null ? 0 : p.textures().size();
        int models = p == null ? 0 : p.models().size();
        c.drawText(textRenderer, "Runtime executor: ACTIVE", x, y + 44, ACCENT_2, true);
        c.drawText(textRenderer, "Commands: " + commands + "  Items: " + items + "  Blocks: " + blocks + "  Textures: " + textures + "  Models: " + models, x, y + 68, MUTED, false);
        c.drawText(textRenderer, "/igc reload — register project commands", x, y + 96, MUTED, false);
        c.drawText(textRenderer, "/igc give <project> <item> — give a runtime item", x, y + 114, MUTED, false);
    }

    private void renderSettings(DrawContext c, CreatorLayout l) {
        int x = l.contentX(), y = l.contentTop() + 10;
        title(c, t("settings"), x, y);
        CreatorPreferences prefs = InGameCreatorClient.preferences();
        c.drawText(textRenderer, "Creator language is independent from Minecraft language.", x, y + 42, MUTED, false);
        button(c, x, y + 64, 240, 24, t("language") + ": " + prefs.language().displayName(), ACCENT);
        c.drawText(textRenderer, "Default: English. This build ships no Russian Creator GUI copy.", x, y + 102, MUTED, false);
        c.drawText(textRenderer, "Open Creator key: F8 (rebind in Minecraft Controls → InGame Creator)", x, y + 124, MUTED, false);
        c.drawText(textRenderer, "Layout adapts to GUI scale, window size and Unicode font metrics.", x, y + 146, MUTED, false);
        c.drawText(textRenderer, "Blueprint transfer buffer works across projects during this game session.", x, y + 168, MUTED, false);
    }

    private void renderEdges(DrawContext c, BlueprintGeometry g) {
        for (BlueprintEdge edge : state.activeGraph().edges()) {
            BlueprintNode from = state.activeGraph().nodes().get(edge.fromNode());
            BlueprintNode to = state.activeGraph().nodes().get(edge.toNode());
            if (from == null || to == null) continue;
            PortPoint a = portPoint(from, edge.fromPort(), g);
            PortPoint b = portPoint(to, edge.toPort(), g);
            if (a == null || b == null) continue;
            int color = 0xFF71829A;
            int mid = (a.x + b.x) / 2;
            lineH(c, a.x, mid, a.y, color);
            lineV(c, mid, a.y, b.y, color);
            lineH(c, mid, b.x, b.y, color);
        }
    }

    private void renderNodes(DrawContext c, BlueprintGeometry g, int mouseX, int mouseY) {
        for (BlueprintNode node : state.activeGraph().nodes().values()) {
            NodeType type = type(node.typeId());
            if (type == null) continue;
            int nx = nodeScreenX(node, g), ny = nodeScreenY(node, g), nw = scaled(180);
            int rows = (int) Math.max(type.ports().stream().filter(p -> p.direction() == PortDirection.INPUT).count(), type.ports().stream().filter(p -> p.direction() == PortDirection.OUTPUT).count());
            int nh = scaled(26) + Math.max(scaled(38), rows * scaled(17) + scaled(14));
            int border = selectedNodes.contains(node.id()) ? ACCENT_2 : BORDER;
            c.fill(nx - 1, ny - 1, nx + nw + 1, ny + nh + 1, border);
            c.fill(nx, ny, nx + nw, ny + nh, PANEL);
            c.fill(nx, ny, nx + nw, ny + scaled(24), categoryColor(type.category()));
            c.drawText(textRenderer, fit(type.title(), nw - 14), nx + 7, ny + scaled(7), TEXT, true);
            List<PortDefinition> ins = type.ports().stream().filter(p -> p.direction() == PortDirection.INPUT).toList();
            List<PortDefinition> outs = type.ports().stream().filter(p -> p.direction() == PortDirection.OUTPUT).toList();
            for (int i = 0; i < ins.size(); i++) drawPort(c, node, ins.get(i), i, g, nw, true);
            for (int i = 0; i < outs.size(); i++) drawPort(c, node, outs.get(i), i, g, nw, false);
        }
    }

    private void drawPort(DrawContext c, BlueprintNode node, PortDefinition port, int index, BlueprintGeometry g, int nw, boolean input) {
        int nx = nodeScreenX(node, g), ny = nodeScreenY(node, g);
        int py = ny + scaled(24) + scaled(14 + index * 17);
        int px = input ? nx : nx + nw;
        int color = portColor(port);
        c.fill(px - 3, py - 3, px + 4, py + 4, color);
        String label = port.name();
        int tw = textRenderer.getWidth(label);
        int tx = input ? nx + 8 : nx + nw - 8 - tw;
        c.drawText(textRenderer, label, tx, py - 4, MUTED, false);
    }

    private void drawGrid(DrawContext c, BlueprintGeometry g) {
        int step = Math.max(12, scaled(32));
        int ox = Math.floorMod((long) panX, step), oy = Math.floorMod((long) panY, step);
        for (int xx = g.x() + ox; xx < g.x() + g.w(); xx += step) c.fill(xx, g.y(), xx + 1, g.y() + g.h(), 0xFF151D27);
        for (int yy = g.y() + oy; yy < g.y() + g.h(); yy += step) c.fill(g.x(), yy, g.x() + g.w(), yy + 1, 0xFF151D27);
    }

    private void drawSelectionBox(DrawContext c) {
        int x1 = (int) Math.min(boxStartX, boxEndX), x2 = (int) Math.max(boxStartX, boxEndX);
        int y1 = (int) Math.min(boxStartY, boxEndY), y2 = (int) Math.max(boxStartY, boxEndY);
        c.fill(x1, y1, x2, y1 + 1, ACCENT);
        c.fill(x1, y2, x2, y2 + 1, ACCENT);
        c.fill(x1, y1, x1 + 1, y2, ACCENT);
        c.fill(x2, y1, x2 + 1, y2, ACCENT);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        CreatorLayout l = layout();
        int rowH = Math.max(26, Math.min(34, (height - l.topHeight() - l.statusHeight() - 50) / IdeSection.values().length));
        int sy = l.topHeight() + 14;
        if (mouseX < l.sidebarWidth()) {
            for (IdeSection section : IdeSection.values()) {
                if (inside(mouseX, mouseY, 4, sy, l.sidebarWidth() - 8, rowH)) {
                    state.selectSection(section); activeField = ActiveField.NONE; return true;
                }
                sy += rowH;
            }
        }
        return switch (state.section()) {
            case DASHBOARD -> clickDashboard((int) mouseX, (int) mouseY, l);
            case PROJECTS -> clickProjects((int) mouseX, (int) mouseY, l);
            case COMMANDS -> clickCommands((int) mouseX, (int) mouseY, l);
            case BLUEPRINTS -> clickBlueprints((int) mouseX, (int) mouseY, button, l);
            case ELEMENTS -> clickElements((int) mouseX, (int) mouseY, button, l);
            case SETTINGS -> clickSettings((int) mouseX, (int) mouseY, l);
            default -> super.mouseClicked(mouseX, mouseY, button);
        };
    }

    private boolean clickDashboard(int mx, int my, CreatorLayout l) {
        int x = l.contentX(), y = l.contentTop() + 10 + 34 + 102;
        if (inside(mx, my, x, y + 100, 165, 24)) { state.setOverlayEnabled(!state.overlayEnabled()); status = "HUD: " + (state.overlayEnabled() ? "ON" : "OFF"); return true; }
        return false;
    }

    private boolean clickProjects(int mx, int my, CreatorLayout l) {
        int x = l.contentX(), y = l.contentTop() + 10;
        int available = width - x - 12, half = Math.max(130, (available - 16) / 2);
        if (inside(mx, my, x, y + 44, half, 22)) { activeField = ActiveField.PROJECT_ID; return true; }
        if (inside(mx, my, x + half + 12, y + 44, half, 22)) { activeField = ActiveField.PROJECT_NAME; return true; }
        if (inside(mx, my, x, y + 82, Math.min(190, available), 24)) {
            try { state.createProject(projectId, projectName); clearBlueprintUi(); status = "Project created: " + projectId; }
            catch (Exception e) { status = "Error: " + shortMessage(e); }
            return true;
        }
        int rowY = y + 146;
        try {
            for (String id : state.projectIds()) {
                if (rowY + 24 > l.contentBottom()) break;
                if (inside(mx, my, x, rowY, Math.min(580, available), 24)) {
                    state.openProject(id); clearBlueprintUi(); status = "Opened project: " + id; return true;
                }
                rowY += 29;
            }
        } catch (Exception e) { status = "Error: " + shortMessage(e); }
        return false;
    }

    private boolean clickCommands(int mx, int my, CreatorLayout l) {
        int x = l.contentX(), y = l.contentTop() + 10, available = width - x - 12, half = Math.max(130, (available - 16) / 2);
        if (state.activeProject().isEmpty()) return false;
        if (inside(mx, my, x, y + 44, half, 22)) { activeField = ActiveField.COMMAND_NAME; return true; }
        if (inside(mx, my, x + half + 12, y + 44, half, 22)) { activeField = ActiveField.COMMAND_ROLE; return true; }
        if (inside(mx, my, x, y + 82, Math.min(200, available), 24)) {
            try { state.createQuickCommand(commandName); state.selectSection(IdeSection.BLUEPRINTS); clearSelection(); status = "Created /" + commandName + " and starter Blueprint"; }
            catch (Exception e) { status = "Error: " + shortMessage(e); } return true;
        }
        if (available > 430 && inside(mx, my, x + 212, y + 82, 230, 24)) {
            try { state.createQuickRoleCommand(commandName, commandRole); state.selectSection(IdeSection.BLUEPRINTS); clearSelection(); status = "Created role-aware /" + commandName; }
            catch (Exception e) { status = "Error: " + shortMessage(e); } return true;
        }
        int rowY = y + 146;
        for (CommandDefinition command : state.activeProject().orElseThrow().commands().values().stream().sorted(Comparator.comparing(CommandDefinition::name)).toList()) {
            if (inside(mx, my, x, rowY, Math.min(700, available), 30) && !command.blueprintHandlerId().isBlank()) {
                try { state.openBlueprint(command.blueprintHandlerId()); state.selectSection(IdeSection.BLUEPRINTS); clearBlueprintUi(); status = "Blueprint: " + command.blueprintHandlerId(); }
                catch (Exception e) { status = "Error: " + shortMessage(e); } return true;
            }
            rowY += 35;
        }
        return false;
    }

    private boolean clickBlueprints(int mx, int my, int button, CreatorLayout l) {
        int x = l.paletteX(), y = l.contentTop();
        if (inside(mx, my, x + 10, y + 40, l.paletteWidth() - 20, 22)) { activeField = ActiveField.NODE_SEARCH; return true; }
        if (state.activeProject().isEmpty()) return false;
        if (state.activeBlueprintId().isBlank()) {
            if (inside(mx, my, x + 10, y + 84, l.paletteWidth() - 20, 22)) { activeField = ActiveField.BLUEPRINT_NAME; return true; }
            if (inside(mx, my, x + 10, y + 122, l.paletteWidth() - 20, 24)) {
                try { state.createBlueprint(blueprintName); clearBlueprintUi(); status = "Created Blueprint: " + blueprintName; }
                catch (Exception e) { status = "Error: " + shortMessage(e); } return true;
            }
            int by = y + 182;
            try {
                for (String id : state.blueprintIds()) {
                    if (by + 22 > l.contentBottom()) break;
                    if (inside(mx, my, x + 8, by, l.paletteWidth() - 16, 22)) { state.openBlueprint(id); clearBlueprintUi(); status = "Opened Blueprint: " + id; return true; }
                    by += 26;
                }
            } catch (Exception e) { status = "Error: " + shortMessage(e); }
            return false;
        }

        int actionY = y + 96, bw = Math.max(58, (l.paletteWidth() - 26) / 2);
        if (inside(mx, my, x + 8, actionY, bw, 20)) { copySelection(); return true; }
        if (inside(mx, my, x + 12 + bw, actionY, bw, 20)) { pasteSelection(); return true; }
        if (inside(mx, my, x + 8, actionY + 24, bw, 20)) { blueprintTransferBuffer = state.exportWholeGraph(); status = "Whole graph exported to Creator transfer buffer"; return true; }
        if (inside(mx, my, x + 12 + bw, actionY + 24, bw, 20)) { importWholeGraph(); return true; }

        BlueprintGeometry g = blueprintGeometry(l);
        if (primarySelectedNode != null && g.inspectorW() > 0) {
            int ix = g.x() + g.w() + 8, iy = g.y(), iw = g.inspectorW();
            if (inside(mx, my, ix + 10, iy + 50, iw - 20, 22)) { activeField = ActiveField.NODE_PROPERTY_KEY; return true; }
            if (inside(mx, my, ix + 10, iy + 88, Math.max(70, iw - 100), 22)) { activeField = ActiveField.NODE_PROPERTY_VALUE; return true; }
            if (inside(mx, my, ix + iw - 82, iy + 88, 72, 22)) {
                try { recordHistory(); state.activeGraph().setNodeProperty(primarySelectedNode, nodePropertyKey, nodePropertyValue); saveGraph("Node property saved: " + nodePropertyKey); }
                catch (Exception e) { status = "Property error: " + shortMessage(e); } return true;
            }
        }

        List<NodeType> palette = state.searchNodes(nodeSearch);
        int py = actionY + 52, maxRows = Math.max(1, (l.contentBottom() - py - 4) / 30);
        for (int i = 0; i < Math.min(maxRows, palette.size()); i++) {
            if (inside(mx, my, x + 8, py, l.paletteWidth() - 16, 26)) {
                NodeType type = palette.get(i); recordHistory();
                BlueprintNode created = state.activeGraph().addNode(type.id(), Math.max(0, (40 - panX) / zoom), Math.max(0, (80 + state.activeGraph().nodes().size() * 26 - panY) / zoom));
                selectSingle(created); saveGraph("Added node: " + type.title()); return true;
            }
            py += 30;
        }

        if (!inside(mx, my, g.x(), g.y(), g.w(), g.h())) return false;
        PortHit port = findPort(mx, my, g);
        if (port != null && button == 0) { handlePortClick(port); return true; }
        BlueprintNode node = findNode(mx, my, g);
        if (node != null) {
            if (button == 1) { recordHistory(); removeNodes(Set.of(node.id())); saveGraph("Node deleted"); return true; }
            if (button == 0) {
                boolean shift = shiftDown();
                if (shift) toggleSelection(node); else if (!selectedNodes.contains(node.id())) selectSingle(node); else primarySelectedNode = node.id();
                recordHistory(); draggingNode = node.id(); lastMouseX = mx; lastMouseY = my; return true;
            }
        }
        if (button == 2) { panning = true; lastMouseX = mx; lastMouseY = my; return true; }
        if (button == 0) {
            pendingPort = null;
            if (!shiftDown()) clearSelection();
            selectingBox = true; boxStartX = boxEndX = mx; boxStartY = boxEndY = my;
            return true;
        }
        return true;
    }

    private boolean clickElements(int mx, int my, int button, CreatorLayout l) {
        int x = l.contentX(), y = l.contentTop() + 6, available = width - x - 12;
        if (state.activeProject().isEmpty()) return false;
        int tabW = Math.max(64, Math.min(100, (available - 16) / ElementMode.values().length));
        int tx = x;
        for (ElementMode mode : ElementMode.values()) {
            if (inside(mx, my, tx, y + 30, tabW, 22)) { elementMode = mode; activeField = ActiveField.NONE; return true; }
            tx += tabW + 4;
        }
        if (inside(mx, my, x, y + 60, Math.min(220, available), 24)) {
            try {
                var report = InGameCreatorClient.exportActiveResourcePack();
                status = "Resource pack exported: " + report.root().getFileName() + " (" + report.filesWritten() + " files)";
            } catch (Exception e) {
                status = "Export error: " + shortMessage(e);
            }
            return true;
        }
        int bodyY = y + 98;
        return switch (elementMode) {
            case ITEMS -> clickItems(mx, my, l, x, bodyY, available);
            case BLOCKS -> clickBlocks(mx, my, l, x, bodyY, available);
            case TEXTURES -> clickTextures(mx, my, button, l, x, bodyY, available);
            case MODELS -> clickModels(mx, my, l, x, bodyY, available);
            case TRIGGERS -> clickTriggers(mx, my, l, x, bodyY, available);
        };
    }

    private boolean clickItems(int mx, int my, CreatorLayout l, int x, int y, int available) {
        int col = Math.max(110, (available - 24) / 3);
        if (inside(mx,my,x,y+28,col,22)) { activeField=ActiveField.ITEM_ID; return true; }
        if (inside(mx,my,x+col+8,y+28,col,22)) { activeField=ActiveField.ITEM_NAME; return true; }
        if (inside(mx,my,x+(col+8)*2,y+28,Math.min(col,120),22)) { activeField=ActiveField.ITEM_MAX_STACK; return true; }
        if (inside(mx,my,x,y+68,Math.min(available,col*2+8),22)) { activeField=ActiveField.ITEM_DESCRIPTION; return true; }
        if (inside(mx,my,x,y+108,col,22)) { activeField=ActiveField.ITEM_TEXTURE; return true; }
        if (inside(mx,my,x+col+8,y+108,col,22)) { activeField=ActiveField.ITEM_MODEL; return true; }
        if (inside(mx,my,x+(col+8)*2,y+108,Math.min(120,col),22)) { itemGlint=!itemGlint; status="Glint: "+(itemGlint?"ON":"OFF"); return true; }
        if (inside(mx,my,x,y+146,Math.min(220,available),24)) {
            try { RuntimeItemDefinition item=state.createRuntimeItem(itemId,itemName,itemDescription,Integer.parseInt(itemMaxStack.trim()),itemGlint,itemTexture,itemModel); status="Created runtime item: "+item.id(); }
            catch(Exception e){ status="Error: "+shortMessage(e);} return true;
        }
        return false;
    }

    private boolean clickBlocks(int mx, int my, CreatorLayout l, int x, int y, int available) {
        int col = Math.max(105, (available - 24) / 3);
        if (inside(mx,my,x,y+28,col,22)) { activeField=ActiveField.BLOCK_ID; return true; }
        if (inside(mx,my,x+col+8,y+28,col,22)) { activeField=ActiveField.BLOCK_NAME; return true; }
        if (inside(mx,my,x+(col+8)*2,y+28,Math.min(col,120),22)) { activeField=ActiveField.BLOCK_LUMINANCE; return true; }
        if (inside(mx,my,x,y+68,col,22)) { activeField=ActiveField.BLOCK_HARDNESS; return true; }
        if (inside(mx,my,x+col+8,y+68,col,22)) { activeField=ActiveField.BLOCK_RESISTANCE; return true; }
        if (inside(mx,my,x,y+108,col,22)) { activeField=ActiveField.BLOCK_TEXTURE; return true; }
        if (inside(mx,my,x+col+8,y+108,col,22)) { activeField=ActiveField.BLOCK_MODEL; return true; }
        if (inside(mx,my,x,y+146,Math.min(220,available),24)) {
            try { RuntimeBlockDefinition block=state.createRuntimeBlock(blockId,blockName,Float.parseFloat(blockHardness),Float.parseFloat(blockResistance),Integer.parseInt(blockLuminance),blockTexture,blockModel); status="Created runtime block: "+block.id(); }
            catch(Exception e){ status="Error: "+shortMessage(e);} return true;
        }
        return false;
    }

    private boolean clickTextures(int mx, int my, int button, CreatorLayout l, int x, int y, int available) {
        int controlW=Math.min(200,Math.max(150,available/4));
        if(inside(mx,my,x,y+28,controlW,22)){activeField=ActiveField.TEXTURE_ID;return true;}
        if(inside(mx,my,x,y+68,controlW,22)){activeField=ActiveField.TEXTURE_SIZE;return true;}
        if(inside(mx,my,x,y+108,controlW,22)){activeField=ActiveField.TEXTURE_HEX;return true;}
        if(inside(mx,my,x,y+146,controlW,22)){
            try{int size=Integer.parseInt(textureSize); editingTexture=state.createTexture(textureId,size,size); status="Created texture: "+editingTexture.id();}
            catch(Exception e){status="Error: "+shortMessage(e);} return true;
        }
        if(inside(mx,my,x,y+172,controlW,22)){
            try{if(editingTexture!=null){state.saveTexture(editingTexture);status="Texture saved: "+editingTexture.id();}}
            catch(Exception e){status="Error: "+shortMessage(e);} return true;
        }
        int listY=y+226;
        for(TextureAsset tex:state.activeProject().orElseThrow().textures().values()){
            if(listY+22>l.contentBottom())break;
            if(inside(mx,my,x,listY,controlW,22)){editingTexture=tex;textureId=tex.id();textureSize=Integer.toString(tex.width());status="Editing texture: "+tex.id();return true;}
            listY+=25;
        }
        if(editingTexture!=null){
            int gx=x+controlW+20,gy=y+28,maxPixels=Math.max(96,Math.min(available-controlW-30,l.contentBottom()-gy-8));
            int cell=Math.max(3,Math.min(18,maxPixels/Math.max(editingTexture.width(),editingTexture.height())));
            if(inside(mx,my,gx,gy,cell*editingTexture.width(),cell*editingTexture.height())){
                int px=(mx-gx)/cell,py=(my-gy)/cell;
                try{editingTexture.setPixel(px,py,button==1?0:parseColor(textureHex));state.saveTexture(editingTexture);status=(button==1?"Erased ":"Painted ")+px+","+py;}
                catch(Exception e){status="Paint error: "+shortMessage(e);}return true;
            }
        }
        return false;
    }

    private boolean clickModels(int mx, int my, CreatorLayout l, int x, int y, int available) {
        int controlW=Math.min(340,Math.max(250,available/2)),small=Math.max(70,(controlW-16)/3);
        if(inside(mx,my,x,y+28,controlW,22)){activeField=ActiveField.MODEL_ID;return true;}
        if(inside(mx,my,x,y+68,controlW,22)){activeField=ActiveField.CUBE_NAME;return true;}
        ActiveField[] fields={ActiveField.CUBE_X,ActiveField.CUBE_Y,ActiveField.CUBE_Z,ActiveField.CUBE_W,ActiveField.CUBE_H,ActiveField.CUBE_D,ActiveField.CUBE_RX,ActiveField.CUBE_RY,ActiveField.CUBE_RZ};
        int idx=0;for(int row=0;row<3;row++)for(int col=0;col<3;col++){if(inside(mx,my,x+col*(small+8),y+108+row*40,small,22)){activeField=fields[idx];return true;}idx++;}
        if(inside(mx,my,x,y+228,controlW,22)){activeField=ActiveField.CUBE_TEXTURE;return true;}
        if(inside(mx,my,x,y+266,Math.min(150,controlW),22)){
            try{modelCuboids.add(new CuboidModelAsset.Cuboid(cubeName,d(cubeX),d(cubeY),d(cubeZ),d(cubeW),d(cubeH),d(cubeD),d(cubeRotX),d(cubeRotY),d(cubeRotZ),cubeTexture));status="Cuboid added";}
            catch(Exception e){status="Error: "+shortMessage(e);}return true;
        }
        if(inside(mx,my,x+Math.min(158,controlW/2),y+266,Math.min(150,controlW),22)){
            try{state.saveModel(new CuboidModelAsset(modelId,List.copyOf(modelCuboids)));status="Model saved: "+modelId;}
            catch(Exception e){status="Error: "+shortMessage(e);}return true;
        }
        return false;
    }

    private boolean clickTriggers(int mx,int my,CreatorLayout l,int x,int y,int available){
        int w=Math.min(360,available);
        if(inside(mx,my,x,y+28,140,22)){triggerKind=triggerKind==TriggerBinding.ElementKind.ITEM?TriggerBinding.ElementKind.BLOCK:TriggerBinding.ElementKind.ITEM;status="Trigger kind: "+triggerKind;return true;}
        if(inside(mx,my,x,y+66,w,22)){activeField=ActiveField.TRIGGER_ELEMENT;return true;}
        if(inside(mx,my,x,y+106,w,22)){activeField=ActiveField.TRIGGER_EVENT;return true;}
        if(inside(mx,my,x,y+146,w,22)){activeField=ActiveField.TRIGGER_BLUEPRINT;return true;}
        if(inside(mx,my,x,y+184,Math.min(220,w),24)){
            try{state.bindTrigger(triggerKind,triggerElement,triggerEvent,triggerBlueprint);status="Trigger bound: "+triggerEvent+" → "+triggerBlueprint;}
            catch(Exception e){status="Error: "+shortMessage(e);}return true;
        }
        return false;
    }

    private boolean clickSettings(int mx, int my, CreatorLayout l) {
        int x=l.contentX(),y=l.contentTop()+10;
        if(inside(mx,my,x,y+64,240,24)){InGameCreatorClient.preferences().cycleLanguage();status="Creator language: "+InGameCreatorClient.preferences().language().displayName();return true;}
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX,double mouseY,int button,double deltaX,double deltaY){
        if(state.section()==IdeSection.BLUEPRINTS&&!state.activeBlueprintId().isBlank()){
            if(draggingNode!=null&&button==0){
                double dx=(mouseX-lastMouseX)/zoom,dy=(mouseY-lastMouseY)/zoom;
                Set<UUID> moved=selectedNodes.contains(draggingNode)?Set.copyOf(selectedNodes):Set.of(draggingNode);
                for(UUID id:moved){BlueprintNode n=state.activeGraph().nodes().get(id);if(n!=null)state.activeGraph().moveNode(id,n.x()+dx,n.y()+dy);}
                lastMouseX=mouseX;lastMouseY=mouseY;return true;
            }
            if(panning&&button==2){panX+=mouseX-lastMouseX;panY+=mouseY-lastMouseY;lastMouseX=mouseX;lastMouseY=mouseY;return true;}
            if(selectingBox&&button==0){boxEndX=mouseX;boxEndY=mouseY;return true;}
        }
        return super.mouseDragged(mouseX,mouseY,button,deltaX,deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX,double mouseY,int button){
        if(draggingNode!=null&&button==0){draggingNode=null;saveGraph("Node position saved");return true;}
        if(panning&&button==2){panning=false;return true;}
        if(selectingBox&&button==0){selectingBox=false;selectNodesInBox();return true;}
        return super.mouseReleased(mouseX,mouseY,button);
    }

    @Override
    public boolean mouseScrolled(double mouseX,double mouseY,double amount){
        if(state.section()==IdeSection.BLUEPRINTS){CreatorLayout l=layout();BlueprintGeometry g=blueprintGeometry(l);if(inside(mouseX,mouseY,g.x(),g.y(),g.w(),g.h())){zoom=Math.max(0.45,Math.min(1.6,zoom+amount*0.08));return true;}}
        return super.mouseScrolled(mouseX,mouseY,amount);
    }

    @Override
    public boolean charTyped(char chr,int modifiers){
        if(activeField==ActiveField.NONE||Character.isISOControl(chr))return super.charTyped(chr,modifiers);
        String value=fieldValue(activeField);if(value.length()>=128)return true;setFieldValue(activeField,value+chr);return true;
    }

    @Override
    public boolean keyPressed(int keyCode,int scanCode,int modifiers){
        boolean ctrl=(modifiers&GLFW.GLFW_MOD_CONTROL)!=0,shift=(modifiers&GLFW.GLFW_MOD_SHIFT)!=0;
        if(activeField!=ActiveField.NONE){
            if(ctrl&&keyCode==GLFW.GLFW_KEY_A){return true;}
            if(keyCode==GLFW.GLFW_KEY_BACKSPACE){String value=fieldValue(activeField);if(!value.isEmpty())setFieldValue(activeField,value.substring(0,value.length()-1));return true;}
            if(keyCode==GLFW.GLFW_KEY_ESCAPE){activeField=ActiveField.NONE;return true;}
        }
        if(state.section()==IdeSection.BLUEPRINTS&&!state.activeBlueprintId().isBlank()&&ctrl){
            if(keyCode==GLFW.GLFW_KEY_A){selectedNodes.clear();selectedNodes.addAll(state.activeGraph().nodes().keySet());primarySelectedNode=selectedNodes.stream().findFirst().orElse(null);status="Selected all nodes";return true;}
            if(keyCode==GLFW.GLFW_KEY_C){copySelection();return true;}
            if(keyCode==GLFW.GLFW_KEY_X){copySelection();deleteSelection();return true;}
            if(keyCode==GLFW.GLFW_KEY_V){pasteSelection();return true;}
            if(keyCode==GLFW.GLFW_KEY_D){duplicateSelection();return true;}
            if(keyCode==GLFW.GLFW_KEY_S){saveGraph("Blueprint saved");return true;}
            if(keyCode==GLFW.GLFW_KEY_F){activeField=ActiveField.NODE_SEARCH;return true;}
            if(keyCode==GLFW.GLFW_KEY_Z){if(shift)redo();else undo();return true;}
            if(keyCode==GLFW.GLFW_KEY_Y){redo();return true;}
        }
        if(state.section()==IdeSection.BLUEPRINTS&&keyCode==GLFW.GLFW_KEY_DELETE){deleteSelection();return true;}
        if(keyCode==GLFW.GLFW_KEY_ESCAPE){close();return true;}
        return super.keyPressed(keyCode,scanCode,modifiers);
    }

    private void copySelection(){
        if(selectedNodes.isEmpty()){status="Nothing selected";return;}
        blueprintTransferBuffer=state.exportSelection(Set.copyOf(selectedNodes));status="Copied "+selectedNodes.size()+" node(s) to Creator transfer buffer";
    }

    private void pasteSelection(){
        if(blueprintTransferBuffer==null||blueprintTransferBuffer.isBlank()){status="Creator transfer buffer is empty";return;}
        try{recordHistory();Set<UUID> ids=state.importSelection(blueprintTransferBuffer,32,32);selectedNodes.clear();selectedNodes.addAll(ids);primarySelectedNode=ids.stream().findFirst().orElse(null);status="Pasted "+ids.size()+" node(s)";}
        catch(Exception e){status="Import error: "+shortMessage(e);}
    }

    private void importWholeGraph(){
        if(blueprintTransferBuffer==null||blueprintTransferBuffer.isBlank()){status="Creator transfer buffer is empty";return;}
        try{recordHistory();state.replaceActiveGraph(blueprintTransferBuffer);clearSelection();status="Imported graph from Creator transfer buffer";}
        catch(Exception e){status="Import error: "+shortMessage(e);}
    }

    private void duplicateSelection(){if(selectedNodes.isEmpty()){status="Nothing selected";return;}blueprintTransferBuffer=state.exportSelection(Set.copyOf(selectedNodes));pasteSelection();}
    private void deleteSelection(){if(selectedNodes.isEmpty())return;recordHistory();removeNodes(Set.copyOf(selectedNodes));saveGraph("Deleted selected nodes");}
    private void removeNodes(Set<UUID> ids){for(UUID id:ids)state.activeGraph().removeNode(id);selectedNodes.removeAll(ids);if(primarySelectedNode!=null&&ids.contains(primarySelectedNode))primarySelectedNode=selectedNodes.stream().findFirst().orElse(null);pendingPort=null;}

    private void recordHistory(){
        if(state.activeBlueprintId().isBlank())return;
        try{String snap=state.exportWholeGraph();if(undo.isEmpty()||!undo.peekLast().equals(snap)){undo.addLast(snap);while(undo.size()>50)undo.removeFirst();}redo.clear();}catch(Exception ignored){}
    }
    private void undo(){if(undo.isEmpty()){status="Nothing to undo";return;}try{String current=state.exportWholeGraph();String prior=undo.removeLast();redo.addLast(current);state.replaceActiveGraph(prior);clearSelection();status="Undo";}catch(Exception e){status="Undo error: "+shortMessage(e);}}
    private void redo(){if(redo.isEmpty()){status="Nothing to redo";return;}try{String current=state.exportWholeGraph();String next=redo.removeLast();undo.addLast(current);state.replaceActiveGraph(next);clearSelection();status="Redo";}catch(Exception e){status="Redo error: "+shortMessage(e);}}

    private void selectSingle(BlueprintNode node){selectedNodes.clear();selectedNodes.add(node.id());primarySelectedNode=node.id();loadInspector(node);}
    private void toggleSelection(BlueprintNode node){if(!selectedNodes.add(node.id()))selectedNodes.remove(node.id());primarySelectedNode=selectedNodes.contains(node.id())?node.id():selectedNodes.stream().findFirst().orElse(null);if(primarySelectedNode!=null)loadInspector(state.activeGraph().nodes().get(primarySelectedNode));}
    private void clearSelection(){selectedNodes.clear();primarySelectedNode=null;pendingPort=null;}
    private void loadInspector(BlueprintNode node){if(node==null)return;String preferred=switch(node.typeId()){case "value.text","value.role"->"value";case "function.call"->"function";case "chat.send"->"text";default->node.properties().keySet().stream().findFirst().orElse("value");};nodePropertyKey=preferred;nodePropertyValue=node.properties().getOrDefault(preferred,"");}

    private void selectNodesInBox(){
        CreatorLayout l=layout();BlueprintGeometry g=blueprintGeometry(l);double x1=Math.min(boxStartX,boxEndX),x2=Math.max(boxStartX,boxEndX),y1=Math.min(boxStartY,boxEndY),y2=Math.max(boxStartY,boxEndY);
        if(!shiftDown())selectedNodes.clear();
        for(BlueprintNode node:state.activeGraph().nodes().values()){NodeType type=type(node.typeId());if(type==null)continue;int nx=nodeScreenX(node,g),ny=nodeScreenY(node,g),nw=scaled(180);int rows=(int)Math.max(type.ports().stream().filter(p->p.direction()==PortDirection.INPUT).count(),type.ports().stream().filter(p->p.direction()==PortDirection.OUTPUT).count());int nh=scaled(26)+Math.max(scaled(38),rows*scaled(17)+scaled(14));if(nx+nw>=x1&&nx<=x2&&ny+nh>=y1&&ny<=y2)selectedNodes.add(node.id());}
        primarySelectedNode=selectedNodes.stream().findFirst().orElse(null);if(primarySelectedNode!=null)loadInspector(state.activeGraph().nodes().get(primarySelectedNode));status="Selected "+selectedNodes.size()+" node(s)";
    }

    private void handlePortClick(PortHit hit){
        if(pendingPort==null){pendingPort=new PendingPort(hit.nodeId,hit.port.id(),hit.port.direction());status="Selected port "+hit.port.name();return;}
        if(pendingPort.nodeId.equals(hit.nodeId)&&pendingPort.portId.equals(hit.port.id())){pendingPort=null;status="Connection cancelled";return;}
        try{recordHistory();if(pendingPort.direction==PortDirection.OUTPUT&&hit.port.direction()==PortDirection.INPUT)state.activeGraph().connect(pendingPort.nodeId,pendingPort.portId,hit.nodeId,hit.port.id());else if(pendingPort.direction==PortDirection.INPUT&&hit.port.direction()==PortDirection.OUTPUT)state.activeGraph().connect(hit.nodeId,hit.port.id(),pendingPort.nodeId,pendingPort.portId);else{status="Connect OUTPUT → INPUT";pendingPort=null;return;}pendingPort=null;saveGraph("Ports connected");}catch(Exception e){pendingPort=null;status="Cannot connect: "+shortMessage(e);}
    }

    private PortHit findPort(int mx,int my,BlueprintGeometry g){for(BlueprintNode node:state.activeGraph().nodes().values()){NodeType type=type(node.typeId());if(type==null)continue;for(PortDefinition port:type.ports()){PortPoint point=portPoint(node,port.id(),g);if(point!=null&&Math.abs(mx-point.x)<=7&&Math.abs(my-point.y)<=7)return new PortHit(node.id(),port);}}return null;}
    private BlueprintNode findNode(int mx,int my,BlueprintGeometry g){List<BlueprintNode> nodes=new ArrayList<>(state.activeGraph().nodes().values());for(int i=nodes.size()-1;i>=0;i--){BlueprintNode node=nodes.get(i);NodeType type=type(node.typeId());if(type==null)continue;int nx=nodeScreenX(node,g),ny=nodeScreenY(node,g),nw=scaled(180);int rows=(int)Math.max(type.ports().stream().filter(p->p.direction()==PortDirection.INPUT).count(),type.ports().stream().filter(p->p.direction()==PortDirection.OUTPUT).count());int nh=scaled(26)+Math.max(scaled(38),rows*scaled(17)+scaled(14));if(inside(mx,my,nx,ny,nw,nh))return node;}return null;}
    private PortPoint portPoint(BlueprintNode node,String portId,BlueprintGeometry g){NodeType type=type(node.typeId());if(type==null)return null;PortDefinition port=type.port(portId).orElse(null);if(port==null)return null;List<PortDefinition> same=type.ports().stream().filter(p->p.direction()==port.direction()).toList();int index=same.indexOf(port);if(index<0)return null;int nx=nodeScreenX(node,g),ny=nodeScreenY(node,g),nw=scaled(180),py=ny+scaled(24)+scaled(14+index*17),px=port.direction()==PortDirection.INPUT?nx:nx+nw;return new PortPoint(px,py);}

    private NodeType type(String id){return state.searchNodes("").stream().filter(t->t.id().equals(id)).findFirst().orElse(null);}
    private int nodeScreenX(BlueprintNode node,BlueprintGeometry g){return(int)Math.round(g.x()+panX+node.x()*zoom);}
    private int nodeScreenY(BlueprintNode node,BlueprintGeometry g){return(int)Math.round(g.y()+panY+node.y()*zoom);}
    private int scaled(int value){return Math.max(1,(int)Math.round(value*zoom));}

    private BlueprintGeometry blueprintGeometry(CreatorLayout l){int inspector=(l.canvasWidth()>=620&&primarySelectedNode!=null)?Math.min(230,l.canvasWidth()/4):0;int gap=inspector>0?8:0;int w=Math.max(220,l.canvasWidth()-inspector-gap);return new BlueprintGeometry(l.canvasX(),l.contentTop(),w,l.canvasHeight(),inspector);}
    private CreatorLayout layout(){return CreatorLayout.compute(Math.max(width,320),Math.max(height,200));}

    private void clearBlueprintUi(){clearSelection();undo.clear();redo.clear();panX=panY=0;zoom=0.85;}
    private void saveGraph(String ok){try{state.saveActiveBlueprint();status=ok;}catch(Exception e){status="Save error: "+shortMessage(e);}}

    private void drawInput(DrawContext c,ActiveField field,String label,String value,int x,int y,int w){c.drawText(textRenderer,label,x,y-12,field==activeField?ACCENT_2:MUTED,false);c.fill(x,y,x+w,y+22,0xFF101822);c.fill(x,y+21,x+w,y+22,field==activeField?ACCENT_2:BORDER);c.drawText(textRenderer,fit(value.isEmpty()?"…":value,w-14),x+7,y+7,value.isEmpty()?0xFF59687D:TEXT,false);}
    private void button(DrawContext c,int x,int y,int w,int h,String label,int color){c.fill(x,y,x+w,y+h,color);c.drawText(textRenderer,fit(label,w-12),x+7,y+Math.max(6,(h-8)/2),TEXT,true);}
    private void title(DrawContext c,String text,int x,int y){c.drawText(textRenderer,text,x,y,TEXT,true);c.fill(x,y+20,Math.min(width-12,x+Math.max(240,width-x-80)),y+21,BORDER);}
    private void card(DrawContext c,int x,int y,int w,int h,String title,String body){c.fill(x,y,x+w,y+h,PANEL);c.drawText(textRenderer,fit(title,w-16),x+8,y+9,TEXT,true);String[] lines=body.split("\\n");for(int i=0;i<lines.length&&i<3;i++)c.drawText(textRenderer,fit(lines[i],w-16),x+8,y+28+i*14,MUTED,false);}
    private void lineH(DrawContext c,int x1,int x2,int y,int color){if(x2<x1){int t=x1;x1=x2;x2=t;}c.fill(x1,y,x2+1,y+2,color);}
    private void lineV(DrawContext c,int x,int y1,int y2,int color){if(y2<y1){int t=y1;y1=y2;y2=t;}c.fill(x,y1,x+2,y2+1,color);}

    private int categoryColor(String category){return switch(category){case "Events"->0xFFC27B2D;case "Commands","Command"->0xFF395B9D;case "Roles & Permissions","Role"->0xFF247D64;case "Flow Control","Flow"->0xFF9B3B4B;case "Functions"->0xFF7650A6;case "Player"->0xFF2D759F;case "Items"->0xFF91712F;case "Blocks"->0xFF597A36;case "World"->0xFF337E78;default->0xFF536377;};}
    private int portColor(PortDefinition port){return switch(port.type()){case EXEC->0xFFFFFFFF;case BOOLEAN->0xFFFF6B78;case NUMBER->0xFF6BE6A6;case TEXT->0xFFFFB36B;case PLAYER->0xFF6CB8FF;case ROLE,ROLES->0xFF39D3A2;case COMMAND_CONTEXT,COMMANDS->0xFF7B91FF;case ENTITY->0xFFFF9E80;case ITEM->0xFFFFD54F;case BLOCK->0xFF8BC34A;case WORLD->0xFF4DB6AC;case POSITION->0xFF9575CD;case ANY->0xFFC1A3FF;};}

    private String sectionLabel(IdeSection section){return switch(section){case DASHBOARD->t("dashboard");case PROJECTS->t("projects");case COMMANDS->t("commands");case ROLES->t("roles");case BLUEPRINTS->t("blueprints");case ELEMENTS->t("elements");case TEST->t("test");case SETTINGS->t("settings");};}
    private String t(String key){return InGameCreatorClient.preferences().text(key);}
    private String fit(String value,int maxWidth){if(value==null)return"";if(maxWidth<=12)return"";if(textRenderer.getWidth(value)<=maxWidth)return value;String suffix="…";int n=value.length();while(n>0&&textRenderer.getWidth(value.substring(0,n)+suffix)>maxWidth)n--;return n<=0?suffix:value.substring(0,n)+suffix;}
    private static boolean inside(double mx,double my,int x,int y,int w,int h){return mx>=x&&my>=y&&mx<x+w&&my<y+h;}
    private boolean shiftDown(){MinecraftClient mc=MinecraftClient.getInstance();if(mc==null||mc.getWindow()==null)return false;long handle=mc.getWindow().getHandle();return InputUtil.isKeyPressed(handle,GLFW.GLFW_KEY_LEFT_SHIFT)||InputUtil.isKeyPressed(handle,GLFW.GLFW_KEY_RIGHT_SHIFT);}
    private static int parseColor(String value){String s=value==null?"":value.trim();if(s.startsWith("#"))s=s.substring(1);if(s.length()==6)s="FF"+s;if(s.length()!=8)throw new IllegalArgumentException("use #RRGGBB or #AARRGGBB");return(int)Long.parseLong(s,16);}
    private static double d(String value){return Double.parseDouble(value.trim());}
    private static String blankDash(String value){return value==null||value.isBlank()?"—":value;}
    private static String shortMessage(Throwable error){String m=error.getMessage();return m==null||m.isBlank()?error.getClass().getSimpleName():m;}

    private String fieldValue(ActiveField f){return switch(f){case PROJECT_ID->projectId;case PROJECT_NAME->projectName;case COMMAND_NAME->commandName;case COMMAND_ROLE->commandRole;case BLUEPRINT_NAME->blueprintName;case NODE_SEARCH->nodeSearch;case NODE_PROPERTY_KEY->nodePropertyKey;case NODE_PROPERTY_VALUE->nodePropertyValue;case ITEM_ID->itemId;case ITEM_NAME->itemName;case ITEM_DESCRIPTION->itemDescription;case ITEM_MAX_STACK->itemMaxStack;case ITEM_TEXTURE->itemTexture;case ITEM_MODEL->itemModel;case BLOCK_ID->blockId;case BLOCK_NAME->blockName;case BLOCK_HARDNESS->blockHardness;case BLOCK_RESISTANCE->blockResistance;case BLOCK_LUMINANCE->blockLuminance;case BLOCK_TEXTURE->blockTexture;case BLOCK_MODEL->blockModel;case TEXTURE_ID->textureId;case TEXTURE_SIZE->textureSize;case TEXTURE_HEX->textureHex;case MODEL_ID->modelId;case CUBE_NAME->cubeName;case CUBE_X->cubeX;case CUBE_Y->cubeY;case CUBE_Z->cubeZ;case CUBE_W->cubeW;case CUBE_H->cubeH;case CUBE_D->cubeD;case CUBE_RX->cubeRotX;case CUBE_RY->cubeRotY;case CUBE_RZ->cubeRotZ;case CUBE_TEXTURE->cubeTexture;case TRIGGER_ELEMENT->triggerElement;case TRIGGER_EVENT->triggerEvent;case TRIGGER_BLUEPRINT->triggerBlueprint;case NONE->"";};}
    private void setFieldValue(ActiveField f,String v){switch(f){case PROJECT_ID->projectId=v;case PROJECT_NAME->projectName=v;case COMMAND_NAME->commandName=v;case COMMAND_ROLE->commandRole=v;case BLUEPRINT_NAME->blueprintName=v;case NODE_SEARCH->nodeSearch=v;case NODE_PROPERTY_KEY->nodePropertyKey=v;case NODE_PROPERTY_VALUE->nodePropertyValue=v;case ITEM_ID->itemId=v;case ITEM_NAME->itemName=v;case ITEM_DESCRIPTION->itemDescription=v;case ITEM_MAX_STACK->itemMaxStack=v;case ITEM_TEXTURE->itemTexture=v;case ITEM_MODEL->itemModel=v;case BLOCK_ID->blockId=v;case BLOCK_NAME->blockName=v;case BLOCK_HARDNESS->blockHardness=v;case BLOCK_RESISTANCE->blockResistance=v;case BLOCK_LUMINANCE->blockLuminance=v;case BLOCK_TEXTURE->blockTexture=v;case BLOCK_MODEL->blockModel=v;case TEXTURE_ID->textureId=v;case TEXTURE_SIZE->textureSize=v;case TEXTURE_HEX->textureHex=v;case MODEL_ID->modelId=v;case CUBE_NAME->cubeName=v;case CUBE_X->cubeX=v;case CUBE_Y->cubeY=v;case CUBE_Z->cubeZ=v;case CUBE_W->cubeW=v;case CUBE_H->cubeH=v;case CUBE_D->cubeD=v;case CUBE_RX->cubeRotX=v;case CUBE_RY->cubeRotY=v;case CUBE_RZ->cubeRotZ=v;case CUBE_TEXTURE->cubeTexture=v;case TRIGGER_ELEMENT->triggerElement=v;case TRIGGER_EVENT->triggerEvent=v;case TRIGGER_BLUEPRINT->triggerBlueprint=v;case NONE->{}}}

    private enum ElementMode{ITEMS("Items"),BLOCKS("Blocks"),TEXTURES("Textures"),MODELS("3D Models"),TRIGGERS("Triggers");private final String label;ElementMode(String label){this.label=label;}}
    private enum ActiveField{NONE,PROJECT_ID,PROJECT_NAME,COMMAND_NAME,COMMAND_ROLE,BLUEPRINT_NAME,NODE_SEARCH,NODE_PROPERTY_KEY,NODE_PROPERTY_VALUE,ITEM_ID,ITEM_NAME,ITEM_DESCRIPTION,ITEM_MAX_STACK,ITEM_TEXTURE,ITEM_MODEL,BLOCK_ID,BLOCK_NAME,BLOCK_HARDNESS,BLOCK_RESISTANCE,BLOCK_LUMINANCE,BLOCK_TEXTURE,BLOCK_MODEL,TEXTURE_ID,TEXTURE_SIZE,TEXTURE_HEX,MODEL_ID,CUBE_NAME,CUBE_X,CUBE_Y,CUBE_Z,CUBE_W,CUBE_H,CUBE_D,CUBE_RX,CUBE_RY,CUBE_RZ,CUBE_TEXTURE,TRIGGER_ELEMENT,TRIGGER_EVENT,TRIGGER_BLUEPRINT}
    private record BlueprintGeometry(int x,int y,int w,int h,int inspectorW){}
    private record PortPoint(int x,int y){}
    private record PortHit(UUID nodeId,PortDefinition port){}
    private record PendingPort(UUID nodeId,String portId,PortDirection direction){}
}
