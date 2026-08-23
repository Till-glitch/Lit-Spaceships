package com.peaceman.alpha.helper;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;

import java.util.UUID;

/**
 * Zentrales Diagnose- & Logging-Tool für den gesamten Lebenszyklus der Geschütztürme,
 * Zielausrichtung (Freelook & Lock) sowie Netzwerk-Synchronisation.
 */
public class TurretDebugLogger {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String PREFIX = "[Alpha-Turret] ";

    public static void logMount(String playerName, BlockPos weaponPos, UUID shipId, boolean isClient) {
        LOGGER.info("{} [{}] Spieler '{}' bemannt Geschuetzturm bei {} (Schiff: {})",
                PREFIX, isClient ? "CLIENT" : "SERVER", playerName, weaponPos != null ? weaponPos.toShortString() : "null", shipId);
    }

    public static void logDismount(String playerName, BlockPos weaponPos, boolean isClient) {
        LOGGER.info("{} [{}] Spieler '{}' verlaesst Geschuetzturm bei {}",
                PREFIX, isClient ? "CLIENT" : "SERVER", playerName, weaponPos != null ? weaponPos.toShortString() : "null");
    }

    public static void logClientAimSent(BlockPos weaponPos, float yaw, float pitch) {
        LOGGER.info(String.format("%s [CLIENT-INPUT] Sende TurretAimSyncPayload -> Pos: %s, Yaw: %.1f°, Pitch: %.1f°",
                PREFIX, weaponPos != null ? weaponPos.toShortString() : "null", yaw, pitch));
    }

    public static void logClientLockTriggered(BlockPos weaponPos, String triggerSource) {
        LOGGER.info("{} [CLIENT-INPUT] Lock-Toggle getriggert ({}) fuer Pos: {}. Sende TurretLockTogglePayload.",
                PREFIX, triggerSource, weaponPos != null ? weaponPos.toShortString() : "null");
    }

    public static void logServerAimReceived(String playerName, BlockPos weaponPos, float yaw, float pitch, boolean isLocked) {
        LOGGER.info(String.format("%s [SERVER-SYNC] TurretAimSyncPayload von '%s' empfangen -> Pos: %s, Yaw: %.1f°, Pitch: %.1f°, isLocked: %b",
                PREFIX, playerName, weaponPos != null ? weaponPos.toShortString() : "null", yaw, pitch, isLocked));
    }

    public static void logServerLockToggled(String playerName, BlockPos weaponPos, boolean newLockState) {
        LOGGER.info("{} [SERVER-LOCK] TurretLockTogglePayload von '{}' -> Neuer Lock-Status: {} fuer Pos: {}",
                PREFIX, playerName, newLockState, weaponPos != null ? weaponPos.toShortString() : "null");
    }

    public static void logCombatAim(BlockPos weaponPos, float yaw, float pitch, double dirX, double dirY, double dirZ) {
        LOGGER.info(String.format("%s [COMBAT-AIM] Geschuetzfeuer/Strahl bei %s -> Yaw: %.1f°, Pitch: %.1f°, Vector: (%.2f, %.2f, %.2f)",
                PREFIX, weaponPos != null ? weaponPos.toShortString() : "null", yaw, pitch, dirX, dirY, dirZ));
    }
}
