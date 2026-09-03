package com.lit.spaceships.tests;

import com.lit.spaceships.LitSpaceships;
import com.lit.spaceships.block.entity.SpaceshipReactorBlockEntity;
import com.lit.spaceships.registry.ModBlocks;
import com.lit.spaceships.ship.SpaceshipEnergyManager;
import com.lit.spaceships.ship.domain.ShipState;
import com.lit.spaceships.ship.service.CollisionResolver;
import com.lit.spaceships.ship.service.ServerShipManager;
import com.lit.spaceships.ship.service.ShipCollisionService;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@GameTestHolder(LitSpaceships.MODID)
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
        ShipState shipA = createMockShip(helper, new BlockPos(1, 2, 1), 6, 6, 10, false, 0);
        ShipState shipB = createMockShip(helper, new BlockPos(3, 2, 1), 6, 6, 10, false, 0);

        List<BlockPos> collidingVoxels = new ArrayList<>();
        // Echte 5x5x8 Matrix (200 distinkte Voxel), um den Cluster-Explosions-Code auszubilden (size > 100)
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                for (int z = 0; z < 8; z++) {
                    collidingVoxels.add(helper.absolutePos(new BlockPos(2 + x, 2 + y, 1 + z)));
                }
            }
        }

        ShipCollisionService.VoxelCollisionResult collision = buildResult(shipA, shipB, collidingVoxels);

        CollisionResolver.resolve(helper.getLevel(), collision, new Vec3(1, 0, 0));

        // Nach der Explosion sollen Bloecke innerhalb des Explosionsradius zerstoert worden sein.
        helper.succeedIf(() -> {
            helper.assertBlockPresent(Blocks.AIR, new BlockPos(2, 2, 1));
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
            collidingVoxels.add(helper.absolutePos(new BlockPos(2 + i, 2, 1))); 
        }

        ShipCollisionService.VoxelCollisionResult collision = buildResult(shipA, shipB, collidingVoxels);

        CollisionResolver.resolve(helper.getLevel(), collision, new Vec3(1, 0, 0));

        helper.succeedIf(() -> {
            if (shipB.isShieldActive()) {
                helper.fail("Shield B should have collapsed due to insufficient energy!");
            }
        });
    }

    @GameTest(template = "empty")
    public static void testOffVsOn_PointZeroBoundary(GameTestHelper helper) {
        ShipState shipA = createMockShip(helper, new BlockPos(1, 2, 1), 2, 2, 2, false, 0);
        ShipState shipB = createMockShip(helper, new BlockPos(5, 2, 1), 2, 2, 2, true, 200); // Exakt 200 FE fuer 2 Voxel

        List<BlockPos> collidingVoxels = new ArrayList<>();
        collidingVoxels.add(helper.absolutePos(new BlockPos(2, 2, 1)));
        collidingVoxels.add(helper.absolutePos(new BlockPos(2, 3, 1)));

        ShipCollisionService.VoxelCollisionResult collision = buildResult(shipA, shipB, collidingVoxels);

        CollisionResolver.CollisionResolution resolution = CollisionResolver.resolve(
                helper.getLevel(), collision, new Vec3(1, 0, 0)
        );

        helper.succeedIf(() -> {
            if (!resolution.movementStopped()) helper.fail("Movement should have stopped (kinetic impact)");
            int remainingEnergy = SpaceshipEnergyManager.getTotalAvailableEnergy(helper.getLevel(), shipB);
            if (remainingEnergy != 0) {
                helper.fail("Energy should be exactly 0 FE, but was: " + remainingEnergy);
            }
            if (shipB.isShieldActive()) {
                helper.fail("Shield B must collapse and cannot remain active with 0 FE!");
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
        ShipState shipB = createMockShip(helper, new BlockPos(5, 2, 1), 5, 2, 2, false, 0);

        List<BlockPos> collidingVoxels = new ArrayList<>();
        // Echter 5x2x1 Schnitt (10 distinkte Voxel) = 1000 FE erforderlich -> Mid-Drill Collapse
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 2; y++) {
                for (int z = 0; z < 1; z++) {
                    collidingVoxels.add(helper.absolutePos(new BlockPos(5 + x, 2 + y, 1 + z)));
                }
            }
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

    @GameTest(timeoutTicks = 40, template = "empty")
    public static void testOnVsOff_FloatingBlocksUpdate(GameTestHelper helper) {
        ShipState shipA = createMockShip(helper, new BlockPos(1, 2, 1), 2, 2, 2, true, 100000);
        ShipState shipB = createMockShip(helper, new BlockPos(5, 2, 1), 2, 1, 2, false, 0);

        // Platziere abhängigen Block (Fackel) auf dem Huellenblock von Schiff B
        helper.setBlock(new BlockPos(5, 3, 1), Blocks.TORCH);

        List<BlockPos> collidingVoxels = new ArrayList<>();
        collidingVoxels.add(helper.absolutePos(new BlockPos(5, 2, 1)));

        ShipCollisionService.VoxelCollisionResult collision = buildResult(shipA, shipB, collidingVoxels);

        // Führe die Kollision bei Tick 5 aus
        helper.runAtTickTime(5, () -> {
            CollisionResolver.resolve(helper.getLevel(), collision, new Vec3(1, 0, 0));
        });

        // Validiere bei Tick 5, dass der Block entfernt und das Item asynchron gedroppt wurde
        helper.succeedOnTickWhen(5, () -> {
            helper.assertBlockPresent(Blocks.AIR, new BlockPos(5, 2, 1));
            helper.assertItemEntityCountIs(Items.TORCH, new BlockPos(5, 3, 1), 2.0, 1);
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

    @GameTest(template = "empty")
    public static void testMultiCollision_ShieldPriority(GameTestHelper helper) {
        ShipState shipA = createMockShip(helper, new BlockPos(1, 2, 1), 2, 2, 2, true, 100000);
        ShipState shipB = createMockShip(helper, new BlockPos(5, 2, 1), 2, 2, 2, true, 100000);
        ShipState shipC = createMockShip(helper, new BlockPos(5, 2, 5), 2, 2, 2, false, 0);

        int initialEnergyB = SpaceshipEnergyManager.getTotalAvailableEnergy(helper.getLevel(), shipB);

        // Zwei Kollisionen vorbereiten: Schiff A schneidet in B (Schild) und C (ungeschuetzt)
        ShipCollisionService.VoxelCollisionResult collisionB = buildResult(
                shipA, shipB, List.of(helper.absolutePos(new BlockPos(3, 2, 1)))
        );
        ShipCollisionService.VoxelCollisionResult collisionC = buildResult(
                shipA, shipC, List.of(helper.absolutePos(new BlockPos(5, 2, 5)))
        );

        // Uebergib bewusst collisionC ZUERST, um die korrekte Priorisierung (Schild vor Drill) zu pruefen
        CollisionResolver.CollisionResolution resolution = CollisionResolver.resolveMultiple(
                helper.getLevel(), List.of(collisionC, collisionB), new Vec3(1, 0, 0)
        );

        helper.succeedIf(() -> {
            if (!resolution.movementStopped()) {
                helper.fail("Ship A should have been stopped by Ship B's shield!");
            }
            int remainingB = SpaceshipEnergyManager.getTotalAvailableEnergy(helper.getLevel(), shipB);
            if (remainingB >= initialEnergyB) {
                helper.fail("Energy should have been drained on Ship B! Initial: " + initialEnergyB + ", Remaining: " + remainingB);
            }
            // Schiff C muss vollkommen unversehrt sein (keine Phantom-Durchdringung)
            helper.assertBlockPresent(Blocks.IRON_BLOCK, new BlockPos(5, 2, 5));
            if (!shipC.getBlocks().contains(helper.absolutePos(new BlockPos(5, 2, 5)))) {
                helper.fail("Ship C hull blocks were modified despite movement stopping on Ship B!");
            }
        });
    }

}
