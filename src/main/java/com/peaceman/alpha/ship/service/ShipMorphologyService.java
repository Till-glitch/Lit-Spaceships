package com.peaceman.alpha.ship.service;

import com.peaceman.alpha.network.ShieldBubbleSyncPacket;
import com.peaceman.alpha.ship.ShieldMorphology;
import com.peaceman.alpha.ship.domain.ShipState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Kapselt die asynchronen mathematischen Berechnungen für Schild-Morphologien.
 * Nutzt Java 21 Virtual Threads und unmodifizierbare Snapshots gegen Concurrency-Kollaps (Blueprint 3).
 */
public class ShipMorphologyService {

    /**
     * Führt die volumetrische Dilatation asynchron auf einem Virtual Thread aus.
     */
    public static CompletableFuture<Set<BlockPos>> calculateShieldBubbleAsync(Set<BlockPos> shipBlocks, int radius) {
        if (shipBlocks == null || shipBlocks.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptySet());
        }

        final Set<BlockPos> snapshot = Set.copyOf(shipBlocks);
        return CompletableFuture.supplyAsync(() -> performVolumetricDilation(snapshot, radius));
    }

    public static Set<BlockPos> performVolumetricDilation(Set<BlockPos> immutableBlocks, int radius) {
        return ShieldMorphology.calculateShieldBubble(immutableBlocks, radius);
    }

    /**
     * Berechnet die Schildblase asynchron via Java 21 Virtual Thread und synchronisiert
     * das Ergebnis nach Validierung thread-sicher auf dem Server-Main-Thread (Blueprint 3).
     */
    public static void calculateAndSyncShieldAsync(ShipState ship, ServerLevel serverLevel, int radius) {
        if (ship == null || serverLevel == null) return;

        if (ship.getShields().isEmpty()) {
            PacketDistributor.sendToAllPlayers(new ShieldBubbleSyncPacket(ship.getId(), ship.getControllerPos(), Collections.emptySet()));
            return;
        }

        // 1. Unveränderlichen Snapshot auf dem Main-Thread erstellen
        final Set<BlockPos> immutableStructureSnapshot = ship.getImmutableBlockSnapshot();
        final UUID targetId = ship.getId();
        final BlockPos targetAnchor = ship.getControllerPos();

        // 2. Auslagerung auf einen Java 21 Virtual Thread
        Thread.ofVirtual().name("Morphology-Calc-" + targetId.toString().substring(0, 8))
                .start(() -> {
                    Set<BlockPos> calculatedBubble = performVolumetricDilation(immutableStructureSnapshot, radius);

                    // 3. Rückführung auf den Main Server Thread
                    serverLevel.getServer().execute(() -> {
                        ShipState currentShip = ServerShipManager.getShip(targetId);
                        if (currentShip != null) {
                            Set<BlockPos> relativeBlocks = new HashSet<>(calculatedBubble.size());
                            for (BlockPos absPos : calculatedBubble) {
                                relativeBlocks.add(absPos.subtract(targetAnchor));
                            }
                            PacketDistributor.sendToAllPlayers(new ShieldBubbleSyncPacket(targetId, targetAnchor, relativeBlocks));
                        }
                    });
                });
    }
}
