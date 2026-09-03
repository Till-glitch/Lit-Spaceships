package com.lit.spaceships.ship.combat.aim;

import net.minecraft.util.Mth;

/**
 * Definiert die mechanischen Bewegungsgrenzen (Gimbal Limits in Grad)
 * für das Gier- (Yaw) und Neige- (Pitch) Gelenk eines Geschützturms.
 */
public record GimbalLimits(float minYaw, float maxYaw, float minPitch, float maxPitch) {

    /**
     * Standard-Turm: Voller 360° horizontaler Schwenkbereich (-180° bis +180°), -90° bis +90° Neigung.
     */
    public static final GimbalLimits DEFAULT_TURRET = new GimbalLimits(-180.0f, 180.0f, -90.0f, 90.0f);

    /**
     * Volle 360° Kuppel: -180° bis +180° Yaw, -90° bis +90° Pitch.
     */
    public static final GimbalLimits DOME_TURRET = new GimbalLimits(-180.0f, 180.0f, -90.0f, 90.0f);

    /**
     * Unbeschränkt: Voller 360° Sphärenbereich.
     */
    public static final GimbalLimits UNRESTRICTED = new GimbalLimits(-180.0f, 180.0f, -90.0f, 90.0f);

    public AimAngles clamp(AimAngles angles) {
        if (angles == null) return AimAngles.ZERO;
        float wrappedYaw = Mth.wrapDegrees(angles.yaw());
        float clampedYaw = Math.max(minYaw, Math.min(wrappedYaw, maxYaw));
        float clampedPitch = Math.max(minPitch, Math.min(angles.pitch(), maxPitch));
        return new AimAngles(clampedYaw, clampedPitch);
    }
}

