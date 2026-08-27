package com.peaceman.alpha.ship;

import com.peaceman.alpha.ship.domain.ShipState;
import com.peaceman.alpha.ship.service.ServerShipManager;
import com.peaceman.alpha.ship.service.ShipMovementService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class SpaceshipNavigationManager {

    // 1. Speichert die aktuelle Position als neuen Wegpunkt
    public static void saveHome(Level level, ShipState ship, String homeName) {
        if (ship != null) {
            ship.addHome(homeName, ship.getControllerPos());
            ServerShipManager.saveData(level);
        }
    }

    // 2. Berechnet die Route und gibt sie an den ShipMovementService weiter
    public static void teleportToHome(Level level, ShipState ship, String homeName, Player player) {
        if (ship != null && ship.getHomes().containsKey(homeName)) {
            BlockPos targetPos = ship.getHomes().get(homeName);
            BlockPos currentPos = ship.getControllerPos();

            int dx = targetPos.getX() - currentPos.getX();
            int dy = targetPos.getY() - currentPos.getY();
            int dz = targetPos.getZ() - currentPos.getZ();

            ShipMovementService.moveShip(level, ship, dx, dy, dz, player);
        } else {
            if (player != null) {
                player.displayClientMessage(Component.translatable(com.peaceman.alpha.registry.ModI18n.Message.WAYPOINT_NOT_FOUND, homeName), true);
            }
        }
    }

    // 3. Führt einen dimensionalen Sprung durch
    public static boolean jumpToDimension(net.minecraft.server.level.ServerLevel originLevel, net.minecraft.server.level.ServerLevel targetLevel, ShipState ship, BlockPos targetPos, Player player) {
        return com.peaceman.alpha.ship.service.ShipTeleportationService.teleportShip(originLevel, targetLevel, ship, targetPos, player);
    }
}