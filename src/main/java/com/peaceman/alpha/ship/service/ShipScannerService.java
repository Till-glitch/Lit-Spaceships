package com.peaceman.alpha.ship.service;

import com.peaceman.alpha.ship.domain.VoxelGridCache;
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
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Service für die Breitensuche (BFS) zur Erkennung zusammenhängender
 * Raumschiff-Strukturen in der Welt.
 */
public class ShipScannerService {

    public static final int MAX_SHIP_BLOCKS = 10000;

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

                            if (neighborState.getValue(BlockStateProperties.HORIZONTAL_FACING) == state
                                    .getValue(BlockStateProperties.HORIZONTAL_FACING)) {
                                toAdd.add(neighbor);
                            }
                        }
                    }
                }
            }
        }
        shipBlocks.addAll(toAdd);
    }

    public static final int MAX_SHIELD_GENERATORS = 64;

    /**
     * Berechnet die 3D-Voronoi-Tesselierung für jeden im VoxelGridCache gesetzten Hüllen-Voxel
     * basierend auf der quadrierten euklidischen Distanz zu allen aktiven Schildgeneratoren.
     *
     * @param cache         Der zu aktualisierende VoxelGridCache
     * @param generators    Liste der Generator-Positionen (absolut oder relativ zum controllerPos)
     * @param controllerPos Position des Controllers (wenn null, werden generators als relativ interpretiert)
     */
    public static void calculateVoronoiZones(VoxelGridCache cache, List<BlockPos> generators, BlockPos controllerPos) {
        if (cache == null || cache.isEmpty() || generators == null || generators.isEmpty()) {
            return;
        }

        int count = generators.size();
        if (count > MAX_SHIELD_GENERATORS) {
            com.peaceman.alpha.Alpha.LOGGER.warn("Ship has {} shield generators, exceeding max limit of {}. Truncating to {}.",
                    count, MAX_SHIELD_GENERATORS, MAX_SHIELD_GENERATORS);
            count = MAX_SHIELD_GENERATORS;
        }

        int[][] genRelCoords = new int[count][3];
        for (int i = 0; i < count; i++) {
            BlockPos genPos = generators.get(i);
            if (controllerPos != null) {
                genRelCoords[i][0] = genPos.getX() - controllerPos.getX();
                genRelCoords[i][1] = genPos.getY() - controllerPos.getY();
                genRelCoords[i][2] = genPos.getZ() - controllerPos.getZ();
            } else {
                genRelCoords[i][0] = genPos.getX();
                genRelCoords[i][1] = genPos.getY();
                genRelCoords[i][2] = genPos.getZ();
            }
        }

        BlockPos minOffset = cache.getMinOffset();
        int sizeX = cache.getSizeX();
        int sizeY = cache.getSizeY();
        int sizeZ = cache.getSizeZ();

        for (int x = 0; x < sizeX; x++) {
            int relX = minOffset.getX() + x;
            for (int y = 0; y < sizeY; y++) {
                int relY = minOffset.getY() + y;
                for (int z = 0; z < sizeZ; z++) {
                    int relZ = minOffset.getZ() + z;

                    if (!cache.isSet(relX, relY, relZ)) {
                        continue;
                    }

                    long minDistanceSq = Long.MAX_VALUE;
                    byte bestId = 1;

                    for (int g = 0; g < count; g++) {
                        long dx = relX - genRelCoords[g][0];
                        long dy = relY - genRelCoords[g][1];
                        long dz = relZ - genRelCoords[g][2];
                        long distSq = dx * dx + dy * dy + dz * dz;

                        // Deterministischer Tie-Break: Strikt kleiner (<) sorgt dafür, dass bei gleicher Distanz
                        // stets die kleinere Generator-ID (früherer Index) gewinnt.
                        if (distSq < minDistanceSq) {
                            minDistanceSq = distSq;
                            bestId = (byte) (g + 1);
                        }
                    }

                    cache.setShieldId(relX, relY, relZ, bestId);
                }
            }
        }
    }
}
