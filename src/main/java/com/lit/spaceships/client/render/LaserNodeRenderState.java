package com.lit.spaceships.client.render;

import com.lit.spaceships.block.entity.AbstractLaserNodeBlockEntity;
import com.lit.spaceships.ship.combat.LaserWeaponTier;
import com.lit.spaceships.ship.combat.aim.AimTransformMath;
import net.minecraft.core.Direction;

/**
 * Entkoppelter Render-State für Laser-Node Geschütztürme.
 * Ermöglicht Thread-sichere Extraktion von Ausrichtung, Kinematik und
 * Waffen-Tier
 * gemäß der NeoForge 1.21 State-Extraction Architektur.
 */
public record LaserNodeRenderState(Direction facing, float yaw, float pitch, LaserWeaponTier tier) {

    public static LaserNodeRenderState extract(AbstractLaserNodeBlockEntity laserBE, float partialTick) {
        float renderYaw = AimTransformMath.interpolateAngle(laserBE.getPrevTargetYaw(), laserBE.getTargetYaw(),
                partialTick);
        float renderPitch = AimTransformMath.interpolateAngle(laserBE.getPrevTargetPitch(), laserBE.getTargetPitch(),
                partialTick);
        Direction facing = laserBE.getFacing();
        return new LaserNodeRenderState(facing, renderYaw, renderPitch, laserBE.getTier());
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public Direction getFacing() {
        return facing;
    }

    public LaserWeaponTier getTier() {
        return tier;
    }
}
