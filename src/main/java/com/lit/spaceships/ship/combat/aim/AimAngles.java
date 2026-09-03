package com.peaceman.alpha.ship.combat.aim;

/**
 * Repräsentiert lokale Euler-Winkel (Yaw und Pitch in Grad) eines Geschützturms.
 */
public record AimAngles(float yaw, float pitch) {

    public static final AimAngles ZERO = new AimAngles(0.0f, 0.0f);

    public AimAngles withYaw(float newYaw) {
        return new AimAngles(newYaw, this.pitch);
    }

    public AimAngles withPitch(float newPitch) {
        return new AimAngles(this.yaw, newPitch);
    }
}
