package com.lit.spaceships.ship.combat.aim;

import com.lit.spaceships.ship.domain.ShipState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

/**
 * Modulares Interface für verschiedene Zielerfassungs-Strategien
 * (Konzept A: Freelook / Co-Pilot, Konzept B: Designator, Konzept C: Radar).
 */
public interface IAimStrategy {

    enum AimType {
        FREELOOK,
        DESIGNATOR,
        RADAR
    }

    AimType getType();

    boolean requiresPassengerSeat();

    /**
     * Berechnet die resultierenden lokalen Zielwinkel für den Geschützturm unter Berücksichtigung
     * der Schiffsrotation und der mechanischen Gimbal-Limits.
     */
    AimAngles calculateAimAngles(ShipState ship, BlockPos weaponPos, Player player, GimbalLimits limits);
}
