package com.lit.spaceships.ship.service;

import com.lit.spaceships.block.entity.AbstractLaserNodeBlockEntity;
import com.lit.spaceships.block.entity.PulseLaserBlockEntity;
import com.lit.spaceships.block.entity.SpaceshipReactorBlockEntity;
import com.lit.spaceships.block.entity.SpaceshipShieldBlockEntity;
import com.lit.spaceships.ship.domain.ShipState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ServerShipManagerTest {

    @Mock
    private Level mockLevel;

    @Mock
    private SpaceshipReactorBlockEntity mockReactor;

    @Mock
    private SpaceshipShieldBlockEntity mockShield;

    @Mock
    private PulseLaserBlockEntity mockWeapon;

    @Mock
    private BlockEntity mockNormalBlock;

    @Test
    @DisplayName("populateAndSyncShipState categorizes function blocks correctly")
    void testPopulateAndSyncShipState_CategorizesBlocks() {
        // Arrange
        BlockPos controllerPos = new BlockPos(0, 0, 0);
        BlockPos reactorPos = new BlockPos(1, 0, 0);
        BlockPos shieldPos = new BlockPos(2, 0, 0);
        BlockPos weaponPos = new BlockPos(3, 0, 0);
        BlockPos normalPos = new BlockPos(4, 0, 0);

        Set<BlockPos> blocks = Set.of(controllerPos, reactorPos, shieldPos, weaponPos, normalPos);
        ShipState ship = new ShipState(controllerPos, blocks);

        when(mockLevel.getBlockEntity(reactorPos)).thenReturn(mockReactor);
        when(mockLevel.getBlockEntity(shieldPos)).thenReturn(mockShield);
        when(mockLevel.getBlockEntity(weaponPos)).thenReturn(mockWeapon);
        when(mockLevel.getBlockEntity(normalPos)).thenReturn(mockNormalBlock);
        when(mockLevel.getBlockEntity(controllerPos)).thenReturn(null);

        when(mockLevel.isClientSide()).thenReturn(true); // Verhindert Async-Aufruf im Test

        // Act
        ServerShipManager.populateAndSyncShipState(mockLevel, ship);

        // Assert
        assertEquals(1, ship.getReactors().size());
        assertTrue(ship.getReactors().contains(reactorPos));

        assertEquals(1, ship.getShields().size());
        assertTrue(ship.getShields().contains(shieldPos));

        assertEquals(1, ship.getWeapons().size());
        assertTrue(ship.getWeapons().contains(weaponPos));
    }

    @Test
    @DisplayName("populateAndSyncShipState deactivates shield when no shield generators exist")
    void testPopulateAndSyncShipState_DeactivatesEmptyShield() {
        // Arrange
        BlockPos controllerPos = new BlockPos(0, 0, 0);
        Set<BlockPos> blocks = Set.of(controllerPos);
        ShipState ship = new ShipState(controllerPos, blocks);
        ship.setShieldActive(true); // Fake active state

        when(mockLevel.getBlockEntity(controllerPos)).thenReturn(null);
        when(mockLevel.isClientSide()).thenReturn(true);

        // Act
        ServerShipManager.populateAndSyncShipState(mockLevel, ship);

        // Assert
        assertTrue(ship.getShields().isEmpty());
        assertFalse(ship.isShieldActive());
        assertTrue(ship.getShieldVoxelCache().isEmpty());
    }
}
