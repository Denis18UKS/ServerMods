package dev.ingamecreator.core.ui;

import dev.ingamecreator.core.blueprint.BlueprintGraph;
import dev.ingamecreator.core.blueprint.BlueprintClipboard;
import dev.ingamecreator.core.blueprint.BlueprintNode;
import dev.ingamecreator.core.blueprint.BlueprintStore;
import dev.ingamecreator.core.blueprint.NodeType;
import dev.ingamecreator.core.blueprint.SmartNodeCatalog;
import dev.ingamecreator.core.command.CommandDefinition;
import dev.ingamecreator.core.content.RuntimeItemDefinition;
import dev.ingamecreator.core.content.RuntimeBlockDefinition;
import dev.ingamecreator.core.content.TextureAsset;
import dev.ingamecreator.core.content.CuboidModelAsset;
import dev.ingamecreator.core.content.TriggerBinding;
import dev.ingamecreator.core.project.CreatorProject;
import dev.ingamecreator.core.project.ProjectStore;
import dev.ingamecreator.core.roles.RoleService;

import java.io.IOException;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class IdeState {
    private final ProjectStore projects;
    private final BlueprintStore blueprints;
    private final SmartNodeCatalog catalog;

    private IdeSection section = IdeSection.DASHBOARD;
    private boolean overlayEnabled;
    private CreatorProject activeProject;
    private BlueprintGraph activeGraph;
    private String activeBlueprintId = "";

    public IdeState(ProjectStore projects, BlueprintStore blueprints, SmartNodeCatalog catalog) {
        this.projects = projects;
        this.blueprints = blueprints;
        this.catalog = catalog;
        this.activeGraph = new BlueprintGraph(catalog);
    }

    public IdeSection section() {
        return section;
    }

    public void selectSection(IdeSection section) {
        if (section == null) throw new IllegalArgumentException("section is required");
        this.section = section;
    }

    public boolean overlayEnabled() {
        return overlayEnabled;
    }

    public void setOverlayEnabled(boolean enabled) {
        this.overlayEnabled = enabled;
    }

    public Optional<CreatorProject> activeProject() {
        return Optional.ofNullable(activeProject);
    }

    public BlueprintGraph activeGraph() {
        return activeGraph;
    }

    public String activeBlueprintId() {
        return activeBlueprintId;
    }

    public Set<String> projectIds() throws IOException {
        return projects.listProjectIds();
    }

    public Set<String> blueprintIds() throws IOException {
        CreatorProject project = requireProject();
        return blueprints.listBlueprintIds(project.id());
    }

    public void createProject(String id, String name) throws IOException {
        CreatorProject project = CreatorProject.create(id, name);
        projects.save(project);
        this.activeProject = project;
        this.activeBlueprintId = "";
        this.activeGraph = new BlueprintGraph(catalog);
    }

    public void openProject(String id) throws IOException {
        this.activeProject = projects.load(id);
        this.activeBlueprintId = "";
        this.activeGraph = new BlueprintGraph(catalog);
    }

    public void openBlueprint(String blueprintId) throws IOException {
        CreatorProject project = requireProject();
        this.activeGraph = blueprints.load(project.id(), blueprintId);
        this.activeBlueprintId = blueprintId;
    }

    public void createBlueprint(String blueprintId) throws IOException {
        CreatorProject project = requireProject();
        BlueprintGraph graph = new BlueprintGraph(catalog);
        blueprints.save(project.id(), blueprintId, graph);
        this.activeGraph = graph;
        this.activeBlueprintId = blueprintId;
    }

    public void addCommand(CommandDefinition command) throws IOException {
        CreatorProject project = requireProject();
        project.addCommand(command);
        projects.save(project);
    }

    public RuntimeItemDefinition createRuntimeItem(String id, String displayName, String description, int maxStack, boolean glint) throws IOException {
        return createRuntimeItem(id, displayName, description, maxStack, glint, "", "");
    }

    public RuntimeItemDefinition createRuntimeItem(String id, String displayName, String description, int maxStack, boolean glint, String textureId, String modelId) throws IOException {
        CreatorProject project = requireProject();
        RuntimeItemDefinition item = new RuntimeItemDefinition(id, displayName, description, maxStack, glint, textureId, modelId);
        project.addRuntimeItem(item);
        projects.save(project);
        return item;
    }

    public RuntimeBlockDefinition createRuntimeBlock(String id, String displayName, float hardness, float resistance, int luminance, String textureId, String modelId) throws IOException {
        CreatorProject project = requireProject();
        RuntimeBlockDefinition block = new RuntimeBlockDefinition(id, displayName, hardness, resistance, luminance, textureId, modelId);
        project.addRuntimeBlock(block);
        projects.save(project);
        return block;
    }

    public TextureAsset createTexture(String id, int width, int height) throws IOException {
        CreatorProject project = requireProject();
        TextureAsset texture = TextureAsset.blank(id, width, height);
        project.addTexture(texture);
        projects.save(project);
        return texture;
    }

    public void saveTexture(TextureAsset texture) throws IOException {
        CreatorProject project = requireProject();
        project.putTexture(texture);
        projects.save(project);
    }

    public CuboidModelAsset saveModel(CuboidModelAsset model) throws IOException {
        CreatorProject project = requireProject();
        project.putModel(model);
        projects.save(project);
        return model;
    }

    public TriggerBinding bindTrigger(TriggerBinding.ElementKind kind, String elementId, String eventId, String blueprintId) throws IOException {
        CreatorProject project = requireProject();
        TriggerBinding binding = new TriggerBinding(kind, elementId, eventId, blueprintId);
        project.addTrigger(binding);
        projects.save(project);
        return binding;
    }

    public String exportSelection(Set<UUID> selected) {
        return BlueprintClipboard.exportSelection(BlueprintClipboard.capture(activeGraph, selected));
    }

    public String exportWholeGraph() {
        return exportSelection(new LinkedHashSet<>(activeGraph.nodes().keySet()));
    }

    public Set<UUID> importSelection(String data, double offsetX, double offsetY) throws IOException {
        Set<UUID> ids = BlueprintClipboard.paste(activeGraph, BlueprintClipboard.importSelection(data), offsetX, offsetY);
        saveActiveBlueprint();
        return ids;
    }

    public void replaceActiveGraph(String data) throws IOException {
        CreatorProject project = requireProject();
        if (activeBlueprintId == null || activeBlueprintId.isBlank()) throw new IllegalStateException("No active blueprint");
        BlueprintGraph graph = new BlueprintGraph(catalog);
        BlueprintClipboard.paste(graph, BlueprintClipboard.importSelection(data), 0, 0);
        activeGraph = graph;
        blueprints.save(project.id(), activeBlueprintId, activeGraph);
    }

    public void saveActiveProject() throws IOException {
        projects.save(requireProject());
    }

    public CommandDefinition createQuickCommand(String commandName) throws IOException {
        CreatorProject project = requireProject();
        String normalizedName = CommandDefinition.normalizeName(commandName);
        String blueprintId = "bp_" + normalizedName;
        CommandDefinition command = CommandDefinition.builder(normalizedName)
                .blueprintHandler(blueprintId)
                .build();
        project.addCommand(command);

        BlueprintGraph graph = new BlueprintGraph(catalog);
        BlueprintNode event = graph.addNode("event.command", 40, 60);
        BlueprintNode user = graph.addNode("command.user", 300, 80);
        BlueprintNode commandList = graph.addNode("command.list", 300, 240);
        graph.connect(event.id(), "context", user.id(), "context");
        graph.connect(event.id(), "context", commandList.id(), "context");

        projects.save(project);
        blueprints.save(project.id(), blueprintId, graph);
        this.activeGraph = graph;
        this.activeBlueprintId = blueprintId;
        return command;
    }

    public CommandDefinition createQuickRoleCommand(String commandName, String allowedRole) throws IOException {
        CreatorProject project = requireProject();
        String normalizedName = CommandDefinition.normalizeName(commandName);
        String normalizedRole = RoleService.normalizeRoleName(allowedRole);
        String blueprintId = "bp_" + normalizedName;

        CommandDefinition command = CommandDefinition.builder(normalizedName)
                .allowedRole(normalizedRole)
                .blueprintHandler(blueprintId)
                .build();
        project.addCommand(command);

        BlueprintGraph graph = new BlueprintGraph(catalog);
        BlueprintNode event = graph.addNode("event.command", 40, 60);
        BlueprintNode user = graph.addNode("command.user", 280, 120);
        BlueprintNode roleLiteral = new BlueprintNode(
                UUID.randomUUID(),
                "value.role",
                280,
                280,
                Map.of("value", normalizedRole));
        graph.addNode(roleLiteral);
        BlueprintNode hasRole = graph.addNode("role.has", 540, 150);
        BlueprintNode branch = graph.addNode("flow.branch", 790, 80);

        graph.connect(event.id(), "context", user.id(), "context");
        graph.connect(user.id(), "player", hasRole.id(), "player");
        graph.connect(roleLiteral.id(), "value", hasRole.id(), "role");
        graph.connect(event.id(), "exec", branch.id(), "exec");
        graph.connect(hasRole.id(), "result", branch.id(), "condition");

        projects.save(project);
        blueprints.save(project.id(), blueprintId, graph);
        this.activeGraph = graph;
        this.activeBlueprintId = blueprintId;
        return command;
    }

    public List<NodeType> searchNodes(String query) {
        return catalog.search(query);
    }

    public void saveActiveBlueprint() throws IOException {
        CreatorProject project = requireProject();
        if (activeBlueprintId == null || activeBlueprintId.isBlank()) {
            throw new IllegalStateException("No active blueprint");
        }
        blueprints.save(project.id(), activeBlueprintId, activeGraph);
    }

    private CreatorProject requireProject() {
        if (activeProject == null) throw new IllegalStateException("No active project");
        return activeProject;
    }
}
