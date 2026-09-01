package com.peaceman.alpha.ship.domain;

import net.minecraft.core.BlockPos;

/**
 * Unveränderliches Telemetrie-Record für die räumliche Voronoi-Sektorabdeckung eines Schildgenerators.
 *
 * @param zoneId           Die numerische Zonen-ID (1-basiert, 1 bis 64).
 * @param generatorPos     Die relative oder absolute Position des Schildgenerators.
 * @param assignedVoxels   Anzahl der dieser Zone zugewiesenen Hüllen- und Schildblöcke.
 * @param totalShipVoxels  Gesamtanzahl aller Blöcke des Schiffs.
 * @param minRelative      Minimaler relativer Koordinatenpunkt der geschützten Voxel-Region.
 * @param maxRelative      Maximaler relativer Koordinatenpunkt der geschützten Voxel-Region.
 */
public record SectorCoverage(
        byte zoneId,
        BlockPos generatorPos,
        int assignedVoxels,
        int totalShipVoxels,
        BlockPos minRelative,
        BlockPos maxRelative
) {
    /**
     * Berechnet die relative Abdeckungsrate in Prozent (0.0% bis 100.0%).
     */
    public float getCoverageRatio() {
        if (totalShipVoxels <= 0) return 0.0f;
        return ((float) assignedVoxels / (float) totalShipVoxels) * 100.0f;
    }

    /**
     * Liefert die Ausdehnung des Sektors in Metern (X-Breite).
     */
    public int getSpanX() {
        if (assignedVoxels <= 0) return 0;
        return minRelative != null && maxRelative != null ? (maxRelative.getX() - minRelative.getX() + 1) : 0;
    }

    /**
     * Liefert die Ausdehnung des Sektors in Metern (Y-Höhe).
     */
    public int getSpanY() {
        if (assignedVoxels <= 0) return 0;
        return minRelative != null && maxRelative != null ? (maxRelative.getY() - minRelative.getY() + 1) : 0;
    }

    /**
     * Liefert die Ausdehnung des Sektors in Metern (Z-Tiefe).
     */
    public int getSpanZ() {
        if (assignedVoxels <= 0) return 0;
        return minRelative != null && maxRelative != null ? (maxRelative.getZ() - minRelative.getZ() + 1) : 0;
    }

    /**
     * Fallback-Instanz für uninitialisierte oder leere Sektoren.
     */
    public static SectorCoverage empty(byte zoneId, BlockPos generatorPos) {
        return new SectorCoverage(zoneId, generatorPos, 0, 0, BlockPos.ZERO, BlockPos.ZERO);
    }
}
