package com.peaceman.alpha.ship.relocation.graph;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Repräsentiert einen Knoten im BlockDependencyGraph.
 */
public class RelocationNode {
    private final BlockPos oldPos;
    private final BlockPos newPos;
    private final BlockState state;
    private final CompoundTag nbt;

    // Kanten: dependencies = Knoten, die vor diesem platziert werden müssen (Vorgänger)
    private final Set<RelocationNode> dependencies = new HashSet<>();
    // Kanten: dependents = Knoten, die von diesem abhängen (Nachfolger)
    private final Set<RelocationNode> dependents = new HashSet<>();

    private int clusterId = -1;

    public RelocationNode(BlockPos oldPos, BlockPos newPos, BlockState state, CompoundTag nbt) {
        this.oldPos = oldPos;
        this.newPos = newPos;
        this.state = state;
        this.nbt = nbt;
    }

    public void addDependency(RelocationNode dependency) {
        if (dependency != null && dependency != this) {
            this.dependencies.add(dependency);
            dependency.dependents.add(this);
        }
    }

    public BlockPos getOldPos() {
        return oldPos;
    }

    public BlockPos getNewPos() {
        return newPos;
    }

    public BlockState getState() {
        return state;
    }

    public CompoundTag getNbt() {
        return nbt;
    }

    public Set<RelocationNode> getDependencies() {
        return dependencies;
    }

    public Set<RelocationNode> getDependents() {
        return dependents;
    }

    public int getClusterId() {
        return clusterId;
    }

    public void setClusterId(int clusterId) {
        this.clusterId = clusterId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RelocationNode that = (RelocationNode) o;
        return Objects.equals(oldPos, that.oldPos);
    }

    @Override
    public int hashCode() {
        return oldPos != null ? oldPos.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "RelocationNode{" +
                "oldPos=" + oldPos +
                ", state=" + state +
                ", deps=" + dependencies.size() +
                '}';
    }
}
