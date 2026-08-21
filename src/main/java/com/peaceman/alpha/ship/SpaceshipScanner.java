package com.peaceman.alpha.ship;

import com.peaceman.alpha.ship.service.ShipScannerService;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Set;

/**
 * Adapter/Fassade für Schiff-Scans.
 * Delegiert an den isolierten ShipScannerService.
 */
public class SpaceshipScanner {

    public static Set<BlockPos> scan(Level level, BlockPos startPos) {
        return ShipScannerService.scan(level, startPos);
    }
}