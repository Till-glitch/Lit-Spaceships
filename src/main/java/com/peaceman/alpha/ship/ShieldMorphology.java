package com.peaceman.alpha.ship;

import com.peaceman.alpha.network.ShieldBubbleSyncPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.Set;

public class ShieldMorphology {

    /**
     * Schritt 1: Findet nur die äußersten Blöcke (die Hülle) des Raumschiffs.
     */
    private static Set<BlockPos> getSurfaceBlocks(Set<BlockPos> shipBlocks) {
        Set<BlockPos> surface = new HashSet<>();

        for (BlockPos pos : shipBlocks) {
            // Ein Block gehört zur Hülle, wenn mindestens ein Nachbar NICHT zum Schiff gehört
            for (Direction dir : Direction.values()) {
                if (!shipBlocks.contains(pos.relative(dir))) {
                    surface.add(pos);
                    break; // Ein freier Nachbar reicht, wir können zum nächsten Block!
                }
            }
        }
        return surface;
    }

    /**
     * Schritt 2: Die eigentliche morphologische Dilatation.
     *
     * @param shipBlocks Alle Blöcke des Schiffes
     * @param radius     Wie viele Blöcke dick soll die Schildblase sein?
     * @return Ein Set aus ALLEN Blöcken, die vom Schild abgedeckt werden (inklusive Schiff).
     */
    public static Set<BlockPos> calculateShieldBubble(Set<BlockPos> shipBlocks, int radius) {
        Set<BlockPos> surface = getSurfaceBlocks(shipBlocks);
        Set<BlockPos> shieldBubble = new HashSet<>();

        // Vorberechnetes Quadrat des Radius (viel schneller als Math.sqrt!)
        int rSqr = radius * radius;

        // Wir "zeichnen" eine mathematische Kugel um jeden Hüllen-Block
        for (BlockPos center : surface) {
            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {

                        // Euklidische Distanz: Ist dieser Punkt innerhalb unserer Kugel?
                        if (x * x + y * y + z * z <= rSqr) {
                            shieldBubble.add(center.offset(x, y, z));
                        }
                    }
                }
            }
        }

        // Wir garantieren, dass das Schiffsinnere auch zur geschützten Zone gehört
        shieldBubble.addAll(shipBlocks);

        return shieldBubble;
    }

    /**
     * Prüft effizient, ob sich eine Position innerhalb der Schildblase des Schiffs befindet.
     */
    public static boolean isBlockProtected(Set<BlockPos> shipBlocks, BlockPos target, int radius) {
        if (shipBlocks == null || shipBlocks.isEmpty() || target == null) return false;
        if (shipBlocks.contains(target)) return true;

        int rSqr = radius * radius;
        for (BlockPos block : shipBlocks) {
            if (Math.abs(block.getX() - target.getX()) <= radius &&
                Math.abs(block.getY() - target.getY()) <= radius &&
                Math.abs(block.getZ() - target.getZ()) <= radius) {
                if (block.distSqr(target) <= rSqr) {
                    return true;
                }
            }
        }
        return false;
    }
}