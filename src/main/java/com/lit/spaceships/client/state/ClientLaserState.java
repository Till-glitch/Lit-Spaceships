package com.lit.spaceships.client.state;

import com.lit.spaceships.ship.combat.LaserWeaponTier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * View-Model-Zustand für aktive Laserstrahlen auf dem logischen Client.
 * Verwaltet zeitgesteuerte Pulse-Laser-Animationen und kontinuierliche Dauerstrahlen.
 */
public class ClientLaserState {

    public record ActivePulseLaser(
            UUID shooterShipId,
            Vec3 startPos,
            Vec3 endPos,
            LaserWeaponTier tier,
            long createdAtMs,
            long durationMs
    ) {
        public float getProgress(long currentMs) {
            long elapsed = currentMs - createdAtMs;
            if (elapsed >= durationMs) return 1.0f;
            return Math.max(0.0f, (float) elapsed / (float) durationMs);
        }

        public boolean isExpired(long currentMs) {
            return (currentMs - createdAtMs) > durationMs;
        }
    }

    public record ActiveContinuousBeam(
            UUID shooterShipId,
            BlockPos relativeWeaponPos,
            LaserWeaponTier tier,
            long lastUpdatedMs
    ) {}

    // Thread-sichere Sammlungen für den Render-Loop
    private static final CopyOnWriteArrayList<ActivePulseLaser> ACTIVE_PULSES = new CopyOnWriteArrayList<>();
    private static final Map<String, ActiveContinuousBeam> ACTIVE_CONTINUOUS_BEAMS = new ConcurrentHashMap<>();

    public static void addPulse(UUID shooterShipId, Vec3 startPos, Vec3 endPos, LaserWeaponTier tier) {
        long now = System.currentTimeMillis();
        ACTIVE_PULSES.add(new ActivePulseLaser(shooterShipId, startPos, endPos, tier, now, 250L));
    }

    public static void setContinuousBeam(UUID shooterShipId, BlockPos weaponPos, boolean isFiring, LaserWeaponTier tier) {
        ClientShipState ship = ClientShipManager.getShip(shooterShipId);
        BlockPos anchor = ship != null && ship.getAnchorPos() != null ? ship.getAnchorPos() : BlockPos.ZERO;
        BlockPos relativePos = weaponPos.subtract(anchor);
        String key = shooterShipId + "_" + relativePos.asLong();
        if (isFiring) {
            ACTIVE_CONTINUOUS_BEAMS.put(key, new ActiveContinuousBeam(shooterShipId, relativePos, tier, System.currentTimeMillis()));
        } else {
            ACTIVE_CONTINUOUS_BEAMS.remove(key);
        }
    }

    public static CopyOnWriteArrayList<ActivePulseLaser> getActivePulses() {
        return ACTIVE_PULSES;
    }

    public static Map<String, ActiveContinuousBeam> getActiveContinuousBeams() {
        return ACTIVE_CONTINUOUS_BEAMS;
    }

    public static void cleanExpired(long currentMs) {
        ACTIVE_PULSES.removeIf(pulse -> pulse.isExpired(currentMs));
    }

    public static void removeBeamsForShip(UUID shipId) {
        if (shipId == null) return;
        ACTIVE_PULSES.removeIf(p -> shipId.equals(p.shooterShipId()));
        ACTIVE_CONTINUOUS_BEAMS.entrySet().removeIf(entry -> shipId.equals(entry.getValue().shooterShipId()));
    }

    public static void clearAll() {
        ACTIVE_PULSES.clear();
        ACTIVE_CONTINUOUS_BEAMS.clear();
    }
}
