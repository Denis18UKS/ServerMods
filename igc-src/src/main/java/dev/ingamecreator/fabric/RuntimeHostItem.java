package dev.ingamecreator.fabric;

import dev.ingamecreator.core.content.TriggerBinding;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.Map;

/** Single registered item used as a host for user-created runtime item definitions. */
public final class RuntimeHostItem extends Item {
    static final String GLINT_KEY = "igc_glint";

    public RuntimeHostItem(Settings settings) { super(settings); }

    @Override
    public boolean hasGlint(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.getBoolean(GLINT_KEY) || super.hasGlint(stack);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, net.minecraft.entity.player.PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient && user instanceof ServerPlayerEntity serverPlayer) runRightClickTrigger(serverPlayer, stack);
        return TypedActionResult.success(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        if (!world.isClient && entity instanceof ServerPlayerEntity serverPlayer) {
            RuntimeItemStacks.identity(stack).ifPresent(identity -> RuntimeTriggerRunner.run(
                    serverPlayer, identity.projectId(), TriggerBinding.ElementKind.ITEM, identity.itemId(),
                    "inventory_tick", Map.of(
                            "event", "inventory_tick",
                            "item", identity.itemId(),
                            "slot", Integer.toString(slot),
                            "selected", Boolean.toString(selected))));
        }
    }

    private static void runRightClickTrigger(ServerPlayerEntity player, ItemStack stack) {
        RuntimeItemStacks.identity(stack).ifPresent(identity -> RuntimeTriggerRunner.run(
                player, identity.projectId(), TriggerBinding.ElementKind.ITEM, identity.itemId(), "right_click",
                Map.of("event", "right_click", "item", identity.itemId())));
    }

}
