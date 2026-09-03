package com.peaceman.alpha.ship.combat.aim;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Mathematische Kernroutinen für Koordinaten-Transformationen zwischen
 * globalem Weltraum und lokalem Schiffs-Voxel-Grid sowie Winkelkompression.
 */
public final class AimTransformMath {

    private AimTransformMath() {
    }

    /**
     * Berechnet aus globalen Euler-Winkeln der Spielerkamera einen normalisierten
     * 3D-Blickvektor.
     */
    public static Vec3 calculateWorldLookVector(float yawDeg, float pitchDeg) {
        float yawRad = yawDeg * Mth.DEG_TO_RAD;
        float pitchRad = pitchDeg * Mth.DEG_TO_RAD;

        double x = -Math.sin(yawRad) * Math.cos(pitchRad);
        double y = -Math.sin(pitchRad);
        double z = Math.cos(yawRad) * Math.cos(pitchRad);

        return new Vec3(x, y, z).normalize();
    }

    /**
     * Transformiert einen globalen Richtungsvektor via inverser Schiffs-Rotation in
     * den lokalen Schiffs-Raum.
     */
    public static Vec3 transformWorldToLocal(Vec3 worldVector, Quaternionf shipRotation) {
        if (shipRotation == null
                || (shipRotation.x == 0 && shipRotation.y == 0 && shipRotation.z == 0 && shipRotation.w == 1)) {
            return worldVector;
        }

        Quaternionf inv = new Quaternionf(shipRotation).conjugate();
        Vector3f local = inv
                .transform(new Vector3f((float) worldVector.x, (float) worldVector.y, (float) worldVector.z));
        return new Vec3(local.x(), local.y(), local.z()).normalize();
    }

    /**
     * Transformiert einen lokalen Schiffsvektor via Schiffs-Rotation in den
     * globalen Weltraum.
     */
    public static Vec3 transformLocalToWorld(Vec3 localVector, Quaternionf shipRotation) {
        if (shipRotation == null
                || (shipRotation.x == 0 && shipRotation.y == 0 && shipRotation.z == 0 && shipRotation.w == 1)) {
            return worldVector(localVector);
        }

        Vector3f world = new Quaternionf(shipRotation)
                .transform(new Vector3f((float) localVector.x, (float) localVector.y, (float) localVector.z));
        return new Vec3(world.x(), world.y(), world.z()).normalize();
    }

    private static Vec3 worldVector(Vec3 vec) {
        return vec.normalize();
    }

    /**
     * Wandelt einen lokalen Richtungsvektor in lokale Euler-Winkel (Yaw und Pitch
     * in Grad) um.
     */
    public static AimAngles vectorToLocalEuler(Vec3 localVec) {
        Vec3 norm = localVec.normalize();
        double yawRad = Math.atan2(-norm.x, norm.z);
        double clampedY = Math.max(-1.0, Math.min(1.0, -norm.y));
        double pitchRad = Math.asin(clampedY);

        float yawDeg = (float) (yawRad * Mth.RAD_TO_DEG);
        float pitchDeg = (float) (pitchRad * Mth.RAD_TO_DEG);

        return new AimAngles(Mth.wrapDegrees(yawDeg), Mth.wrapDegrees(pitchDeg));
    }

    /**
     * Wandelt lokale Euler-Winkel in einen normalisierten lokalen
     * 3D-Richtungsvektor um.
     */
    public static Vec3 localEulerToVector(float yawDeg, float pitchDeg) {
        float yawRad = yawDeg * Mth.DEG_TO_RAD;
        float pitchRad = pitchDeg * Mth.DEG_TO_RAD;

        double x = -Math.sin(yawRad) * Math.cos(pitchRad);
        double y = -Math.sin(pitchRad);
        double z = Math.cos(yawRad) * Math.cos(pitchRad);

        return new Vec3(x, y, z).normalize();
    }

    /**
     * Komprimiert einen Winkel im Bereich [-180°, 180°] verlustarm in einen 16-Bit
     * Short.
     */
    public static short compressAngle(float angleDeg) {
        float wrapped = Mth.wrapDegrees(angleDeg);
        return (short) Math.round((wrapped / 180.0f) * 32767.0f);
    }

    /**
     * Dekomprimiert einen 16-Bit Short zurück in einen Float-Winkel [-180°, 180°].
     */
    public static float decompressAngle(short compressed) {
        return (compressed / 32767.0f) * 180.0f;
    }

    /**
     * Berechnet die kürzeste Winkeldifferenz (unter Berücksichtigung des
     * 180°-Wraps) für stotterfreie Interpolation.
     */
    public static float normalizeAngleDelta(float current, float previous) {
        return Mth.wrapDegrees(current - previous);
    }

    /**
     * Interpoliert sphärisch/linear zwischen zwei Winkeln unter Erhaltung des
     * kürzesten Drehwegs.
     */
    public static float interpolateAngle(float previous, float current, float partialTick) {
        float delta = normalizeAngleDelta(current, previous);
        return previous + delta * partialTick;
    }

    /**
     * Gibt die Rotations-Matrix (Quaternion) für eine Wand-/Boden-/Decken-Montage zurück.
     */
    public static org.joml.Quaternionf getRotationForFacing(net.minecraft.core.Direction facing) {
        switch (facing) {
            case DOWN:
                return new org.joml.Quaternionf().rotationX((float) Math.PI);
            case UP:
                return new org.joml.Quaternionf();
            case NORTH:
                return new org.joml.Quaternionf().rotationX((float) -Math.PI / 2F);
            case SOUTH:
                return new org.joml.Quaternionf().rotationX((float) Math.PI / 2F);
            case WEST:
                return new org.joml.Quaternionf().rotationZ((float) Math.PI / 2F);
            case EAST:
                return new org.joml.Quaternionf().rotationZ((float) -Math.PI / 2F);
            default:
                return new org.joml.Quaternionf();
        }
    }
}
