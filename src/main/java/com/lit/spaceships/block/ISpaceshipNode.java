package com.lit.spaceships.block;

import com.lit.spaceships.ship.domain.ShipState;
import com.lit.spaceships.ship.service.ServerShipManager;

import java.util.UUID;

public interface ISpaceshipNode {
    UUID getShipId();

    void setShipId(UUID shipId);

    default ShipState getShip() {
        if (getShipId() == null) {
            return null;
        }
        return ServerShipManager.getShip(getShipId());
    }
}
