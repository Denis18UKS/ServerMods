package dev.ingamecreator.fabric;

import dev.ingamecreator.InGameCreatorMod;
import dev.ingamecreator.core.roles.RoleService;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class CreatorAccessNetworking {
    public static final Identifier REQUEST_OPEN = new Identifier(InGameCreatorMod.MOD_ID, "request_open_creator");
    public static final Identifier OPEN_CREATOR = new Identifier(InGameCreatorMod.MOD_ID, "open_creator");

    private CreatorAccessNetworking() {}

    public static void registerServer(RoleService roles) {
        ServerPlayNetworking.registerGlobalReceiver(REQUEST_OPEN, (server, player, handler, buf, responseSender) ->
                server.execute(() -> {
                    if (roles.hasRole(player.getUuid(), CoderCommands.CODER_ROLE)) {
                        ServerPlayNetworking.send(player, OPEN_CREATOR, PacketByteBufs.empty());
                    } else {
                        player.sendMessage(Text.literal("[InGame Creator] Access denied. Required role: coder"), false);
                    }
                }));
    }
}
