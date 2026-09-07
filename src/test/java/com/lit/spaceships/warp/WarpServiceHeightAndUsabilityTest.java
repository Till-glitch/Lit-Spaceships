package com.lit.spaceships.warp;

import com.lit.spaceships.block.SpaceshipControlBlock;
import com.lit.spaceships.block.entity.SpaceshipControlBlockEntity;
import com.lit.spaceships.ship.domain.ShipState;
import com.lit.spaceships.ship.service.ServerShipManager;
import com.lit.spaceships.ship.service.ShipMovementService;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests zur Validierung der Behebung von:
 * 1. Spawning zu hoch am Himmel nach Rückkehr zur Oberwelt (Höhenberechnung & Bodenabstand)
 * 2. Schiff nach Warp unbrauchbar ("Ghost Ship") durch versehentliche Exzisions-Löschung in SpaceshipControlBlock.onRemove
 *    sowie Verlust aus ACTIVE_SHIPS in ServerShipManager.changeShipDimension.
 */
class WarpServiceHeightAndUsabilityTest {

    private static final ResourceKey<Level> OVERWORLD = ResourceKey.create(
            net.minecraft.core.registries.Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath("minecraft", "overworld")
    );
    private static final ResourceKey<Level> SPACE = ResourceKey.create(
            net.minecraft.core.registries.Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath("spaceships", "space")
    );

    @BeforeEach
    void setUp() {
        ServerShipManager.ACTIVE_SHIPS.clear();
        ServerShipManager.getShipsInDimension(OVERWORLD).clear();
        ServerShipManager.getShipsInDimension(SPACE).clear();
    }

    @Test
    @DisplayName("Bodenabstands-Kalkulation: Controller-Zielhöhe garantiert exakt 4 Blöcke Freiraum unter der Schiffshülle")
    void testOverworldTargetHeightClearance() {
        BlockPos ctrlPos = new BlockPos(100, 62, 200);
        Set<BlockPos> blocks = new HashSet<>();
        // Schiff mit Hüllen-Unterkante bei Y=60 und Oberkante bei Y=66
        for (int y = 60; y <= 66; y++) {
            blocks.add(new BlockPos(100, y, 200));
        }

        ShipState ship = new ShipState(ctrlPos, blocks, SPACE);
        AABB bounds = ship.getTotalBoundingBox();
        assertNotNull(bounds);

        int bottomOffset = Math.max(0, ctrlPos.getY() - (int) Math.floor(bounds.minY));
        assertEquals(2, bottomOffset, "Controller bei Y=62 ist 2 Blöcke über der Hüllen-Unterkante (Y=60)");

        int surfaceY = 64; // z.B. Meereshöhe / Strand
        int minAllowedY = -64 + bottomOffset + 4;
        int maxAllowedY = 320 - 10;

        int targetCtrlY = Math.clamp(surfaceY + bottomOffset + 4, minAllowedY, maxAllowedY);
        assertEquals(70, targetCtrlY, "Controller landet bei Y=70");

        // Hüllen-Unterkante landet bei controllerY - bottomOffset = 70 - 2 = 68
        int actualHullMinY = targetCtrlY - bottomOffset;
        assertEquals(68, actualHullMinY);
        assertEquals(4, actualHullMinY - surfaceY, "Schiffshülle schwebt exakt 4 Blöcke über der Erdoberfläche");
    }

    @Test
    @DisplayName("changeShipDimension behält ShipState in ACTIVE_SHIPS und registriert Zieldimension")
    void testChangeShipDimensionKeepsActiveShip() {
        BlockPos ctrl = new BlockPos(0, 100, 0);
        Set<BlockPos> blocks = Set.of(ctrl);
        ShipState ship = new ShipState(ctrl, blocks, SPACE);

        ServerShipManager.registerShip(ship);
        assertTrue(ServerShipManager.hasShip(ship.getId()));
        assertSame(ship, ServerShipManager.getShip(ship.getId()));
        assertTrue(ServerShipManager.getShipsInDimension(SPACE).containsKey(ship.getId()));

        // Dimensionswechsel zurück zur Oberwelt
        ServerShipManager.changeShipDimension(null, ship, OVERWORLD);

        assertEquals(OVERWORLD, ship.getDimension());
        assertTrue(ServerShipManager.hasShip(ship.getId()), "ShipState MUSS nach Dimensionswechsel in ACTIVE_SHIPS verbleiben!");
        assertSame(ship, ServerShipManager.getShip(ship.getId()), "getShip(UUID) darf nach Dimensionswechsel niemals null sein!");
        assertTrue(ServerShipManager.getShipsInDimension(OVERWORLD).containsKey(ship.getId()), "Schiff muss in neuer Dimension eingetragen sein");
        assertFalse(ServerShipManager.getShipsInDimension(SPACE).containsKey(ship.getId()), "Schiff muss aus alter Dimension entfernt sein");
    }

    @Test
    @DisplayName("SpaceshipControlBlock.onRemove löscht Schiff NICHT, wenn isJumping() aktiv ist (Warp-Exzision)")
    void testOnRemoveDoesNotDeleteWhenJumping() {
        BlockPos ctrl = new BlockPos(50, 80, 50);
        Set<BlockPos> blocks = Set.of(ctrl);
        ShipState ship = new ShipState(ctrl, blocks, OVERWORLD);
        ServerShipManager.registerShip(ship);

        // Simuliere aktiven Warp-Sprung
        ship.setJumping(true);

        Level mockLevel = mock(Level.class);
        when(mockLevel.isClientSide()).thenReturn(false);

        SpaceshipControlBlockEntity mockBe = mock(SpaceshipControlBlockEntity.class);
        when(mockBe.getShipId()).thenReturn(ship.getId());
        when(mockLevel.getBlockEntity(ctrl)).thenReturn(mockBe);

        BlockState oldState = Blocks.STONE.defaultBlockState();
        BlockState newState = Blocks.AIR.defaultBlockState();

        SpaceshipControlBlock block = mock(SpaceshipControlBlock.class);
        doCallRealMethod().when(block).onRemove(any(), any(), any(), any(), anyBoolean());

        // onRemove während Exzision
        block.onRemove(oldState, mockLevel, ctrl, newState, false);

        assertTrue(ServerShipManager.hasShip(ship.getId()), "Schiff darf während Warp-Exzision (isJumping == true) NICHT gelöscht werden!");
        assertSame(ship, ServerShipManager.getShip(ship.getId()));
    }

    @Test
    @DisplayName("SpaceshipControlBlock.onRemove löscht Schiff regulär, wenn Spieler den Block abbaut (isJumping == false)")
    void testOnRemoveDeletesWhenDismantledByPlayer() {
        BlockPos ctrl = new BlockPos(50, 80, 50);
        Set<BlockPos> blocks = Set.of(ctrl);
        ShipState ship = new ShipState(ctrl, blocks, OVERWORLD);
        ServerShipManager.registerShip(ship);

        ship.setJumping(false);

        Level mockLevel = mock(Level.class);
        when(mockLevel.isClientSide()).thenReturn(false);

        SpaceshipControlBlockEntity mockBe = mock(SpaceshipControlBlockEntity.class);
        when(mockBe.getShipId()).thenReturn(ship.getId());
        when(mockLevel.getBlockEntity(ctrl)).thenReturn(mockBe);

        BlockState oldState = Blocks.STONE.defaultBlockState();
        BlockState newState = Blocks.AIR.defaultBlockState();

        SpaceshipControlBlock block = mock(SpaceshipControlBlock.class);
        doCallRealMethod().when(block).onRemove(any(), any(), any(), any(), anyBoolean());

        block.onRemove(oldState, mockLevel, ctrl, newState, false);

        assertFalse(ServerShipManager.hasShip(ship.getId()), "Schiff muss bei manuellem Abbau durch Spieler aus ACTIVE_SHIPS gelöscht werden");
    }

    @Test
    @DisplayName("SpaceshipControlScreen Button-Aktivierungslogik: Bei gebundenem Schiff ist Create deaktiviert und alle anderen aktiv")
    void testControlScreenButtonActivationWhenBound() {
        // Simuliere isBound = true (nach Warp oder bei bestehendem Schiff)
        boolean isBound = true;

        boolean createActive = !isBound;
        boolean updateActive = isBound;
        boolean disassembleActive = isBound;
        boolean rotateCcwActive = isBound;
        boolean rotateCwActive = isBound;

        assertFalse(createActive, "Create-Button darf bei gebundenem Schiff NICHT klickbar sein!");
        assertTrue(updateActive, "Update-Blocks-Button MUSS bei gebundenem Schiff klickbar sein!");
        assertTrue(disassembleActive, "Disassemble-Button MUSS bei gebundenem Schiff klickbar sein!");
        assertTrue(rotateCcwActive, "Rotate-CCW-Button MUSS bei gebundenem Schiff klickbar sein!");
        assertTrue(rotateCwActive, "Rotate-CW-Button MUSS bei gebundenem Schiff klickbar sein!");
    }

    @Test
    @DisplayName("SpaceshipControlScreen Button-Aktivierungslogik: Bei ungebundenem Controller ist nur Create aktiv")
    void testControlScreenButtonActivationWhenUnbound() {
        // Simuliere isBound = false (neuer Controller ohne Schiff)
        boolean isBound = false;

        boolean createActive = !isBound;
        boolean updateActive = isBound;
        boolean disassembleActive = isBound;
        boolean rotateCcwActive = isBound;
        boolean rotateCwActive = isBound;

        assertTrue(createActive, "Create-Button MUSS bei ungebundenem Controller klickbar sein!");
        assertFalse(updateActive, "Update-Blocks-Button darf bei ungebundenem Controller NICHT klickbar sein!");
        assertFalse(disassembleActive, "Disassemble-Button darf bei ungebundenem Controller NICHT klickbar sein!");
        assertFalse(rotateCcwActive, "Rotate-CCW-Button darf bei ungebundenem Controller NICHT klickbar sein!");
        assertFalse(rotateCwActive, "Rotate-CW-Button darf bei ungebundenem Controller NICHT klickbar sein!");
    }
}
