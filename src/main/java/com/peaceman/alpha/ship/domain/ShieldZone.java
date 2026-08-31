package com.peaceman.alpha.ship.domain;

import net.minecraft.core.BlockPos;

/**
 * Unveränderliches Data Transfer Object (Record) für eine lokalisierte Schildzone.
 * Verwaltet ID, Generatorposition, aktuelle und maximale Energie sowie Cooldown-Ticks.
 */
public record ShieldZone(
        byte id,
        BlockPos generatorPos,
        int currentEnergy,
        int maxEnergy,
        long cooldownUntil,
        boolean isEnabled
) {
    public ShieldZone(byte id, BlockPos generatorPos, int currentEnergy, int maxEnergy, long cooldownUntil) {
        this(id, generatorPos, currentEnergy, maxEnergy, cooldownUntil, true);
    }

    /**
     * Prüft, ob die Schildzone kollabiert oder im Cooldown ist.
     *
     * @param currentTick Der aktuelle Spiel-Tick (z.B. level.getGameTime())
     * @return true, wenn keine Energie mehr vorhanden ist, die Cooldown-Zeit noch nicht abgelaufen ist oder die Zone deaktiviert ist.
     */
    public boolean isCollapsed(long currentTick) {
        return !isEnabled || currentEnergy <= 0 || currentTick < cooldownUntil;
    }

    /**
     * Erstellt eine Kopie dieser ShieldZone mit neuem Energiewert.
     */
    public ShieldZone withEnergy(int newEnergy) {
        return new ShieldZone(id, generatorPos, Math.max(0, Math.min(maxEnergy, newEnergy)), maxEnergy, cooldownUntil, isEnabled);
    }

    /**
     * Erstellt eine Kopie dieser ShieldZone mit neuem Energiewert und Cooldown.
     */
    public ShieldZone withEnergyAndCooldown(int newEnergy, long newCooldownUntil) {
        return new ShieldZone(id, generatorPos, Math.max(0, Math.min(maxEnergy, newEnergy)), maxEnergy, newCooldownUntil, isEnabled);
    }

    /**
     * Erstellt eine Kopie dieser ShieldZone mit neuem Aktivierungsstatus.
     */
    public ShieldZone withEnabled(boolean newEnabled) {
        return new ShieldZone(id, generatorPos, currentEnergy, maxEnergy, cooldownUntil, newEnabled);
    }
}
