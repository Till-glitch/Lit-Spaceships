package com.peaceman.alpha.ship.service;

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
}
