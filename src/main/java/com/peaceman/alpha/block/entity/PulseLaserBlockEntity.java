package com.peaceman.alpha.block.entity;

import com.peaceman.alpha.registry.ModBlockEntities;
import com.peaceman.alpha.ship.combat.LaserWeaponTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * BlockEntity für diskrete Impulslaser (Pulse-Laser).
 * Verwaltet interne Abklingzeiten nach jedem Schuss.
 */
public class PulseLaserBlockEntity extends AbstractLaserNodeBlockEntity {

    private int cooldownTicks = 0;

    public PulseLaserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PULSE_LASER_BE.get(), pos, state);
    }

    @Override
    public LaserWeaponTier getTier() {
        return LaserWeaponTier.PULSE_LASER;
    }

    @Override
    public boolean isContinuous() {
        return false;
    }

    public boolean canFire() {
        return cooldownTicks <= 0;
    }

    public void triggerCooldown() {
        this.cooldownTicks = getTier().getCooldownTicks();
        setChanged();
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }

    @Override
    public void serverTick(Level level, BlockPos pos, BlockState state) {
        if (cooldownTicks > 0) {
            cooldownTicks--;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("CooldownTicks", cooldownTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.cooldownTicks = tag.getInt("CooldownTicks");
    }
}
