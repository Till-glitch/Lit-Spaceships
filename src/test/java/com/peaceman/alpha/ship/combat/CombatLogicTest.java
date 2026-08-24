package com.peaceman.alpha.ship.combat;

import com.peaceman.alpha.ship.domain.VoxelGridCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit-Tests für die Raycast- und Kampfmathematik (FastVoxelTraversal,
 * LaserWeaponTier, RaycastHitResult).
 */
public class CombatLogicTest {

    @Test
    @DisplayName("FastVoxelTraversal erkennt frontalen Treffer auf Voxel entlang der X-Achse")
    void testFastVoxelTraversal_FrontalHitX() {
        BlockPos controller = new BlockPos(0, 0, 0);
        Set<BlockPos> blocks = Set.of(new BlockPos(5, 0, 0));
        VoxelGridCache cache = VoxelGridCache.buildFromAbsolute(blocks, controller);

        Vec3 origin = new Vec3(-5.0, 0.5, 0.5);
        Vec3 dir = new Vec3(1.0, 0.0, 0.0);
        double maxDist = 20.0;

        Optional<FastVoxelTraversal.VoxelHit> hitOpt = FastVoxelTraversal.traverse(cache, origin, dir, maxDist);

        assertTrue(hitOpt.isPresent(), "Strahl muss den Voxel bei (5,0,0) treffen");
        FastVoxelTraversal.VoxelHit hit = hitOpt.get();

        assertEquals(new BlockPos(5, 0, 0), hit.relativePos());
        assertEquals(Direction.WEST, hit.hitFace(), "Eintrittsseite von links muss WEST sein");
        assertEquals(10.0, hit.distance(), 1e-4);
    }

    @Test
    @DisplayName("FastVoxelTraversal erkennt Treffer von unten entlang der Y-Achse")
    void testFastVoxelTraversal_HitFromBottomY() {
        BlockPos controller = new BlockPos(0, 0, 0);
        Set<BlockPos> blocks = Set.of(new BlockPos(0, 3, 0));
        VoxelGridCache cache = VoxelGridCache.buildFromAbsolute(blocks, controller);

        Vec3 origin = new Vec3(0.5, -2.0, 0.5);
        Vec3 dir = new Vec3(0.0, 1.0, 0.0);
        double maxDist = 15.0;

        Optional<FastVoxelTraversal.VoxelHit> hitOpt = FastVoxelTraversal.traverse(cache, origin, dir, maxDist);

        assertTrue(hitOpt.isPresent(), "Strahl muss den Voxel bei (0,3,0) treffen");
        FastVoxelTraversal.VoxelHit hit = hitOpt.get();

        assertEquals(new BlockPos(0, 3, 0), hit.relativePos());
        assertEquals(Direction.DOWN, hit.hitFace(), "Eintrittsseite von unten muss DOWN sein");
        assertEquals(5.0, hit.distance(), 1e-4);
    }

    @Test
    @DisplayName("FastVoxelTraversal liefert empty bei Fehlschuss oder zu geringer Reichweite")
    void testFastVoxelTraversal_MissAndRangeLimit() {
        BlockPos controller = new BlockPos(0, 0, 0);
        Set<BlockPos> blocks = Set.of(new BlockPos(5, 5, 5));
        VoxelGridCache cache = VoxelGridCache.buildFromAbsolute(blocks, controller);

        // 1. Strahl zielt komplett vorbei
        Vec3 origin = new Vec3(0.5, 0.5, 0.5);
        Vec3 dirMiss = new Vec3(0.0, 1.0, 0.0);
        assertTrue(FastVoxelTraversal.traverse(cache, origin, dirMiss, 50.0).isEmpty());

        // 2. Strahl zielt in die richtige Richtung, aber Reichweite reicht nicht aus
        Vec3 dirHit = new Vec3(1.0, 1.0, 1.0).normalize();
        double distanceToVoxel = origin.distanceTo(new Vec3(5.5, 5.5, 5.5));
        assertTrue(FastVoxelTraversal.traverse(cache, origin, dirHit, distanceToVoxel - 3.0).isEmpty());
    }

    @Test
    @DisplayName("LaserWeaponTier Konstanten und Eigenschaften sind valide")
    void testLaserWeaponTier_Properties() {
        for (LaserWeaponTier tier : LaserWeaponTier.values()) {
            assertTrue(tier.getMaxRange() > 0.0, "Reichweite muss positiv sein für " + tier);
            assertTrue(tier.getEnergyCost() > 0, "Energiekosten müssen positiv sein für " + tier);
            assertTrue(tier.getColorA() > 0.0f, "Alpha muss positiv sein für " + tier);
        }

        assertEquals(20.0f, LaserWeaponTier.PULSE_LASER.getBaseDamage());
        assertEquals(20, LaserWeaponTier.PULSE_LASER.getCooldownTicks());

        assertEquals(3.0f, LaserWeaponTier.HEAVY_BEAM.getBaseDamage());
        assertEquals(0, LaserWeaponTier.HEAVY_BEAM.getCooldownTicks());

        assertEquals(0.0f, LaserWeaponTier.MINING_LASER.getBaseDamage());
    }

    @Test
    @DisplayName("RaycastHitResult Factory-Methoden und Typ-Prädikate")
    void testRaycastHitResult_Predicates() {
        UUID shipId = UUID.randomUUID();
        Vec3 hitPos = new Vec3(10.0, 20.0, 30.0);

        RaycastHitResult miss = RaycastHitResult.miss(hitPos, 50.0);
        assertFalse(miss.isHit());
        assertFalse(miss.isShipHit());
        assertEquals(RaycastHitResult.HitType.MISS, miss.type());

        RaycastHitResult shield = RaycastHitResult.shipShield(shipId, BlockPos.ZERO, BlockPos.ZERO, hitPos,
                Direction.NORTH, 10.0);
        assertTrue(shield.isHit());
        assertTrue(shield.isShipHit());
        assertEquals(RaycastHitResult.HitType.SHIP_SHIELD, shield.type());

        RaycastHitResult hull = RaycastHitResult.shipHull(shipId, BlockPos.ZERO, BlockPos.ZERO, hitPos, Direction.NORTH,
                10.0);
        assertTrue(hull.isHit());
        assertTrue(hull.isShipHit());
        assertEquals(RaycastHitResult.HitType.SHIP_HULL, hull.type());

        RaycastHitResult block = RaycastHitResult.block(BlockPos.ZERO, hitPos, Direction.UP, 5.0);
        assertTrue(block.isHit());
        assertFalse(block.isShipHit());
        assertEquals(RaycastHitResult.HitType.BLOCK, block.type());
    }

    @Test
    @DisplayName("Dauerstrahl- und Impulswaffen-Unterscheidungen und Energiekonfigurationen")
    void testContinuousAndPulseWeaponConfigurations() {
        // Pulse Laser: Hoher Einzelverbrauch, Abklingzeit, 1-Block-Zerstörung
        assertEquals(250, LaserWeaponTier.PULSE_LASER.getEnergyCost());
        assertEquals(20, LaserWeaponTier.PULSE_LASER.getCooldownTicks());
        assertEquals(20.0f, LaserWeaponTier.PULSE_LASER.getBaseDamage());

        // Heavy Beam: Mittlerer Tick-Verbrauch, Dauerstrahl (0 Cooldown)
        assertEquals(50, LaserWeaponTier.HEAVY_BEAM.getEnergyCost());
        assertEquals(0, LaserWeaponTier.HEAVY_BEAM.getCooldownTicks());
        assertEquals(3.0f, LaserWeaponTier.HEAVY_BEAM.getBaseDamage());

        // Mining Laser: Geringer Tick-Verbrauch, 0 Basisschaden gegen Entities
        assertEquals(25, LaserWeaponTier.MINING_LASER.getEnergyCost());
        assertEquals(0, LaserWeaponTier.MINING_LASER.getCooldownTicks());
        assertEquals(0.0f, LaserWeaponTier.MINING_LASER.getBaseDamage());
    }

    @Test
    @DisplayName("calculateAimDirection nutzt lokale Euler-Winkel bei ausgerichteten Turrets")
    void testCalculateAimDirection_DynamicTurret() {
        // Mock oder Dummy für AbstractLaserNodeBlockEntity
        var mockBe = org.mockito.Mockito.mock(com.peaceman.alpha.block.entity.PulseLaserBlockEntity.class);
        org.mockito.Mockito.when(mockBe.isOccupied()).thenReturn(true);
        org.mockito.Mockito.when(mockBe.getTargetYaw()).thenReturn(90.0f); // West
        org.mockito.Mockito.when(mockBe.getTargetPitch()).thenReturn(0.0f); // Horizontal

        Vec3 aimDir = LaserCombatService.calculateAimDirection(mockBe, null);
        assertEquals(-1.0, aimDir.x, 1e-4);
        assertEquals(0.0, aimDir.y, 1e-4);
        assertEquals(0.0, aimDir.z, 1e-4);
    }
}
