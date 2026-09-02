package com.peaceman.alpha.tests;

import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.block.entity.SpaceshipReactorBlockEntity;
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

    @GameTest(template = "empty")
    public static void testShipMovementDownWithRedstoneAndTorch(GameTestHelper helper) {
        BlockPos startCtrl = new BlockPos(2, 3, 2);
        BlockPos startReactor = new BlockPos(2, 3, 3);
        BlockPos startRedstone = new BlockPos(2, 4, 3);
        BlockPos startTorch = new BlockPos(2, 4, 2);

        // 1. Schiff aufbauen (Controller + Reactor + Redstone auf Reactor + Fackel auf Controller)
        helper.setBlock(startCtrl, ModBlocks.SPACESHIP_CONTROL.get());
        helper.setBlock(startReactor, ModBlocks.SPACESHIP_REACTOR.get());
        helper.setBlock(startRedstone, Blocks.REDSTONE_WIRE);
        helper.setBlock(startTorch, Blocks.TORCH);

        BlockPos absHullPos = helper.absolutePos(startReactor);
        if (helper.getLevel().getBlockEntity(absHullPos) instanceof SpaceshipReactorBlockEntity reactor) {
            reactor.getEnergyStorage().receiveEnergy(100000, false);
        }

        BlockPos startAbs = helper.absolutePos(startCtrl);
        ShipState ship = ServerShipManager.createShip(helper.getLevel(), startAbs);

        // 2. Schiff um 1 Block nach UNTEN bewegen (dy = -1)
        ShipMovementService.moveShip(helper.getLevel(), ship, 0, -1, 0, null);

        // 3. Überprüfung: Redstone & Fackel müssen sauber an neuer Position sein und KEINE Items gedroppt werden
        helper.succeedWhen(() -> {
            BlockPos targetCtrl = new BlockPos(2, 2, 2);
            BlockPos targetReactor = new BlockPos(2, 2, 3);
            BlockPos targetRedstone = new BlockPos(2, 3, 3);
            BlockPos targetTorch = new BlockPos(2, 3, 2);

            // An alter oberster Position muss Luft sein
            helper.assertBlockPresent(Blocks.AIR, startRedstone);
            helper.assertBlockPresent(Blocks.AIR, startTorch);

            // An neuer Position müssen alle 4 Blöcke existieren
            helper.assertBlockPresent(ModBlocks.SPACESHIP_CONTROL.get(), targetCtrl);
            helper.assertBlockPresent(ModBlocks.SPACESHIP_REACTOR.get(), targetReactor);
            helper.assertBlockPresent(Blocks.REDSTONE_WIRE, targetRedstone);
            helper.assertBlockPresent(Blocks.TORCH, targetTorch);

            // Keine gedroppten Items im Testbereich
            helper.assertItemEntityCountIs(net.minecraft.world.item.Items.REDSTONE, startRedstone, 5.0, 0);
            helper.assertItemEntityCountIs(net.minecraft.world.item.Items.TORCH, startTorch, 5.0, 0);
        });
    }

    @GameTest(template = "empty")
    public static void testMovement_PreservesTorchesAndDoors(GameTestHelper helper) {
        BlockPos startCtrl = new BlockPos(1, 2, 1);
        BlockPos startReactor = new BlockPos(1, 2, 2);
        BlockPos floorDoor1 = new BlockPos(1, 2, 3);
        BlockPos floorDoor2 = new BlockPos(1, 2, 4);
        BlockPos floorWall = new BlockPos(1, 2, 5);
        BlockPos wall = new BlockPos(1, 3, 5);
        BlockPos torch = new BlockPos(1, 4, 5);
        BlockPos door1Lower = new BlockPos(1, 3, 3);
        BlockPos door1Upper = new BlockPos(1, 4, 3);
        BlockPos door2Lower = new BlockPos(1, 3, 4);
        BlockPos door2Upper = new BlockPos(1, 4, 4);

        // 1. Schiff mit Reaktor, Wand, Fackel und zweiflügeliger Tür aufbauen
        helper.setBlock(startCtrl, ModBlocks.SPACESHIP_CONTROL.get());
        helper.setBlock(startReactor, ModBlocks.SPACESHIP_REACTOR.get());
        helper.setBlock(floorDoor1, Blocks.IRON_BLOCK);
        helper.setBlock(floorDoor2, Blocks.IRON_BLOCK);
        helper.setBlock(floorWall, Blocks.IRON_BLOCK);
        helper.setBlock(wall, Blocks.IRON_BLOCK);
        helper.setBlock(torch, Blocks.TORCH);

        // Zweiflügelige Eichentür (Lower + Upper)
        helper.setBlock(door1Lower, Blocks.OAK_DOOR.defaultBlockState()
                .setValue(net.minecraft.world.level.block.DoorBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER)
                .setValue(net.minecraft.world.level.block.DoorBlock.FACING, net.minecraft.core.Direction.NORTH));
        helper.setBlock(door1Upper, Blocks.OAK_DOOR.defaultBlockState()
                .setValue(net.minecraft.world.level.block.DoorBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER)
                .setValue(net.minecraft.world.level.block.DoorBlock.FACING, net.minecraft.core.Direction.NORTH));

        helper.setBlock(door2Lower, Blocks.OAK_DOOR.defaultBlockState()
                .setValue(net.minecraft.world.level.block.DoorBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER)
                .setValue(net.minecraft.world.level.block.DoorBlock.FACING, net.minecraft.core.Direction.NORTH)
                .setValue(net.minecraft.world.level.block.DoorBlock.HINGE, net.minecraft.world.level.block.state.properties.DoorHingeSide.RIGHT));
        helper.setBlock(door2Upper, Blocks.OAK_DOOR.defaultBlockState()
                .setValue(net.minecraft.world.level.block.DoorBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER)
                .setValue(net.minecraft.world.level.block.DoorBlock.FACING, net.minecraft.core.Direction.NORTH)
                .setValue(net.minecraft.world.level.block.DoorBlock.HINGE, net.minecraft.world.level.block.state.properties.DoorHingeSide.RIGHT));

        BlockPos absReactorPos = helper.absolutePos(startReactor);
        if (helper.getLevel().getBlockEntity(absReactorPos) instanceof SpaceshipReactorBlockEntity reactor) {
            reactor.getEnergyStorage().receiveEnergy(100000, false);
        }

        BlockPos startAbs = helper.absolutePos(startCtrl);
        ShipState ship = ServerShipManager.createShip(helper.getLevel(), startAbs);

        // 2. Schiff um 2 Blöcke in X-Richtung verschieben (dx = 2)
        ShipMovementService.moveShip(helper.getLevel(), ship, 2, 0, 0, null);

        // 3. Überprüfung: Blöcke intakt am Ziel und KEINE Drops am Ursprung
        helper.succeedWhen(() -> {
            BlockPos targetCtrl = new BlockPos(3, 2, 1);
            BlockPos targetReactor = new BlockPos(3, 2, 2);
            BlockPos targetFloorDoor1 = new BlockPos(3, 2, 3);
            BlockPos targetFloorDoor2 = new BlockPos(3, 2, 4);
            BlockPos targetFloorWall = new BlockPos(3, 2, 5);
            BlockPos targetWall = new BlockPos(3, 3, 5);
            BlockPos targetTorch = new BlockPos(3, 4, 5);
            BlockPos targetDoor1Lower = new BlockPos(3, 3, 3);
            BlockPos targetDoor1Upper = new BlockPos(3, 4, 3);
            BlockPos targetDoor2Lower = new BlockPos(3, 3, 4);
            BlockPos targetDoor2Upper = new BlockPos(3, 4, 4);

            // Alte Positionen müssen Luft sein
            helper.assertBlockPresent(Blocks.AIR, startCtrl);
            helper.assertBlockPresent(Blocks.AIR, startReactor);
            helper.assertBlockPresent(Blocks.AIR, wall);
            helper.assertBlockPresent(Blocks.AIR, torch);
            helper.assertBlockPresent(Blocks.AIR, door1Lower);
            helper.assertBlockPresent(Blocks.AIR, door1Upper);
            helper.assertBlockPresent(Blocks.AIR, door2Lower);
            helper.assertBlockPresent(Blocks.AIR, door2Upper);

            // Neue Positionen müssen intakt existieren
            helper.assertBlockPresent(ModBlocks.SPACESHIP_CONTROL.get(), targetCtrl);
            helper.assertBlockPresent(ModBlocks.SPACESHIP_REACTOR.get(), targetReactor);
            helper.assertBlockPresent(Blocks.IRON_BLOCK, targetFloorDoor1);
            helper.assertBlockPresent(Blocks.IRON_BLOCK, targetFloorDoor2);
            helper.assertBlockPresent(Blocks.IRON_BLOCK, targetFloorWall);
            helper.assertBlockPresent(Blocks.IRON_BLOCK, targetWall);
            helper.assertBlockPresent(Blocks.TORCH, targetTorch);
            helper.assertBlockPresent(Blocks.OAK_DOOR, targetDoor1Lower);
            helper.assertBlockPresent(Blocks.OAK_DOOR, targetDoor1Upper);
            helper.assertBlockPresent(Blocks.OAK_DOOR, targetDoor2Lower);
            helper.assertBlockPresent(Blocks.OAK_DOOR, targetDoor2Upper);

            // Keine gedroppten Items im Testbereich
            helper.assertItemEntityCountIs(net.minecraft.world.item.Items.TORCH, torch, 10.0, 0);
            helper.assertItemEntityCountIs(net.minecraft.world.item.Items.OAK_DOOR, door1Lower, 10.0, 0);
        });
    }

    @GameTest(template = "empty")
    public static void testMovement_BlockedByImmuneBlock(GameTestHelper helper) {
        BlockPos startCtrl = new BlockPos(1, 2, 1);
        BlockPos bedrockPos = new BlockPos(1, 2, 2);

        // 1. Schiff mit Kontrollblock und unverschiebbarem Bedrock aufbauen
        helper.setBlock(startCtrl, ModBlocks.SPACESHIP_CONTROL.get());
        helper.setBlock(bedrockPos, Blocks.BEDROCK);

        BlockPos startAbs = helper.absolutePos(startCtrl);
        ShipState ship = ServerShipManager.createShip(helper.getLevel(), startAbs);

        // 2. Versuch, das Schiff zu verschieben (dx = 1)
        ShipMovementService.moveShip(helper.getLevel(), ship, 1, 0, 0, null);

        // 3. Überprüfung: Blöcke dürfen sich wegen Immunität nicht bewegt haben
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(ModBlocks.SPACESHIP_CONTROL.get(), startCtrl);
            helper.assertBlockPresent(Blocks.BEDROCK, bedrockPos);
            helper.assertBlockPresent(Blocks.AIR, new BlockPos(2, 2, 1));
            helper.assertBlockPresent(Blocks.AIR, new BlockPos(2, 2, 2));
        });
    }

    @GameTest(template = "empty")
    public static void testMovement_PreservesExtendedPistons(GameTestHelper helper) {
        BlockPos startCtrl = new BlockPos(1, 2, 1);
        BlockPos startReactor = new BlockPos(1, 2, 2);
        BlockPos floorPiston = new BlockPos(2, 2, 2);
        BlockPos pistonBase = new BlockPos(2, 3, 2);
        BlockPos pistonHead = new BlockPos(2, 4, 2);
        BlockPos redstoneSource = new BlockPos(3, 3, 2);

        // 1. Schiff mit Kontrollblock, Reaktor, Energie und ausgefahrenem Piston aufbauen
        helper.setBlock(startCtrl, ModBlocks.SPACESHIP_CONTROL.get());
        helper.setBlock(startReactor, ModBlocks.SPACESHIP_REACTOR.get());
        helper.setBlock(floorPiston, Blocks.IRON_BLOCK);
        helper.setBlock(redstoneSource, Blocks.REDSTONE_BLOCK);

        // Ausgefahrenen Piston (Base + Head nach UP) platzieren
        helper.setBlock(pistonBase, Blocks.PISTON.defaultBlockState()
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.EXTENDED, true)
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, net.minecraft.core.Direction.UP));
        helper.setBlock(pistonHead, Blocks.PISTON_HEAD.defaultBlockState()
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, net.minecraft.core.Direction.UP));

        BlockPos absReactorPos = helper.absolutePos(startReactor);
        if (helper.getLevel().getBlockEntity(absReactorPos) instanceof SpaceshipReactorBlockEntity reactor) {
            reactor.getEnergyStorage().receiveEnergy(100000, false);
        }

        BlockPos startAbs = helper.absolutePos(startCtrl);
        ShipState ship = ServerShipManager.createShip(helper.getLevel(), startAbs);

        // 2. Schiff um 2 Blöcke in X-Richtung verschieben (dx = 2)
        ShipMovementService.moveShip(helper.getLevel(), ship, 2, 0, 0, null);

        // 3. Überprüfung: Blöcke intakt am Ziel und KEINE Drops am Ursprung
        helper.succeedWhen(() -> {
            BlockPos targetCtrl = new BlockPos(3, 2, 1);
            BlockPos targetReactor = new BlockPos(3, 2, 2);
            BlockPos targetPistonBase = new BlockPos(4, 3, 2);
            BlockPos targetPistonHead = new BlockPos(4, 4, 2);

            // Alte Positionen müssen Luft sein
            helper.assertBlockPresent(Blocks.AIR, startCtrl);
            helper.assertBlockPresent(Blocks.AIR, startReactor);
            helper.assertBlockPresent(Blocks.AIR, pistonBase);
            helper.assertBlockPresent(Blocks.AIR, pistonHead);

            // Neue Positionen müssen intakt existieren
            helper.assertBlockPresent(ModBlocks.SPACESHIP_CONTROL.get(), targetCtrl);
            helper.assertBlockPresent(ModBlocks.SPACESHIP_REACTOR.get(), targetReactor);
            helper.assertBlockPresent(Blocks.PISTON, targetPistonBase);
            helper.assertBlockPresent(Blocks.PISTON_HEAD, targetPistonHead);

            // Keine gedroppten Piston-Items im Testbereich!
            helper.assertItemEntityCountIs(net.minecraft.world.item.Items.PISTON, pistonBase, 10.0, 0);
            helper.assertItemEntityCountIs(net.minecraft.world.item.Items.STICKY_PISTON, pistonBase, 10.0, 0);
        });
    }
}
