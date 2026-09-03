package com.lit.spaceships.block.entity;

import com.lit.spaceships.ship.combat.LaserWeaponTier;
import com.lit.spaceships.ship.combat.aim.AimAngles;
import com.lit.spaceships.ship.combat.aim.FreelookAimStrategy;
import com.lit.spaceships.ship.combat.aim.GimbalLimits;
import com.lit.spaceships.ship.combat.aim.IAimStrategy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Basisklasse für alle schiffsgebundenen Laserwaffen-Knotenpunkte.
 * Unterstützt dynamische Zielausrichtung (Yaw & Pitch) und modulare Aim-Strategien.
 */
public abstract class AbstractLaserNodeBlockEntity extends AbstractSpaceshipNodeBlockEntity {

    protected float targetYaw = 0.0f;
    protected float targetPitch = -90.0f;
    protected float prevTargetYaw = 0.0f;
    protected float prevTargetPitch = -90.0f;
    protected boolean isOccupied = false;
    protected boolean isAimLocked = false;
    protected GimbalLimits gimbalLimits = GimbalLimits.UNRESTRICTED;
    protected IAimStrategy aimStrategy = FreelookAimStrategy.INSTANCE;

    protected BlockPos currentDrillPos = null;
    protected float drillProgress = 0.0f;

    public AbstractLaserNodeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public abstract LaserWeaponTier getTier();

    public double getMaxRange() {
        return getTier().getMaxRange();
    }

    public int getEnergyCost() {
        return getTier().getEnergyCost();
    }

    public float getBaseDamage() {
        return getTier().getBaseDamage();
    }

    public abstract boolean isContinuous();

    public Direction getFacing() {
        BlockState state = getBlockState();
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return state.getValue(BlockStateProperties.FACING);
        }
        return Direction.NORTH;
    }

    public abstract void serverTick(Level level, BlockPos pos, BlockState state);

    public abstract boolean handleFire(Level level, com.lit.spaceships.ship.domain.ShipState shooterShip, BlockPos weaponPos);

    // --- Zielsystem & Ausrichtung ---

    public float getTargetYaw() {
        return targetYaw;
    }

    public void setTargetYaw(float targetYaw) {
        this.targetYaw = targetYaw;
    }

    public float getTargetPitch() {
        return targetPitch;
    }

    public void setTargetPitch(float targetPitch) {
        this.targetPitch = targetPitch;
    }

    public float getPrevTargetYaw() {
        return prevTargetYaw;
    }

    public float getPrevTargetPitch() {
        return prevTargetPitch;
    }

    public AimAngles getAimAngles() {
        return new AimAngles(this.targetYaw, this.targetPitch);
    }

    public void setAimAngles(AimAngles angles) {
        if (angles != null) {
            AimAngles clamped = this.gimbalLimits != null ? this.gimbalLimits.clamp(angles) : angles;
            this.prevTargetYaw = this.targetYaw;
            this.prevTargetPitch = this.targetPitch;
            this.targetYaw = clamped.yaw();
            this.targetPitch = clamped.pitch();
            setChanged();
        }
    }

    /**
     * Rotiert die Zielausrichtung des Geschützturms starr mit dem Schiff mit (Relativ-Modus).
     */
    public void rotateTurret(net.minecraft.world.level.block.Rotation rotation) {
        if (rotation == null || rotation == net.minecraft.world.level.block.Rotation.NONE) return;
        this.prevTargetYaw = com.lit.spaceships.ship.service.ShipRotationMath.rotateYaw(this.prevTargetYaw, rotation);
        this.targetYaw = com.lit.spaceships.ship.service.ShipRotationMath.rotateYaw(this.targetYaw, rotation);
        setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        }
    }

    public boolean isOccupied() {
        return isOccupied;
    }

    public void setOccupied(boolean occupied) {
        this.isOccupied = occupied;
        setChanged();
    }

    public boolean isAimLocked() {
        return isAimLocked;
    }

    public void setAimLocked(boolean aimLocked) {
        this.isAimLocked = aimLocked;
        setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        }
    }

    public GimbalLimits getGimbalLimits() {
        return gimbalLimits;
    }

    public void setGimbalLimits(GimbalLimits gimbalLimits) {
        this.gimbalLimits = gimbalLimits != null ? gimbalLimits : GimbalLimits.DEFAULT_TURRET;
    }

    public IAimStrategy getAimStrategy() {
        return aimStrategy;
    }

    public void setAimStrategy(IAimStrategy aimStrategy) {
        this.aimStrategy = aimStrategy != null ? aimStrategy : FreelookAimStrategy.INSTANCE;
    }

    // --- Mining- / Drill-Fortschritt ---

    public BlockPos getCurrentDrillPos() {
        return currentDrillPos;
    }

    public void setCurrentDrillPos(BlockPos pos) {
        this.currentDrillPos = pos;
    }

    public float getDrillProgress() {
        return drillProgress;
    }

    public void addDrillProgress(float amount) {
        this.drillProgress += amount;
    }

    public void resetDrillProgress() {
        this.drillProgress = 0.0f;
    }

    public void clearDrillProgress(Level level) {
        if (this.currentDrillPos != null && level != null && !level.isClientSide()) {
            level.destroyBlockProgress(this.getBlockPos().hashCode(), this.currentDrillPos, -1);
            this.currentDrillPos = null;
            this.drillProgress = 0.0f;
        }
    }

    // --- Persistenz & NBT ---

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("TargetYaw", this.targetYaw);
        tag.putFloat("TargetPitch", this.targetPitch);
        tag.putBoolean("IsAimLocked", this.isAimLocked);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("TargetYaw")) {
            this.targetYaw = tag.getFloat("TargetYaw");
            this.prevTargetYaw = this.targetYaw;
        }
        if (tag.contains("TargetPitch")) {
            this.targetPitch = tag.getFloat("TargetPitch");
            this.prevTargetPitch = this.targetPitch;
        }
        if (tag.contains("IsAimLocked")) {
            this.isAimLocked = tag.getBoolean("IsAimLocked");
        }
        this.isOccupied = false;
    }
}
