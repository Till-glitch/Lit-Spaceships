package com.peaceman.alpha.ship;

import com.peaceman.alpha.block.entity.SpaceshipReactorBlockEntity;
import com.peaceman.alpha.ship.domain.ShipState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class SpaceshipEnergyManager {

    // 1. Berechnet die benötigte Energie für den Flug
    public static int calculateMovementCost(ShipState ship, int dx, int dy, int dz) {
        int distance = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
        // Formel: 10 FE pro Block pro zurückgelegtem Meter
        return ship.getBlocks().size() * distance * 10;
    }

    // 2. Sucht alle Reaktoren im Schiff und bündelt die verfügbare Energie
    public static int getTotalAvailableEnergy(Level level, ShipState ship) {
        int totalEnergy = 0;
        for (BlockPos pos : ship.getReactors()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SpaceshipReactorBlockEntity reactor) {
                totalEnergy += reactor.getEnergyStorage().getEnergyStored();
            }
        }
        return totalEnergy;
    }

    // 3. Zieht die Energie der Reihe nach aus den Reaktoren ab
    public static void consumeEnergy(Level level, ShipState ship, int amountToExtract) {
        int remainingCost = amountToExtract;

        for (BlockPos pos : ship.getReactors()) {
            if (remainingCost <= 0) break;

            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SpaceshipReactorBlockEntity reactor) {
                int extracted = reactor.getEnergyStorage().extractEnergy(remainingCost, false);
                remainingCost -= extracted;
            }
        }
    }

    // 4. Die allgemeine Basis-Methode für ALLES (Schilde, Waffen, Flug)
    public static boolean tryConsumeEnergyAmount(Level level, ShipState ship, int amount) {
        if (getTotalAvailableEnergy(level, ship) < amount) {
            return false;
        }

        consumeEnergy(level, ship, amount);
        return true;
    }

    public record FlightEnergyResult(EnergyConsumeResult status, int cost, int available) {}

    // 5. Die spezielle Methode für den Flug
    public static FlightEnergyResult tryConsumeFlightEnergy(Level level, ShipState ship, int dx, int dy, int dz) {
        int cost = calculateMovementCost(ship, dx, dy, dz);
        int available = getTotalAvailableEnergy(level, ship);
        if (available < cost) {
            return new FlightEnergyResult(EnergyConsumeResult.INSUFFICIENT_ENERGY, cost, available);
        }
        consumeEnergy(level, ship, cost);
        return new FlightEnergyResult(EnergyConsumeResult.SUCCESS, cost, available);
    }

    /**
     * Verteilt verfügbare Reaktor-Energie proportional auf alle aktiven (nicht kollabierten)
     * Schildzonen des Schiffs.
     */
    public static int distributeEnergyToShields(Level level, ShipState ship) {
        if (level == null || ship == null || ship.getShieldZones().isEmpty()) {
            return 0;
        }
        int availableEnergy = getTotalAvailableEnergy(level, ship);
        if (availableEnergy <= 0) {
            return 0;
        }

        int transferred = distributeEnergyToShields(availableEnergy, ship, level.getGameTime());
        if (transferred > 0) {
            consumeEnergy(level, ship, transferred);
        }
        return transferred;
    }

    public static final int MAX_CHARGE_RATE_PER_ZONE = 100;

    /**
     * Reine mathematische Zuweisungs-Logik: Teilt die Energie proportional zu den individuellen
     * Defiziten der ladefähigen Zonen auf und nutzt einen Rest-Loop für exakte FE-Masserhaltung.
     */
    public static int distributeEnergyToShields(int availableEnergy, ShipState ship, long currentGameTime) {
        if (availableEnergy <= 0 || ship == null || ship.getShieldZones().isEmpty()) {
            return 0;
        }

        // 1. Defizite aller nicht-kollabierten / nicht-cooldown Zonen ermitteln
        java.util.List<com.peaceman.alpha.ship.domain.ShieldZone> eligibleZones = new java.util.ArrayList<>();
        long totalDeficit = 0L;

        for (com.peaceman.alpha.ship.domain.ShieldZone zone : ship.getShieldZones().values()) {
            if (!zone.isEnabled() || zone.generatorPos() == null || currentGameTime < zone.cooldownUntil()) {
                continue; // Cooldown-Blockade aktiv oder Generator zerstört/deaktiviert
            }
            int deficit = zone.maxEnergy() - zone.currentEnergy();
            deficit = Math.min(deficit, MAX_CHARGE_RATE_PER_ZONE);
            
            if (deficit > 0) {
                eligibleZones.add(zone);
                totalDeficit += deficit;
            }
        }

        if (totalDeficit <= 0 || eligibleZones.isEmpty()) {
            return 0;
        }

        int energyToDistribute = (int) Math.min((long) availableEnergy, totalDeficit);
        int remainingRest = energyToDistribute;

        java.util.Map<Byte, Integer> allocations = new java.util.HashMap<>();

        // 2. Primärer proportionaler Loop: E_transfer = floor(E_avail * (D_i / D_total))
        for (com.peaceman.alpha.ship.domain.ShieldZone zone : eligibleZones) {
            int deficit = zone.maxEnergy() - zone.currentEnergy();
            deficit = Math.min(deficit, MAX_CHARGE_RATE_PER_ZONE);
            int transfer = (int) ((double) energyToDistribute * (double) deficit / (double) totalDeficit);
            transfer = Math.min(transfer, deficit);
            allocations.put(zone.id(), transfer);
            remainingRest -= transfer;
        }

        // 3. Sekundärer Fallback-Loop (Rest-Tröpfchen-Verteilung): Gleichmäßige Aufteilung des Restes
        if (remainingRest > 0) {
            java.util.List<com.peaceman.alpha.ship.domain.ShieldZone> restZones = new java.util.ArrayList<>();
            for (com.peaceman.alpha.ship.domain.ShieldZone zone : eligibleZones) {
                int deficit = zone.maxEnergy() - zone.currentEnergy();
                deficit = Math.min(deficit, MAX_CHARGE_RATE_PER_ZONE);
                if (allocations.getOrDefault(zone.id(), 0) < deficit) {
                    restZones.add(zone);
                }
            }

            if (!restZones.isEmpty()) {
                int div = remainingRest / restZones.size();
                int mod = remainingRest % restZones.size();
                for (com.peaceman.alpha.ship.domain.ShieldZone zone : restZones) {
                    int deficit = zone.maxEnergy() - zone.currentEnergy();
                    deficit = Math.min(deficit, MAX_CHARGE_RATE_PER_ZONE);
                    int currentAlloc = allocations.getOrDefault(zone.id(), 0);
                    int toAdd = Math.min(deficit - currentAlloc, div + (mod > 0 ? 1 : 0));
                    if (mod > 0) mod--;
                    allocations.put(zone.id(), currentAlloc + toAdd);
                    remainingRest -= toAdd;
                }
                
                // Fallback für verbleibenden Rest durch Math.min Limitierung
                boolean distributedAny = true;
                while (remainingRest > 0 && distributedAny) {
                    distributedAny = false;
                    for (com.peaceman.alpha.ship.domain.ShieldZone zone : restZones) {
                        if (remainingRest <= 0) break;
                        int deficit = zone.maxEnergy() - zone.currentEnergy();
                        deficit = Math.min(deficit, MAX_CHARGE_RATE_PER_ZONE);
                        int currentAlloc = allocations.getOrDefault(zone.id(), 0);
                        if (currentAlloc < deficit) {
                            allocations.put(zone.id(), currentAlloc + 1);
                            remainingRest--;
                            distributedAny = true;
                        }
                    }
                }
            }
        }

        // 4. ShipState Zonen atomar aktualisieren
        int totalTransferred = 0;
        for (java.util.Map.Entry<Byte, Integer> entry : allocations.entrySet()) {
            int add = entry.getValue();
            if (add > 0) {
                com.peaceman.alpha.ship.domain.ShieldZone zone = ship.getShieldZone(entry.getKey());
                if (zone != null) {
                    ship.updateShieldZoneEnergy(entry.getKey(), zone.currentEnergy() + add);
                    totalTransferred += add;
                }
            }
        }

        return totalTransferred;
    }
}