package com.lit.spaceships.block.entity;

import com.lit.spaceships.network.LaserStateSyncPayload;
import com.lit.spaceships.registry.ModBlockEntities;
import com.lit.spaceships.ship.SpaceshipEnergyManager;
import com.lit.spaceships.ship.combat.LaserWeaponTier;
import com.lit.spaceships.ship.domain.ShipState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * BlockEntity für kontinuierliche Strahlwaffen (Heavy Beam).
 * Verwaltet den Dauerfeuer-Status isFiring und konsumiert pro Tick Reaktor-Energie.
 */
public class HeavyBeamBlockEntity extends AbstractLaserNodeBlockEntity {

    private boolean isFiring = false;

    public HeavyBeamBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEAVY_BEAM_BE.get(), pos, state);
    }

    @Override
    public LaserWeaponTier getTier() {
        return LaserWeaponTier.HEAVY_BEAM;
    }

    @Override
    public boolean isContinuous() {
        return true;
    }

    public boolean isFiring() {
        return isFiring;
    }

    public void setFiring(boolean firing) {
        if (this.isFiring != firing) {
            this.isFiring = firing;
            if (!firing) {
                clearDrillProgress(this.level);
            }
            setChanged();
            if (this.level != null && !this.level.isClientSide) {
                this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
            }
        }
    }

    @Override
    public void serverTick(Level level, BlockPos pos, BlockState state) {
        if (isFiring) {
            ShipState ship = getShip();
            if (ship == null || !SpaceshipEnergyManager.tryConsumeEnergyAmount(level, ship, getEnergyCost())) {
                setFiring(false);
                if (ship != null && level instanceof ServerLevel serverLevel) {
                    PacketDistributor.sendToPlayersTrackingChunk(serverLevel, new ChunkPos(pos),
                            new LaserStateSyncPayload(ship.getId(), pos, false, getTier()));
                }
            } else {
                com.lit.spaceships.ship.combat.LaserCombatService.tickContinuousWeapon(level, ship, pos, this);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // IsFiring wird absichtlich nicht gespeichert, damit Laser beim Server-Neustart standardmäßig aus sind.
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // isFiring bleibt standardmäßig false.
    }

    @Override
    public boolean handleFire(Level level, ShipState shooterShip, BlockPos weaponPos) {
        boolean newState = !isFiring();
        setFiring(newState);
        PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, new ChunkPos(weaponPos),
                new LaserStateSyncPayload(shooterShip.getId(), weaponPos, newState, getTier()));
        return newState;
    }
}
