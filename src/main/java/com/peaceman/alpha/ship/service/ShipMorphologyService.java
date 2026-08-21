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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Kapselt die asynchronen mathematischen Berechnungen für Schild-Morphologien.
 * Nutzt Java 21 Virtual Threads, um den Minecraft Main-Thread (TPS) nicht zu blockieren.
 */
public class ShipMorphologyService {

    private static final ExecutorService VIRTUAL_THREAD_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * Führt die volumetrische Dilatation asynchron auf einem Virtual Thread aus.
     */
    public static CompletableFuture<Set<BlockPos>> calculateShieldBubbleAsync(Set<BlockPos> shipBlocks, int radius) {
        if (shipBlocks == null || shipBlocks.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptySet());
        }

        // Snapshot erstellen zur Thread-Sicherheit
        final Set<BlockPos> snapshot = Set.copyOf(shipBlocks);

        return CompletableFuture.supplyAsync(() -> ShieldMorphology.calculateShieldBubble(snapshot, radius), VIRTUAL_THREAD_EXECUTOR);
    }

    /**
     * Berechnet die Schildblase asynchron und synchronisiert das Ergebnis thread-sicher auf dem Server-Main-Thread.
     */
    public static void calculateAndSyncShieldAsync(ShipState ship, ServerLevel serverLevel, int radius) {
        if (ship.getShields().isEmpty()) {
            PacketDistributor.sendToAllPlayers(new ShieldBubbleSyncPacket(ship.getId(), ship.getControllerPos(), Collections.emptySet()));
            return;
        }

        final BlockPos controllerPos = ship.getControllerPos();
        calculateShieldBubbleAsync(ship.getBlocks(), radius).thenAccept(calculatedBubble -> {
            // Rückkehr zum Minecraft Server Main-Thread
            serverLevel.getServer().execute(() -> {
                Set<BlockPos> relativeBlocks = new HashSet<>(calculatedBubble.size());
                for (BlockPos absPos : calculatedBubble) {
                    relativeBlocks.add(absPos.subtract(controllerPos));
                }

                PacketDistributor.sendToAllPlayers(new ShieldBubbleSyncPacket(ship.getId(), controllerPos, relativeBlocks));
            });
        });
    }
}
