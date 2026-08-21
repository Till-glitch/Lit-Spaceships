package com.peaceman.alpha.ship;

import com.peaceman.alpha.block.entity.SpaceshipReactorBlockEntity;
import com.peaceman.alpha.ship.domain.ShipState;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.energy.EnergyStorage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit-Tests mit Mockito für den SpaceshipEnergyManager (Berechnung, Bündelung, sequenzieller Drain, Rollback).
 */
@ExtendWith(MockitoExtension.class)
public class SpaceshipEnergyManagerTest {

    @BeforeAll
    static void initMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Mock
    private Level level;

    @Test
    @DisplayName("calculateMovementCost berechnet 10 FE pro Block und Distanzmeter")
    void testCalculateMovementCost() {
        Set<BlockPos> blocks = Set.of(
                new BlockPos(0, 0, 0), new BlockPos(1, 0, 0),
                new BlockPos(2, 0, 0), new BlockPos(3, 0, 0),
                new BlockPos(4, 0, 0)
        ); // 5 Blöcke
        ShipState ship = new ShipState(BlockPos.ZERO, blocks);

        // Bewegung um dx=10, dy=-5, dz=2 -> Distanz = 17m
        // Kosten: 5 Blöcke * 17m * 10 FE = 850 FE
        int cost = SpaceshipEnergyManager.calculateMovementCost(ship, 10, -5, 2);
        assertEquals(850, cost);

        // Distanz 0m -> Kosten 0 FE
        int zeroCost = SpaceshipEnergyManager.calculateMovementCost(ship, 0, 0, 0);
        assertEquals(0, zeroCost);
    }

    @Test
    @DisplayName("getTotalAvailableEnergy summiert alle Reaktoren des Schiffs")
    void testGetTotalAvailableEnergy_MultipleReactors() {
        BlockPos reactor1Pos = new BlockPos(1, 0, 0);
        BlockPos reactor2Pos = new BlockPos(2, 0, 0);

        SpaceshipReactorBlockEntity reactor1 = mock(SpaceshipReactorBlockEntity.class);
        EnergyStorage storage1 = new EnergyStorage(100000, 100000, 100000);
        storage1.receiveEnergy(10000, false);
        when(reactor1.getEnergyStorage()).thenReturn(storage1);

        SpaceshipReactorBlockEntity reactor2 = mock(SpaceshipReactorBlockEntity.class);
        EnergyStorage storage2 = new EnergyStorage(100000, 100000, 100000);
        storage2.receiveEnergy(25000, false);
        when(reactor2.getEnergyStorage()).thenReturn(storage2);

        when(level.getBlockEntity(reactor1Pos)).thenReturn(reactor1);
        when(level.getBlockEntity(reactor2Pos)).thenReturn(reactor2);

        ShipState ship = new ShipState(BlockPos.ZERO, Set.of(BlockPos.ZERO));
        ship.setReactors(List.of(reactor1Pos, reactor2Pos));

        int totalEnergy = SpaceshipEnergyManager.getTotalAvailableEnergy(level, ship);
        assertEquals(35000, totalEnergy);
    }

    @Test
    @DisplayName("getTotalAvailableEnergy liefert 0 bei leerer Reaktorliste oder ungültigen BlockEntities")
    void testGetTotalAvailableEnergy_EmptyOrInvalid() {
        ShipState ship = new ShipState(BlockPos.ZERO, Set.of(BlockPos.ZERO));
        ship.setReactors(Collections.emptyList());

        assertEquals(0, SpaceshipEnergyManager.getTotalAvailableEnergy(level, ship));

        // Reaktor-Position verweist auf nicht-Reaktor BlockEntity
        BlockPos invalidPos = new BlockPos(5, 5, 5);
        ship.setReactors(List.of(invalidPos));
        when(level.getBlockEntity(invalidPos)).thenReturn(null);

        assertEquals(0, SpaceshipEnergyManager.getTotalAvailableEnergy(level, ship));
    }

    @Test
    @DisplayName("tryConsumeEnergyAmount zieht Energie sequenziell aus Reaktoren ab wenn genug Energie vorhanden")
    void testTryConsumeEnergyAmount_Success() {
        BlockPos r1Pos = new BlockPos(1, 0, 0);
        BlockPos r2Pos = new BlockPos(2, 0, 0);

        SpaceshipReactorBlockEntity r1 = mock(SpaceshipReactorBlockEntity.class);
        EnergyStorage storage1 = new EnergyStorage(100000, 100000, 100000);
        storage1.receiveEnergy(5000, false);
        when(r1.getEnergyStorage()).thenReturn(storage1);

        SpaceshipReactorBlockEntity r2 = mock(SpaceshipReactorBlockEntity.class);
        EnergyStorage storage2 = new EnergyStorage(100000, 100000, 100000);
        storage2.receiveEnergy(10000, false);
        when(r2.getEnergyStorage()).thenReturn(storage2);

        when(level.getBlockEntity(r1Pos)).thenReturn(r1);
        when(level.getBlockEntity(r2Pos)).thenReturn(r2);

        ShipState ship = new ShipState(BlockPos.ZERO, Set.of(BlockPos.ZERO));
        ship.setReactors(List.of(r1Pos, r2Pos));

        // 8000 FE anfordern (5000 aus R1 + 3000 aus R2)
        boolean result = SpaceshipEnergyManager.tryConsumeEnergyAmount(level, ship, 8000);

        assertTrue(result);
        assertEquals(0, storage1.getEnergyStored(), "Reaktor 1 muss vollständig entleert sein");
        assertEquals(7000, storage2.getEnergyStored(), "Reaktor 2 muss 7000 FE übrig haben");
    }

    @Test
    @DisplayName("tryConsumeEnergyAmount schlägt fehl und zieht keine Energie ab bei Energiemangel")
    void testTryConsumeEnergyAmount_InsufficientEnergy() {
        BlockPos rPos = new BlockPos(1, 0, 0);

        SpaceshipReactorBlockEntity r = mock(SpaceshipReactorBlockEntity.class);
        EnergyStorage storage = new EnergyStorage(100000, 100000, 100000);
        storage.receiveEnergy(3000, false);
        when(r.getEnergyStorage()).thenReturn(storage);

        when(level.getBlockEntity(rPos)).thenReturn(r);

        ShipState ship = new ShipState(BlockPos.ZERO, Set.of(BlockPos.ZERO));
        ship.setReactors(List.of(rPos));

        // 5000 FE anfordern bei nur 3000 FE vorhanden
        boolean result = SpaceshipEnergyManager.tryConsumeEnergyAmount(level, ship, 5000);

        assertFalse(result);
        assertEquals(3000, storage.getEnergyStored(), "Energie darf bei Fehlschlag nicht abgezogen werden");
    }

    @Test
    @DisplayName("tryConsumeFlightEnergy prüft Bewegungskosten und gibt Erfolgsstatus zurück")
    void testTryConsumeFlightEnergy_CalculatesAndConsumes() {
        BlockPos controller = new BlockPos(0, 0, 0);
        BlockPos rPos = new BlockPos(1, 0, 0);
        Set<BlockPos> blocks = Set.of(controller, rPos); // 2 Blöcke
        ShipState ship = new ShipState(controller, blocks);

        SpaceshipReactorBlockEntity r = mock(SpaceshipReactorBlockEntity.class);
        EnergyStorage storage = new EnergyStorage(100000, 100000, 100000);
        storage.receiveEnergy(1000, false);
        when(r.getEnergyStorage()).thenReturn(storage);
        when(level.getBlockEntity(rPos)).thenReturn(r);

        ship.setReactors(List.of(rPos));

        // Flug um 5m -> 2 Blöcke * 5m * 10 FE = 100 FE benötigt (vorhanden: 1000 FE)
        boolean success = SpaceshipEnergyManager.tryConsumeFlightEnergy(level, ship, 5, 0, 0, null);
        assertTrue(success);
        assertEquals(900, storage.getEnergyStored());

        // Flug um 100m -> 2 Blöcke * 100m * 10 FE = 2000 FE benötigt (vorhanden: 900 FE) -> Fehlschlag
        boolean fail = SpaceshipEnergyManager.tryConsumeFlightEnergy(level, ship, 100, 0, 0, null);
        assertFalse(fail);
        assertEquals(900, storage.getEnergyStored());
    }
}
