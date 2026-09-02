package com.example.demonicascension.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public class AbyssManager {

    /** Platforms sit this far apart, so nobody stumbles into a neighbour's. */
    private static final int SPACING = 20000;

    /** How many platforms per row before wrapping to the next. */
    private static final int GRID_WIDTH = 1000;

    public static final int PLATFORM_Y = 64;
    private static final int RADIUS = 8;   // 17x17 overall

    /**
     * Each player gets a deterministic slot derived from their UUID, so the
     * same player always returns to the same platform.
     */
    public static BlockPos platformCenter(UUID playerId) {
        // Non-negative hash, spread across a grid.
        int slot = Math.abs(playerId.hashCode() % (GRID_WIDTH * GRID_WIDTH));
        int gridX = slot % GRID_WIDTH;
        int gridZ = slot / GRID_WIDTH;

        return new BlockPos(gridX * SPACING, PLATFORM_Y, gridZ * SPACING);
    }

    /** Builds the platform if it isn't already there. */
    public static void ensurePlatform(ServerLevel level, BlockPos center) {
        // If the centre block is already solid, assume it's been built.
        if (!level.getBlockState(center).isAir()) {
            return;
        }

        BlockState blackstone = Blocks.BLACKSTONE.defaultBlockState();
        BlockState polished = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
        BlockState bricks = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        BlockState chiseled = Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState();

        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                int distance = Math.max(Math.abs(dx), Math.abs(dz));

                // Trim the corners so it reads as an octagon rather than a square.
                if (Math.abs(dx) + Math.abs(dz) > RADIUS + 4) {
                    continue;
                }

                BlockPos surface = center.offset(dx, 0, dz);

                BlockState top;
                if (distance == RADIUS) {
                    top = bricks;              // outer ring
                } else if (distance == 0) {
                    top = chiseled;            // centre marker
                } else if ((dx + dz) % 2 == 0) {
                    top = polished;            // checker pattern
                } else {
                    top = blackstone;
                }

                level.setBlockAndUpdate(surface, top);

                // Two layers of plain blackstone underneath for thickness.
                level.setBlockAndUpdate(surface.below(), blackstone);
                level.setBlockAndUpdate(surface.below(2), blackstone);
            }
        }

        // Four pillars at the corners of the inner ring, with soul lanterns on top.
        int p = RADIUS - 2;
        int[][] corners = {{p, p}, {p, -p}, {-p, p}, {-p, -p}};

        for (int[] corner : corners) {
            BlockPos base = center.offset(corner[0], 1, corner[1]);
            for (int h = 0; h < 4; h++) {
                level.setBlockAndUpdate(base.above(h),
                        Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
            }
            level.setBlockAndUpdate(base.above(4), Blocks.SOUL_LANTERN.defaultBlockState());
        }
    }

    /** Builds the platform if needed and puts the player safely on top of it. */
    public static void sendToAbyss(ServerPlayer player, ServerLevel abyss) {
        BlockPos center = platformCenter(player.getUUID());

        ensurePlatform(abyss, center);

        player.teleportTo(abyss,
                center.getX() + 0.5,
                center.getY() + 1.0,
                center.getZ() + 0.5,
                player.getYRot(),
                player.getXRot());

        player.resetFallDistance();
    }
}