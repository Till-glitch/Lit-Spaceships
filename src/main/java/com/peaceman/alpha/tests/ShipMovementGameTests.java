package com.peaceman.alpha.tests;

import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.registry.ModBlocks;
import com.peaceman.alpha.block.entity.SpaceshipReactorBlockEntity;
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

    @GameTest(template = "empty")
    public static void testShipMovementRelocation(GameTestHelper helper) {
        BlockPos startRel = new BlockPos(1, 2, 1);
        BlockPos startHullRel = new BlockPos(1, 2, 2);

        // 1. Schiff aufbauen
        helper.setBlock(startRel, ModBlocks.SPACESHIP_CONTROL.get());
        helper.setBlock(startHullRel, ModBlocks.SPACESHIP_REACTOR.get());

        BlockPos absHullPos = helper.absolutePos(startHullRel);
        if (helper.getLevel().getBlockEntity(absHullPos) instanceof SpaceshipReactorBlockEntity reactor) {
            reactor.getEnergyStorage().receiveEnergy(100000, false);
        }

        BlockPos startAbs = helper.absolutePos(startRel);
        ShipState ship = ServerShipManager.createShip(helper.getLevel(), startAbs);

        // 2. Schiff um 2 Blöcke in X-Richtung verschieben
        ShipMovementService.moveShip(helper.getLevel(), ship, 2, 0, 0, null);

        // 3. Überprüfung
        helper.succeedWhen(() -> {
            BlockPos targetRel = new BlockPos(3, 2, 1);
            BlockPos targetHullRel = new BlockPos(3, 2, 2);

            // An alter Position muss Luft sein
            helper.assertBlockPresent(Blocks.AIR, startRel);
            helper.assertBlockPresent(Blocks.AIR, startHullRel);

            // An neuer Position müssen die Blöcke existieren
            helper.assertBlockPresent(ModBlocks.SPACESHIP_CONTROL.get(), targetRel);
            helper.assertBlockPresent(ModBlocks.SPACESHIP_REACTOR.get(), targetHullRel);
        });
    }

    @GameTest(template = "empty")
    public static void testShipMovementPreservesShieldEnergy(GameTestHelper helper) {
        BlockPos startCtrlRel = new BlockPos(1, 2, 1);
        BlockPos startReactorRel = new BlockPos(1, 2, 2);
        BlockPos startShieldRel = new BlockPos(1, 2, 3);

        // 1. Schiff mit Schildgenerator aufbauen
        helper.setBlock(startCtrlRel, ModBlocks.SPACESHIP_CONTROL.get());
        helper.setBlock(startReactorRel, ModBlocks.SPACESHIP_REACTOR.get());
        helper.setBlock(startShieldRel, ModBlocks.SPACESHIP_SHIELD.get());

        BlockPos absReactorPos = helper.absolutePos(startReactorRel);
        if (helper.getLevel().getBlockEntity(absReactorPos) instanceof SpaceshipReactorBlockEntity reactor) {
            reactor.getEnergyStorage().receiveEnergy(100000, false);
        }

        BlockPos startCtrlAbs = helper.absolutePos(startCtrlRel);
        ShipState ship = ServerShipManager.createShip(helper.getLevel(), startCtrlAbs);

        // Schildzone mit 50.000 FE aufladen
        if (ship != null && !ship.getShieldZones().isEmpty()) {
            ship.updateShieldZoneEnergy((byte) 1, 50000);
        }

        // 2. Schiff verschieben (dx=2, dy=0, dz=0)
        ShipMovementService.moveShip(helper.getLevel(), ship, 2, 0, 0, null);

        // 3. Überprüfung nach Bewegung
        helper.succeedWhen(() -> {
            BlockPos targetShieldRel = new BlockPos(3, 2, 3);
            helper.assertBlockPresent(ModBlocks.SPACESHIP_SHIELD.get(), targetShieldRel);

            if (ship == null) {
                helper.fail("ShipState ist null");
                return;
            }

            com.peaceman.alpha.ship.domain.ShieldZone zone = ship.getShieldZone((byte) 1);
            if (zone == null) {
                helper.fail("ShieldZone 1 nicht gefunden nach Bewegung");
                return;
            }

            BlockPos expectedAbsShieldPos = helper.absolutePos(targetShieldRel);
            if (!expectedAbsShieldPos.equals(zone.generatorPos())) {
                helper.fail("Generator-Position in ShieldZone falsch. Erwartet: " + expectedAbsShieldPos + ", Gefunden: " + zone.generatorPos());
                return;
            }

            if (zone.currentEnergy() != 50000) {
                helper.fail("Schildenergie wurde zurückgesetzt! Erwartet 50000, aber gefunden: " + zone.currentEnergy());
            }
        });
    }
}
