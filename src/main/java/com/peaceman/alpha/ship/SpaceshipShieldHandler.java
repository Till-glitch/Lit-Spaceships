package com.peaceman.alpha.ship;

import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.block.entity.SpaceshipShieldBlockEntity;
import com.peaceman.alpha.helper.ShieldLifecycleLogger;
import com.peaceman.alpha.network.ShieldBubbleSyncPacket;
import com.peaceman.alpha.network.ShipStateSyncPayload;
import com.peaceman.alpha.ship.domain.ShipState;
import com.peaceman.alpha.ship.service.ServerShipManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Koordiniert die Server-Domainlogik für Schutzschilde:
 * - Prüfung auf vorhandene Schildgeneratoren (Bug 1 Fix)
 * - Sofortige Deaktivierung & Bereinigung bei Zerstörung von Schildgeneratoren
 * - Umschalten / Validieren des Schildstatus
 * - Schadensabfang bei Explosionen & Energieverbrauch
 * - Erweiterbar für variable Schildradien, Farbwerte und Generatortypen
 */
@EventBusSubscriber(modid = Alpha.MODID, bus = EventBusSubscriber.Bus.GAME)
public class SpaceshipShieldHandler {

    public static final int ENERGY_COST_PER_BLOCK = 50;
    public static final int DEFAULT_SHIELD_RADIUS = 5;

    /**
     * Prüft, ob das Schiff mindestens einen funktionsfähigen Schildgenerator besitzt.
     */
    public static boolean hasShieldGenerator(ShipState ship) {
        return ship != null && !ship.getShields().isEmpty();
    }

    /**
     * Ermittelt den Schildradius des Schiffes (zukunftssicher erweiterbar nach Generatortyp).
     */
    public static int getShieldRadius(ShipState ship) {
        if (!hasShieldGenerator(ship)) return 0;
        return DEFAULT_SHIELD_RADIUS;
    }

    /**
     * Schaltet den Schildzustand sicher um.
     * Eine Aktivierung gelingt nur dann, wenn mindestens ein Schildgenerator verbaut ist.
     * @return true, falls der Schild nun aktiv ist, false falls inaktiv oder kein Generator vorhanden.
     */
    public static boolean toggleShield(Level level, ShipState ship) {
        if (ship == null) return false;

        boolean canActivate = hasShieldGenerator(ship);

        if (!canActivate) {
            // Kein Generator vorhanden: Schild muss zwingend inaktiv bleiben!
            ship.setShieldActive(false);
            ShieldLifecycleLogger.logShieldToggled(ship.getId(), false);
            if (!level.isClientSide() && level instanceof ServerLevel) {
                PacketDistributor.sendToAllPlayers(new ShipStateSyncPayload(ship.getId(), 0, false));
                ServerShipManager.saveData(level);
            }
            return false;
        }

        // Generator vorhanden: Zustand umschalten
        boolean newState = !ship.isShieldActive();
        ship.setShieldActive(newState);
        ShieldLifecycleLogger.logShieldToggled(ship.getId(), newState);

        if (!level.isClientSide() && level instanceof ServerLevel) {
            PacketDistributor.sendToAllPlayers(new ShipStateSyncPayload(ship.getId(), 0, newState));
            ServerShipManager.saveData(level);
        }
        return newState;
    }

    /**
     * Wird aufgerufen, wenn ein Schildgenerator-Block abgebaut oder zerstört wird.
     * Deaktiviert den Schild sofort und leert das VBO-Mesh auf den Clients, falls kein weiterer Generator mehr existiert.
     */
    public static void onShieldBlockDestroyed(Level level, BlockPos pos, UUID shipId) {
        if (level.isClientSide() || shipId == null) return;

        ShipState ship = ServerShipManager.getShip(shipId);
        if (ship == null) return;

        // Position aus den Schildblöcken und Schiffsblöcken austragen
        ship.getShields().remove(pos);
        ship.getBlocks().remove(pos);

        if (ship.getShields().isEmpty()) {
            // Letzter Generator zerstört: Schild sofort deaktivieren und leeres Mesh an alle Clients senden!
            ship.setShieldActive(false);
            ShieldLifecycleLogger.logShieldToggled(ship.getId(), false);
            PacketDistributor.sendToAllPlayers(new ShipStateSyncPayload(ship.getId(), 0, false));
            PacketDistributor.sendToAllPlayers(new ShieldBubbleSyncPacket(ship.getId(), ship.getControllerPos(), Collections.emptySet()));
        } else {
            // Noch weitere Generatoren vorhanden: Schildblase neu berechnen
            ship.syncShieldBubbleToClients(level);
        }

        ServerShipManager.saveData(level);
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        LevelAccessor levelAccessor = event.getLevel();
        if (!(levelAccessor instanceof Level level) || level.isClientSide()) return;

        BlockPos pos = event.getPos();
        BlockEntity be = level.getBlockEntity(pos);

        if (be instanceof SpaceshipShieldBlockEntity shieldBE) {
            UUID shipId = shieldBE.getShipId();
            if (shipId != null) {
                onShieldBlockDestroyed(level, pos, shipId);
            }
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();

        // Logik passiert IMMER nur auf dem Server
        if (level.isClientSide()) {
            return;
        }

        List<BlockPos> protectedBlocks = new ArrayList<>();

        // 1. Alle aktiven Schiffe durchgehen
        for (ShipState ship : ServerShipManager.ACTIVE_SHIPS.values()) {
            // Nur Schiffe mit aktiven Schilden und Generatoren schützen Blöcke
            if (!ship.isShieldActive() || !hasShieldGenerator(ship)) {
                continue;
            }

            int radius = getShieldRadius(ship);

            // 2. Betroffene Blöcke gegen den Schild prüfen
            for (BlockPos affectedBlock : event.getAffectedBlocks()) {
                if (protectedBlocks.contains(affectedBlock)) {
                    continue;
                }

                // 3. Algorithmischer Check gegen die Schildmorphologie
                if (ShieldMorphology.isBlockProtected(ship.getBlocks(), affectedBlock, radius)) {
                    // 4. Energieverbrauch prüfen
                    if (SpaceshipEnergyManager.tryConsumeEnergyAmount(level, ship, ENERGY_COST_PER_BLOCK)) {
                        protectedBlocks.add(affectedBlock);
                    }
                }
            }
        }

        // 5. Gerettete Blöcke aus der Zerstörungsliste entfernen
        if (!protectedBlocks.isEmpty()) {
            event.getAffectedBlocks().removeAll(protectedBlocks);
        }
    }
}