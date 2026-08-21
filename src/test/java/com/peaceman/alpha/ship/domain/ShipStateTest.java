package com.peaceman.alpha.ship.domain;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit-Tests für das Domain-DTO ShipState (Zustand, BoundingBoxen, Cooldowns).
 */
public class ShipStateTest {

    @Test
    @DisplayName("ShipState Initialisierung berechnet korrekte Hüllen-BoundingBox")
    void testShipState_BoundsCalculation() {
        BlockPos controller = new BlockPos(10, 20, 30);
        Set<BlockPos> blocks = Set.of(
                new BlockPos(10, 20, 30),
                new BlockPos(15, 25, 35),
                new BlockPos(8, 18, 28)
        );

        ShipState ship = new ShipState(controller, blocks);

        assertNotNull(ship.getId());
        assertEquals(controller, ship.getControllerPos());
        assertEquals(3, ship.getBlocks().size());

        AABB box = ship.getHullBoundingBox();
        assertNotNull(box);
        assertEquals(8.0, box.minX, 1e-6);
        assertEquals(18.0, box.minY, 1e-6);
        assertEquals(28.0, box.minZ, 1e-6);
        assertEquals(16.0, box.maxX, 1e-6); // maxPos.getX() + 1
        assertEquals(26.0, box.maxY, 1e-6);
        assertEquals(36.0, box.maxZ, 1e-6);
    }

    @Test
    @DisplayName("ShipState Controller-Verschiebung verschiebt die BoundingBoxen mit")
    void testShipState_ControllerMoveTranslatesBounds() {
        BlockPos controller = new BlockPos(0, 0, 0);
        Set<BlockPos> blocks = Set.of(new BlockPos(0, 0, 0), new BlockPos(2, 2, 2));

        ShipState ship = new ShipState(controller, blocks);
        AABB initialBox = ship.getHullBoundingBox();

        // Controller um (10, 5, -3) verschieben
        BlockPos newController = new BlockPos(10, 5, -3);
        ship.setControllerPos(newController);

        AABB movedBox = ship.getHullBoundingBox();
        assertNotNull(movedBox);
        assertEquals(initialBox.minX + 10, movedBox.minX, 1e-6);
        assertEquals(initialBox.minY + 5, movedBox.minY, 1e-6);
        assertEquals(initialBox.minZ - 3, movedBox.minZ, 1e-6);
        assertEquals(initialBox.maxX + 10, movedBox.maxX, 1e-6);
        assertEquals(initialBox.maxY + 5, movedBox.maxY, 1e-6);
        assertEquals(initialBox.maxZ - 3, movedBox.maxZ, 1e-6);
    }

    @Test
    @DisplayName("getImmutableBlockSnapshot liefert unveränderliche Kopie")
    void testShipState_ImmutableBlockSnapshot() {
        BlockPos controller = new BlockPos(0, 0, 0);
        Set<BlockPos> blocks = new HashSet<>(List.of(new BlockPos(0, 0, 0), new BlockPos(1, 1, 1)));

        ShipState ship = new ShipState(controller, blocks);
        Set<BlockPos> snapshot = ship.getImmutableBlockSnapshot();

        assertEquals(2, snapshot.size());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add(new BlockPos(2, 2, 2)));
    }

    @Test
    @DisplayName("Schild- und Bewegungs-Cooldown-Berechnungen arbeiten deterministisch")
    void testShipState_CooldownLogic() {
        ShipState ship = new ShipState(BlockPos.ZERO, Set.of(BlockPos.ZERO));

        long currentTime = 1000L;
        assertFalse(ship.isShieldOnCooldown(currentTime));
        assertEquals(0L, ship.getShieldCooldownRemaining(currentTime));

        // Schild-Cooldown auf 1200 setzen (200 Ticks)
        ship.setShieldCooldownUntil(currentTime + ShipState.SHIELD_COOLDOWN_TICKS);
        assertTrue(ship.isShieldOnCooldown(currentTime));
        assertTrue(ship.isShieldOnCooldown(1100L));
        assertEquals(200L, ship.getShieldCooldownRemaining(currentTime));
        assertEquals(100L, ship.getShieldCooldownRemaining(1100L));

        // Nach Ablauf
        assertFalse(ship.isShieldOnCooldown(1201L));
        assertEquals(0L, ship.getShieldCooldownRemaining(1201L));

        // Bewegungs-Cooldown
        ship.setMovementCooldownUntil(currentTime + ShipState.MOVEMENT_COOLDOWN_TICKS);
        assertTrue(ship.isMovementOnCooldown(currentTime));
        assertEquals(20L, ship.getMovementCooldownRemaining(currentTime));
        assertFalse(ship.isMovementOnCooldown(1021L));
    }

    @Test
    @DisplayName("Schild-Toggle wechselt Zustand und berücksichtigt leere Schilde")
    void testShipState_ToggleShield() {
        ShipState ship = new ShipState(BlockPos.ZERO, Set.of(BlockPos.ZERO));

        // Ohne Schild-Blöcke
        ship.setShields(Collections.emptyList());
        ship.toggleShieldActive();
        assertFalse(ship.isShieldActive());

        // Mit Schild-Blöcken
        ship.setShields(List.of(new BlockPos(0, 1, 0)));
        ship.setShieldActive(true);
        assertTrue(ship.isShieldActive());

        ship.toggleShieldActive();
        assertFalse(ship.isShieldActive());

        ship.toggleShieldActive();
        assertTrue(ship.isShieldActive());
    }

    @Test
    @DisplayName("Weapons-Liste lässt sich korrekt setzen und abfragen")
    void testShipState_WeaponsTracking() {
        ShipState ship = new ShipState(BlockPos.ZERO, Set.of(BlockPos.ZERO));
        assertTrue(ship.getWeapons().isEmpty());

        List<BlockPos> weaponPositions = List.of(new BlockPos(1, 0, 0), new BlockPos(-1, 0, 0));
        ship.setWeapons(weaponPositions);

        assertEquals(2, ship.getWeapons().size());
        assertTrue(ship.getWeapons().contains(new BlockPos(1, 0, 0)));
        assertTrue(ship.getWeapons().contains(new BlockPos(-1, 0, 0)));
    }
}
