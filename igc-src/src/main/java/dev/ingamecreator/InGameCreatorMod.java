package dev.ingamecreator;

import dev.ingamecreator.core.roles.FileRoleRepository;
import dev.ingamecreator.core.roles.RoleService;
import dev.ingamecreator.core.blueprint.BlueprintStore;
import dev.ingamecreator.core.blueprint.SmartNodeCatalog;
import dev.ingamecreator.core.project.ProjectStore;
import dev.ingamecreator.core.runtime.ProjectRuntimeService;
import dev.ingamecreator.fabric.RoleCommands;
import dev.ingamecreator.fabric.CreatorContent;
import dev.ingamecreator.fabric.CreatorCommands;
import dev.ingamecreator.fabric.CoderCommands;
import dev.ingamecreator.fabric.CreatorAccessNetworking;
import dev.ingamecreator.fabric.OwnerPrivileges;
import dev.ingamecreator.fabric.GeneratedCommandRegistrar;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public final class InGameCreatorMod implements ModInitializer {
    public static final String MOD_ID = "ingamecreator";
    private static RoleService roleService;
    private static ProjectStore projectStore;
    private static BlueprintStore blueprintStore;
    private static ProjectRuntimeService runtimeService;

    @Override
    public void onInitialize() {
        Path dataDir = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
        roleService = new RoleService(new FileRoleRepository(dataDir.resolve("roles.properties")));
        ensureConvenienceRoles(roleService);
        SmartNodeCatalog catalog = SmartNodeCatalog.standard();
        projectStore = new ProjectStore(dataDir.resolve("projects"));
        blueprintStore = new BlueprintStore(dataDir.resolve("projects"), catalog);
        runtimeService = new ProjectRuntimeService(projectStore, blueprintStore, roleService, System::currentTimeMillis);
        CreatorContent.register();
        RoleCommands.register(roleService);
        CoderCommands.register(roleService);
        CreatorAccessNetworking.registerServer(roleService);
        OwnerPrivileges.register(roleService);
        GeneratedCommandRegistrar generated = new GeneratedCommandRegistrar(projectStore, runtimeService);
        CreatorCommands.register(projectStore, roleService, generated);
    }

    public static RoleService roles() {
        if (roleService == null) throw new IllegalStateException("InGame Creator is not initialized");
        return roleService;
    }

    public static ProjectStore projects() {
        if (projectStore == null) throw new IllegalStateException("InGame Creator is not initialized");
        return projectStore;
    }

    public static BlueprintStore blueprints() {
        if (blueprintStore == null) throw new IllegalStateException("InGame Creator is not initialized");
        return blueprintStore;
    }

    public static ProjectRuntimeService runtime() {
        if (runtimeService == null) throw new IllegalStateException("InGame Creator is not initialized");
        return runtimeService;
    }

    private static void ensureConvenienceRoles(RoleService roles) {
        for (String role : new String[]{"admin", "moderator", "developer", "tester", "vip", "coder"}) {
            roles.createRole(role);
        }
    }
}
