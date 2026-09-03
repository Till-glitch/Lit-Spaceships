package com.peaceman.alpha.ship.domain;

import net.minecraft.core.BlockPos;

import java.util.BitSet;
import java.util.Collection;

/**
 * Speicheroptimierter, linearisierter 3D-Voxel-Cache für Schiffsgeometrien (Hülle oder Schild).
 * Verwendet BitSets und 3D-Linearisierung (Index-Formel: x + y*W + z*W*H) für microsekundenschnelle
 * Kollisionsabfragen und maximale L1-Cache-Lokalität (Schritt 1).
 */
public class VoxelGridCache {

    public static final VoxelGridCache EMPTY = new VoxelGridCache(BlockPos.ZERO, 0, 0, 0, new BitSet(0));

    private final BlockPos minOffset; // Minimaler lokaler Offset relativ zum Controller
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;
    private final BitSet bitSet;
    private final byte[] shieldMap;

    public VoxelGridCache(BlockPos minOffset, int sizeX, int sizeY, int sizeZ, BitSet bitSet) {
        this(minOffset, sizeX, sizeY, sizeZ, bitSet, null);
    }

    public VoxelGridCache(BlockPos minOffset, int sizeX, int sizeY, int sizeZ, BitSet bitSet, byte[] shieldMap) {
        this.minOffset = minOffset != null ? minOffset : BlockPos.ZERO;
        this.sizeX = Math.max(0, sizeX);
        this.sizeY = Math.max(0, sizeY);
        this.sizeZ = Math.max(0, sizeZ);
        this.bitSet = bitSet != null ? bitSet : new BitSet(0);
        int totalVolume = this.sizeX * this.sizeY * this.sizeZ;
        if (shieldMap != null && shieldMap.length == totalVolume) {
            this.shieldMap = shieldMap;
        } else {
            this.shieldMap = new byte[totalVolume];
        }
    }

    public BlockPos getMinOffset() {
        return minOffset;
    }

    public int getSizeX() {
        return sizeX;
    }

    public int getSizeY() {
        return sizeY;
    }

    public int getSizeZ() {
        return sizeZ;
    }

    public BitSet getBitSet() {
        return bitSet;
    }

    public byte[] getShieldMap() {
        return shieldMap;
    }

    public boolean isEmpty() {
        return bitSet.isEmpty() || sizeX == 0 || sizeY == 0 || sizeZ == 0;
    }

    /**
     * Prüft, ob an der relativen BlockPos (relativ zum Controller) ein Voxel gesetzt ist.
     */
    public boolean isSet(BlockPos relativePos) {
        return relativePos != null && isSet(relativePos.getX(), relativePos.getY(), relativePos.getZ());
    }

    public boolean isSet(int relX, int relY, int relZ) {
        int x = relX - minOffset.getX();
        int y = relY - minOffset.getY();
        int z = relZ - minOffset.getZ();
        if (x < 0 || x >= sizeX || y < 0 || y >= sizeY || z < 0 || z >= sizeZ) {
            return false;
        }
        int index = x + (y * sizeX) + (z * sizeX * sizeY);
        return bitSet.get(index);
    }

    /**
     * Weist dem Voxel an den angegebenen relativen Koordinaten eine Schild-ID (1-64) zu.
     *
     * @throws IndexOutOfBoundsException wenn die Koordinaten außerhalb des Grids liegen
     */
    public void setShieldId(int relX, int relY, int relZ, byte id) {
        int x = relX - minOffset.getX();
        int y = relY - minOffset.getY();
        int z = relZ - minOffset.getZ();
        if (x < 0 || x >= sizeX || y < 0 || y >= sizeY || z < 0 || z >= sizeZ) {
            throw new IndexOutOfBoundsException("Coordinates out of VoxelGrid bounds: (" + relX + ", " + relY + ", " + relZ + ") with bounds size (" + sizeX + "," + sizeY + "," + sizeZ + ") and minOffset " + minOffset);
        }
        int index = x + (y * sizeX) + (z * sizeX * sizeY);
        shieldMap[index] = id;
    }

    public void setShieldId(BlockPos relativePos, byte id) {
        if (relativePos == null) {
            throw new NullPointerException("relativePos cannot be null");
        }
        setShieldId(relativePos.getX(), relativePos.getY(), relativePos.getZ(), id);
    }

    /**
     * Ermittelt die Schild-ID (1-64) für den Voxel an den relativen Koordinaten in O(1).
     * Gibt 0 zurück, wenn kein Schild zugewiesen ist oder die Koordinaten außerhalb der Bounds liegen.
     */
    public byte getShieldId(int relX, int relY, int relZ) {
        int x = relX - minOffset.getX();
        int y = relY - minOffset.getY();
        int z = relZ - minOffset.getZ();
        if (x < 0 || x >= sizeX || y < 0 || y >= sizeY || z < 0 || z >= sizeZ || shieldMap == null) {
            return 0;
        }
        int index = x + (y * sizeX) + (z * sizeX * sizeY);
        return shieldMap[index];
    }

    public byte getShieldId(BlockPos relativePos) {
        if (relativePos == null) return 0;
        return getShieldId(relativePos.getX(), relativePos.getY(), relativePos.getZ());
    }

    /**
     * Baut einen VoxelGridCache aus einer Menge relativer Blockpositionen.
     */
    public static VoxelGridCache buildFromRelative(Collection<BlockPos> relativePositions) {
        if (relativePositions == null || relativePositions.isEmpty()) {
            return EMPTY;
        }

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        for (BlockPos pos : relativePositions) {
            if (pos.getX() < minX) minX = pos.getX();
            if (pos.getY() < minY) minY = pos.getY();
            if (pos.getZ() < minZ) minZ = pos.getZ();
            if (pos.getX() > maxX) maxX = pos.getX();
            if (pos.getY() > maxY) maxY = pos.getY();
            if (pos.getZ() > maxZ) maxZ = pos.getZ();
        }

        int sizeX = maxX - minX + 1;
        int sizeY = maxY - minY + 1;
        int sizeZ = maxZ - minZ + 1;
        int totalVolume = sizeX * sizeY * sizeZ;

        BitSet bitSet = new BitSet(totalVolume);
        for (BlockPos pos : relativePositions) {
            int x = pos.getX() - minX;
            int y = pos.getY() - minY;
            int z = pos.getZ() - minZ;
            int index = x + (y * sizeX) + (z * sizeX * sizeY);
            bitSet.set(index);
        }

        return new VoxelGridCache(new BlockPos(minX, minY, minZ), sizeX, sizeY, sizeZ, bitSet);
    }

    /**
     * Baut einen VoxelGridCache aus absoluten Weltkoordinaten relativ zum übergebenen ControllerPos.
     */
    public static VoxelGridCache buildFromAbsolute(Collection<BlockPos> absolutePositions, BlockPos controllerPos) {
        if (absolutePositions == null || absolutePositions.isEmpty() || controllerPos == null) {
            return EMPTY;
        }

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        for (BlockPos pos : absolutePositions) {
            int rx = pos.getX() - controllerPos.getX();
            int ry = pos.getY() - controllerPos.getY();
            int rz = pos.getZ() - controllerPos.getZ();
            if (rx < minX) minX = rx;
            if (ry < minY) minY = ry;
            if (rz < minZ) minZ = rz;
            if (rx > maxX) maxX = rx;
            if (ry > maxY) maxY = ry;
            if (rz > maxZ) maxZ = rz;
        }

        int sizeX = maxX - minX + 1;
        int sizeY = maxY - minY + 1;
        int sizeZ = maxZ - minZ + 1;
        int totalVolume = sizeX * sizeY * sizeZ;

        BitSet bitSet = new BitSet(totalVolume);
        for (BlockPos pos : absolutePositions) {
            int x = (pos.getX() - controllerPos.getX()) - minX;
            int y = (pos.getY() - controllerPos.getY()) - minY;
            int z = (pos.getZ() - controllerPos.getZ()) - minZ;
            int index = x + (y * sizeX) + (z * sizeX * sizeY);
            bitSet.set(index);
        }

        return new VoxelGridCache(new BlockPos(minX, minY, minZ), sizeX, sizeY, sizeZ, bitSet);
    }
}
