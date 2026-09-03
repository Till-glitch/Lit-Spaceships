package com.peaceman.alpha.block;

import com.peaceman.alpha.ship.domain.ShipState;
import com.peaceman.alpha.ship.service.ServerShipManager;

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
