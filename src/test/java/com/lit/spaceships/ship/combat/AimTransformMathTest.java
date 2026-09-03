package com.lit.spaceships.ship.combat;

import com.lit.spaceships.ship.combat.aim.AimAngles;
import com.lit.spaceships.ship.combat.aim.AimTransformMath;
import com.lit.spaceships.ship.combat.aim.FreelookAimStrategy;
import com.lit.spaceships.ship.combat.aim.GimbalLimits;
import com.lit.spaceships.ship.domain.ShipState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

public class AimTransformMathTest {

    private static final double EPSILON = 1e-4;

    @Test
    @DisplayName("calculateWorldLookVector berechnet korrekte Blickvektoren für Himmelsrichtungen")
    void testCalculateWorldLookVector() {
        // South (Yaw = 0, Pitch = 0) -> (0, 0, 1)
        Vec3 south = AimTransformMath.calculateWorldLookVector(0.0f, 0.0f);
        assertEquals(0.0, south.x, EPSILON);
        assertEquals(0.0, south.y, EPSILON);
        assertEquals(1.0, south.z, EPSILON);

        // North (Yaw = 180, Pitch = 0) -> (0, 0, -1)
        Vec3 north = AimTransformMath.calculateWorldLookVector(180.0f, 0.0f);
        assertEquals(0.0, north.x, EPSILON);
        assertEquals(0.0, north.y, EPSILON);
        assertEquals(-1.0, north.z, EPSILON);

        // East (Yaw = -90, Pitch = 0) -> (1, 0, 0)
        Vec3 east = AimTransformMath.calculateWorldLookVector(-90.0f, 0.0f);
        assertEquals(1.0, east.x, EPSILON);
        assertEquals(0.0, east.y, EPSILON);
        assertEquals(0.0, east.z, EPSILON);

        // West (Yaw = 90, Pitch = 0) -> (-1, 0, 0)
        Vec3 west = AimTransformMath.calculateWorldLookVector(90.0f, 0.0f);
        assertEquals(-1.0, west.x, EPSILON);
        assertEquals(0.0, west.y, EPSILON);
        assertEquals(0.0, west.z, EPSILON);

        // Up (Pitch = -90) -> (0, 1, 0)
        Vec3 up = AimTransformMath.calculateWorldLookVector(0.0f, -90.0f);
        assertEquals(0.0, up.x, EPSILON);
        assertEquals(1.0, up.y, EPSILON);
        assertEquals(0.0, up.z, EPSILON);

        // Down (Pitch = 90) -> (0, -1, 0)
        Vec3 down = AimTransformMath.calculateWorldLookVector(0.0f, 90.0f);
        assertEquals(0.0, down.x, EPSILON);
        assertEquals(-1.0, down.y, EPSILON);
        assertEquals(0.0, down.z, EPSILON);
    }

    @Test
    @DisplayName("vectorToLocalEuler und localEulerToVector arbeiten symmetrisch")
    void testVectorToLocalEulerAndBack() {
        float testYaw = 45.0f;
        float testPitch = -30.0f;

        Vec3 dir = AimTransformMath.localEulerToVector(testYaw, testPitch);
        AimAngles converted = AimTransformMath.vectorToLocalEuler(dir);

        assertEquals(testYaw, converted.yaw(), 0.05f);
        assertEquals(testPitch, converted.pitch(), 0.05f);
    }

    @Test
    @DisplayName("transformWorldToLocal rotiert Vektoren korrekt um die Schiffs-Quaternion")
    void testTransformWorldToLocalWithShipRotation() {
        // Schiff ist um 90 Grad um die Y-Achse rotiert
        Quaternionf shipRot = new Quaternionf(new AxisAngle4f((float) Math.toRadians(90.0), 0.0f, 1.0f, 0.0f));

        // Blickvektor zeigt nach Süden (0, 0, 1) im Weltraum
        Vec3 worldLook = new Vec3(0.0, 0.0, 1.0);

        // Inverse Transformation von (0, 0, 1) um +90° rotiert nach (-1, 0, 0)
        Vec3 localLook = AimTransformMath.transformWorldToLocal(worldLook, shipRot);

        assertEquals(-1.0, localLook.x, EPSILON);
        assertEquals(0.0, localLook.y, EPSILON);
        assertEquals(0.0, localLook.z, EPSILON);

        // Rücktransformation via transformLocalToWorld
        Vec3 backWorld = AimTransformMath.transformLocalToWorld(localLook, shipRot);
        assertEquals(worldLook.x, backWorld.x, EPSILON);
        assertEquals(worldLook.y, backWorld.y, EPSILON);
        assertEquals(worldLook.z, backWorld.z, EPSILON);
    }

    @Test
    @DisplayName("GimbalLimits limitieren Winkel zuverlässig")
    void testGimbalLimitsClamping() {
        GimbalLimits customLimits = new GimbalLimits(-90.0f, 90.0f, -45.0f, 60.0f);

        // Gültiger Winkel bleibt unverändert
        AimAngles valid = new AimAngles(45.0f, 20.0f);
        assertEquals(valid, customLimits.clamp(valid));

        // Überschreitung Yaw
        AimAngles exceedYaw = new AimAngles(120.0f, 20.0f);
        AimAngles clampedYaw = customLimits.clamp(exceedYaw);
        assertEquals(90.0f, clampedYaw.yaw());
        assertEquals(20.0f, clampedYaw.pitch());

        // Unterschreitung Pitch
        AimAngles exceedPitch = new AimAngles(0.0f, -60.0f);
        AimAngles clampedPitch = customLimits.clamp(exceedPitch);
        assertEquals(0.0f, clampedPitch.yaw());
        assertEquals(-45.0f, clampedPitch.pitch());

        // 360-Grad UNRESTRICTED erlaubt jeden Winkel und normalisiert
        GimbalLimits unrestricted = GimbalLimits.UNRESTRICTED;
        AimAngles fullWest = new AimAngles(90.0f, 0.0f);
        assertEquals(90.0f, unrestricted.clamp(fullWest).yaw());

        AimAngles fullNorth = new AimAngles(180.0f, 0.0f);
        assertEquals(-180.0f, unrestricted.clamp(fullNorth).yaw(), 1e-4);

        AimAngles fullEast = new AimAngles(-90.0f, 0.0f);
        assertEquals(-90.0f, unrestricted.clamp(fullEast).yaw(), 1e-4);
    }

    @Test
    @DisplayName("16-Bit Short Kompression und Dekompression erhalten Winkelpräzision")
    void testAngleCompressionPrecision() {
        for (float angle = -180.0f; angle <= 180.0f; angle += 0.5f) {
            short compressed = AimTransformMath.compressAngle(angle);
            float decompressed = AimTransformMath.decompressAngle(compressed);

            // Maximaler Zirkulär-Winkelfehler darf 0.01 Grad nicht überschreiten
            float delta = Math.abs(AimTransformMath.normalizeAngleDelta(angle, decompressed));
            assertEquals(0.0f, delta, 0.01f, "Fehler bei Winkel: " + angle);
        }
    }

    @Test
    @DisplayName("normalizeAngleDelta berechnet kürzesten Pfad über den 180-Grad-Wrap")
    void testNormalizeAngleDelta() {
        // Von 179° zu -179° ist der kürzeste Weg +2° (nicht -358°)
        float deltaForward = AimTransformMath.normalizeAngleDelta(-179.0f, 179.0f);
        assertEquals(2.0f, deltaForward, 0.01f);

        // Von -179° zu 179° ist der kürzeste Weg -2° (nicht +358°)
        float deltaBackward = AimTransformMath.normalizeAngleDelta(179.0f, -179.0f);
        assertEquals(-2.0f, deltaBackward, 0.01f);

        // Interpolation zur Hälfte (partialTick = 0.5) von 179° zu -179° ergibt 180° / -180°
        float mid = AimTransformMath.interpolateAngle(179.0f, -179.0f, 0.5f);
        assertEquals(180.0f, Math.abs(mid), 0.01f);
    }

    @Test
    @DisplayName("ShipState verwaltet Rotation deterministisch")
    void testShipStateRotation() {
        ShipState ship = new ShipState(BlockPos.ZERO, new HashSet<>());
        assertNotNull(ship.getRotation());
        assertEquals(1.0f, ship.getRotation().w);

        Quaternionf newRot = new Quaternionf(new AxisAngle4f((float) Math.toRadians(45.0), 0.0f, 1.0f, 0.0f));
        ship.setRotation(newRot);
        assertEquals(newRot, ship.getRotation());
    }
}
