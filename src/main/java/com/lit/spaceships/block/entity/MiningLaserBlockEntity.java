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
 * BlockEntity für blockmodifizierende Werkzeuge (Mining-Laser).
 * Zieht Reaktor-Energie ab und baut Terrain-Blöcke pro Tick ab.
 */
public class MiningLaserBlockEntity extends AbstractLaserNodeBlockEntity {

    private boolean isMining = false;

    public MiningLaserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MINING_LASER_BE.get(), pos, state);
    }

    @Override
    public LaserWeaponTier getTier() {
        return LaserWeaponTier.MINING_LASER;
    }

    @Override
    public boolean isContinuous() {
        return true;
    }

    public boolean isMining() {
        return isMining;
    }

    public void setMining(boolean mining) {
        if (this.isMining != mining) {
            this.isMining = mining;
            if (!mining) {
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
        if (isMining) {
            ShipState ship = getShip();
            if (ship == null || !SpaceshipEnergyManager.tryConsumeEnergyAmount(level, ship, getEnergyCost())) {
                setMining(false);
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
        // IsMining wird absichtlich nicht gespeichert, damit Laser beim Server-Neustart standardmäßig aus sind.
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // isMining bleibt standardmäßig false.
    }

    @Override
    public boolean handleFire(Level level, ShipState shooterShip, BlockPos weaponPos) {
        boolean newState = !isMining();
        setMining(newState);
        PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, new ChunkPos(weaponPos),
                new LaserStateSyncPayload(shooterShip.getId(), weaponPos, newState, getTier()));
        return newState;
    }
}
