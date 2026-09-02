package com.peaceman.alpha.ship.service;

import com.peaceman.alpha.ship.domain.VoxelGridCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
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

            // 4. Ausgefahrene Pistons & Piston-Heads
            if (state.getBlock() instanceof net.minecraft.world.level.block.piston.PistonBaseBlock &&
                    state.hasProperty(BlockStateProperties.EXTENDED) &&
                    state.getValue(BlockStateProperties.EXTENDED) &&
                    state.hasProperty(BlockStateProperties.FACING)) {
                Direction facing = state.getValue(BlockStateProperties.FACING);
                toAdd.add(pos.relative(facing));
            } else if (state.getBlock() instanceof net.minecraft.world.level.block.piston.PistonHeadBlock &&
                    state.hasProperty(BlockStateProperties.FACING)) {
                Direction facing = state.getValue(BlockStateProperties.FACING);
                toAdd.add(pos.relative(facing.getOpposite()));
            }

            // 5. Universelle Mod-Multiblock Erfassung über NBT-Pointers (masterPos, controllerPos)
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null) {
                try {
                    CompoundTag tag = be.saveWithFullMetadata(level.registryAccess());
                    findReferencedPositions(tag, pos, 32, toAdd, level);
                } catch (Exception ignored) {
                }
            }

            // 6. Cluster-Tag (#c:relocates_as_cluster)
            if (state.is(com.peaceman.alpha.registry.ModTags.Blocks.RELOCATES_AS_CLUSTER)) {
                for (Direction dir : Direction.values()) {
                    BlockPos neighbor = pos.relative(dir);
                    if (level.getBlockState(neighbor).is(com.peaceman.alpha.registry.ModTags.Blocks.RELOCATES_AS_CLUSTER)) {
                        toAdd.add(neighbor);
                    }
                }
            }
        }
        shipBlocks.addAll(toAdd);
    }

    private static void findReferencedPositions(CompoundTag tag, BlockPos origin, int maxDist, Set<BlockPos> out, Level level) {
        if (tag == null) return;
        if (tag.contains("x", Tag.TAG_INT) && tag.contains("y", Tag.TAG_INT) && tag.contains("z", Tag.TAG_INT)) {
            BlockPos target = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
            if (!target.equals(origin) && target.closerThan(origin, maxDist) && !level.getBlockState(target).isAir()) {
                out.add(target);
            }
        }
        for (String key : tag.getAllKeys()) {
            String lowerKey = key.toLowerCase(Locale.ROOT);
            Tag child = tag.get(key);
            if (child instanceof CompoundTag childCompound) {
                if (lowerKey.contains("master") || lowerKey.contains("controller") || lowerKey.contains("core")
                        || lowerKey.contains("parent") || lowerKey.contains("link") || lowerKey.contains("target")) {
                    if (childCompound.contains("x", Tag.TAG_INT) && childCompound.contains("y", Tag.TAG_INT) && childCompound.contains("z", Tag.TAG_INT)) {
                        BlockPos target = new BlockPos(childCompound.getInt("x"), childCompound.getInt("y"), childCompound.getInt("z"));
                        if (!target.equals(origin) && target.closerThan(origin, maxDist) && !level.getBlockState(target).isAir()) {
                            out.add(target);
                        }
                    }
                }
                findReferencedPositions(childCompound, origin, maxDist, out, level);
            } else if (child instanceof IntArrayTag intArrayTag) {
                if (lowerKey.contains("master") || lowerKey.contains("controller") || lowerKey.contains("core")
                        || lowerKey.contains("pos")) {
                    int[] arr = intArrayTag.getAsIntArray();
                    if (arr.length == 3) {
                        BlockPos target = new BlockPos(arr[0], arr[1], arr[2]);
                        if (!target.equals(origin) && target.closerThan(origin, maxDist) && !level.getBlockState(target).isAir()) {
                            out.add(target);
                        }
                    }
                }
            }
        }
    }

    public static final int MAX_SHIELD_GENERATORS = 64;

    /**
     * Berechnet die 3D-Voronoi-Tesselierung für jeden im VoxelGridCache gesetzten Hüllen-Voxel
     * basierend auf der quadrierten euklidischen Distanz zu allen aktiven Schildgeneratoren
     * und liefert die berechneten Sektor-Abdeckungsdaten (SectorCoverage) zurück.
     *
     * @param cache         Der zu aktualisierende VoxelGridCache
     * @param generators    Liste der Generator-Positionen (absolut oder relativ zum controllerPos)
     * @param controllerPos Position des Controllers (wenn null, werden generators als relativ interpretiert)
     * @return Map aller berechneten SectorCoverages für jede Zonen-ID (1-basiert)
     */
    public static java.util.Map<Byte, com.peaceman.alpha.ship.domain.SectorCoverage> calculateVoronoiZones(
            VoxelGridCache cache, List<BlockPos> generators, BlockPos controllerPos) {
        if (cache == null || cache.isEmpty() || generators == null || generators.isEmpty()) {
            return java.util.Collections.emptyMap();
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

        int[] assignedVoxels = new int[count];
        int[] minRelX = new int[count]; java.util.Arrays.fill(minRelX, Integer.MAX_VALUE);
        int[] maxRelX = new int[count]; java.util.Arrays.fill(maxRelX, Integer.MIN_VALUE);
        int[] minRelY = new int[count]; java.util.Arrays.fill(minRelY, Integer.MAX_VALUE);
        int[] maxRelY = new int[count]; java.util.Arrays.fill(maxRelY, Integer.MIN_VALUE);
        int[] minRelZ = new int[count]; java.util.Arrays.fill(minRelZ, Integer.MAX_VALUE);
        int[] maxRelZ = new int[count]; java.util.Arrays.fill(maxRelZ, Integer.MIN_VALUE);
        int totalVoxels = 0;

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

                    totalVoxels++;
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

                    int gIdx = (bestId & 0xFF) - 1;
                    if (gIdx >= 0 && gIdx < count) {
                        assignedVoxels[gIdx]++;
                        if (relX < minRelX[gIdx]) minRelX[gIdx] = relX;
                        if (relX > maxRelX[gIdx]) maxRelX[gIdx] = relX;
                        if (relY < minRelY[gIdx]) minRelY[gIdx] = relY;
                        if (relY > maxRelY[gIdx]) maxRelY[gIdx] = relY;
                        if (relZ < minRelZ[gIdx]) minRelZ[gIdx] = relZ;
                        if (relZ > maxRelZ[gIdx]) maxRelZ[gIdx] = relZ;
                    }
                }
            }
        }

        java.util.Map<Byte, com.peaceman.alpha.ship.domain.SectorCoverage> coverages = new java.util.HashMap<>();
        for (int i = 0; i < count; i++) {
            byte zId = (byte) (i + 1);
            BlockPos genPos = generators.get(i);
            BlockPos minB = assignedVoxels[i] > 0 ? new BlockPos(minRelX[i], minRelY[i], minRelZ[i]) : BlockPos.ZERO;
            BlockPos maxB = assignedVoxels[i] > 0 ? new BlockPos(maxRelX[i], maxRelY[i], maxRelZ[i]) : BlockPos.ZERO;
            coverages.put(zId, new com.peaceman.alpha.ship.domain.SectorCoverage(zId, genPos, assignedVoxels[i], totalVoxels, minB, maxB));
        }

        return coverages;
    }
}
