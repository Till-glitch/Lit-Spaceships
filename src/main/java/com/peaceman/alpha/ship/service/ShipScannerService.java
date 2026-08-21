package com.peaceman.alpha.ship.service;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

/**
 * Service für die Breitensuche (BFS) zur Erkennung zusammenhängender Raumschiff-Strukturen in der Welt.
 */
public class ShipScannerService {

    public static final int MAX_SHIP_BLOCKS = 5000;

    public static Set<BlockPos> scan(Level level, BlockPos startPos) {
        Set<BlockPos> shipBlocks = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();

        queue.add(startPos);
        shipBlocks.add(startPos);

        while (!queue.isEmpty() && shipBlocks.size() < MAX_SHIP_BLOCKS) {
            BlockPos current = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);
                if (!level.getBlockState(neighbor).isAir() && !shipBlocks.contains(neighbor)) {
                    shipBlocks.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        ensureMultipartBlocks(level, shipBlocks);

        return shipBlocks;
    }

    private static void ensureMultipartBlocks(Level level, Set<BlockPos> shipBlocks) {
        Set<BlockPos> toAdd = new HashSet<>();
        for (BlockPos pos : shipBlocks) {
            BlockState state = level.getBlockState(pos);

            // 1. Türen, hohe Blumen etc.
            if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
                if (state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER) {
                    toAdd.add(pos.above());
                } else {
                    toAdd.add(pos.below());
                }
            }

            // 2. Betten
            if (state.hasProperty(BlockStateProperties.BED_PART)) {
                Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
                if (state.getValue(BlockStateProperties.BED_PART) == BedPart.HEAD) {
                    toAdd.add(pos.relative(facing.getOpposite()));
                } else {
                    toAdd.add(pos.relative(facing));
                }
            }

            // 3. Doppeltruhen
            if (state.hasProperty(BlockStateProperties.CHEST_TYPE)) {
                ChestType type = state.getValue(BlockStateProperties.CHEST_TYPE);
                if (type != ChestType.SINGLE) {
                    for (Direction dir : Direction.Plane.HORIZONTAL) {
                        BlockPos neighbor = pos.relative(dir);
                        BlockState neighborState = level.getBlockState(neighbor);

                        if (neighborState.getBlock() == state.getBlock() &&
                                neighborState.hasProperty(BlockStateProperties.CHEST_TYPE) &&
                                neighborState.getValue(BlockStateProperties.CHEST_TYPE) != ChestType.SINGLE) {

                            if (neighborState.getValue(BlockStateProperties.HORIZONTAL_FACING) == state.getValue(BlockStateProperties.HORIZONTAL_FACING)) {
                                toAdd.add(neighbor);
                            }
                        }
                    }
                }
            }
        }
        shipBlocks.addAll(toAdd);
    }
}
