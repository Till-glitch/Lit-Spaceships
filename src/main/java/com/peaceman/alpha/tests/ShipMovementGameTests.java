package com.peaceman.alpha.tests;

import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.registry.ModBlocks;
import com.peaceman.alpha.ship.domain.ShipState;
import com.peaceman.alpha.ship.service.ServerShipManager;
import com.peaceman.alpha.ship.service.ShipMovementService;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;

/**
 * GameTests für physische Schiffsbewegungen in der Minecraft-Welt.
 */
@GameTestHolder(Alpha.MODID)
public class ShipMovementGameTests {

    @GameTest
    public static void testShipMovementRelocation(GameTestHelper helper) {
        BlockPos startRel = new BlockPos(1, 2, 1);
        BlockPos startHullRel = new BlockPos(1, 2, 2);

        // 1. Schiff aufbauen
        helper.setBlock(startRel, ModBlocks.SPACESHIP_CONTROL.get());
        helper.setBlock(startHullRel, Blocks.IRON_BLOCK);

        BlockPos startAbs = helper.absolutePos(startRel);
        ShipState ship = ServerShipManager.createShip(helper.getLevel(), startAbs);

        // 2. Schiff um 2 Blöcke in X-Richtung verschieben
        ShipMovementService.moveShip(helper.getLevel(), ship, 2, 0, 0, null);

        // 3. Überprüfung
        helper.succeedIf(() -> {
            BlockPos targetRel = new BlockPos(3, 2, 1);
            BlockPos targetHullRel = new BlockPos(3, 2, 2);

            // An alter Position muss Luft sein
            helper.assertBlockPresent(Blocks.AIR, startRel);
            helper.assertBlockPresent(Blocks.AIR, startHullRel);

            // An neuer Position müssen die Blöcke existieren
            helper.assertBlockPresent(ModBlocks.SPACESHIP_CONTROL.get(), targetRel);
            helper.assertBlockPresent(Blocks.IRON_BLOCK, targetHullRel);
        });
    }
}
