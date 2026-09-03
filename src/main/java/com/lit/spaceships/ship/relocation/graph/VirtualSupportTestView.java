package com.lit.spaceships.ship.relocation.graph;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Leichtgewichtiger LevelReader-Wrapper für datengetriebene canSurvive-Proben.
 * Erlaubt das virtuelle Maskieren einzelner Nachbarblöcke als AIR oder benutzerdefinierte Zustände,
 * um festzustellen, ob ein Block (auch aus Dritt-Mods) von einem Nachbarn abhängt.
 */
@SuppressWarnings("deprecation")
public class VirtualSupportTestView implements LevelReader {

    private final LevelReader delegate;
    private final Map<BlockPos, RelocationNode> contextNodes;
    private final BlockPos maskedPos;
    private final BlockState maskedState;

    public VirtualSupportTestView(LevelReader delegate, Map<BlockPos, RelocationNode> contextNodes, BlockPos maskedPos, BlockState maskedState) {
        this.delegate = delegate;
        this.contextNodes = contextNodes;
        this.maskedPos = maskedPos;
        this.maskedState = maskedState != null ? maskedState : Blocks.AIR.defaultBlockState();
    }

    public VirtualSupportTestView(LevelReader delegate, Map<BlockPos, RelocationNode> contextNodes, BlockPos maskedPos) {
        this(delegate, contextNodes, maskedPos, Blocks.AIR.defaultBlockState());
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        if (maskedPos != null && pos.equals(maskedPos)) {
            return maskedState;
        }
        if (contextNodes != null) {
            RelocationNode node = contextNodes.get(pos);
            if (node != null && node.getState() != null) {
                return node.getState();
            }
        }
        if (delegate != null) {
            return delegate.getBlockState(pos);
        }
        return Blocks.AIR.defaultBlockState();
    }

    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        if (maskedPos != null && pos.equals(maskedPos)) {
            return null;
        }
        if (delegate != null) {
            return delegate.getBlockEntity(pos);
        }
        return null;
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        if (maskedPos != null && pos.equals(maskedPos)) {
            return Fluids.EMPTY.defaultFluidState();
        }
        if (delegate != null) {
            return delegate.getFluidState(pos);
        }
        return Fluids.EMPTY.defaultFluidState();
    }

    @Override
    public int getHeight() {
        return delegate != null ? delegate.getHeight() : 384;
    }

    @Override
    public int getMinBuildHeight() {
        return delegate != null ? delegate.getMinBuildHeight() : -64;
    }

    // --- Delegierte LevelReader Methoden ---

    @Override
    public ChunkAccess getChunk(int x, int z, ChunkStatus status, boolean load) {
        return delegate != null ? delegate.getChunk(x, z, status, load) : null;
    }

    @Override
    public boolean hasChunk(int x, int z) {
        return delegate != null ? delegate.hasChunk(x, z) : true;
    }

    @Override
    public int getHeight(Heightmap.Types type, int x, int z) {
        return delegate != null ? delegate.getHeight(type, x, z) : 0;
    }

    @Override
    public int getSkyDarken() {
        return delegate != null ? delegate.getSkyDarken() : 0;
    }

    @Override
    public BiomeManager getBiomeManager() {
        return delegate != null ? delegate.getBiomeManager() : null;
    }

    @Override
    public Holder<Biome> getUncachedNoiseBiome(int x, int y, int z) {
        return delegate != null ? delegate.getUncachedNoiseBiome(x, y, z) : null;
    }

    @Override
    public boolean isClientSide() {
        return delegate != null ? delegate.isClientSide() : false;
    }

    @Override
    public int getSeaLevel() {
        return delegate != null ? delegate.getSeaLevel() : 63;
    }

    @Override
    public DimensionType dimensionType() {
        return delegate != null ? delegate.dimensionType() : null;
    }

    @Override
    public RegistryAccess registryAccess() {
        return delegate != null ? delegate.registryAccess() : RegistryAccess.EMPTY;
    }

    @Override
    public FeatureFlagSet enabledFeatures() {
        return delegate != null ? delegate.enabledFeatures() : FeatureFlagSet.of();
    }

    @Override
    public float getShade(Direction direction, boolean shade) {
        return delegate != null ? delegate.getShade(direction, shade) : 1.0f;
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return delegate != null ? delegate.getLightEngine() : null;
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver colorResolver) {
        return delegate != null ? delegate.getBlockTint(pos, colorResolver) : 0;
    }

    @Override
    public WorldBorder getWorldBorder() {
        return delegate != null ? delegate.getWorldBorder() : new WorldBorder();
    }

    @Override
    public BlockGetter getChunkForCollisions(int x, int z) {
        return delegate != null ? delegate.getChunkForCollisions(x, z) : EmptyBlockGetter.INSTANCE;
    }

    @Override
    public List<VoxelShape> getEntityCollisions(net.minecraft.world.entity.Entity entity, AABB aabb) {
        return delegate != null ? delegate.getEntityCollisions(entity, aabb) : Collections.emptyList();
    }
}
