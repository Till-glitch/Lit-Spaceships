package com.lit.spaceships.ship.relocation;

import com.lit.spaceships.ship.relocation.graph.BlockDependencyGraph;
import com.lit.spaceships.ship.relocation.graph.RelocationNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BlockDependencyGraph Unit Tests")
class BlockDependencyGraphTest {

    @Test
    @DisplayName("Topologischer DAG: Fundament vor Multiblock-Wurzel und Wurzel vor Aufsatz")
    void testTopologicalOrder_FoundationLowerUpper() {
        BlockDependencyGraph graph = new BlockDependencyGraph();

        BlockPos posFloor = new BlockPos(0, 1, 0);
        BlockPos posLowerDoor = new BlockPos(0, 2, 0);
        BlockPos posUpperDoor = new BlockPos(0, 3, 0);

        BlockState floorState = Blocks.STONE.defaultBlockState();
        BlockState lowerDoorState = Blocks.OAK_DOOR.defaultBlockState()
                .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
        BlockState upperDoorState = Blocks.OAK_DOOR.defaultBlockState()
                .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);

        RelocationNode floorNode = graph.addNode(posFloor, posFloor.offset(1, 0, 0), floorState, null);
        RelocationNode lowerNode = graph.addNode(posLowerDoor, posLowerDoor.offset(1, 0, 0), lowerDoorState, null);
        RelocationNode upperNode = graph.addNode(posUpperDoor, posUpperDoor.offset(1, 0, 0), upperDoorState, null);

        graph.buildDependencies(null);

        // Prüfe Kanten
        assertTrue(upperNode.getDependencies().contains(lowerNode), "Upper Door muss von Lower Door abhängen");
        assertTrue(lowerNode.getDependents().contains(upperNode), "Lower Door muss Upper Door als Nachfolger haben");

        List<List<RelocationNode>> batches = graph.resolveTopologicalBatches();
        assertFalse(batches.isEmpty(), "Batches dürfen nicht leer sein");

        // Finde Batch-Indizes
        int floorBatch = findBatchIndex(batches, floorNode);
        int lowerBatch = findBatchIndex(batches, lowerNode);
        int upperBatch = findBatchIndex(batches, upperNode);

        assertTrue(lowerBatch >= floorBatch, "Lower Door darf nicht vor dem Boden platziert werden");
        assertTrue(upperBatch > lowerBatch, "Upper Door muss strikt nach Lower Door platziert werden");
    }

    @Test
    @DisplayName("Topologischer DAG: Wand vor Wandfackel")
    void testTopologicalOrder_WallBeforeWallTorch() {
        BlockDependencyGraph graph = new BlockDependencyGraph();

        BlockPos posWall = new BlockPos(2, 2, 2);
        // Fackel an der Südwand -> FACING zeigt nach SOUTH, Trägerblock ist NORTH (z - 1) -> posWall
        BlockPos posTorch = new BlockPos(2, 2, 3);

        BlockState wallState = Blocks.STONE.defaultBlockState();
        BlockState torchState = Blocks.WALL_TORCH.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);

        RelocationNode wallNode = graph.addNode(posWall, posWall.offset(0, 1, 0), wallState, null);
        RelocationNode torchNode = graph.addNode(posTorch, posTorch.offset(0, 1, 0), torchState, null);

        graph.buildDependencies(null);

        assertTrue(torchNode.getDependencies().contains(wallNode), "Wandfackel muss von Wand abhängen");

        List<List<RelocationNode>> batches = graph.resolveTopologicalBatches();
        int wallBatch = findBatchIndex(batches, wallNode);
        int torchBatch = findBatchIndex(batches, torchNode);

        assertTrue(torchBatch > wallBatch, "Wand muss vor Wandfackel platziert werden");
    }

    @Test
    @DisplayName("Topologischer DAG: Bett-Fußteil vor Kopfteil")
    void testTopologicalOrder_BedFootBeforeHead() {
        BlockDependencyGraph graph = new BlockDependencyGraph();

        // FACING = SOUTH -> Kopfteil zeigt nach SOUTH, Fußteil liegt NORTH davon (pos.north() = pos.relative(SOUTH.getOpposite()))
        BlockPos posFoot = new BlockPos(5, 1, 4);
        BlockPos posHead = new BlockPos(5, 1, 5);

        BlockState footState = Blocks.RED_BED.defaultBlockState()
                .setValue(BlockStateProperties.BED_PART, BedPart.FOOT)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
        BlockState headState = Blocks.RED_BED.defaultBlockState()
                .setValue(BlockStateProperties.BED_PART, BedPart.HEAD)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);

        RelocationNode footNode = graph.addNode(posFoot, posFoot.offset(0, 0, 1), footState, null);
        RelocationNode headNode = graph.addNode(posHead, posHead.offset(0, 0, 1), headState, null);

        graph.buildDependencies(null);

        assertTrue(headNode.getDependencies().contains(footNode), "Bett-Kopfteil muss vom Fußteil abhängen");

        List<List<RelocationNode>> batches = graph.resolveTopologicalBatches();
        int footBatch = findBatchIndex(batches, footNode);
        int headBatch = findBatchIndex(batches, headNode);

        assertTrue(headBatch > footBatch, "Bett-Fußteil muss vor Kopfteil platziert werden");
    }

    @Test
    @DisplayName("Topologischer DAG: Piston-Basis vor Piston-Kopf (vertikal & horizontal)")
    void testTopologicalOrder_PistonBaseBeforePistonHead() {
        BlockDependencyGraph graph = new BlockDependencyGraph();

        // 1. Vertikaler Piston (nach oben ausgefahren)
        BlockPos posBaseVert = new BlockPos(1, 1, 1);
        BlockPos posHeadVert = new BlockPos(1, 2, 1);

        BlockState baseVertState = Blocks.PISTON.defaultBlockState()
                .setValue(BlockStateProperties.EXTENDED, true)
                .setValue(BlockStateProperties.FACING, Direction.UP);
        BlockState headVertState = Blocks.PISTON_HEAD.defaultBlockState()
                .setValue(BlockStateProperties.FACING, Direction.UP);

        RelocationNode baseVertNode = graph.addNode(posBaseVert, posBaseVert.offset(1, 0, 0), baseVertState, null);
        RelocationNode headVertNode = graph.addNode(posHeadVert, posHeadVert.offset(1, 0, 0), headVertState, null);

        // 2. Horizontaler Sticky Piston (nach EAST ausgefahren)
        BlockPos posBaseHoriz = new BlockPos(3, 1, 1);
        BlockPos posHeadHoriz = new BlockPos(4, 1, 1);

        BlockState baseHorizState = Blocks.STICKY_PISTON.defaultBlockState()
                .setValue(BlockStateProperties.EXTENDED, true)
                .setValue(BlockStateProperties.FACING, Direction.EAST);
        BlockState headHorizState = Blocks.PISTON_HEAD.defaultBlockState()
                .setValue(BlockStateProperties.FACING, Direction.EAST);

        RelocationNode baseHorizNode = graph.addNode(posBaseHoriz, posBaseHoriz.offset(0, 1, 0), baseHorizState, null);
        RelocationNode headHorizNode = graph.addNode(posHeadHoriz, posHeadHoriz.offset(0, 1, 0), headHorizState, null);

        graph.buildDependencies(null);

        // Kanten prüfen
        assertTrue(headVertNode.getDependencies().contains(baseVertNode), "Vertikaler Piston-Kopf muss von der Basis abhängen");
        assertTrue(headHorizNode.getDependencies().contains(baseHorizNode), "Horizontaler Piston-Kopf muss von der Basis abhängen");

        List<List<RelocationNode>> batches = graph.resolveTopologicalBatches();

        int baseVertBatch = findBatchIndex(batches, baseVertNode);
        int headVertBatch = findBatchIndex(batches, headVertNode);
        int baseHorizBatch = findBatchIndex(batches, baseHorizNode);
        int headHorizBatch = findBatchIndex(batches, headHorizNode);

        assertTrue(headVertBatch > baseVertBatch, "Vertikale Piston-Basis muss vor dem Kopf platziert werden");
        assertTrue(headHorizBatch > baseHorizBatch, "Horizontale Piston-Basis muss vor dem Kopf platziert werden");
    }

    private int findBatchIndex(List<List<RelocationNode>> batches, RelocationNode node) {
        for (int i = 0; i < batches.size(); i++) {
            if (batches.get(i).contains(node)) {
                return i;
            }
        }
        return -1;
    }
}
