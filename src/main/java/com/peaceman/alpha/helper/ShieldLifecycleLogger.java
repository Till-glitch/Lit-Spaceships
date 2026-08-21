package com.peaceman.alpha.helper;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import org.slf4j.Logger;

import java.util.UUID;

/**
 * Zentrales Diagnose- & Logging-Tool für den gesamten Lebenszyklus der Schutzschilde.
 * Ermöglicht die lückenlose Echtzeit-Überwachung aller State-Transitions in der Konsole.
 */
public class ShieldLifecycleLogger {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String PREFIX = "[Shield-Lifecycle] ";

    public static void logServerChunkSent(UUID shipId, BlockPos anchor, ChunkPos chunkPos, String playerName) {
        LOGGER.info("{} [SERVER] Chunk {} an Spieler '{}' gesendet. Trigger Sync fuer Schiff '{}' bei {}",
                PREFIX, chunkPos, playerName, shipId, anchor != null ? anchor.toShortString() : "null");
    }

    public static void logClientPayloadReceived(String payloadType, UUID shipId, BlockPos anchor, boolean chunkLoaded) {
        LOGGER.info("{} [CLIENT] {} empfangen fuer Schiff '{}' bei {}. Chunk bereits geladen: {}",
                PREFIX, payloadType, shipId, anchor != null ? anchor.toShortString() : "null", chunkLoaded);
    }

    public static void logClientPendingSyncQueued(UUID shipId, ChunkPos chunkPos) {
        LOGGER.info("{} [CLIENT] Chunk {} noch nicht geladen! Schild-Sync fuer Schiff '{}' in PENDING_SYNCS eingereiht.",
                PREFIX, chunkPos, shipId);
    }

    public static void logClientPendingSyncApplied(UUID shipId, ChunkPos chunkPos) {
        LOGGER.info("{} [CLIENT] Chunk {} geladen (ChunkEvent.Load). Wende gepufferten Schild-Sync fuer Schiff '{}' an.",
                PREFIX, chunkPos, shipId);
    }

    public static void logClientVramDisposed(UUID shipId, String reason) {
        LOGGER.info("{} [CLIENT] VRAM (VBO) fuer Schiff '{}' freigegeben. Grund: {}",
                PREFIX, shipId, reason);
    }

    public static void logMorphologyStarted(UUID shipId, long version, int blockCount) {
        LOGGER.info("{} [SERVER] Schild-Berechnung (Gen #{}) fuer Schiff '{}' gestartet ({} Bloecke auf Virtual Thread).",
                PREFIX, version, shipId, blockCount);
    }

    public static void logMorphologyCompleted(UUID shipId, long version, int bubbleVoxelCount, boolean applied) {
        LOGGER.info("{} [SERVER] Schild-Berechnung (Gen #{}) fuer Schiff '{}' beendet ({} Voxel). Angewendet: {}",
                PREFIX, version, shipId, bubbleVoxelCount, applied);
    }

    public static void logShieldToggled(UUID shipId, boolean newState) {
        LOGGER.info("{} [SERVER] Schild-Status fuer Schiff '{}' umgeschaltet -> isShieldActive: {}. Broadcast StateSync.",
                PREFIX, shipId, newState);
    }

    public static void logClientReset(String reason) {
        LOGGER.info("{} [CLIENT] Alle Client-Schiffe und VRAM-Buffer bereinigt. Grund: {}",
                PREFIX, reason);
    }
}
