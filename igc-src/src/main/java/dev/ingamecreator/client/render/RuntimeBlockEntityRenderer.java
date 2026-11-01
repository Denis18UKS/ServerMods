package dev.ingamecreator.client.render;

import dev.ingamecreator.fabric.CreatorContent;
import dev.ingamecreator.fabric.RuntimeBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

/** Renders each runtime host block using the block item's CustomModelData model. */
public final class RuntimeBlockEntityRenderer implements BlockEntityRenderer<RuntimeBlockEntity> {
    public RuntimeBlockEntityRenderer(BlockEntityRendererFactory.Context context) {}

    @Override
    public void render(RuntimeBlockEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
        int customModelData = entity.customModelData();
        if (customModelData <= 0) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getItemRenderer() == null) return;
        ItemStack stack = new ItemStack(CreatorContent.RUNTIME_BLOCK_ITEM);
        stack.getOrCreateNbt().putInt("CustomModelData", customModelData);
        matrices.push();
        matrices.translate(0.5, 0.5, 0.5);
        client.getItemRenderer().renderItem(
                stack,
                ModelTransformationMode.FIXED,
                light,
                overlay,
                matrices,
                vertexConsumers,
                entity.getWorld(),
                entity.getPos().hashCode());
        matrices.pop();
    }
}
