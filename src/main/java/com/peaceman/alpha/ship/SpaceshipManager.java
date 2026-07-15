package com.peaceman.alpha.ship;

import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.block.entity.SpaceshipControlBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = Alpha.MODID)
public class SpaceshipManager {

    // Das Gedächtnis des Servers
    public static final Map<UUID, Spaceship> ACTIVE_SHIPS = new HashMap<>();

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        ShipSavedData.get(event.getServer().overworld());
        System.out.println("=== RAUMSCHIFFE GELADEN: " + ACTIVE_SHIPS.size() + " ===");
    }

    // --- 1. SCHIFF ABRUFEN (Hilfsmethode für deine Payloads) ---
    public static Spaceship getShip(UUID shipId) {
        if (shipId == null) return null;
        return ACTIVE_SHIPS.get(shipId);
    }

    // --- 2. SCHIFF ERSTELLEN ---
    public static Spaceship createShip(Level level, BlockPos startPos) {
        if (level.getBlockEntity(startPos) instanceof SpaceshipControlBlockEntity be) {

            // 1. Sicherheitscheck
            if (be.getShipId() != null && ACTIVE_SHIPS.containsKey(be.getShipId())) {
                System.out.println("Fehler: Block ist bereits mit einem Schiff verknüpft!");
                return null;
            }

            // 2. Wir nutzen unseren ausgelagerten Scanner
            Set<BlockPos> shipBlocks = SpaceshipScanner.scan(level, startPos);

            // 3. Schiffsobjekt grundlegend erschaffen
            Spaceship newShip = new Spaceship(startPos, shipBlocks);

            // --- HIER PASSIERT DIE NEUE MAGIE ---
            // 4. Das schwere Struktur-Update aufrufen!
            // (Das sortiert Reaktoren/Schilde, berechnet die Hitbox und sendet das Shader-Paket an die Spieler)
            newShip.setBlocks(shipBlocks, level);
            // ------------------------------------

            // 5. Schiff in die aktive Welt aufnehmen
            ACTIVE_SHIPS.put(newShip.getId(), newShip);

            // 6. Allen Knotenpunkten (Controller, Reaktoren, etc.) die ID mitteilen
            for (BlockPos pos : shipBlocks) {
                BlockEntity entityAtPos = level.getBlockEntity(pos);
                if (entityAtPos instanceof com.peaceman.alpha.block.ISpaceshipNode node) {
                    // ID im BlockEntity speichern
                    node.setShipId(newShip.getId());
                    entityAtPos.setChanged(); // Wichtig fürs Speichern auf die Festplatte

                    // Dem Client (Grafikkarte) mitteilen, dass sich die Daten geändert haben!
                    // Das ersetzt den fehlerhaften Aufruf im Konstruktor von vorhin.
                    level.sendBlockUpdated(pos, entityAtPos.getBlockState(), entityAtPos.getBlockState(), 3);
                }
            }

            System.out.println("Neues Schiff erstellt! UUID: " + newShip.getId() + " mit " + newShip.getShieldBubble().size() + " Schild-Blöcken.");
            saveData(level);
            return newShip;
        }
        return null;
    }

    // --- 3. SCHIFF AKTUALISIEREN ---
    // Übergibt jetzt direkt das Spaceship-Objekt! Keine UUIDs oder BlockPos-Parameter mehr nötig.
    public static void updateShipBlocks(Level level, Spaceship ship) {
        if (ship != null) {
            // Der Manager holt sich die Startposition direkt aus dem Schiffsobjekt!
            Set<BlockPos> newBlocks = SpaceshipScanner.scan(level, ship.getControllerPos());
            ship.setBlocks(newBlocks, level);

            for (BlockPos pos : newBlocks) {
                if (level.getBlockEntity(pos) instanceof com.peaceman.alpha.block.ISpaceshipNode node) {
                    node.setShipId(ship.getId());
                }
            }
            System.out.println("Struktur aktualisiert! Neue Block-Anzahl: " + newBlocks.size());
            saveData(level);
        }
    }

    // --- 4. SCHIFF LÖSCHEN ---
    // Übergibt auch hier direkt das Schiffsobjekt!
    public static void deleteShip(Level level, Spaceship ship) {
        if (ship != null) {
            // 1. UUID von allen Blöcken entfernen
            for (BlockPos pos : ship.getBlocks()) {
                if (level.getBlockEntity(pos) instanceof com.peaceman.alpha.block.ISpaceshipNode node) {
                    node.setShipId(null);
                }
            }
            // 2. Aus der Liste entfernen
            ACTIVE_SHIPS.remove(ship.getId());
            System.out.println("Schiffsinstanz mit UUID: " + ship.getId() + " wurde zerstört!");
            saveData(level);
        }
    }

    // Kleine interne Hilfsmethode, damit wir den NBT-Speicher-Code nicht 3x schreiben müssen
    private static void saveData(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            ShipSavedData.get(serverLevel).setDirty();
        }
    }
}