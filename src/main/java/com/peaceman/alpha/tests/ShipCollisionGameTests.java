package com.peaceman.alpha.tests;

import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.block.entity.SpaceshipReactorBlockEntity;
import com.peaceman.alpha.registry.ModBlocks;
import com.peaceman.alpha.ship.SpaceshipEnergyManager;
import com.peaceman.alpha.ship.domain.ShipState;
import com.peaceman.alpha.ship.service.CollisionResolver;
import com.peaceman.alpha.ship.service.ServerShipManager;
import com.peaceman.alpha.ship.service.ShipCollisionService;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@GameTestHolder(Alpha.MODID)
public class ShipCollisionGameTests {

    /**
     * Erstellt ein simuliertes Schiff (AABB) im Test-Template aus massiven Eisenbloecken und gibt den registrierten ShipState zurueck.
     */
    private static ShipState createMockShip(GameTestHelper helper, BlockPos start, int width, int height, int depth, boolean withShield, int energy) {
        BlockPos absStart = helper.absolutePos(start);
        Set<BlockPos> blocks = new HashSet<>();

        // Rumpf bauen
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < depth; z++) {
                    BlockPos relPos = start.offset(x, y, z);
                    helper.setBlock(relPos, Blocks.IRON_BLOCK);
                    blocks.add(helper.absolutePos(relPos));
                }
            }
        }

        // Reaktor hinzufuegen, falls Energie gefordert ist (damit Energie via SpaceshipEnergyManager verbraucht werden kann)
        if (energy > 0) {
            BlockPos relReactor = start.offset(0, height, 0); // Oben aufsetzen
            helper.setBlock(relReactor, ModBlocks.SPACESHIP_REACTOR.get());
            BlockPos absReactor = helper.absolutePos(relReactor);
            blocks.add(absReactor);

            BlockEntity be = helper.getLevel().getBlockEntity(absReactor);
            if (be instanceof SpaceshipReactorBlockEntity reactor) {
                reactor.getEnergyStorage().receiveEnergy(energy, false);
            }
        }

        ShipState ship = new ShipState(absStart, blocks);
        
        // Schild erzwingen (Schild-Generatoren muessen theoretisch da sein fuer toggleShield, aber fuer CollisionResolver reichts)
        ship.setShieldActive(withShield);
        
        if (withShield) {
            // Fake einen Schildgenerator, damit SpaceshipShieldHandler weiss, dass das Schiff Shields hat
            BlockPos relShield = start.offset(0, height + 1, 0);
            helper.setBlock(relShield, ModBlocks.SPACESHIP_SHIELD.get());
            BlockPos absShield = helper.absolutePos(relShield);
            ship.getBlocks().add(absShield);
            ship.getShields().add(absShield);
        }

        if (energy > 0) {
            BlockPos absReactor = helper.absolutePos(start.offset(0, height, 0));
            ship.getReactors().add(absReactor);
        }
        
        ship.recalculateHullBounds();
        ServerShipManager.registerShip(ship);
        return ship;
    }

    private static ShipCollisionService.VoxelCollisionResult buildResult(ShipState a, ShipState b, List<BlockPos> voxels) {
        return new ShipCollisionService.VoxelCollisionResult(
                a, b, true, a.isShieldActive(), b.isShieldActive(), voxels, new AABB(0,0,0,1,1,1)
        );
    }

    // ==========================================
    // 1. Fall: OFF vs OFF (Gegenseitige Zerstoerung)
    // ==========================================

    @GameTest(template = "empty")
    public static void testOffVsOff_StandardCollision(GameTestHelper helper) {
        ShipState shipA = createMockShip(helper, new BlockPos(1, 2, 1), 2, 2, 2, false, 0);
        ShipState shipB = createMockShip(helper, new BlockPos(5, 2, 1), 2, 2, 2, false, 0);

        List<BlockPos> collidingVoxels = new ArrayList<>();
        // Waehle 4 explizite Rumpf-Bloecke von Schiff A, die durch die Kollision zerstoert werden sollen
        BlockPos c1 = helper.absolutePos(new BlockPos(2, 2, 1));
        BlockPos c2 = helper.absolutePos(new BlockPos(2, 3, 1));
        BlockPos c3 = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos c4 = helper.absolutePos(new BlockPos(2, 3, 2));
        collidingVoxels.addAll(List.of(c1, c2, c3, c4));

        ShipCollisionService.VoxelCollisionResult collision = buildResult(shipA, shipB, collidingVoxels);

        CollisionResolver.CollisionResolution resolution = CollisionResolver.resolve(
                helper.getLevel(), collision, new Vec3(1, 0, 0)
        );

        helper.succeedIf(() -> {
            if (!resolution.movementStopped()) helper.fail("Movement should have stopped!");
            // Ueberpruefe, ob die 4 Bloecke aus der Welt entfernt wurden
            helper.assertBlockPresent(Blocks.AIR, new BlockPos(2, 2, 1));
            helper.assertBlockPresent(Blocks.AIR, new BlockPos(2, 3, 1));
            helper.assertBlockPresent(Blocks.AIR, new BlockPos(2, 2, 2));
            helper.assertBlockPresent(Blocks.AIR, new BlockPos(2, 3, 2));
        });
    }

    @GameTest(template = "empty")
    public static void testOffVsOff_ExplosionDamage(GameTestHelper helper) {
        ShipState shipA = createMockShip(helper, new BlockPos(1, 2, 1), 5, 1, 5, false, 0);
        ShipState shipB = createMockShip(helper, new BlockPos(7, 2, 1), 5, 1, 5, false, 0);

        List<BlockPos> collidingVoxels = new ArrayList<>();
        // Fuege sehr viele Kollisionsvoxel ein, um den Cluster-Explosions-Code auszubilden (size > 100)
        BlockPos centerPos = helper.absolutePos(new BlockPos(5, 2, 1));
        for(int i = 0; i < 200; i++) {
            collidingVoxels.add(centerPos); 
        }

        ShipCollisionService.VoxelCollisionResult collision = buildResult(shipA, shipB, collidingVoxels);

        CollisionResolver.resolve(helper.getLevel(), collision, new Vec3(1, 0, 0));

        // Nach der Explosion sollen Bloecke innerhalb des Explosionsradius, aber ausserhalb der strikten Voxel-Schnittmenge zerstoert worden sein.
        helper.succeedIf(() -> {
            // (4,2,1) ist nah an (5,2,1) und muss der Cluster-Explosion zum Opfer gefallen sein.
            helper.assertBlockPresent(Blocks.AIR, new BlockPos(4, 2, 1));
        });
    }

    // ==========================================
    // 2. Fall: OFF vs ON (Kinetischer Aufprall auf Schild)
    // ==========================================

    @GameTest(template = "empty")
    public static void testOffVsOn_Absorption(GameTestHelper helper) {
        ShipState shipA = createMockShip(helper, new BlockPos(1, 2, 1), 2, 2, 2, false, 0);
        ShipState shipB = createMockShip(helper, new BlockPos(5, 2, 1), 2, 2, 2, true, 1000000); // 1 Mio FE

        List<BlockPos> collidingVoxels = new ArrayList<>();
        // Schiff A rammt Schild B
        collidingVoxels.add(helper.absolutePos(new BlockPos(2, 2, 1)));
        collidingVoxels.add(helper.absolutePos(new BlockPos(2, 3, 1)));

        ShipCollisionService.VoxelCollisionResult collision = buildResult(shipA, shipB, collidingVoxels);

        int initialEnergy = SpaceshipEnergyManager.getTotalAvailableEnergy(helper.getLevel(), shipB);

        CollisionResolver.CollisionResolution resolution = CollisionResolver.resolve(
                helper.getLevel(), collision, new Vec3(1, 0, 0)
        );

        helper.succeedIf(() -> {
            if (!resolution.movementStopped()) helper.fail("Movement should have stopped (kinetic impact)");
            
            int expectedDrain = collidingVoxels.size() * CollisionResolver.ENERGY_PER_VOXEL_IMPACT;
            int newEnergy = SpaceshipEnergyManager.getTotalAvailableEnergy(helper.getLevel(), shipB);
            
            if (initialEnergy - newEnergy != expectedDrain) {
                helper.fail("Energy drain was incorrect! Expected: " + expectedDrain + ", Actual: " + (initialEnergy - newEnergy));
            }
            if (!shipB.isShieldActive()) {
                helper.fail("Shield should still be active");
            }
            // Block von Schiff A muss intakt sein
            helper.assertBlockPresent(Blocks.IRON_BLOCK, new BlockPos(2, 2, 1));
        });
    }

    @GameTest(template = "empty")
    public static void testOffVsOn_ShieldCollapse(GameTestHelper helper) {
        ShipState shipA = createMockShip(helper, new BlockPos(1, 2, 1), 2, 2, 2, false, 0);
        ShipState shipB = createMockShip(helper, new BlockPos(5, 2, 1), 2, 2, 2, true, 200); // Nur fuer 2 Voxel FE

        List<BlockPos> collidingVoxels = new ArrayList<>();
        // 5 Voxel Ueberschneidung = 500 FE gefordert
        for (int i = 0; i < 5; i++) {
            collidingVoxels.add(helper.absolutePos(new BlockPos(2, 2, 1))); 
        }

        ShipCollisionService.VoxelCollisionResult collision = buildResult(shipA, shipB, collidingVoxels);

        CollisionResolver.resolve(helper.getLevel(), collision, new Vec3(1, 0, 0));

        helper.succeedIf(() -> {
            if (shipB.isShieldActive()) {
                helper.fail("Shield B should have collapsed due to insufficient energy!");
            }
        });
    }

    // ==========================================
    // 3. Fall: ON vs OFF (Bohrer-Modus)
    // ==========================================

    @GameTest(template = "empty")
    public static void testOnVsOff_Drill(GameTestHelper helper) {
        ShipState shipA = createMockShip(helper, new BlockPos(1, 2, 1), 2, 2, 2, true, 100000);
        ShipState shipB = createMockShip(helper, new BlockPos(5, 2, 1), 2, 2, 2, false, 0);

        List<BlockPos> collidingVoxels = new ArrayList<>();
        // Die getroffenen Voxel gehoeren zum Hüllen-Schiff B
        BlockPos c1 = helper.absolutePos(new BlockPos(5, 2, 1));
        BlockPos c2 = helper.absolutePos(new BlockPos(5, 3, 1));
        collidingVoxels.add(c1);
        collidingVoxels.add(c2);
        
        ShipCollisionService.VoxelCollisionResult collision = buildResult(shipA, shipB, collidingVoxels);

        int initialEnergy = SpaceshipEnergyManager.getTotalAvailableEnergy(helper.getLevel(), shipA);

        CollisionResolver.CollisionResolution resolution = CollisionResolver.resolve(
                helper.getLevel(), collision, new Vec3(1, 0, 0)
        );

        helper.succeedIf(() -> {
            if (resolution.movementStopped()) helper.fail("Movement should NOT have stopped (Drill mode)");
            
            int expectedDrain = collidingVoxels.size() * CollisionResolver.ENERGY_PER_VOXEL_DRILL;
            int newEnergy = SpaceshipEnergyManager.getTotalAvailableEnergy(helper.getLevel(), shipA);
            
            if (initialEnergy - newEnergy != expectedDrain) {
                helper.fail("Energy drain on ship A was incorrect! Expected: " + expectedDrain);
            }
            
            // Die durchbohrten Bloecke von B muessen aus der Welt verschwinden
            helper.assertBlockPresent(Blocks.AIR, new BlockPos(5, 2, 1));
            helper.assertBlockPresent(Blocks.AIR, new BlockPos(5, 3, 1));
            
            // Sie muessen auch im ShipState geloescht werden
            if (shipB.getBlocks().contains(c1) || shipB.getBlocks().contains(c2)) {
                helper.fail("Blocks were not removed from ShipB's state");
            }
        });
    }

    @GameTest(template = "empty")
    public static void testOnVsOff_MidDrillCollapse(GameTestHelper helper) {
        ShipState shipA = createMockShip(helper, new BlockPos(1, 2, 1), 2, 2, 2, true, 500); // 500 FE = max 5 Voxel Drill
        ShipState shipB = createMockShip(helper, new BlockPos(5, 2, 1), 2, 2, 2, false, 0);

        List<BlockPos> collidingVoxels = new ArrayList<>();
        // 10 Voxel = 1000 FE erforderlich -> Mid-Drill Collapse
        for (int i = 0; i < 10; i++) {
            collidingVoxels.add(helper.absolutePos(new BlockPos(5, 2, 1))); 
        }

        ShipCollisionService.VoxelCollisionResult collision = buildResult(shipA, shipB, collidingVoxels);

        CollisionResolver.CollisionResolution resolution = CollisionResolver.resolve(
                helper.getLevel(), collision, new Vec3(1, 0, 0)
        );

        helper.succeedIf(() -> {
            if (!resolution.movementStopped()) helper.fail("Movement should have stopped after drill collapsed");
            if (shipA.isShieldActive()) helper.fail("Shield A should have collapsed mid-drill");
        });
    }

    // ==========================================
    // 4. Fall: ON vs ON (Schild-Zusammenstoss)
    // ==========================================

    @GameTest(template = "empty")
    public static void testOnVsOn_StandardClash(GameTestHelper helper) {
        ShipState shipA = createMockShip(helper, new BlockPos(1, 2, 1), 2, 2, 2, true, 100000);
        ShipState shipB = createMockShip(helper, new BlockPos(5, 2, 1), 2, 2, 2, true, 100000);

        List<BlockPos> collidingVoxels = new ArrayList<>();
        // Dummy-Kollision im leeren Raum (zwischen den Schilden)
        collidingVoxels.add(helper.absolutePos(new BlockPos(3, 2, 1)));

        ShipCollisionService.VoxelCollisionResult collision = buildResult(shipA, shipB, collidingVoxels);

        int initialA = SpaceshipEnergyManager.getTotalAvailableEnergy(helper.getLevel(), shipA);
        int initialB = SpaceshipEnergyManager.getTotalAvailableEnergy(helper.getLevel(), shipB);

        CollisionResolver.CollisionResolution resolution = CollisionResolver.resolve(
                helper.getLevel(), collision, new Vec3(1, 0, 0)
        );

        helper.succeedIf(() -> {
            if (!resolution.movementStopped()) helper.fail("Movement should have stopped on shield clash");
            
            int expectedDrain = collidingVoxels.size() * CollisionResolver.ENERGY_PER_VOXEL_SHIELD_CLASH;
            
            if (initialA - SpaceshipEnergyManager.getTotalAvailableEnergy(helper.getLevel(), shipA) != expectedDrain) {
                helper.fail("Energy A drain incorrect");
            }
            if (initialB - SpaceshipEnergyManager.getTotalAvailableEnergy(helper.getLevel(), shipB) != expectedDrain) {
                helper.fail("Energy B drain incorrect");
            }
            
            if (!shipA.isShieldActive() || !shipB.isShieldActive()) {
                helper.fail("Both shields should remain active since they had plenty of energy");
            }
        });
    }

    @GameTest(template = "empty")
    public static void testOnVsOn_AsymmetricCollapse(GameTestHelper helper) {
        ShipState shipA = createMockShip(helper, new BlockPos(1, 2, 1), 2, 2, 2, true, 100000);
        ShipState shipB = createMockShip(helper, new BlockPos(5, 2, 1), 2, 2, 2, true, 100); // Nur 100 FE!

        List<BlockPos> collidingVoxels = new ArrayList<>();
        // 2 Voxel = 300 FE clash cost
        collidingVoxels.add(helper.absolutePos(new BlockPos(3, 2, 1)));
        collidingVoxels.add(helper.absolutePos(new BlockPos(3, 3, 1)));

        ShipCollisionService.VoxelCollisionResult collision = buildResult(shipA, shipB, collidingVoxels);

        CollisionResolver.CollisionResolution resolution = CollisionResolver.resolve(
                helper.getLevel(), collision, new Vec3(1, 0, 0)
        );

        helper.succeedIf(() -> {
            if (!resolution.movementStopped()) helper.fail("Movement should have stopped");
            if (!shipA.isShieldActive()) helper.fail("Shield A should still be active");
            if (shipB.isShieldActive()) helper.fail("Shield B should have collapsed due to lack of energy");
        });
    }

}
