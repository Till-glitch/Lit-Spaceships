package com.peaceman.alpha.ship;

import com.peaceman.alpha.ship.domain.ShipState;
import com.peaceman.alpha.ship.service.ShipMovementService;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Adapter/Fassade für translatorische Schiffsbewegungen.
 * Delegiert an den asynchron budgetierten ShipMovementService.
 */
public class SpaceshipMover {

    public static void moveShip(Level level, ShipState ship, int dx, int dy, int dz, Player player) {
        ShipMovementService.moveShip(level, ship, dx, dy, dz, player);
    }
}