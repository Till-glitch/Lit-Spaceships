package com.peaceman.alpha.world;

import com.peaceman.alpha.registry.ModItems;
import com.peaceman.alpha.ship.domain.ShipState;
import com.peaceman.alpha.ship.service.ServerShipManager;
import com.peaceman.alpha.world.environment.SpaceEnvironmentService;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SpaceEnvironmentTest {

    @BeforeAll
    static void initMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    @DisplayName("Kreativ- und Spectator-Spieler sind stets vor Vakuum geschützt")
    void testCreativePlayerProtected() {
        Player player = mock(Player.class);
        when(player.isCreative()).thenReturn(true);
        when(player.getItemBySlot(EquipmentSlot.HEAD)).thenReturn(ItemStack.EMPTY);

        assertTrue(SpaceEnvironmentService.isProtectedFromVacuum(player));

        when(player.isCreative()).thenReturn(false);
        when(player.isSpectator()).thenReturn(true);
        assertTrue(SpaceEnvironmentService.isProtectedFromVacuum(player));
    }

    @Test
    @DisplayName("Spieler mit Raumanzug-Helm sind vor Vakuum geschützt")
    void testSpaceSuitHelmetProtects() {
        Player player = mock(Player.class);
        when(player.isCreative()).thenReturn(false);
        when(player.isSpectator()).thenReturn(false);

        ItemStack spaceHelmet = mock(ItemStack.class);
        com.peaceman.alpha.item.SpaceSuitItem helmetItem = mock(com.peaceman.alpha.item.SpaceSuitItem.class);
        when(spaceHelmet.isEmpty()).thenReturn(false);
        when(spaceHelmet.getItem()).thenReturn(helmetItem);
        when(player.getItemBySlot(EquipmentSlot.HEAD)).thenReturn(spaceHelmet);

        assertTrue(SpaceEnvironmentService.isProtectedFromVacuum(player));
    }

    @Test
    @DisplayName("Objekte im Inneren eines aktiven Schiffs sind vor Vakuum geschützt")
    void testInsideShipProtected() {
        Player player = mock(Player.class);
        when(player.isCreative()).thenReturn(false);
        when(player.isSpectator()).thenReturn(false);
        when(player.getItemBySlot(EquipmentSlot.HEAD)).thenReturn(ItemStack.EMPTY);

        BlockPos shipPos = new BlockPos(50, 60, 50);
        when(player.blockPosition()).thenReturn(shipPos);

        ShipState ship = new ShipState(shipPos, Set.of(shipPos, shipPos.offset(1, 0, 0)), ModDimensions.SPACE_LEVEL);
        ServerShipManager.registerShip(ship);

        try {
            assertTrue(SpaceEnvironmentService.isProtectedFromVacuum(player));

            // Außerhalb des Schiffs
            when(player.blockPosition()).thenReturn(new BlockPos(500, 60, 500));
            assertFalse(SpaceEnvironmentService.isProtectedFromVacuum(player));
        } finally {
            ServerShipManager.unregisterShip(ship);
        }
    }
}
