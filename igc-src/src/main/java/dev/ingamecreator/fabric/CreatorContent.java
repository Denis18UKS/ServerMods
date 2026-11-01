package dev.ingamecreator.fabric;

import dev.ingamecreator.InGameCreatorMod;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class CreatorContent {
    public static final Item CREATOR_TABLET = registerItem("creator_tablet", new Item(new Item.Settings().maxCount(1)));
    public static final Item BLUEPRINT_CHIP = registerItem("blueprint_chip", new Item(new Item.Settings()));
    public static final Item COMMAND_TOKEN = registerItem("command_token", new Item(new Item.Settings()));
    public static final Item ROLE_BADGE = registerItem("role_badge", new Item(new Item.Settings()));
    public static final Item RUNTIME_ITEM = registerItem("runtime_item", new RuntimeHostItem(new Item.Settings().maxCount(64)));
    public static final Block RUNTIME_BLOCK = registerBlock("runtime_block", new RuntimeHostBlock(
            FabricBlockSettings.create().strength(2.0f, 6.0f)));
    public static final BlockEntityType<RuntimeBlockEntity> RUNTIME_BLOCK_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE, id("runtime_block"),
            FabricBlockEntityTypeBuilder.create(RuntimeBlockEntity::new, RUNTIME_BLOCK).build());
    public static final Item RUNTIME_BLOCK_ITEM = registerItem("runtime_block",
            new BlockItem(RUNTIME_BLOCK, new Item.Settings()));

    public static final RegistryKey<ItemGroup> GROUP_KEY = RegistryKey.of(
            RegistryKeys.ITEM_GROUP,
            id("creator_tools"));

    private static boolean registered;

    private CreatorContent() {}

    public static void register() {
        if (registered) return;
        registered = true;
        Registry.register(Registries.ITEM_GROUP, GROUP_KEY, FabricItemGroup.builder()
                .displayName(Text.translatable("itemGroup.ingamecreator.creator_tools"))
                .icon(() -> new ItemStack(CREATOR_TABLET))
                .entries((context, entries) -> {
                    entries.add(CREATOR_TABLET);
                    entries.add(BLUEPRINT_CHIP);
                    entries.add(COMMAND_TOKEN);
                    entries.add(ROLE_BADGE);
                    entries.add(RUNTIME_ITEM);
                    entries.add(RUNTIME_BLOCK_ITEM);
                })
                .build());
    }

    private static Block registerBlock(String path, Block block) {
        return Registry.register(Registries.BLOCK, id(path), block);
    }

    private static Item registerItem(String path, Item item) {
        return Registry.register(Registries.ITEM, id(path), item);
    }

    private static Identifier id(String path) {
        return new Identifier(InGameCreatorMod.MOD_ID, path);
    }
}
