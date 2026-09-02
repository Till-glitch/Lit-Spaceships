package com.peaceman.alpha.ship.relocation.util;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;

import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

/**
 * Universeller NBT-Koordinator-Remapper für Master-Slave-Multiblöcke aus Dritt-Mods
 * (z. B. Mekanism, Immersive Engineering, Create, AE2).
 * Inspiziert rekursiv CompoundTags und transformiert absolute BlockPos-Referenzen,
 * die innerhalb des Schiffs liegen, auf die neuen Zielkoordinaten.
 */
public final class NbtCoordinateRemapper {

    private NbtCoordinateRemapper() {}

    /**
     * Rekursiver Scan und Transformation aller Koordinaten im NBT eines BlockEntitys.
     *
     * @param tag           Das zu scannende CompoundTag
     * @param oldShipBlocks Die Menge aller alten Block-Koordinaten des Schiffs
     * @param transform     Die Transformationsfunktion (z. B. pos -> pos.offset(dx, dy, dz) oder Rotation)
     * @return true, falls mindestens eine Koordinate umgeschrieben wurde
     */
    public static boolean remapCoordinates(CompoundTag tag, Set<BlockPos> oldShipBlocks, Function<BlockPos, BlockPos> transform) {
        if (tag == null || oldShipBlocks == null || oldShipBlocks.isEmpty() || transform == null) {
            return false;
        }
        return remapCompound(tag, oldShipBlocks, transform);
    }

    private static boolean remapCompound(CompoundTag tag, Set<BlockPos> oldShipBlocks, Function<BlockPos, BlockPos> transform) {
        boolean modified = false;

        // 1. Prüfen auf direktes {x, y, z} oder {X, Y, Z} Muster
        if (tag.contains("x", Tag.TAG_INT) && tag.contains("y", Tag.TAG_INT) && tag.contains("z", Tag.TAG_INT)) {
            BlockPos pos = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
            if (oldShipBlocks.contains(pos)) {
                BlockPos newPos = transform.apply(pos);
                tag.putInt("x", newPos.getX());
                tag.putInt("y", newPos.getY());
                tag.putInt("z", newPos.getZ());
                modified = true;
            }
        } else if (tag.contains("X", Tag.TAG_INT) && tag.contains("Y", Tag.TAG_INT) && tag.contains("Z", Tag.TAG_INT)) {
            BlockPos pos = new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"));
            if (oldShipBlocks.contains(pos)) {
                BlockPos newPos = transform.apply(pos);
                tag.putInt("X", newPos.getX());
                tag.putInt("Y", newPos.getY());
                tag.putInt("Z", newPos.getZ());
                modified = true;
            }
        }

        // 2. Rekursive Iteration über alle Tags im Compound
        for (String key : tag.getAllKeys()) {
            Tag child = tag.get(key);
            if (child instanceof CompoundTag childCompound) {
                if (remapCompound(childCompound, oldShipBlocks, transform)) {
                    modified = true;
                }
            } else if (child instanceof ListTag listTag) {
                if (remapList(listTag, oldShipBlocks, transform)) {
                    modified = true;
                }
            } else if (child instanceof IntArrayTag intArrayTag) {
                int[] array = intArrayTag.getAsIntArray();
                if (array.length == 3) {
                    BlockPos pos = new BlockPos(array[0], array[1], array[2]);
                    if (oldShipBlocks.contains(pos)) {
                        BlockPos newPos = transform.apply(pos);
                        tag.putIntArray(key, new int[]{newPos.getX(), newPos.getY(), newPos.getZ()});
                        modified = true;
                    }
                }
            } else if (child instanceof LongTag longTag) {
                String lowerKey = key.toLowerCase(Locale.ROOT);
                if (lowerKey.contains("pos") || lowerKey.contains("master") || lowerKey.contains("controller")
                        || lowerKey.contains("core") || lowerKey.contains("target") || lowerKey.contains("link")
                        || lowerKey.contains("parent") || lowerKey.contains("origin")) {
                    try {
                        BlockPos pos = BlockPos.of(longTag.getAsLong());
                        if (oldShipBlocks.contains(pos)) {
                            BlockPos newPos = transform.apply(pos);
                            tag.putLong(key, newPos.asLong());
                            modified = true;
                        }
                    } catch (Exception ignored) {
                        // Kein valides BlockPos Long
                    }
                }
            }
        }

        return modified;
    }

    private static boolean remapList(ListTag listTag, Set<BlockPos> oldShipBlocks, Function<BlockPos, BlockPos> transform) {
        boolean modified = false;
        for (int i = 0; i < listTag.size(); i++) {
            Tag element = listTag.get(i);
            if (element instanceof CompoundTag compound) {
                if (remapCompound(compound, oldShipBlocks, transform)) {
                    modified = true;
                }
            } else if (element instanceof IntArrayTag intArrayTag) {
                int[] array = intArrayTag.getAsIntArray();
                if (array.length == 3) {
                    BlockPos pos = new BlockPos(array[0], array[1], array[2]);
                    if (oldShipBlocks.contains(pos)) {
                        BlockPos newPos = transform.apply(pos);
                        listTag.set(i, new IntArrayTag(new int[]{newPos.getX(), newPos.getY(), newPos.getZ()}));
                        modified = true;
                    }
                }
            } else if (element instanceof LongTag longTag) {
                try {
                    BlockPos pos = BlockPos.of(longTag.getAsLong());
                    if (oldShipBlocks.contains(pos)) {
                        BlockPos newPos = transform.apply(pos);
                        listTag.set(i, LongTag.valueOf(newPos.asLong()));
                        modified = true;
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return modified;
    }
}
