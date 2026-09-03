package com.lit.spaceships.ship.relocation.graph;

import com.lit.spaceships.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;

import java.util.*;

/**
 * Konstruiert einen gerichteten Abhängigkeitsgraphen (DAG) für alle Schiffsblöcke
 * und berechnet die deterministische topologische Setz- und Lösch-Reihenfolge.
 */
public class BlockDependencyGraph {

    private final Map<BlockPos, RelocationNode> nodes = new LinkedHashMap<>();

    public BlockDependencyGraph() {}

    /**
     * Erstellt einen RelocationNode und fügt ihn zum Graphen hinzu.
     */
    public RelocationNode addNode(BlockPos oldPos, BlockPos newPos, BlockState state, CompoundTag nbt) {
        RelocationNode node = new RelocationNode(oldPos, newPos, state, nbt);
        nodes.put(oldPos, node);
        return node;
    }

    public Map<BlockPos, RelocationNode> getNodes() {
        return nodes;
    }

    public RelocationNode getNode(BlockPos pos) {
        return nodes.get(pos);
    }

    /**
     * Ermittelt datengetrieben alle Kanten (Abhängigkeiten) zwischen den Knoten:
     * A -> B bedeutet: Block A stützt Block B (A muss vor B platziert werden).
     */
    public void buildDependencies(Level level) {
        for (RelocationNode node : nodes.values()) {
            BlockPos pos = node.getOldPos();
            BlockState state = node.getState();

            if (state == null || state.isAir()) {
                continue;
            }

            // 1. Multiblock-Hälften (z. B. Türen: Upper hängt von Lower ab)
            if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
                DoubleBlockHalf half = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
                if (half == DoubleBlockHalf.UPPER) {
                    RelocationNode lowerNode = nodes.get(pos.below());
                    if (lowerNode != null) {
                        node.addDependency(lowerNode);
                    }
                }
            }

            // 2. Betten (Head hängt von Foot ab)
            if (state.hasProperty(BlockStateProperties.BED_PART)) {
                BedPart part = state.getValue(BlockStateProperties.BED_PART);
                if (part == BedPart.HEAD && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                    Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
                    // Das Fußteil befindet sich in Gegenrichtung zum Kopfteil
                    RelocationNode footNode = nodes.get(pos.relative(facing.getOpposite()));
                    if (footNode != null) {
                        node.addDependency(footNode);
                    }
                }
            }

            // 3. Face-Attached Blöcke (Hebel, Knöpfe mit FLOOR / CEILING / WALL)
            if (state.hasProperty(BlockStateProperties.ATTACH_FACE)) {
                AttachFace face = state.getValue(BlockStateProperties.ATTACH_FACE);
                BlockPos supportPos = switch (face) {
                    case FLOOR -> pos.below();
                    case CEILING -> pos.above();
                    case WALL -> state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                            ? pos.relative(state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite())
                            : pos.below();
                };
                RelocationNode supportNode = nodes.get(supportPos);
                if (supportNode != null) {
                    node.addDependency(supportNode);
                }
            }

            // 4. Piston-Heads & Piston-Bases (Piston-Head hängt strikt von Piston-Base ab)
            if (state.getBlock() instanceof net.minecraft.world.level.block.piston.PistonHeadBlock &&
                    state.hasProperty(BlockStateProperties.FACING)) {
                Direction facing = state.getValue(BlockStateProperties.FACING);
                BlockPos basePos = pos.relative(facing.getOpposite());
                RelocationNode baseNode = nodes.get(basePos);
                if (baseNode != null) {
                    node.addDependency(baseNode);
                }
            } else if (state.getBlock() instanceof net.minecraft.world.level.block.piston.PistonBaseBlock &&
                    state.hasProperty(BlockStateProperties.EXTENDED) &&
                    state.getValue(BlockStateProperties.EXTENDED) &&
                    state.hasProperty(BlockStateProperties.FACING)) {
                Direction facing = state.getValue(BlockStateProperties.FACING);
                BlockPos headPos = pos.relative(facing);
                RelocationNode headNode = nodes.get(headPos);
                if (headNode != null) {
                    headNode.addDependency(node);
                }
            }

            // 5. Universelles datengetriebenes Virtual canSurvive Probing (alle 6 Nachbarachsen)
            // Ersetzt alle hardcodierten instanceof-Listen für Fackeln, Schienen, Kabel, Schilder, Mod-Deko etc.
            VirtualSupportTestView fullView = new VirtualSupportTestView(level, nodes, null);
            boolean isSolidFullBlock = state.isSolidRender(fullView, pos) && state.isCollisionShapeFullBlock(fullView, pos);
            if (!isSolidFullBlock) {
                for (Direction dir : Direction.values()) {
                    BlockPos neighborPos = pos.relative(dir);
                    RelocationNode neighborNode = nodes.get(neighborPos);
                    if (neighborNode != null) {
                        try {
                            VirtualSupportTestView maskedView = new VirtualSupportTestView(level, nodes, neighborPos, Blocks.AIR.defaultBlockState());
                            boolean survivesNormally = state.canSurvive(fullView, pos);
                            boolean survivesWithoutNeighbor = state.canSurvive(maskedView, pos);
                            if (survivesNormally && !survivesWithoutNeighbor) {
                                node.addDependency(neighborNode);
                            }
                        } catch (Exception ignored) {
                            // Fallback für exotische Mod-Blöcke
                        }
                    }
                }
            }

            // 8. Standard Gravitations- & Fundament-Heuristik:
            // Wenn ein Block auf einem soliden Vollblock ruht, stützt der untere Block den oberen
            RelocationNode belowNode = nodes.get(pos.below());
            if (belowNode != null) {
                BlockState belowState = belowNode.getState();
                if (belowState != null && !belowState.isAir() &&
                        (belowState.isSolidRender(level != null ? level : net.minecraft.world.level.EmptyBlockGetter.INSTANCE, pos.below())
                                || belowState.isCollisionShapeFullBlock(level != null ? level : net.minecraft.world.level.EmptyBlockGetter.INSTANCE, pos.below()))) {
                    // Nicht-solide Blöcke (z. B. Stufen, Maschinen, Deko) hängen vom soliden Fundament ab
                    if (!state.isSolidRender(level != null ? level : net.minecraft.world.level.EmptyBlockGetter.INSTANCE, pos)) {
                        node.addDependency(belowNode);
                    }
                }
            }
        }
    }

    /**
     * Berechnet die topologisch sortierten Platzierungs-Batches mittels Kahn-Algorithmus
     * und Tarjan SCC für Zyklen.
     *
     * @return Eine geordnete Liste von Knoten-Batches. Jeder Batch kann sicher platziert werden,
     *         sobald die vorherigen Batches existieren. Innerhalb jedes Batches sind die Knoten
     *         nach aufsteigender Y-Koordinate sortiert.
     */
    public List<List<RelocationNode>> resolveTopologicalBatches() {
        if (nodes.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. Zyklen-Erkennung via Tarjan SCC
        List<Set<RelocationNode>> sccs = findStronglyConnectedComponents();
        int clusterCounter = 0;
        for (Set<RelocationNode> scc : sccs) {
            if (scc.size() > 1) {
                for (RelocationNode n : scc) {
                    n.setClusterId(clusterCounter);
                }
                clusterCounter++;
            }
        }

        // 2. Kahn's Algorithmus zur Generierung von Schichten (Batches)
        Map<RelocationNode, Integer> inDegree = new HashMap<>();
        for (RelocationNode node : nodes.values()) {
            inDegree.put(node, node.getDependencies().size());
        }

        List<List<RelocationNode>> batches = new ArrayList<>();
        Set<RelocationNode> visited = new HashSet<>();

        while (visited.size() < nodes.size()) {
            List<RelocationNode> currentBatch = new ArrayList<>();

            for (RelocationNode node : nodes.values()) {
                if (!visited.contains(node) && inDegree.getOrDefault(node, 0) == 0) {
                    currentBatch.add(node);
                }
            }

            // Falls kein Knoten mit inDegree 0 gefunden wurde, liegt ein ungelöster Zyklus vor -> Notfall-Batch
            if (currentBatch.isEmpty()) {
                for (RelocationNode node : nodes.values()) {
                    if (!visited.contains(node)) {
                        currentBatch.add(node);
                        break;
                    }
                }
            }

            for (RelocationNode node : currentBatch) {
                visited.add(node);
                for (RelocationNode dependent : node.getDependents()) {
                    if (!visited.contains(dependent)) {
                        inDegree.put(dependent, inDegree.get(dependent) - 1);
                    }
                }
            }

            // Innerhalb des Batches aufsteigend nach neuer Y-Koordinate sortieren (Fundamente zuerst)
            currentBatch.sort(Comparator.comparingInt(n -> n.getNewPos().getY()));
            batches.add(currentBatch);
        }

        return batches;
    }

    /**
     * Findet alle stark zusammenhängenden Komponenten (SCCs) via Tarjan's Algorithmus.
     */
    private List<Set<RelocationNode>> findStronglyConnectedComponents() {
        List<Set<RelocationNode>> result = new ArrayList<>();
        Map<RelocationNode, Integer> indices = new HashMap<>();
        Map<RelocationNode, Integer> lowLinks = new HashMap<>();
        Deque<RelocationNode> stack = new ArrayDeque<>();
        Set<RelocationNode> onStack = new HashSet<>();
        int[] index = {0};

        for (RelocationNode node : nodes.values()) {
            if (!indices.containsKey(node)) {
                strongConnect(node, indices, lowLinks, stack, onStack, index, result);
            }
        }
        return result;
    }

    private void strongConnect(RelocationNode v, Map<RelocationNode, Integer> indices,
                               Map<RelocationNode, Integer> lowLinks, Deque<RelocationNode> stack,
                               Set<RelocationNode> onStack, int[] index, List<Set<RelocationNode>> result) {
        indices.put(v, index[0]);
        lowLinks.put(v, index[0]);
        index[0]++;
        stack.push(v);
        onStack.add(v);

        for (RelocationNode w : v.getDependents()) {
            if (!indices.containsKey(w)) {
                strongConnect(w, indices, lowLinks, stack, onStack, index, result);
                lowLinks.put(v, Math.min(lowLinks.get(v), lowLinks.get(w)));
            } else if (onStack.contains(w)) {
                lowLinks.put(v, Math.min(lowLinks.get(v), indices.get(w)));
            }
        }

        if (lowLinks.get(v).equals(indices.get(v))) {
            Set<RelocationNode> scc = new HashSet<>();
            RelocationNode w;
            do {
                w = stack.pop();
                onStack.remove(w);
                scc.add(w);
            } while (w != v);
            result.add(scc);
        }
    }
}
