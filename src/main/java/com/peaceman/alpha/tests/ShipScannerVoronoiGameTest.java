package com.peaceman.alpha.tests;

import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.registry.ModBlocks;
import com.peaceman.alpha.ship.domain.ShipState;
import com.peaceman.alpha.ship.service.ServerShipManager;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;

/**
 * GameTest für die Voronoi-Zuweisung und Erfassung lokalisierter Schildzonen.
 */
@GameTestHolder(Alpha.MODID)
public class ShipScannerVoronoiGameTest {

    @GameTest(template = "empty")
    public static void testShipScannerVoronoiZoning(GameTestHelper helper) {
        BlockPos ctrlRel = new BlockPos(2, 2, 2);
        helper.setBlock(ctrlRel, ModBlocks.SPACESHIP_CONTROL.get());

        // 3 Schildgeneratoren platzieren
        BlockPos s1Rel = new BlockPos(1, 2, 2);
        BlockPos s2Rel = new BlockPos(3, 2, 2);
        BlockPos s3Rel = new BlockPos(2, 2, 3);
        helper.setBlock(s1Rel, ModBlocks.SPACESHIP_SHIELD.get());
        helper.setBlock(s2Rel, ModBlocks.SPACESHIP_SHIELD.get());
        helper.setBlock(s3Rel, ModBlocks.SPACESHIP_SHIELD.get());

        // Hüllenblöcke platzieren
        for (int i = 0; i < 5; i++) {
            helper.setBlock(new BlockPos(2, 3 + i, 2), Blocks.IRON_BLOCK);
        }

        BlockPos ctrlAbs = helper.absolutePos(ctrlRel);
        ShipState ship = ServerShipManager.createShip(helper.getLevel(), ctrlAbs);

        helper.succeedIf(() -> {
            if (ship == null) {
                helper.fail("ShipState konnte nicht erstellt werden");
                return;
            }
            if (ship.getShieldZones().size() != 3) {
                helper.fail("Erwartet 3 ShieldZones, aber gefunden: " + ship.getShieldZones().size());
                return;
            }
            // Prüfe, dass kein Voxel des Hüllen-Caches die Shield-ID 0 aufweist
            for (BlockPos b : ship.getBlocks()) {
                BlockPos rel = b.subtract(ctrlAbs);
                byte id = ship.getHullVoxelCache().getShieldId(rel);
                if (id == 0) {
                    helper.fail("Hüllenblock bei " + rel + " hat Shield-ID 0 erhalten (ungeschützt)");
                    return;
                }
            }
        });
    }
}
