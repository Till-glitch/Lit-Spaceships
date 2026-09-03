package com.lit.spaceships.ship.service;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit-5-Tests für die Erkennung und Sortierung zerbrechlicher / abhängiger Blöcke
 * (Redstone-Wire, Fackeln, Hebel, Repeater etc.) bei Schiffsbewegungen.
 */
public class ShipMovementFragileSortingTest {

    @BeforeAll
    static void initMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    @DisplayName("isFragileBlock erkennt Redstone Wire, Fackeln und Repeater korrekt als zerbrechlich")
    void testIsFragileBlock_DetectsFragileBlocks() {
        BlockState redstoneState = Blocks.REDSTONE_WIRE.defaultBlockState();
        BlockState torchState = Blocks.TORCH.defaultBlockState();
        BlockState repeaterState = Blocks.REPEATER.defaultBlockState();
        BlockState leverState = Blocks.LEVER.defaultBlockState();

        assertTrue(ShipMovementService.isFragileBlock(redstoneState, null, BlockPos.ZERO),
                "Redstone Wire muss als fragil eingestuft werden");
        assertTrue(ShipMovementService.isFragileBlock(torchState, null, BlockPos.ZERO),
                "Fackel muss als fragil eingestuft werden");
        assertTrue(ShipMovementService.isFragileBlock(repeaterState, null, BlockPos.ZERO),
                "Repeater muss als fragil eingestuft werden");
        assertTrue(ShipMovementService.isFragileBlock(leverState, null, BlockPos.ZERO),
                "Hebel muss als fragil eingestuft werden");
    }

    @Test
    @DisplayName("isFragileBlock stuft massive Vollblöcke und Luft nicht als zerbrechlich ein")
    void testIsFragileBlock_SolidBlocksAreNotFragile() {
        BlockState ironState = Blocks.IRON_BLOCK.defaultBlockState();
        BlockState stoneState = Blocks.STONE.defaultBlockState();
        BlockState airState = Blocks.AIR.defaultBlockState();

        assertFalse(ShipMovementService.isFragileBlock(ironState, null, BlockPos.ZERO),
                "Eisenblock darf nicht als fragil eingestuft werden");
        assertFalse(ShipMovementService.isFragileBlock(stoneState, null, BlockPos.ZERO),
                "Stein darf nicht als fragil eingestuft werden");
        assertFalse(ShipMovementService.isFragileBlock(airState, null, BlockPos.ZERO),
                "Luft darf nicht als fragil eingestuft werden");
        assertFalse(ShipMovementService.isFragileBlock(null, null, BlockPos.ZERO),
                "Null-State darf nicht als fragil eingestuft werden");
    }

    @Test
    @DisplayName("Entfernungs-Sortierung sortiert absteigend nach Y (oberste Blöcke zuerst)")
    void testRemovalSorting_DescendingY() {
        List<BlockPos> positions = new ArrayList<>(List.of(
                new BlockPos(0, 10, 0),
                new BlockPos(0, 15, 0),
                new BlockPos(0, 12, 0),
                new BlockPos(0, 8, 0)
        ));

        positions.sort((a, b) -> Integer.compare(b.getY(), a.getY()));

        assertEquals(15, positions.get(0).getY());
        assertEquals(12, positions.get(1).getY());
        assertEquals(10, positions.get(2).getY());
        assertEquals(8, positions.get(3).getY());
    }

    @Test
    @DisplayName("Platzierungs-Sortierung sortiert aufsteigend nach Y (unterste Trägerblöcke zuerst)")
    void testPlacementSorting_AscendingY() {
        List<BlockPos> positions = new ArrayList<>(List.of(
                new BlockPos(0, 10, 0),
                new BlockPos(0, 15, 0),
                new BlockPos(0, 12, 0),
                new BlockPos(0, 8, 0)
        ));

        positions.sort(Comparator.comparingInt(BlockPos::getY));

        assertEquals(8, positions.get(0).getY());
        assertEquals(10, positions.get(1).getY());
        assertEquals(12, positions.get(2).getY());
        assertEquals(15, positions.get(3).getY());
    }
}
