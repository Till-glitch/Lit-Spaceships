package com.lit.spaceships.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashSet;
import java.util.Set;

public class ShipHighlightRenderer {

    public static boolean isHighlightActive = false;
    public static Set<BlockPos> shipBlocks = new HashSet<>();

    public static boolean isHighlightActive() {
        return isHighlightActive;
    }

    // --- Ein- und Ausschalten ---
    public static void toggleHighlight(net.minecraft.world.level.Level level, BlockPos startPos) {
        if (isHighlightActive) {
            // Ausschalten
            isHighlightActive = false;
            shipBlocks.clear();
        } else {
            // Einschalten
            shipBlocks = com.lit.spaceships.ship.service.ShipScannerService.scan(level, startPos);
            isHighlightActive = true;
        }
    }

    public static void toggleHighlight(Set<BlockPos> blocks) {
        if (isHighlightActive) {
            isHighlightActive = false;
            shipBlocks.clear();
        } else {
            shipBlocks = new HashSet<>(blocks);
            isHighlightActive = true;
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (isHighlightActive && !shipBlocks.isEmpty()) {
            var level = Minecraft.getInstance().level;

            if (level != null) {
                // Ein bisschen schneller (alle 5 Ticks), damit die Hülle gut sichtbar ist
                if (level.getGameTime() % 10 == 0) {

                    for (BlockPos pos : shipBlocks) {
                        // Prüfe alle 6 Seiten des Blocks
                        for (Direction dir : Direction.values()) {

                            // Wenn der Block auf DIESER Seite NICHT zum Schiff gehört...
                            if (!shipBlocks.contains(pos.relative(dir))) {

                                double x = pos.getX() + 0.5 + (dir.getStepX() * 0.55);
                                double y = pos.getY() + 0.5 + (dir.getStepY() * 0.55);
                                double z = pos.getZ() + 0.5 + (dir.getStepZ() * 0.55);

                                if (dir.getAxis() != Direction.Axis.X) x += (level.random.nextDouble() - 0.5);
                                if (dir.getAxis() != Direction.Axis.Y) y += (level.random.nextDouble() - 0.5);
                                if (dir.getAxis() != Direction.Axis.Z) z += (level.random.nextDouble() - 0.5);

                                level.addParticle(ParticleTypes.END_ROD, x, y, z, 0, 0, 0);
                            }
                        }
                    }
                }
            }
        }
    }
}