package com.eveportalnether.world.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public class PortalRecord {
    private final ResourceKey<Level> dimension;
    private final BlockPos pos;
    private int ticksRemaining;
    private int jumpsRemaining;

    public PortalRecord(ResourceKey<Level> dimension, BlockPos pos, int ticksRemaining, int jumpsRemaining) {
        this.dimension = dimension;
        this.pos = pos;
        this.ticksRemaining = ticksRemaining;
        this.jumpsRemaining = jumpsRemaining;
    }

    public ResourceKey<Level> getDimension() {
        return dimension;
    }

    public BlockPos getPos() {
        return pos;
    }

    public int getTicksRemaining() {
        return ticksRemaining;
    }

    public void setTicksRemaining(int ticksRemaining) {
        this.ticksRemaining = ticksRemaining;
    }

    public int getJumpsRemaining() {
        return jumpsRemaining;
    }

    public void setJumpsRemaining(int jumpsRemaining) {
        this.jumpsRemaining = jumpsRemaining;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Dimension", dimension.location().toString());
        tag.put("Pos", NbtUtils.writeBlockPos(pos));
        tag.putInt("TicksRemaining", ticksRemaining);
        tag.putInt("JumpsRemaining", jumpsRemaining);
        return tag;
    }

    public static PortalRecord load(CompoundTag tag) {
        ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(tag.getString("Dimension")));
        BlockPos pos = NbtUtils.readBlockPos(tag, "Pos").orElse(BlockPos.ZERO);
        int ticks = tag.getInt("TicksRemaining");
        int jumps = tag.getInt("JumpsRemaining");
        return new PortalRecord(dim, pos, ticks, jumps);
    }
}
