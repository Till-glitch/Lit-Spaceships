package com.peaceman.alpha.block.entity;

import com.peaceman.alpha.registry.ModBlockEntities;
import com.peaceman.alpha.ship.SpaceshipEnergyManager;
import com.peaceman.alpha.ship.combat.LaserWeaponTier;
import com.peaceman.alpha.ship.domain.ShipState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

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
            } else {
                com.peaceman.alpha.ship.combat.LaserCombatService.tickContinuousWeapon(level, ship, pos, this);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("IsFiring", isFiring);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.isFiring = tag.getBoolean("IsFiring");
    }
}
