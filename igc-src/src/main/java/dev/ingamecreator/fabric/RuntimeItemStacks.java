package dev.ingamecreator.fabric;

import dev.ingamecreator.core.content.RuntimeItemDefinition;
import dev.ingamecreator.core.content.RuntimeItemIdentity;
import dev.ingamecreator.core.content.RuntimeAssetIds;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

import java.util.Map;
import java.util.Optional;

public final class RuntimeItemStacks {
    public static final String DESCRIPTION_KEY = "igc_description";
    public static final String MAX_STACK_KEY = "igc_max_stack";
    public static final String TEXTURE_KEY = "igc_texture";
    public static final String MODEL_KEY = "igc_model";

    private RuntimeItemStacks() {}

    public static ItemStack create(String projectId, RuntimeItemDefinition definition) {
        RuntimeItemIdentity identity = new RuntimeItemIdentity(projectId, definition.id());
        ItemStack stack = new ItemStack(CreatorContent.RUNTIME_ITEM);
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putString(RuntimeItemIdentity.PROJECT_KEY, identity.projectId());
        nbt.putString(RuntimeItemIdentity.ITEM_KEY, identity.itemId());
        nbt.putString(DESCRIPTION_KEY, definition.description());
        nbt.putInt(MAX_STACK_KEY, definition.maxStack());
        nbt.putBoolean(RuntimeHostItem.GLINT_KEY, definition.glint());
        nbt.putString(TEXTURE_KEY, definition.textureId());
        nbt.putString(MODEL_KEY, definition.modelId());
        nbt.putInt("CustomModelData", RuntimeAssetIds.customModelData(projectId, definition.id()));
        stack.setCustomName(Text.literal(definition.displayName()));
        return stack;
    }

    public static Optional<RuntimeItemIdentity> identity(ItemStack stack) {
        if (stack == null) return Optional.empty();
        NbtCompound nbt = stack.getNbt();
        if (nbt == null) return Optional.empty();
        return RuntimeItemIdentity.fromTags(Map.of(
                RuntimeItemIdentity.PROJECT_KEY, nbt.getString(RuntimeItemIdentity.PROJECT_KEY),
                RuntimeItemIdentity.ITEM_KEY, nbt.getString(RuntimeItemIdentity.ITEM_KEY)));
    }
}
