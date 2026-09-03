package com.lit.spaceships.tests;

import com.lit.spaceships.LitSpaceships;
import com.lit.spaceships.registry.ModBlocks;
import com.lit.spaceships.ship.service.ShipScannerService;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.util.Set;

/**
 * GameTests für den Breitensuche-Scanner (BFS) zur Struktur- und Multiblock-Erkennung.
 */
@GameTestHolder(LitSpaceships.MODID)
public class ShipScannerGameTests {

    @GameTest(template = "empty")
    public static void testShipScannerConnectedBlocks(GameTestHelper helper) {
        BlockPos controllerRel = new BlockPos(1, 2, 1);
        BlockPos iron1Rel = new BlockPos(1, 2, 2);
        BlockPos iron2Rel = new BlockPos(1, 2, 3);
        BlockPos iron3Rel = new BlockPos(2, 2, 2);
        BlockPos isolatedRel = new BlockPos(4, 2, 4);

        // 1. Controller und 3 verbundene Eisenblöcke platzieren
        helper.setBlock(controllerRel, ModBlocks.SPACESHIP_CONTROL.get());
        helper.setBlock(iron1Rel, Blocks.IRON_BLOCK);
        helper.setBlock(iron2Rel, Blocks.IRON_BLOCK);
        helper.setBlock(iron3Rel, Blocks.IRON_BLOCK);

        // Einen unverbundenen, isolierten Block platzieren (darf nicht im Scan sein)
        helper.setBlock(isolatedRel, Blocks.IRON_BLOCK);

        // 2. Scan durchführen
        BlockPos controllerAbs = helper.absolutePos(controllerRel);
        Set<BlockPos> scannedBlocks = ShipScannerService.scan(helper.getLevel(), controllerAbs);

        // 3. Überprüfung: Exakt 4 Blöcke
        helper.succeedIf(() -> {
            if (scannedBlocks.size() != 4) {
                helper.fail("ShipScannerService sollte exakt 4 verbundene Blöcke finden, fand aber: " + scannedBlocks.size());
                return;
            }

            BlockPos iron1Abs = helper.absolutePos(iron1Rel);
            BlockPos iron2Abs = helper.absolutePos(iron2Rel);
            BlockPos iron3Abs = helper.absolutePos(iron3Rel);
            BlockPos isolatedAbs = helper.absolutePos(isolatedRel);

            if (!scannedBlocks.contains(controllerAbs) ||
                !scannedBlocks.contains(iron1Abs) ||
                !scannedBlocks.contains(iron2Abs) ||
                !scannedBlocks.contains(iron3Abs)) {
                helper.fail("ShipScannerService enthält nicht alle erwarteten verbundenen Blöcke!");
                return;
            }

            if (scannedBlocks.contains(isolatedAbs)) {
                helper.fail("ShipScannerService hat einen nicht verbundenen Block mitgescannt!");
            }
        });
    }

    @GameTest(template = "empty")
    public static void testShipScannerDiagonalIgnored(GameTestHelper helper) {
        BlockPos controllerRel = new BlockPos(1, 2, 1);
        BlockPos diagonalRel = new BlockPos(2, 2, 2); // Nur diagonal berührend

        helper.setBlock(controllerRel, ModBlocks.SPACESHIP_CONTROL.get());
        helper.setBlock(diagonalRel, Blocks.IRON_BLOCK);

        BlockPos controllerAbs = helper.absolutePos(controllerRel);
        Set<BlockPos> scannedBlocks = ShipScannerService.scan(helper.getLevel(), controllerAbs);

        helper.succeedIf(() -> {
            if (scannedBlocks.size() != 1) {
                helper.fail("Diagonale Blöcke dürfen nicht erfasst werden! Gefunden: " + scannedBlocks.size());
            }
        });
    }

    @GameTest(template = "empty")
    public static void testShipScannerDoorMultiblock(GameTestHelper helper) {
        BlockPos controllerRel = new BlockPos(1, 2, 1);
        BlockPos doorBottomRel = new BlockPos(1, 2, 2);
        BlockPos doorTopRel = new BlockPos(1, 3, 2);

        helper.setBlock(controllerRel, ModBlocks.SPACESHIP_CONTROL.get());
        // Unteren Türblock platzieren
        helper.setBlock(doorBottomRel, Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));

        BlockPos controllerAbs = helper.absolutePos(controllerRel);
        Set<BlockPos> scannedBlocks = ShipScannerService.scan(helper.getLevel(), controllerAbs);

        helper.succeedIf(() -> {
            BlockPos doorTopAbs = helper.absolutePos(doorTopRel);
            if (!scannedBlocks.contains(doorTopAbs)) {
                helper.fail("Multiblock-Erweiterung hat den oberen Türblock nicht mit einbezogen!");
            }
        });
    }

    @GameTest(template = "empty")
    public static void testShipScannerPistonMultiblock(GameTestHelper helper) {
        BlockPos controllerRel = new BlockPos(1, 2, 1);
        BlockPos pistonBaseRel = new BlockPos(1, 2, 2);
        BlockPos pistonHeadRel = new BlockPos(1, 3, 2);

        helper.setBlock(controllerRel, ModBlocks.SPACESHIP_CONTROL.get());
        // Ausgefahrenen Piston (Base nach UP) platzieren
        helper.setBlock(pistonBaseRel, Blocks.PISTON.defaultBlockState()
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.EXTENDED, true)
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, net.minecraft.core.Direction.UP));
        helper.setBlock(pistonHeadRel, Blocks.PISTON_HEAD.defaultBlockState()
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, net.minecraft.core.Direction.UP));

        BlockPos controllerAbs = helper.absolutePos(controllerRel);
        Set<BlockPos> scannedBlocks = ShipScannerService.scan(helper.getLevel(), controllerAbs);

        helper.succeedIf(() -> {
            BlockPos headAbs = helper.absolutePos(pistonHeadRel);
            if (!scannedBlocks.contains(headAbs)) {
                helper.fail("Multiblock-Erweiterung hat den ausgefahrenen Piston-Kopf nicht mit einbezogen!");
            }
        });
    }
}
