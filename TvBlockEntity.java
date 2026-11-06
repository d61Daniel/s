package com.ps5mod;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;

public class TvBlockEntity extends BlockEntity {
    private BlockPos linked;

    public TvBlockEntity(BlockPos pos, BlockState state) {
        super(Ps5Mod.TV_BLOCK_ENTITY, pos, state);
    }

    public BlockPos getLinked() {
        return linked;
    }

    public void setLinked(BlockPos ps5) {
        this.linked = ps5;
        markDirty();
        if (world != null) {
            world.updateListeners(pos, getCachedState(), getCachedState(), Block.NOTIFY_ALL);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        if (linked != null) {
            nbt.putInt("lx", linked.getX());
            nbt.putInt("ly", linked.getY());
            nbt.putInt("lz", linked.getZ());
        }
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        linked = nbt.contains("lx") ? new BlockPos(nbt.getInt("lx"), nbt.getInt("ly"), nbt.getInt("lz")) : null;
    }

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup lookup) {
        return createNbt(lookup);
    }
}
