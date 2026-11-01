package dev.ingamecreator.fabric;

import dev.ingamecreator.core.content.RuntimeBlockDefinition;
import dev.ingamecreator.core.content.RuntimeBlockIdentity;
import dev.ingamecreator.core.content.RuntimeAssetIds;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

import java.util.Map;
import java.util.Optional;

public final class RuntimeBlockStacks {
    public static final String TEXTURE_KEY = "igc_texture";
    public static final String MODEL_KEY = "igc_model";

    private RuntimeBlockStacks() {}

    public static Optional<RuntimeBlockIdentity> identity(ItemStack stack) {
        if (stack == null) return Optional.empty();
        NbtCompound tag = stack.getSubNbt("BlockEntityTag");
        if (tag == null) return Optional.empty();
        return RuntimeBlockIdentity.fromTags(Map.of(
                RuntimeBlockIdentity.PROJECT_KEY, tag.getString(RuntimeBlockIdentity.PROJECT_KEY),
                RuntimeBlockIdentity.BLOCK_KEY, tag.getString(RuntimeBlockIdentity.BLOCK_KEY)));
    }

    public static ItemStack create(String projectId, RuntimeBlockDefinition definition) {
        RuntimeBlockIdentity identity = new RuntimeBlockIdentity(projectId, definition.id());
        ItemStack stack = new ItemStack(CreatorContent.RUNTIME_BLOCK_ITEM);
        NbtCompound root = stack.getOrCreateNbt();
        root.putString(TEXTURE_KEY, definition.textureId());
        root.putString(MODEL_KEY, definition.modelId());
        root.putFloat("igc_hardness", definition.hardness());
        root.putFloat("igc_resistance", definition.resistance());
        root.putInt("igc_luminance", definition.luminance());
        root.putInt("CustomModelData", RuntimeAssetIds.customModelData(projectId, definition.id()));
        NbtCompound blockEntityTag = stack.getOrCreateSubNbt("BlockEntityTag");
        blockEntityTag.putString(RuntimeBlockIdentity.PROJECT_KEY, identity.projectId());
        blockEntityTag.putString(RuntimeBlockIdentity.BLOCK_KEY, identity.blockId());
        blockEntityTag.putInt("CustomModelData", RuntimeAssetIds.customModelData(projectId, definition.id()));
        stack.setCustomName(Text.literal(definition.displayName()));
        return stack;
    }
}
