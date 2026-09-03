package com.peaceman.alpha.ship.relocation;

import com.peaceman.alpha.ship.relocation.graph.BlockDependencyGraph;
import com.peaceman.alpha.ship.relocation.graph.RelocationNode;
import com.peaceman.alpha.ship.relocation.graph.VirtualSupportTestView;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("VirtualSupportTestView & Datengetriebenes Support-Probing Unit Tests")
class VirtualSupportTestViewTest {

    @Test
    @DisplayName("VirtualSupportTestView maskiert Blöcke virtuell als AIR")
    void testVirtualMasking() {
        BlockPos pos1 = new BlockPos(0, 0, 0);
        BlockPos pos2 = new BlockPos(0, 1, 0);

        Map<BlockPos, RelocationNode> context = new HashMap<>();
        RelocationNode node1 = new RelocationNode(pos1, pos1, Blocks.STONE.defaultBlockState(), null);
        RelocationNode node2 = new RelocationNode(pos2, pos2, Blocks.TORCH.defaultBlockState(), null);
        context.put(pos1, node1);
        context.put(pos2, node2);

        VirtualSupportTestView unmasked = new VirtualSupportTestView(null, context, null);
        assertEquals(Blocks.STONE.defaultBlockState(), unmasked.getBlockState(pos1));
        assertEquals(Blocks.TORCH.defaultBlockState(), unmasked.getBlockState(pos2));

        VirtualSupportTestView masked = new VirtualSupportTestView(null, context, pos1, Blocks.AIR.defaultBlockState());
        assertEquals(Blocks.AIR.defaultBlockState(), masked.getBlockState(pos1), "pos1 muss als AIR maskiert sein");
        assertEquals(Blocks.TORCH.defaultBlockState(), masked.getBlockState(pos2), "pos2 darf nicht maskiert sein");
    }

    @Test
    @DisplayName("Virtual canSurvive Probing erkennt Boden- und Wand-Trägerblöcke datengetrieben")
    void testVirtualCanSurviveProbing() {
        BlockDependencyGraph graph = new BlockDependencyGraph();

        // 1. Wand-Fackel an Wandblock
        BlockPos wallPos = new BlockPos(10, 5, 10);
        BlockPos wallTorchPos = new BlockPos(10, 5, 11); // FACING = SOUTH -> Träger liegt NORTH (pos.north() = wallPos)

        BlockState wallState = Blocks.STONE.defaultBlockState();
        BlockState wallTorchState = Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.SOUTH);

        RelocationNode wallNode = graph.addNode(wallPos, wallPos.offset(1, 0, 0), wallState, null);
        RelocationNode wallTorchNode = graph.addNode(wallTorchPos, wallTorchPos.offset(1, 0, 0), wallTorchState, null);

        // 2. Boden-Fackel auf Fundament
        BlockPos floorPos = new BlockPos(20, 1, 20);
        BlockPos floorTorchPos = new BlockPos(20, 2, 20);

        BlockState floorState = Blocks.STONE.defaultBlockState();
        BlockState floorTorchState = Blocks.TORCH.defaultBlockState();

        RelocationNode floorNode = graph.addNode(floorPos, floorPos.offset(0, 1, 0), floorState, null);
        RelocationNode floorTorchNode = graph.addNode(floorTorchPos, floorTorchPos.offset(0, 1, 0), floorTorchState, null);

        // Datengetriebene Kantenberechnung via VirtualSupportTestView (ohne instanceof Torch/WallTorch)
        graph.buildDependencies(null);

        assertTrue(wallTorchNode.getDependencies().contains(wallNode), "Wand-Fackel muss über Virtual Probing von der Wand abhängen");
        assertTrue(floorTorchNode.getDependencies().contains(floorNode), "Boden-Fackel muss über Virtual Probing vom Boden abhängen");

        List<List<RelocationNode>> batches = graph.resolveTopologicalBatches();

        int wallBatch = findBatch(batches, wallNode);
        int wallTorchBatch = findBatch(batches, wallTorchNode);
        int floorBatch = findBatch(batches, floorNode);
        int floorTorchBatch = findBatch(batches, floorTorchNode);

        assertTrue(wallTorchBatch > wallBatch, "Wand muss vor Wandfackel platziert werden");
        assertTrue(floorTorchBatch > floorBatch, "Boden muss vor Bodenfackel platziert werden");
    }

    private int findBatch(List<List<RelocationNode>> batches, RelocationNode node) {
        for (int i = 0; i < batches.size(); i++) {
            if (batches.get(i).contains(node)) return i;
        }
        return -1;
    }
}
