package com.eveportalnether.world;

import com.eveportalnether.Config;
import com.eveportalnether.block.WormholeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

public final class PortalShape {
    private PortalShape() {
    }

    public static int rollWidth(PortalTier tier, RandomSource random) {
        return rollSize(tier, random, Config.MIN_PORTAL_WIDTH.get(), Config.MAX_PORTAL_WIDTH.get());
    }

    public static int rollHeight(PortalTier tier, RandomSource random) {
        return rollSize(tier, random, Config.MIN_PORTAL_HEIGHT.get(), Config.MAX_PORTAL_HEIGHT.get());
    }

    private static int rollSize(PortalTier tier, RandomSource random, int min, int max) {
        max = Math.max(min, max);
        int mid = (min + max) / 2;
        int low = tier == PortalTier.MASSIVE ? mid : min;
        int high = tier == PortalTier.UNSTABLE ? mid : max;
        return low + random.nextInt(high - low + 1);
    }

    public static BlockPos offset(BlockPos origin, int across, int up, Direction.Axis axis) {
        return axis == Direction.Axis.X
                ? origin.offset(0, up, across)
                : origin.offset(across, up, 0);
    }

    public static boolean isFrame(int across, int up, int width, int height) {
        return across == 0 || across == width - 1 || up == 0 || up == height - 1;
    }

    public static BlockPos standPos(BlockPos origin, Direction.Axis axis, int width, int height) {
        return offset(origin, width / 2, 1, axis);
    }

    public static boolean contains(BlockPos origin, Direction.Axis axis, int width, int height, BlockPos pos) {
        for (int across = 0; across < width; across++) {
            for (int up = 0; up < height; up++) {
                if (offset(origin, across, up, axis).equals(pos)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isVolumeReplaceable(Level level, BlockPos origin, Direction.Axis axis, int width, int height) {
        for (int across = 0; across < width; across++) {
            for (int up = 0; up < height; up++) {
                if (!isReplaceable(level.getBlockState(offset(origin, across, up, axis)))) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean hasSolidFooting(Level level, BlockPos origin, Direction.Axis axis, int width) {
        for (int across = 0; across < width; across++) {
            BlockPos below = offset(origin, across, 0, axis).below();
            BlockState state = level.getBlockState(below);
            if (!state.isSolidRender(level, below) || !state.getFluidState().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public static boolean isReplaceable(BlockState state) {
        return (state.isAir() || state.canBeReplaced()) && state.getFluidState().isEmpty();
    }

    /**
     * Clears a short air pocket on both faces of the inner window so a player
     * is not entombed in netherrack and can mine blocks beyond the portal.
     */
    public static void carveLandingPocket(Level level, BlockPos origin, Direction.Axis axis, int width, int height) {
        Direction.Axis normal = axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        for (int side : new int[] {-2, -1, 1, 2}) {
            for (int across = 1; across < width - 1; across++) {
                for (int up = 1; up < height - 1; up++) {
                    BlockPos onPortal = offset(origin, across, up, axis);
                    BlockPos pocket = onPortal.relative(Direction.fromAxisAndDirection(normal, side > 0
                            ? Direction.AxisDirection.POSITIVE
                            : Direction.AxisDirection.NEGATIVE), Math.abs(side));
                    tryCarve(level, pocket);
                }
            }
        }

        // Stable floor just in front of the window, both sides.
        for (int side : new int[] {-1, 1}) {
            for (int across = 1; across < width - 1; across++) {
                BlockPos window = offset(origin, across, 1, axis);
                BlockPos floor = window.relative(Direction.fromAxisAndDirection(normal, side > 0
                        ? Direction.AxisDirection.POSITIVE
                        : Direction.AxisDirection.NEGATIVE)).below();
                BlockState floorState = level.getBlockState(floor);
                if (!floorState.isSolidRender(level, floor) || !floorState.getFluidState().isEmpty()) {
                    if (canCarve(level, floor, floorState) || floorState.getFluidState().is(Fluids.LAVA)
                            || floorState.isAir()) {
                        boolean nether = level.dimension() == Level.NETHER;
                        level.setBlock(floor, (nether ? Blocks.NETHERRACK : Blocks.COBBLESTONE).defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private static void tryCarve(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!canCarve(level, pos, state) && state.getFluidState().isEmpty()) {
            return;
        }
        if (state.getBlock() instanceof WormholeBlock) {
            return;
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    private static boolean canCarve(Level level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof WormholeBlock) {
            return false;
        }
        if (state.getDestroySpeed(level, pos) < 0.0F) {
            return false;
        }
        if (level.getBlockEntity(pos) != null) {
            return false;
        }
        return !state.isAir();
    }
}
