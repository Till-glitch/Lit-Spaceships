package com.peaceman.alpha.ship.combat.aim;

import com.peaceman.alpha.ship.domain.ShipState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/**
 * Konzept A: Freelook / Co-Pilot Zielerfassung.
 * Der Spieler sitzt im Turm und steuert die Ausrichtung direkt über seine Kamerarotation.
 */
public class FreelookAimStrategy implements IAimStrategy {

    public static final FreelookAimStrategy INSTANCE = new FreelookAimStrategy();

    @Override
    public AimType getType() {
        return AimType.FREELOOK;
    }

    @Override
    public boolean requiresPassengerSeat() {
        return true;
    }

    @Override
    public AimAngles calculateAimAngles(ShipState ship, BlockPos weaponPos, Player player, GimbalLimits limits) {
        if (player == null) {
            return AimAngles.ZERO;
        }

        // 1. Berechne globalen Blickvektor aus Spielerkamera
        Vec3 worldLookVec = AimTransformMath.calculateWorldLookVector(player.getYRot(), player.getXRot());

        // 2. Transformiere Vektor in den lokalen Schiffsraum
        Quaternionf shipRot = ship != null ? ship.getRotation() : new Quaternionf();
        Vec3 localLookVec = AimTransformMath.transformWorldToLocal(worldLookVec, shipRot);

        // 3. Konvertiere lokalen Vektor in lokale Euler-Winkel
        AimAngles rawAngles = AimTransformMath.vectorToLocalEuler(localLookVec);

        // 4. Limitiere Winkel anhand der mechanischen Gimbal Limits
        if (limits != null) {
            return limits.clamp(rawAngles);
        }

        return rawAngles;
    }
}
