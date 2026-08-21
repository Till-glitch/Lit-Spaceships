package com.peaceman.alpha.network;

import com.peaceman.alpha.block.entity.SpaceshipControlBlockEntity;
import com.peaceman.alpha.ship.SpaceshipNavigationManager;
import com.peaceman.alpha.ship.domain.ShipState;
import com.peaceman.alpha.ship.service.ServerShipManager;
import com.peaceman.alpha.ship.service.ShipMovementService;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ServerPayloadHandler {

    public static void handleAction(final ShipActionPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player == null) return;
            Level level = player.level();
            var pos = payload.pos();

            // 1. SONDERFALL: SCHIFF ERSTELLEN
            if (payload.actionType() == ShipActionPayload.ActionType.CREATE) {
                if (level.getBlockEntity(pos) instanceof SpaceshipControlBlockEntity blockEntity) {
                    ShipState newShip = ServerShipManager.createShip(level, pos);
                    if (newShip != null) {
                        blockEntity.setShipId(newShip.getId());
                    }
                }
                return;
            }

            // 2. FÜR ALLE ANDEREN AKTIONEN: UUID BENÖTIGT
            if (payload.shipId().isEmpty()) {
                return;
            }

            ShipState ship = ServerShipManager.getShip(payload.shipId().get());
            if (ship == null) {
                return;
            }

            int dist = payload.value();
            String targetName = payload.targetName();

            // 3. TYPISIERTE AKTION AUSFÜHREN
            switch (payload.actionType()) {
                case SAVE_HOME -> SpaceshipNavigationManager.saveHome(level, ship, targetName);
                case TP_HOME -> SpaceshipNavigationManager.teleportToHome(level, ship, targetName, player);
                case UPDATE_BLOCKS -> ServerShipManager.updateShipBlocks(level, ship);
                case DELETE_SHIP -> ServerShipManager.deleteShip(level, ship);
                case MOVE_UP -> ShipMovementService.moveShip(level, ship, 0, dist, 0, player);
                case MOVE_DOWN -> ShipMovementService.moveShip(level, ship, 0, -dist, 0, player);
                case MOVE_FORWARD, MOVE_BACKWARD, MOVE_LEFT, MOVE_RIGHT -> {
                    Direction forward = player.getDirection();
                    Direction right = forward.getClockWise();
                    int dx = 0;
                    int dz = 0;

                    switch (payload.actionType()) {
                        case MOVE_FORWARD -> {
                            dx = forward.getStepX() * dist;
                            dz = forward.getStepZ() * dist;
                        }
                        case MOVE_BACKWARD -> {
                            dx = -forward.getStepX() * dist;
                            dz = -forward.getStepZ() * dist;
                        }
                        case MOVE_RIGHT -> {
                            dx = right.getStepX() * dist;
                            dz = right.getStepZ() * dist;
                        }
                        case MOVE_LEFT -> {
                            dx = -right.getStepX() * dist;
                            dz = -right.getStepZ() * dist;
                        }
                        default -> {}
                    }
                    ShipMovementService.moveShip(level, ship, dx, 0, dz, player);
                }
                case TOGGLE_SHIELD -> ship.toggleShieldActive(level);
                default -> {}
            }
        });
    }
}
