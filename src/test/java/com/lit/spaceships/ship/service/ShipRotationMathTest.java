package com.lit.spaceships.ship.service;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit-Tests für 90°-Rotationsmathematik (Voxel, BlockStates, Entities, Blickwinkel).
 */
public class ShipRotationMathTest {

    @BeforeAll
    static void setup() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @Test
    @DisplayName("rotateRelativeBlockPos 90° CW transformiert (rx, rz) -> (-rz, rx) für alle Quadranten")
    void testRotateRelativeBlockPos_90CW() {
        // North (0, -2) -> East (2, 0)
        BlockPos north = new BlockPos(0, 5, -2);
        BlockPos rotNorth = ShipRotationMath.rotateRelativeBlockPos(north, Rotation.CLOCKWISE_90);
        assertEquals(new BlockPos(2, 5, 0), rotNorth);

        // East (3, 0) -> South (0, 3)
        BlockPos east = new BlockPos(3, 10, 0);
        BlockPos rotEast = ShipRotationMath.rotateRelativeBlockPos(east, Rotation.CLOCKWISE_90);
        assertEquals(new BlockPos(0, 10, 3), rotEast);

        // South (0, 4) -> West (-4, 0)
        BlockPos south = new BlockPos(0, 0, 4);
        BlockPos rotSouth = ShipRotationMath.rotateRelativeBlockPos(south, Rotation.CLOCKWISE_90);
        assertEquals(new BlockPos(-4, 0, 0), rotSouth);

        // West (-5, 0) -> North (0, -5)
        BlockPos west = new BlockPos(-5, -2, 0);
        BlockPos rotWest = ShipRotationMath.rotateRelativeBlockPos(west, Rotation.CLOCKWISE_90);
        assertEquals(new BlockPos(0, -2, -5), rotWest);

        // Diagonal (+2, +3) -> (-3, +2)
        BlockPos diag = new BlockPos(2, 1, 3);
        BlockPos rotDiag = ShipRotationMath.rotateRelativeBlockPos(diag, Rotation.CLOCKWISE_90);
        assertEquals(new BlockPos(-3, 1, 2), rotDiag);
    }

    @Test
    @DisplayName("rotateRelativeBlockPos 90° CCW transformiert (rx, rz) -> (rz, -rx) für alle Quadranten")
    void testRotateRelativeBlockPos_90CCW() {
        // North (0, -2) -> West (-2, 0)
        BlockPos north = new BlockPos(0, 5, -2);
        BlockPos rotNorth = ShipRotationMath.rotateRelativeBlockPos(north, Rotation.COUNTERCLOCKWISE_90);
        assertEquals(new BlockPos(-2, 5, 0), rotNorth);

        // West (-4, 0) -> South (0, 4)
        BlockPos west = new BlockPos(-4, 0, 0);
        BlockPos rotWest = ShipRotationMath.rotateRelativeBlockPos(west, Rotation.COUNTERCLOCKWISE_90);
        assertEquals(new BlockPos(0, 0, 4), rotWest);

        // South (0, 3) -> East (3, 0)
        BlockPos south = new BlockPos(0, 10, 3);
        BlockPos rotSouth = ShipRotationMath.rotateRelativeBlockPos(south, Rotation.COUNTERCLOCKWISE_90);
        assertEquals(new BlockPos(3, 10, 0), rotSouth);

        // East (5, 0) -> North (0, -5)
        BlockPos east = new BlockPos(5, -2, 0);
        BlockPos rotEast = ShipRotationMath.rotateRelativeBlockPos(east, Rotation.COUNTERCLOCKWISE_90);
        assertEquals(new BlockPos(0, -2, -5), rotEast);

        // Diagonal (+2, +3) -> (+3, -2)
        BlockPos diag = new BlockPos(2, 1, 3);
        BlockPos rotDiag = ShipRotationMath.rotateRelativeBlockPos(diag, Rotation.COUNTERCLOCKWISE_90);
        assertEquals(new BlockPos(3, 1, -2), rotDiag);
    }

    @Test
    @DisplayName("rotateAbsoluteBlockPos rotiert korrekt um einen versetzten Pivot-Punkt")
    void testRotateAbsoluteBlockPos() {
        BlockPos pivot = new BlockPos(100, 64, 200);

        // Block bei (105, 64, 200) -> 5 Blöcke östlich vom Pivot
        BlockPos blockEast = new BlockPos(105, 64, 200);

        // 90° CW Drehung -> 5 Blöcke südlich vom Pivot: (100, 64, 205)
        BlockPos rotCW = ShipRotationMath.rotateAbsoluteBlockPos(blockEast, pivot, Rotation.CLOCKWISE_90);
        assertEquals(new BlockPos(100, 64, 205), rotCW);

        // 90° CCW Drehung -> 5 Blöcke nördlich vom Pivot: (100, 64, 195)
        BlockPos rotCCW = ShipRotationMath.rotateAbsoluteBlockPos(blockEast, pivot, Rotation.COUNTERCLOCKWISE_90);
        assertEquals(new BlockPos(100, 64, 195), rotCCW);

        // Pivot selbst bleibt an Ort und Stelle
        BlockPos rotPivot = ShipRotationMath.rotateAbsoluteBlockPos(pivot, pivot, Rotation.CLOCKWISE_90);
        assertEquals(pivot, rotPivot);
    }

    @Test
    @DisplayName("rotateEntityPos rotiert Fließkomma-Koordinaten exakt um das Pivot-Zentrum (Pivot + 0.5)")
    void testRotateEntityPos() {
        BlockPos pivot = new BlockPos(10, 60, 20); // Pivot Center: (10.5, 60.0, 20.5)

        // Entity steht 2.5 Blöcke in +X und 1.0 Blöcke in +Z: (13.0, 61.0, 21.5)
        // Delta: dx = +2.5, dz = +1.0
        Vec3 entityPos = new Vec3(13.0, 61.0, 21.5);

        // 90° CW: newDx = -dz = -1.0, newDz = dx = +2.5
        // newPos = (10.5 - 1.0, 61.0, 20.5 + 2.5) = (9.5, 61.0, 23.0)
        Vec3 rotCW = ShipRotationMath.rotateEntityPos(entityPos, pivot, Rotation.CLOCKWISE_90);
        assertEquals(9.5, rotCW.x, 1e-6);
        assertEquals(61.0, rotCW.y, 1e-6);
        assertEquals(23.0, rotCW.z, 1e-6);

        // 90° CCW: newDx = dz = +1.0, newDz = -dx = -2.5
        // newPos = (10.5 + 1.0, 61.0, 20.5 - 2.5) = (11.5, 61.0, 18.0)
        Vec3 rotCCW = ShipRotationMath.rotateEntityPos(entityPos, pivot, Rotation.COUNTERCLOCKWISE_90);
        assertEquals(11.5, rotCCW.x, 1e-6);
        assertEquals(61.0, rotCCW.y, 1e-6);
        assertEquals(18.0, rotCCW.z, 1e-6);
    }

    @Test
    @DisplayName("rotateYaw dreht Blickwinkel (+90° CW, -90° CCW) und normalisiert auf [-180, 180]")
    void testRotateYaw() {
        // 0° (South) + 90° CW -> 90° (West)
        assertEquals(90.0f, ShipRotationMath.rotateYaw(0.0f, Rotation.CLOCKWISE_90), 1e-5f);

        // 90° (West) + 90° CW -> 180° (North)
        assertEquals(180.0f, ShipRotationMath.rotateYaw(90.0f, Rotation.CLOCKWISE_90), 1e-5f);

        // 180° (North) + 90° CW -> 270° -> -90° (East)
        assertEquals(-90.0f, ShipRotationMath.rotateYaw(180.0f, Rotation.CLOCKWISE_90), 1e-5f);

        // -90° (East) + 90° CW -> 0° (South)
        assertEquals(0.0f, ShipRotationMath.rotateYaw(-90.0f, Rotation.CLOCKWISE_90), 1e-5f);

        // 0° (South) - 90° CCW -> -90° (East)
        assertEquals(-90.0f, ShipRotationMath.rotateYaw(0.0f, Rotation.COUNTERCLOCKWISE_90), 1e-5f);

        // -90° (East) - 90° CCW -> -180° (North)
        assertEquals(-180.0f, ShipRotationMath.rotateYaw(-90.0f, Rotation.COUNTERCLOCKWISE_90), 1e-5f);

        // -180° - 90° CCW -> -270° -> 90° (West)
        assertEquals(90.0f, ShipRotationMath.rotateYaw(-180.0f, Rotation.COUNTERCLOCKWISE_90), 1e-5f);
    }

    @Test
    @DisplayName("4x 90° CW Rotationen ergeben exakt die Ursprungskoordinaten (Identität)")
    void testFourRotationsIdentity() {
        BlockPos pivot = new BlockPos(50, 100, -75);
        BlockPos original = new BlockPos(63, 112, -61);

        BlockPos rot1 = ShipRotationMath.rotateAbsoluteBlockPos(original, pivot, Rotation.CLOCKWISE_90);
        BlockPos rot2 = ShipRotationMath.rotateAbsoluteBlockPos(rot1, pivot, Rotation.CLOCKWISE_90);
        BlockPos rot3 = ShipRotationMath.rotateAbsoluteBlockPos(rot2, pivot, Rotation.CLOCKWISE_90);
        BlockPos rot4 = ShipRotationMath.rotateAbsoluteBlockPos(rot3, pivot, Rotation.CLOCKWISE_90);

        assertEquals(original, rot4);
    }

    @Test
    @DisplayName("BlockState rotate mit Rotation.CLOCKWISE_90 dreht Directional FACING korrekt")
    void testRotateBlockState_Facing() {
        BlockState stairsNorth = Blocks.OAK_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);

        BlockState stairsEast = stairsNorth.rotate(Rotation.CLOCKWISE_90);
        assertEquals(Direction.EAST, stairsEast.getValue(BlockStateProperties.HORIZONTAL_FACING));

        BlockState stairsSouth = stairsEast.rotate(Rotation.CLOCKWISE_90);
        assertEquals(Direction.SOUTH, stairsSouth.getValue(BlockStateProperties.HORIZONTAL_FACING));

        BlockState stairsWest = stairsSouth.rotate(Rotation.CLOCKWISE_90);
        assertEquals(Direction.WEST, stairsWest.getValue(BlockStateProperties.HORIZONTAL_FACING));

        BlockState stairsBackToNorth = stairsWest.rotate(Rotation.CLOCKWISE_90);
        assertEquals(Direction.NORTH, stairsBackToNorth.getValue(BlockStateProperties.HORIZONTAL_FACING));
    }
}
