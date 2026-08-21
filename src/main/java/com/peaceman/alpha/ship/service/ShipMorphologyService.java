package com.peaceman.alpha.ship.service;

import com.peaceman.alpha.helper.ShieldLifecycleLogger;
import com.peaceman.alpha.network.ShieldBubbleSyncPacket;
import com.peaceman.alpha.ship.ShieldMorphology;
import com.peaceman.alpha.ship.domain.ShipState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Kapselt die asynchronen mathematischen Berechnungen für Schild-Morphologien.
 * Nutzt Java 21 Virtual Threads, unmodifizierbare Snapshots und Generation-Tracking
 * zur Vermeidung von Concurrency-Kollaps und Out-of-Order-Race-Conditions (Blueprint 3 & Szenario 3).
 */
public class ShipMorphologyService {

    // Versionstracking pro Schiff gegen Out-of-Order Race Conditions bei schnellen Folge-Updates
    private static final Map<UUID, AtomicLong> CALCULATION_VERSIONS = new ConcurrentHashMap<>();

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
     * das Ergebnis nach Validierung thread-sicher auf dem Server-Main-Thread.
     */
    public static void calculateAndSyncShieldAsync(ShipState ship, ServerLevel serverLevel, int radius) {
        if (ship == null || serverLevel == null) return;

        final UUID targetId = ship.getId();
        final BlockPos targetAnchor = ship.getControllerPos();

        if (ship.getShields().isEmpty()) {
            PacketDistributor.sendToAllPlayers(new ShieldBubbleSyncPacket(targetId, targetAnchor, Collections.emptySet()));
            return;
        }

        // 1. Generation-Version erhöhen (Debouncing / Out-of-Order Guard)
        final long version = CALCULATION_VERSIONS.computeIfAbsent(targetId, k -> new AtomicLong(0)).incrementAndGet();

        // 2. Unveränderlichen Snapshot auf dem Main-Thread erstellen
        final Set<BlockPos> immutableStructureSnapshot = ship.getImmutableBlockSnapshot();
        ShieldLifecycleLogger.logMorphologyStarted(targetId, version, immutableStructureSnapshot.size());

        // 3. Auslagerung auf einen Java 21 Virtual Thread
        Thread.ofVirtual().name("Morphology-Calc-" + targetId.toString().substring(0, 8) + "-v" + version)
                .start(() -> {
                    Set<BlockPos> calculatedBubble = performVolumetricDilation(immutableStructureSnapshot, radius);

                    // 4. Rückführung auf den Main Server Thread
                    serverLevel.getServer().execute(() -> {
                        AtomicLong latestVersion = CALCULATION_VERSIONS.get(targetId);
                        boolean isLatest = (latestVersion != null && latestVersion.get() == version);
                        boolean shipExists = ServerShipManager.hasShip(targetId);

                        ShieldLifecycleLogger.logMorphologyCompleted(targetId, version, calculatedBubble.size(), isLatest && shipExists);

                        // Nur anwenden, wenn in der Zwischenzeit keine neuere Berechnung gestartet wurde
                        if (isLatest && shipExists) {
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
