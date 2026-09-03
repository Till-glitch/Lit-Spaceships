package com.lit.spaceships.ship.domain;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
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
    private final Map<Byte, ShieldZone> shieldZones = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<Byte, SectorCoverage> sectorCoverages = new java.util.concurrent.ConcurrentHashMap<>();
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

    // Energie- & Leistungs-Priorität und Telemetrie
    private volatile PowerPriority powerPriority = PowerPriority.BALANCED;
    private volatile int lastGenerationRate = 0;
    private volatile int lastShieldDrain = 0;
    private volatile int lastWeaponDrain = 0;
    private volatile int lastEngineDrain = 0;
    private volatile int currentTickShieldDrain = 0;
    private volatile int currentTickWeaponDrain = 0;
    private volatile int currentTickEngineDrain = 0;

    // Caches für Chunk-Sends
    private volatile Map<BlockPos, Byte> cachedRelBubble = null;

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

    /**
     * Verschiebt alle positionsabhängigen Daten des Schiffs atomar um (dx, dy, dz).
     * Aktualisiert ControllerPos, Blocks, BoundingBoxen, Reaktoren, Schilde, Waffen und ShieldZones
     * unter vollständiger Beibehaltung der Zonenergien und Zustände.
     */
    public synchronized void translate(int dx, int dy, int dz) {
        if (dx == 0 && dy == 0 && dz == 0) return;

        if (this.controllerPos != null) {
            this.controllerPos = this.controllerPos.offset(dx, dy, dz);
        }

        if (this.blocks != null && !this.blocks.isEmpty()) {
            Set<BlockPos> newBlocks = new HashSet<>(this.blocks.size());
            for (BlockPos pos : this.blocks) {
                newBlocks.add(pos.offset(dx, dy, dz));
            }
            this.blocks = newBlocks;
        }

        if (this.hullBoundingBox != null) {
            this.hullBoundingBox = this.hullBoundingBox.move(dx, dy, dz);
        }
        if (this.shieldBoundingBox != null) {
            this.shieldBoundingBox = this.shieldBoundingBox.move(dx, dy, dz);
        }

        if (this.reactors != null && !this.reactors.isEmpty()) {
            List<BlockPos> newReactors = new ArrayList<>(this.reactors.size());
            for (BlockPos pos : this.reactors) {
                newReactors.add(pos.offset(dx, dy, dz));
            }
            this.reactors = newReactors;
        }

        if (this.shields != null && !this.shields.isEmpty()) {
            List<BlockPos> newShields = new ArrayList<>(this.shields.size());
            for (BlockPos pos : this.shields) {
                newShields.add(pos.offset(dx, dy, dz));
            }
            this.shields = newShields;
        }

        if (this.weapons != null && !this.weapons.isEmpty()) {
            List<BlockPos> newWeapons = new ArrayList<>(this.weapons.size());
            for (BlockPos pos : this.weapons) {
                newWeapons.add(pos.offset(dx, dy, dz));
            }
            this.weapons = newWeapons;
        }

        if (!this.shieldZones.isEmpty()) {
            Map<Byte, ShieldZone> newZones = new HashMap<>();
            for (Map.Entry<Byte, ShieldZone> entry : this.shieldZones.entrySet()) {
                ShieldZone z = entry.getValue();
                BlockPos newGenPos = z.generatorPos() != null ? z.generatorPos().offset(dx, dy, dz) : null;
                newZones.put(entry.getKey(), new ShieldZone(z.id(), newGenPos, z.currentEnergy(), z.maxEnergy(), z.cooldownUntil(), z.isEnabled()));
            }
            this.shieldZones.clear();
            this.shieldZones.putAll(newZones);
        }
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



    public Map<Byte, ShieldZone> getShieldZones() {
        return shieldZones;
    }

    public ShieldZone getShieldZone(byte id) {
        return shieldZones.get(id);
    }

    public void setShieldZone(ShieldZone zone) {
        if (zone != null) {
            this.shieldZones.put(zone.id(), zone);
        }
    }

    public void setShieldZones(Map<Byte, ShieldZone> zones) {
        this.shieldZones.clear();
        if (zones != null) {
            this.shieldZones.putAll(zones);
        }
    }

    public void updateShieldZoneEnergy(byte id, int newEnergy) {
        this.shieldZones.computeIfPresent(id, (k, zone) -> zone.withEnergy(newEnergy));
    }

    public void updateShieldZoneEnergyAndChargeRate(byte id, int newEnergy, int chargeRate) {
        this.shieldZones.computeIfPresent(id, (k, zone) -> zone.withEnergyAndChargeRate(newEnergy, chargeRate));
    }

    public void updateShieldZoneEnergyAndCooldown(byte id, int newEnergy, long cooldownUntil) {
        this.shieldZones.computeIfPresent(id, (k, zone) -> zone.withEnergyAndCooldown(newEnergy, cooldownUntil));
    }

    public Map<Byte, SectorCoverage> getSectorCoverages() {
        return Collections.unmodifiableMap(this.sectorCoverages);
    }

    public SectorCoverage getSectorCoverage(byte id) {
        return this.sectorCoverages.get(id);
    }

    public void setSectorCoverages(Map<Byte, SectorCoverage> coverages) {
        this.sectorCoverages.clear();
        if (coverages != null) {
            this.sectorCoverages.putAll(coverages);
        }
    }

    public void toggleShieldActive() {
        if (this.shields.isEmpty()) {
            this.isShieldActive = false;
        } else {
            this.isShieldActive = !this.isShieldActive;
        }
        com.lit.spaceships.helper.ShieldLifecycleLogger.logShieldToggled(this.id, this.isShieldActive);
    }

    public void toggleShieldZoneActive(byte id) {
        this.shieldZones.computeIfPresent(id, (k, zone) -> zone.withEnabled(!zone.isEnabled()));
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
        this.cachedRelBubble = null; // Invalidate cached bubble when shield updates
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

    public Map<BlockPos, Byte> getCachedRelBubble() {
        return cachedRelBubble;
    }

    public void setCachedRelBubble(Map<BlockPos, Byte> cachedRelBubble) {
        this.cachedRelBubble = cachedRelBubble;
    }

    public byte[] encodeZoneEnergies() {
        byte[] zoneEnergies = new byte[64];
        for (ShieldZone zone : this.shieldZones.values()) {
            int id = zone.id() & 0xFF;
            if (id >= 1 && id <= 64) {
                float percentage = Math.clamp((float) zone.currentEnergy() / Math.max(1.0f, (float) zone.maxEnergy()), 0.0f, 1.0f);
                zoneEnergies[id - 1] = (byte) (percentage * 255.0f);
            }
        }
        return zoneEnergies;
    }

    public PowerPriority getPowerPriority() {
        return powerPriority != null ? powerPriority : PowerPriority.BALANCED;
    }

    public void setPowerPriority(PowerPriority powerPriority) {
        this.powerPriority = powerPriority != null ? powerPriority : PowerPriority.BALANCED;
    }

    public int getLastGenerationRate() {
        return lastGenerationRate;
    }

    public void setLastGenerationRate(int lastGenerationRate) {
        this.lastGenerationRate = Math.max(0, lastGenerationRate);
    }

    public int getLastShieldDrain() {
        return lastShieldDrain;
    }

    public void setLastShieldDrain(int lastShieldDrain) {
        this.lastShieldDrain = Math.max(0, lastShieldDrain);
    }

    public int getLastWeaponDrain() {
        return lastWeaponDrain;
    }

    public void setLastWeaponDrain(int lastWeaponDrain) {
        this.lastWeaponDrain = Math.max(0, lastWeaponDrain);
    }

    public int getLastEngineDrain() {
        return lastEngineDrain;
    }

    public void setLastEngineDrain(int lastEngineDrain) {
        this.lastEngineDrain = Math.max(0, lastEngineDrain);
    }

    public void addShieldDrain(int fe) {
        if (fe > 0) this.currentTickShieldDrain += fe;
    }

    public void addWeaponDrain(int fe) {
        if (fe > 0) this.currentTickWeaponDrain += fe;
    }

    public void addEngineDrain(int fe) {
        if (fe > 0) this.currentTickEngineDrain += fe;
    }

    public void endTickTelemetry() {
        this.lastShieldDrain = this.currentTickShieldDrain;
        this.lastWeaponDrain = this.currentTickWeaponDrain;
        this.lastEngineDrain = this.currentTickEngineDrain;
        this.currentTickShieldDrain = 0;
        this.currentTickWeaponDrain = 0;
        this.currentTickEngineDrain = 0;
    }

    public int getLastConsumptionRate() {
        return lastShieldDrain + lastWeaponDrain + lastEngineDrain;
    }

    public int getNetEnergyThroughput() {
        return lastGenerationRate - getLastConsumptionRate();
    }
}
