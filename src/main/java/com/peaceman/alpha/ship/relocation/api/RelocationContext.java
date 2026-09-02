package com.peaceman.alpha.ship.relocation.api;

import com.peaceman.alpha.ship.domain.ShipState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Rotation;

import java.util.Set;

/**
 * Kontextinformationen für Block-Relokations-Handler.
 */
public record RelocationContext(
        ServerLevel level,
        ShipState ship,
        int dx, int dy, int dz,
        Rotation rotation,
        Set<BlockPos> oldBlocks,
        Set<BlockPos> newBlocks,
        Player player
) {
    public boolean isRotation() {
        return rotation != null && rotation != Rotation.NONE;
    }
}
