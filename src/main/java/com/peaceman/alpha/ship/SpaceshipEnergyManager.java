package com.peaceman.alpha.ship;

import com.peaceman.alpha.block.entity.SpaceshipReactorBlockEntity;
import com.peaceman.alpha.ship.domain.ShipState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
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

    // 5. Die spezielle Methode für den Flug
    public static boolean tryConsumeFlightEnergy(Level level, ShipState ship, int dx, int dy, int dz, Player player) {
        int cost = calculateMovementCost(ship, dx, dy, dz);
        boolean success = tryConsumeEnergyAmount(level, ship, cost);

        if (!success && player != null) {
            int available = getTotalAvailableEnergy(level, ship);
            player.displayClientMessage(
                    Component.translatable(com.peaceman.alpha.registry.ModI18n.Message.ENERGY_INSUFFICIENT,
                            String.format("%,d", cost), String.format("%,d", available)), true);
        }

        return success;
    }

    // 6. Die Methode für 90-Grad Schiffsrotationen
    public static int calculateRotationCost(ShipState ship, net.minecraft.world.level.block.Rotation rotation) {
        if (ship == null || rotation == null || rotation == net.minecraft.world.level.block.Rotation.NONE) return 0;
        return ship.getBlocks().size() * 5;
    }

    public static boolean tryConsumeRotationEnergy(Level level, ShipState ship, net.minecraft.world.level.block.Rotation rotation, Player player) {
        int cost = calculateRotationCost(ship, rotation);
        boolean success = tryConsumeEnergyAmount(level, ship, cost);

        if (!success && player != null) {
            int available = getTotalAvailableEnergy(level, ship);
            player.displayClientMessage(
                    Component.translatable(com.peaceman.alpha.registry.ModI18n.Message.ENERGY_INSUFFICIENT,
                            String.format("%,d", cost), String.format("%,d", available)), true);
        }

        return success;
    }
}