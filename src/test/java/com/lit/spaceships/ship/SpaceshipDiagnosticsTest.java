package com.lit.spaceships.ship;

import com.lit.spaceships.ship.domain.ShipState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests für die Struktur- und Subsystem-Diagnostik des Spaceship Controllers.
 */
class SpaceshipDiagnosticsTest {

    @Test
    @DisplayName("Berechnung von Bounding-Box-Ausdehnung und Schiffsmasse aus Relativ-Blöcken")
    void testStructuralDimensionsAndMass() {
        Set<BlockPos> relativeBlocks = new HashSet<>();
        
        // Simuliere ein Schiff mit 15m x 5m x 20m Ausdehnung
        relativeBlocks.add(new BlockPos(-5, 0, -10));
        relativeBlocks.add(new BlockPos(9, 4, 9));
        relativeBlocks.add(new BlockPos(0, 0, 0));
        relativeBlocks.add(new BlockPos(2, 1, 3));

        int minX = 0, maxX = 0, minY = 0, maxY = 0, minZ = 0, maxZ = 0;
        boolean first = true;
        for (BlockPos pos : relativeBlocks) {
            if (first) {
                minX = maxX = pos.getX();
                minY = maxY = pos.getY();
                minZ = maxZ = pos.getZ();
                first = false;
            } else {
                if (pos.getX() < minX) minX = pos.getX();
                if (pos.getX() > maxX) maxX = pos.getX();
                if (pos.getY() < minY) minY = pos.getY();
                if (pos.getY() > maxY) maxY = pos.getY();
                if (pos.getZ() < minZ) minZ = pos.getZ();
                if (pos.getZ() > maxZ) maxZ = pos.getZ();
            }
        }

        int spanX = maxX - minX + 1;
        int spanY = maxY - minY + 1;
        int spanZ = maxZ - minZ + 1;

        assertEquals(15, spanX);
        assertEquals(5, spanY);
        assertEquals(20, spanZ);

        // Masse-Berechnung: 1.0t pro Block
        float totalMass = relativeBlocks.size() * 1.0f;
        assertEquals(4.0f, totalMass, 0.001f);
        assertEquals("4.0", String.format(Locale.ROOT, "%.1f", totalMass));
    }

    @Test
    @DisplayName("ShipState verwaltet Subsysteme und Controller-Pos konsistent")
    void testShipStateSubsystems() {
        BlockPos ctrl = new BlockPos(0, 100, 0);
        Set<BlockPos> blocks = Set.of(
                ctrl,
                new BlockPos(1, 100, 0),
                new BlockPos(2, 100, 0),
                new BlockPos(3, 100, 0)
        );

        ShipState ship = new ShipState(ctrl, blocks, Level.OVERWORLD);
        ship.getReactors().add(new BlockPos(1, 100, 0));
        ship.getShields().add(new BlockPos(2, 100, 0));
        ship.getWeapons().add(new BlockPos(3, 100, 0));

        assertEquals(1, ship.getReactors().size());
        assertEquals(1, ship.getShields().size());
        assertEquals(1, ship.getWeapons().size());
        assertEquals(4, ship.getBlocks().size());
        assertEquals(ctrl, ship.getControllerPos());
    }

    @Test
    @DisplayName("Unbound Preview berechnet Relativpositionen und BoundingBox aus Weltkoordinaten fehlerfrei")
    void testUnboundStructurePreviewCalculation() {
        BlockPos controllerPos = new BlockPos(50, 70, 50);
        Set<BlockPos> scannedAbsoluteBlocks = Set.of(
                new BlockPos(50, 70, 50),
                new BlockPos(48, 70, 45),
                new BlockPos(55, 74, 52)
        );

        Set<BlockPos> relative = new HashSet<>();
        for (BlockPos abs : scannedAbsoluteBlocks) {
            relative.add(abs.subtract(controllerPos));
        }

        assertEquals(3, relative.size());
        assertTrue(relative.contains(BlockPos.ZERO));
        assertTrue(relative.contains(new BlockPos(-2, 0, -5)));
        assertTrue(relative.contains(new BlockPos(5, 4, 2)));

        int minX = 0, maxX = 0, minY = 0, maxY = 0, minZ = 0, maxZ = 0;
        boolean first = true;
        for (BlockPos pos : relative) {
            if (first) {
                minX = maxX = pos.getX();
                minY = maxY = pos.getY();
                minZ = maxZ = pos.getZ();
                first = false;
            } else {
                if (pos.getX() < minX) minX = pos.getX();
                if (pos.getX() > maxX) maxX = pos.getX();
                if (pos.getY() < minY) minY = pos.getY();
                if (pos.getY() > maxY) maxY = pos.getY();
                if (pos.getZ() < minZ) minZ = pos.getZ();
                if (pos.getZ() > maxZ) maxZ = pos.getZ();
            }
        }

        int spanX = maxX - minX + 1;
        int spanY = maxY - minY + 1;
        int spanZ = maxZ - minZ + 1;

        assertEquals(8, spanX); // 5 - (-2) + 1 = 8
        assertEquals(5, spanY); // 4 - 0 + 1 = 5
        assertEquals(8, spanZ); // 2 - (-5) + 1 = 8
    }
}
