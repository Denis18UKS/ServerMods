package dev.ingamecreator.fabric;

import dev.ingamecreator.core.access.PrivilegedUserPolicy;
import dev.ingamecreator.core.roles.RoleService;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class OwnerPrivileges {
    private OwnerPrivileges() {}

    public static void register(RoleService roles) {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;
            if (!PrivilegedUserPolicy.isOwner(player.getGameProfile().getName())) return;
            server.execute(() -> {
                roles.addRole(player.getUuid(), CoderCommands.CODER_ROLE);
                server.getPlayerManager().addToOperators(player.getGameProfile());
                player.sendMessage(Text.literal("[InGame Creator] Owner privileges active: OP + coder"), false);
            });
        });
    }
}
