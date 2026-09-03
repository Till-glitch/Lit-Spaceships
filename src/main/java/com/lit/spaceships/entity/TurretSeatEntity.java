package com.lit.spaceships.entity;

import com.lit.spaceships.block.entity.AbstractLaserNodeBlockEntity;
import com.lit.spaceships.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Leichtgewichtige, unsichtbare Sitz-Entität zur exklusiven Belegung von Geschütztürmen.
 * Nutzt das native Minecraft-Passenger-System zur fehlerfreien Multiplayer-Synchronisation.
 */
public class TurretSeatEntity extends Entity {

    private static final net.minecraft.network.syncher.EntityDataAccessor<BlockPos> DATA_WEAPON_POS =
            SynchedEntityData.defineId(TurretSeatEntity.class, net.minecraft.network.syncher.EntityDataSerializers.BLOCK_POS);
    private static final net.minecraft.network.syncher.EntityDataAccessor<java.util.Optional<UUID>> DATA_SHIP_ID =
            SynchedEntityData.defineId(TurretSeatEntity.class, net.minecraft.network.syncher.EntityDataSerializers.OPTIONAL_UUID);

    public TurretSeatEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public TurretSeatEntity(Level level, BlockPos weaponPos, UUID shipId) {
        this(ModEntities.TURRET_SEAT.get(), level);
        setShipId(shipId);
        setWeaponPos(weaponPos);
        this.setPos(weaponPos.getX() + 0.5, weaponPos.getY() + 0.1, weaponPos.getZ() + 0.5);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_WEAPON_POS, BlockPos.ZERO);
        builder.define(DATA_SHIP_ID, java.util.Optional.empty());
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide()) {
            return;
        }

        // 1. Prüfe, ob Passagier vorhanden und lebendig ist
        if (getPassengers().isEmpty()) {
            releaseSeatAndDiscard();
            return;
        }

        Entity rider = getFirstPassenger();
        if (rider == null || !rider.isAlive()) {
            releaseSeatAndDiscard();
            return;
        }

        // 2. Prüfe, ob der zugehörige Geschützturm-Block noch existiert
        BlockPos pos = getWeaponPos();
        if (pos != null) {
            if (!(level().getBlockEntity(pos) instanceof AbstractLaserNodeBlockEntity laserBE)) {
                ejectPassengers();
                releaseSeatAndDiscard();
            } else if (!laserBE.isOccupied()) {
                laserBE.setOccupied(true);
            }
        }
    }

    @Override
    public Vec3 getPassengerRidingPosition(Entity entity) {
        return new Vec3(this.getX(), this.getY() + 0.25, this.getZ());
    }

    @Override
    public void onPassengerTurned(Entity passenger) {
        // Überschreibt die standardmäßige Vanilla-105°-Sperre (clampRotation) für volle 360°-Rundumsicht
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty();
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        if (passenger instanceof Player player) {
            com.lit.spaceships.helper.TurretDebugLogger.logMount(player.getName().getString(), getWeaponPos(), getShipId(), level().isClientSide());
        }
    }

    @Override
    protected void removePassenger(Entity passenger) {
        if (passenger instanceof Player player) {
            com.lit.spaceships.helper.TurretDebugLogger.logDismount(player.getName().getString(), getWeaponPos(), level().isClientSide());
        }
        super.removePassenger(passenger);
        if (!level().isClientSide()) {
            releaseSeatAndDiscard();
        }
    }

    private void releaseSeatAndDiscard() {
        BlockPos pos = getWeaponPos();
        if (!level().isClientSide() && pos != null) {
            if (level().getBlockEntity(pos) instanceof AbstractLaserNodeBlockEntity laserBE) {
                laserBE.setOccupied(false);
            }
        }
        this.discard();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("WeaponPos")) {
            setWeaponPos(NbtUtils.readBlockPos(tag, "WeaponPos").orElse(null));
        }
        if (tag.hasUUID("ShipId")) {
            setShipId(tag.getUUID("ShipId"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        BlockPos pos = getWeaponPos();
        if (pos != null) {
            tag.put("WeaponPos", NbtUtils.writeBlockPos(pos));
        }
        UUID id = getShipId();
        if (id != null) {
            tag.putUUID("ShipId", id);
        }
    }

    public BlockPos getWeaponPos() {
        BlockPos pos = this.entityData.get(DATA_WEAPON_POS);
        return (pos == null || BlockPos.ZERO.equals(pos)) ? null : pos;
    }

    public void setWeaponPos(BlockPos pos) {
        this.entityData.set(DATA_WEAPON_POS, pos != null ? pos : BlockPos.ZERO);
    }

    public UUID getShipId() {
        java.util.Optional<UUID> optional = this.entityData.get(DATA_SHIP_ID);
        if (optional != null && optional.isPresent()) {
            return optional.get();
        }
        // Fallback: If we have weaponPos and block entity on client
        BlockPos weaponPos = getWeaponPos();
        if (weaponPos != null && level().getBlockEntity(weaponPos) instanceof com.lit.spaceships.block.ISpaceshipNode node) {
            return node.getShipId();
        }
        return null;
    }

    public void setShipId(UUID shipId) {
        this.entityData.set(DATA_SHIP_ID, java.util.Optional.ofNullable(shipId));
    }
}
