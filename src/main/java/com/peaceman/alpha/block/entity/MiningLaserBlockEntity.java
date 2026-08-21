package com.peaceman.alpha.block.entity;

import com.peaceman.alpha.network.LaserStateSyncPayload;
import com.peaceman.alpha.registry.ModBlockEntities;
import com.peaceman.alpha.ship.SpaceshipEnergyManager;
import com.peaceman.alpha.ship.combat.LaserWeaponTier;
import com.peaceman.alpha.ship.domain.ShipState;
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
                com.peaceman.alpha.ship.combat.LaserCombatService.tickContinuousWeapon(level, ship, pos, this);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("IsMining", isMining);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.isMining = tag.getBoolean("IsMining");
    }
}
