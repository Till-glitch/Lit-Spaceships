package com.peaceman.alpha.block.entity;

import com.peaceman.alpha.ship.combat.LaserWeaponTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Basisklasse für alle schiffsgebundenen Laserwaffen-Knotenpunkte.
 */
public abstract class AbstractLaserNodeBlockEntity extends AbstractSpaceshipNodeBlockEntity {

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
}
