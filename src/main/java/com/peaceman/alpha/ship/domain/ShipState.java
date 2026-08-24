package com.peaceman.alpha.ship.domain;

import com.peaceman.alpha.block.entity.SpaceshipReactorBlockEntity;
import com.peaceman.alpha.block.entity.SpaceshipShieldBlockEntity;
import com.peaceman.alpha.ship.SpaceshipShieldHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

import java.util.*;

/**
 * Reines Server-Domain-DTO für den logischen Zustand eines Raumschiffs.
 * Enthält geometrische Caches (AABBs und VoxelGridCaches mit BitSets) für
 * deterministische, hochperformante Kollisionsprüfungen (Schritt 1).
 */
public class ShipState {

    private final UUID id;
    private BlockPos controllerPos;
    private Set<BlockPos> blocks;
    private final Map<String, BlockPos> homes;
    private List<BlockPos> reactors = new ArrayList<>();
    private List<BlockPos> shields = new ArrayList<>();
    private List<BlockPos> weapons = new ArrayList<>();
    private boolean isShieldActive = true;
    private ResourceKey<Level> dimension = Level.OVERWORLD;
    private volatile boolean isJumping = false;
    private volatile org.joml.Quaternionf rotation = new org.joml.Quaternionf();

    // Cooldown-Timer (absolute Weltzeit via level.getGameTime())
    public static final long SHIELD_COOLDOWN_TICKS = 200L;    // 10 Sekunden
    public static final long MOVEMENT_COOLDOWN_TICKS = 20L;   // 1 Sekunde
    private long shieldCooldownUntil = 0L;
    private long movementCooldownUntil = 0L;

    // Geometrische Caches für Kollisions-Broad- und Narrow-Phase (Schritt 1)
    private volatile AABB hullBoundingBox;
    private volatile AABB shieldBoundingBox;
    private volatile VoxelGridCache hullVoxelCache = VoxelGridCache.EMPTY;
    private volatile VoxelGridCache shieldVoxelCache = VoxelGridCache.EMPTY;

    // Konstruktor für ein neues Schiff
    public ShipState(BlockPos controllerPos, Set<BlockPos> blocks) {
        this(controllerPos, blocks, Level.OVERWORLD);
    }

    public ShipState(BlockPos controllerPos, Set<BlockPos> blocks, ResourceKey<Level> dimension) {
        this.id = UUID.randomUUID();
        this.controllerPos = controllerPos;
        this.blocks = blocks != null ? blocks : new HashSet<>();
        this.homes = new HashMap<>();
        this.isShieldActive = true;
        this.dimension = dimension != null ? dimension : Level.OVERWORLD;
        recalculateHullBounds();
    }

    // Konstruktor für geladene Schiffe aus dem Savegame
    public ShipState(UUID id, BlockPos controllerPos, Set<BlockPos> blocks, Map<String, BlockPos> homes, List<BlockPos> reactors, List<BlockPos> shields, boolean isShieldActive) {
        this(id, controllerPos, blocks, homes, reactors, shields, isShieldActive, Level.OVERWORLD);
    }

    public ShipState(UUID id, BlockPos controllerPos, Set<BlockPos> blocks, Map<String, BlockPos> homes, List<BlockPos> reactors, List<BlockPos> shields, boolean isShieldActive, ResourceKey<Level> dimension) {
        this.id = id;
        this.controllerPos = controllerPos;
        this.blocks = blocks != null ? blocks : new HashSet<>();
        this.homes = homes != null ? homes : new HashMap<>();
        if (reactors != null) this.reactors.addAll(reactors);
        if (shields != null) this.shields.addAll(shields);
        this.isShieldActive = isShieldActive;
        this.dimension = dimension != null ? dimension : Level.OVERWORLD;
        recalculateHullBounds();
    }

    public ResourceKey<Level> getDimension() {
        return dimension;
    }

    public void setDimension(ResourceKey<Level> dimension) {
        this.dimension = dimension != null ? dimension : Level.OVERWORLD;
    }

    public boolean isJumping() {
        return isJumping;
    }

    public void setJumping(boolean jumping) {
        this.isJumping = jumping;
    }

    public UUID getId() {
        return id;
    }

    public BlockPos getControllerPos() {
        return controllerPos;
    }

    public void setControllerPos(BlockPos newPos) {
        if (this.controllerPos != null && newPos != null && !this.controllerPos.equals(newPos)) {
            int dx = newPos.getX() - this.controllerPos.getX();
            int dy = newPos.getY() - this.controllerPos.getY();
            int dz = newPos.getZ() - this.controllerPos.getZ();
            if (this.hullBoundingBox != null) {
                this.hullBoundingBox = this.hullBoundingBox.move(dx, dy, dz);
            }
            if (this.shieldBoundingBox != null) {
                this.shieldBoundingBox = this.shieldBoundingBox.move(dx, dy, dz);
            }
        }
        this.controllerPos = newPos;
    }

    public Set<BlockPos> getBlocks() {
        return blocks;
    }

    public Set<BlockPos> getImmutableBlockSnapshot() {
        return Set.copyOf(this.blocks);
    }

    public void setBlocksRaw(Set<BlockPos> blocks) {
        this.blocks = blocks != null ? blocks : new HashSet<>();
        recalculateHullBounds();
    }

    public List<BlockPos> getReactors() {
        return reactors;
    }

    public void setReactors(List<BlockPos> reactors) {
        this.reactors = reactors != null ? reactors : new ArrayList<>();
    }

    public List<BlockPos> getShields() {
        return shields;
    }

    public void setShields(List<BlockPos> shields) {
        this.shields = shields != null ? shields : new ArrayList<>();
    }

    public Map<String, BlockPos> getHomes() {
        return homes;
    }

    public void addHome(String name, BlockPos pos) {
        this.homes.put(name, pos);
    }

    public boolean isShieldActive() {
        return isShieldActive;
    }

    public void setShieldActive(boolean shieldActive) {
        this.isShieldActive = shieldActive;
    }



    public void toggleShieldActive() {
        if (this.shields.isEmpty()) {
            this.isShieldActive = false;
        } else {
            this.isShieldActive = !this.isShieldActive;
        }
        com.peaceman.alpha.helper.ShieldLifecycleLogger.logShieldToggled(this.id, this.isShieldActive);
    }

    // --- Cooldown-Methoden ---

    public long getShieldCooldownUntil() {
        return shieldCooldownUntil;
    }

    public void setShieldCooldownUntil(long shieldCooldownUntil) {
        this.shieldCooldownUntil = shieldCooldownUntil;
    }

    public boolean isShieldOnCooldown(long currentGameTime) {
        return currentGameTime < shieldCooldownUntil;
    }

    public long getShieldCooldownRemaining(long currentGameTime) {
        return Math.max(0L, shieldCooldownUntil - currentGameTime);
    }

    public long getMovementCooldownUntil() {
        return movementCooldownUntil;
    }

    public void setMovementCooldownUntil(long movementCooldownUntil) {
        this.movementCooldownUntil = movementCooldownUntil;
    }

    public boolean isMovementOnCooldown(long currentGameTime) {
        return currentGameTime < movementCooldownUntil;
    }

    public long getMovementCooldownRemaining(long currentGameTime) {
        return Math.max(0L, movementCooldownUntil - currentGameTime);
    }



    public List<BlockPos> getWeapons() {
        return weapons;
    }

    public void setWeapons(List<BlockPos> weapons) {
        this.weapons = weapons != null ? weapons : new ArrayList<>();
    }

    /**
     * Berechnet die AABB der Hülle sowie den linearen hullVoxelCache neu.
     */
    public synchronized void recalculateHullBounds() {
        if (this.blocks == null || this.blocks.isEmpty()) {
            this.hullBoundingBox = this.controllerPos != null
                    ? new AABB(this.controllerPos)
                    : new AABB(0, 0, 0, 1, 1, 1);
            this.hullVoxelCache = VoxelGridCache.EMPTY;
            return;
        }

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        for (BlockPos pos : this.blocks) {
            if (pos.getX() < minX) minX = pos.getX();
            if (pos.getY() < minY) minY = pos.getY();
            if (pos.getZ() < minZ) minZ = pos.getZ();
            if (pos.getX() > maxX) maxX = pos.getX();
            if (pos.getY() > maxY) maxY = pos.getY();
            if (pos.getZ() > maxZ) maxZ = pos.getZ();
        }

        this.hullBoundingBox = new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);

        if (this.controllerPos != null) {
            this.hullVoxelCache = VoxelGridCache.buildFromAbsolute(this.blocks, this.controllerPos);
        }
    }

    /**
     * Aktualisiert den Schild-Voxel-Cache und die zugehörige BoundingBox.
     */
    public synchronized void updateShieldCache(VoxelGridCache cache, Set<BlockPos> absoluteShieldPositions) {
        this.shieldVoxelCache = cache != null ? cache : VoxelGridCache.EMPTY;
        if (absoluteShieldPositions != null && !absoluteShieldPositions.isEmpty()) {
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
            for (BlockPos pos : absoluteShieldPositions) {
                if (pos.getX() < minX) minX = pos.getX();
                if (pos.getY() < minY) minY = pos.getY();
                if (pos.getZ() < minZ) minZ = pos.getZ();
                if (pos.getX() > maxX) maxX = pos.getX();
                if (pos.getY() > maxY) maxY = pos.getY();
                if (pos.getZ() > maxZ) maxZ = pos.getZ();
            }
            this.shieldBoundingBox = new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
        } else {
            this.shieldBoundingBox = null;
        }
    }

    public AABB getHullBoundingBox() {
        if (this.hullBoundingBox == null) {
            recalculateHullBounds();
        }
        return this.hullBoundingBox;
    }

    public AABB getShieldBoundingBox() {
        return this.shieldBoundingBox;
    }

    public AABB getTotalBoundingBox() {
        if (isShieldActive() && this.shieldBoundingBox != null) {
            return this.shieldBoundingBox;
        }
        return getHullBoundingBox();
    }

    public VoxelGridCache getHullVoxelCache() {
        return this.hullVoxelCache;
    }

    public void setHullVoxelCache(VoxelGridCache hullVoxelCache) {
        this.hullVoxelCache = hullVoxelCache != null ? hullVoxelCache : VoxelGridCache.EMPTY;
    }

    public VoxelGridCache getShieldVoxelCache() {
        return this.shieldVoxelCache;
    }

    public void setShieldVoxelCache(VoxelGridCache shieldVoxelCache) {
        this.shieldVoxelCache = shieldVoxelCache != null ? shieldVoxelCache : VoxelGridCache.EMPTY;
    }

    public org.joml.Quaternionf getRotation() {
        return this.rotation != null ? this.rotation : new org.joml.Quaternionf();
    }

    public void setRotation(org.joml.Quaternionf rotation) {
        this.rotation = rotation != null ? rotation : new org.joml.Quaternionf();
    }

}
