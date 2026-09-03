package com.lit.spaceships.helper;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

import java.util.UUID;

/**
 * Zentrales Diagnose- & Logging-Tool für den gesamten Lebenszyklus der Schutzschilde und Voxel-Kollisionen.
 * Ermöglicht die lückenlose Echtzeit-Überwachung aller State-Transitions und Kollisionen in der Konsole.
 */
public class ShieldLifecycleLogger {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String PREFIX = "[Alpha-Engine] ";

    public static void logServerChunkSent(UUID shipId, BlockPos anchor, ChunkPos chunkPos, String playerName) {
        LOGGER.info("{} [SERVER-SYNC] Chunk {} an Spieler '{}' gesendet. Trigger Sync fuer Schiff '{}' bei {}",
                PREFIX, chunkPos, playerName, shipId, anchor != null ? anchor.toShortString() : "null");
    }

    public static void logClientPayloadReceived(String payloadType, UUID shipId, BlockPos anchor, boolean chunkLoaded) {
        LOGGER.info("{} [CLIENT-SYNC] {} empfangen fuer Schiff '{}' bei {}. Chunk geladen: {}",
                PREFIX, payloadType, shipId, anchor != null ? anchor.toShortString() : "null", chunkLoaded);
    }

    public static void logClientPendingSyncQueued(UUID shipId, ChunkPos chunkPos) {
        LOGGER.info("{} [CLIENT-SYNC] Chunk {} noch nicht geladen! Schild-Sync fuer Schiff '{}' in PENDING_SYNCS eingereiht.",
                PREFIX, chunkPos, shipId);
    }

    public static void logClientPendingSyncApplied(UUID shipId, ChunkPos chunkPos) {
        LOGGER.info("{} [CLIENT-SYNC] Chunk {} geladen (ChunkEvent.Load). Wende gepufferten Schild-Sync fuer Schiff '{}' an.",
                PREFIX, chunkPos, shipId);
    }

    public static void logClientVramDisposed(UUID shipId, String reason) {
        LOGGER.info("{} [CLIENT-VRAM] VRAM (VBO) fuer Schiff '{}' freigegeben. Grund: {}",
                PREFIX, shipId, reason);
    }

    public static void logMorphologyStarted(UUID shipId, long version, int blockCount) {
        LOGGER.info("{} [MORPHOLOGY] Schild-Berechnung (Gen #{}) fuer Schiff '{}' gestartet ({} Bloecke auf Virtual Thread).",
                PREFIX, version, shipId, blockCount);
    }

    public static void logMorphologyCompleted(UUID shipId, long version, int bubbleVoxelCount, boolean applied) {
        LOGGER.info("{} [MORPHOLOGY] Schild-Berechnung (Gen #{}) fuer Schiff '{}' beendet ({} Voxel). Angewendet: {}",
                PREFIX, version, shipId, bubbleVoxelCount, applied);
    }

    public static void logShieldToggled(UUID shipId, boolean newState) {
        LOGGER.info("{} [SHIELD-STATE] Schild-Status fuer Schiff '{}' umgeschaltet -> isShieldActive: {}. Broadcast StateSync.",
                PREFIX, shipId, newState);
    }

    public static void logClientReset(String reason) {
        LOGGER.info("{} [CLIENT-RESET] Alle Client-Schiffe und VRAM-Buffer bereinigt. Grund: {}",
                PREFIX, reason);
    }

    // --- KOLLISIONSSYSTEM LOGGING ---

    public static void logBroadPhaseOverlap(UUID movingShipId, UUID otherShipId, AABB intersectionBox) {
        LOGGER.info("{} [COLLISION-BROAD] Swept-AABB Schnitt detektiert: Schiff '{}' <-> Schiff '{}' | Schnittbox V_int: {}",
                PREFIX, movingShipId, otherShipId, intersectionBox);
    }

    public static void logNarrowPhaseResult(UUID shipA, UUID shipB, boolean isColliding, boolean isShieldA, boolean isShieldB, int voxelCount) {
        LOGGER.info("{} [COLLISION-NARROW] Voxel-DeepCheck: Schiff '{}' (Schild: {}) <-> Schiff '{}' (Schild: {}) | Kollision: {} ({} Voxel)",
                PREFIX, shipA, isShieldA, shipB, isShieldB, isColliding, voxelCount);
    }

    public static void logCollisionResolved(String collisionCase, UUID shipA, UUID shipB, int voxelCount, boolean stopped, String details) {
        LOGGER.info("{} [COLLISION-RESOLVE] Fall [{}] ausgefuehrt fuer Schiff '{}' <-> Schiff '{}' ({} Voxel) | Stopp: {} | Details: {}",
                PREFIX, collisionCase, shipA, shipB, voxelCount, stopped, details);
    }
}
