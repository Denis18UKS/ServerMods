package dev.ingamecreator.fabric;

import dev.ingamecreator.core.content.RuntimeBlockIdentity;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.Optional;

/** Stores which Creator project/block definition a placed runtime host block represents. */
public final class RuntimeBlockEntity extends BlockEntity {
    private RuntimeBlockIdentity identity;
    private int customModelData;

    public RuntimeBlockEntity(BlockPos pos, BlockState state) {
        super(CreatorContent.RUNTIME_BLOCK_ENTITY, pos, state);
    }

    public Optional<RuntimeBlockIdentity> identity() {
        return Optional.ofNullable(identity);
    }

    public void setIdentity(RuntimeBlockIdentity identity) {
        this.identity = identity;
        markDirty();
    }

    public int customModelData() { return customModelData; }

    public void setCustomModelData(int customModelData) {
        this.customModelData = Math.max(0, customModelData);
        markDirty();
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        if (identity != null) {
            nbt.putString(RuntimeBlockIdentity.PROJECT_KEY, identity.projectId());
            nbt.putString(RuntimeBlockIdentity.BLOCK_KEY, identity.blockId());
        }
        if (customModelData > 0) nbt.putInt("CustomModelData", customModelData);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        identity = RuntimeBlockIdentity.fromTags(Map.of(
                RuntimeBlockIdentity.PROJECT_KEY, nbt.getString(RuntimeBlockIdentity.PROJECT_KEY),
                RuntimeBlockIdentity.BLOCK_KEY, nbt.getString(RuntimeBlockIdentity.BLOCK_KEY))).orElse(null);
        customModelData = nbt.getInt("CustomModelData");
    }

    @Override
    public BlockEntityUpdateS2CPacket toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }
}
