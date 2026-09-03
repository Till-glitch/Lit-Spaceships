package com.lit.spaceships.ship.relocation;

import com.lit.spaceships.ship.relocation.graph.BlockDependencyGraph;
import com.lit.spaceships.ship.relocation.graph.RelocationNode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tarjan SCC & Zyklus-Erkennung Tests")
class CycleDetectionTest {

    @Test
    @DisplayName("Zyklische Abhängigkeit (A <-> B) wird in stark zusammenhängende Komponente (SCC) gebündelt")
    void testCycleDetection_TarjanSCC() {
        BlockDependencyGraph graph = new BlockDependencyGraph();

        BlockPos posA = new BlockPos(1, 1, 1);
        BlockPos posB = new BlockPos(1, 2, 1);

        BlockState stateA = Blocks.IRON_BLOCK.defaultBlockState();
        BlockState stateB = Blocks.COPPER_BLOCK.defaultBlockState();

        RelocationNode nodeA = graph.addNode(posA, posA.offset(0, 1, 0), stateA, null);
        RelocationNode nodeB = graph.addNode(posB, posB.offset(0, 1, 0), stateB, null);

        // Künstlicher Zyklus: A hängt von B ab und B hängt von A ab
        nodeA.addDependency(nodeB);
        nodeB.addDependency(nodeA);

        List<List<RelocationNode>> batches = graph.resolveTopologicalBatches();

        assertFalse(batches.isEmpty(), "Batches dürfen trotz Zyklus nicht leer sein");
        assertEquals(nodeA.getClusterId(), nodeB.getClusterId(), "Beide Knoten müssen denselben Cluster-Identifier tragen");
        assertTrue(nodeA.getClusterId() >= 0, "Cluster-ID muss zugewiesen sein");

        // Verifiziere, dass alle Knoten in den Batches enthalten sind
        int totalNodesInBatches = batches.stream().mapToInt(List::size).sum();
        assertEquals(2, totalNodesInBatches, "Alle Knoten müssen in den Batches platziert sein");
    }
}
