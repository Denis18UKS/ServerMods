package dev.ingamecreator.fabric;

import dev.ingamecreator.core.content.TriggerBinding;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Map;

/** Single registered block used as the physical host for user-created runtime block definitions. */
public final class RuntimeHostBlock extends BlockWithEntity {
    public RuntimeHostBlock(Settings settings) {
        super(settings);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new RuntimeBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.INVISIBLE;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        if (!world.isClient && placer instanceof ServerPlayerEntity serverPlayer) {
            RuntimeBlockStacks.identity(itemStack).ifPresent(identity -> {
                BlockEntity blockEntity = world.getBlockEntity(pos);
                if (blockEntity instanceof RuntimeBlockEntity runtimeBlock) {
                    runtimeBlock.setIdentity(identity);
                    if (itemStack.getNbt() != null) {
                        runtimeBlock.setCustomModelData(itemStack.getNbt().getInt("CustomModelData"));
                    }
                    world.updateListeners(pos, state, state, 3);
                }
                RuntimeTriggerRunner.run(serverPlayer, identity.projectId(), TriggerBinding.ElementKind.BLOCK,
                        identity.blockId(), "placed", positionArgs("placed", identity.blockId(), pos));
            });
        }
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof RuntimeBlockEntity runtimeBlock) {
                runtimeBlock.identity().ifPresent(identity -> RuntimeTriggerRunner.run(
                        serverPlayer, identity.projectId(), TriggerBinding.ElementKind.BLOCK, identity.blockId(),
                        "broken", positionArgs("broken", identity.blockId(), pos)));
            }
        }
        super.onBreak(world, pos, state, player);
    }

    private static Map<String, String> positionArgs(String event, String blockId, BlockPos pos) {
        return Map.of(
                "event", event,
                "block", blockId,
                "x", Integer.toString(pos.getX()),
                "y", Integer.toString(pos.getY()),
                "z", Integer.toString(pos.getZ()));
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof RuntimeBlockEntity runtimeBlock) {
                runtimeBlock.identity().ifPresent(identity -> RuntimeTriggerRunner.run(
                        serverPlayer,
                        identity.projectId(),
                        TriggerBinding.ElementKind.BLOCK,
                        identity.blockId(),
                        "right_click",
                        positionArgs("right_click", identity.blockId(), pos)));
            }
        }
        return ActionResult.success(world.isClient);
    }
}
