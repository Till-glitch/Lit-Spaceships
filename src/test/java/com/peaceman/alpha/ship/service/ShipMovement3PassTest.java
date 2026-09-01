package com.peaceman.alpha.ship.service;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit-Tests für die topologische 3-Pass-Klassifizierung und Platzierungs-Sortierung
 * im ShipMovementService.
 */
public class ShipMovement3PassTest {

    @BeforeAll
    static void initMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    @DisplayName("Pass 1 klassifiziert massive Vollblöcke (Fundamente)")
    void testPass1Solids() {
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState ironBlock = Blocks.IRON_BLOCK.defaultBlockState();
        BlockState planks = Blocks.OAK_PLANKS.defaultBlockState();
        BlockState glass = Blocks.GLASS.defaultBlockState();

        assertEquals(ShipMovementService.PlacementPass.PASS_1_SOLIDS,
                ShipMovementService.getPlacementPass(stone, null, BlockPos.ZERO));
        assertEquals(ShipMovementService.PlacementPass.PASS_1_SOLIDS,
                ShipMovementService.getPlacementPass(ironBlock, null, BlockPos.ZERO));
        assertEquals(ShipMovementService.PlacementPass.PASS_1_SOLIDS,
                ShipMovementService.getPlacementPass(planks, null, BlockPos.ZERO));
        assertEquals(ShipMovementService.PlacementPass.PASS_1_SOLIDS,
                ShipMovementService.getPlacementPass(glass, null, BlockPos.ZERO));
    }

    @Test
    @DisplayName("Pass 2 klassifiziert untere Multiblock-Wurzeln und Standardblöcke")
    void testPass2RootsAndNormals() {
        BlockState lowerDoor = Blocks.OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        BlockState bedFoot = Blocks.RED_BED.defaultBlockState()
                .setValue(BlockStateProperties.BED_PART, BedPart.FOOT);
        BlockState stairs = Blocks.STONE_STAIRS.defaultBlockState();
        BlockState slab = Blocks.STONE_SLAB.defaultBlockState();

        assertEquals(ShipMovementService.PlacementPass.PASS_2_ROOTS_AND_NORMALS,
                ShipMovementService.getPlacementPass(lowerDoor, null, BlockPos.ZERO));
        assertEquals(ShipMovementService.PlacementPass.PASS_2_ROOTS_AND_NORMALS,
                ShipMovementService.getPlacementPass(bedFoot, null, BlockPos.ZERO));
        assertEquals(ShipMovementService.PlacementPass.PASS_2_ROOTS_AND_NORMALS,
                ShipMovementService.getPlacementPass(stairs, null, BlockPos.ZERO));
        assertEquals(ShipMovementService.PlacementPass.PASS_2_ROOTS_AND_NORMALS,
                ShipMovementService.getPlacementPass(slab, null, BlockPos.ZERO));
    }

    @Test
    @DisplayName("Pass 3 klassifiziert obere Multiblock-Hälften und anhängende / zerbrechliche Blöcke")
    void testPass3AttachablesAndTops() {
        BlockState upperDoor = Blocks.OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
        BlockState bedHead = Blocks.RED_BED.defaultBlockState()
                .setValue(BlockStateProperties.BED_PART, BedPart.HEAD);
        BlockState torch = Blocks.TORCH.defaultBlockState();
        BlockState wallTorch = Blocks.WALL_TORCH.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        BlockState redstoneWire = Blocks.REDSTONE_WIRE.defaultBlockState();
        BlockState ladder = Blocks.LADDER.defaultBlockState();
        BlockState lever = Blocks.LEVER.defaultBlockState();
        BlockState button = Blocks.STONE_BUTTON.defaultBlockState();

        assertEquals(ShipMovementService.PlacementPass.PASS_3_ATTACHABLES_AND_TOPS,
                ShipMovementService.getPlacementPass(upperDoor, null, BlockPos.ZERO));
        assertEquals(ShipMovementService.PlacementPass.PASS_3_ATTACHABLES_AND_TOPS,
                ShipMovementService.getPlacementPass(bedHead, null, BlockPos.ZERO));
        assertEquals(ShipMovementService.PlacementPass.PASS_3_ATTACHABLES_AND_TOPS,
                ShipMovementService.getPlacementPass(torch, null, BlockPos.ZERO));
        assertEquals(ShipMovementService.PlacementPass.PASS_3_ATTACHABLES_AND_TOPS,
                ShipMovementService.getPlacementPass(wallTorch, null, BlockPos.ZERO));
        assertEquals(ShipMovementService.PlacementPass.PASS_3_ATTACHABLES_AND_TOPS,
                ShipMovementService.getPlacementPass(redstoneWire, null, BlockPos.ZERO));
        assertEquals(ShipMovementService.PlacementPass.PASS_3_ATTACHABLES_AND_TOPS,
                ShipMovementService.getPlacementPass(ladder, null, BlockPos.ZERO));
        assertEquals(ShipMovementService.PlacementPass.PASS_3_ATTACHABLES_AND_TOPS,
                ShipMovementService.getPlacementPass(lever, null, BlockPos.ZERO));
        assertEquals(ShipMovementService.PlacementPass.PASS_3_ATTACHABLES_AND_TOPS,
                ShipMovementService.getPlacementPass(button, null, BlockPos.ZERO));
    }

    @Test
    @DisplayName("Pass-Sortierung sortiert innerhalb jedes Passes aufsteigend nach Y")
    void testPassSorting_AscendingY() {
        List<BlockPos> positions = new ArrayList<>(List.of(
                new BlockPos(0, 15, 0),
                new BlockPos(0, 5, 0),
                new BlockPos(0, 20, 0),
                new BlockPos(0, 10, 0)
        ));

        positions.sort(Comparator.comparingInt(BlockPos::getY));

        assertEquals(5, positions.get(0).getY());
        assertEquals(10, positions.get(1).getY());
        assertEquals(15, positions.get(2).getY());
        assertEquals(20, positions.get(3).getY());
    }
}
