package com.lit.spaceships.ship;

import com.lit.spaceships.LitSpaceships;
import com.lit.spaceships.block.entity.SpaceshipShieldBlockEntity;
import com.lit.spaceships.helper.ShieldLifecycleLogger;
import com.lit.spaceships.network.ShieldBubbleSyncPacket;
import com.lit.spaceships.network.ShipImpactEventPayload;
import com.lit.spaceships.network.ShipStateSyncPayload;
import com.lit.spaceships.ship.domain.ShipState;
import com.lit.spaceships.ship.service.ServerShipManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
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
@EventBusSubscriber(modid = LitSpaceships.MODID)
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
                PacketDistributor.sendToAllPlayers(new ShipStateSyncPayload(ship.getId(), 0, false,
                        ship.getShieldCooldownRemaining(level.getGameTime()),
                        ship.getMovementCooldownRemaining(level.getGameTime())));
                ServerShipManager.saveData(level);
            }
            return false;
        }

        // Cooldown-Check: Soll das Schild aktiviert werden?
        boolean wantsToActivate = !ship.isShieldActive();
        if (wantsToActivate && !level.isClientSide()) {
            long gameTime = level.getGameTime();
            if (ship.isShieldOnCooldown(gameTime)) {
                long remaining = ship.getShieldCooldownRemaining(gameTime);
                LitSpaceships.LOGGER.info("[SpaceshipShieldHandler] Schild-Aktivierung fuer Schiff '{}' blockiert! Cooldown laeuft noch {} Ticks ({} Sek.)",
                        ship.getId(), remaining, remaining / 20);
                return false;
            }
        }

        // Generator vorhanden: Zustand umschalten
        boolean newState = !ship.isShieldActive();
        ship.setShieldActive(newState);
        ShieldLifecycleLogger.logShieldToggled(ship.getId(), newState);

        if (!level.isClientSide() && level instanceof ServerLevel) {
            // Bei Deaktivierung: Cooldown setzen
            if (!newState) {
                long cooldownEnd = level.getGameTime() + ShipState.SHIELD_COOLDOWN_TICKS;
                ship.setShieldCooldownUntil(cooldownEnd);
                LitSpaceships.LOGGER.info("[SpaceshipShieldHandler] Schild-Cooldown fuer Schiff '{}' gesetzt bis Tick {} ({} Sek.)",
                        ship.getId(), cooldownEnd, ShipState.SHIELD_COOLDOWN_TICKS / 20);
            }
            PacketDistributor.sendToAllPlayers(new ShipStateSyncPayload(ship.getId(), 0, newState,
                    ship.getShieldCooldownRemaining(level.getGameTime()),
                    ship.getMovementCooldownRemaining(level.getGameTime())));
            ServerShipManager.saveData(level);
        }
        return newState;
    }

    public static void toggleShieldZone(Level level, ShipState ship, BlockPos generatorPos) {
        if (ship == null || level.isClientSide()) return;

        // Find the shield zone ID matching the block pos
        byte targetZoneId = 0;
        for (com.lit.spaceships.ship.domain.ShieldZone zone : ship.getShieldZones().values()) {
            if (zone.generatorPos() != null && zone.generatorPos().equals(generatorPos)) {
                targetZoneId = zone.id();
                break;
            }
        }

        if (targetZoneId > 0) {
            ship.toggleShieldZoneActive(targetZoneId);
            ServerShipManager.syncShieldZoneStates(level, ship);
            ServerShipManager.saveData(level);
            LitSpaceships.LOGGER.info("[SpaceshipShieldHandler] Schild-Zone {} (Pos: {}) fuer Schiff '{}' umgeschaltet.",
                    targetZoneId, generatorPos, ship.getId());
        }
    }

    /**
     * Wird aufgerufen, wenn ein Schildgenerator-Block abgebaut oder zerstört wird.
     * Deaktiviert den Schild sofort und leert das VBO-Mesh auf den Clients, falls kein weiterer Generator mehr existiert.
     * Existieren noch weitere Generatoren, wird die zerstörte Zone permanent kollabiert (Loch im Schild im PvP).
     */
    public static void onShieldBlockDestroyed(Level level, BlockPos pos, UUID shipId) {
        if (level.isClientSide() || shipId == null) return;

        ShipState ship = ServerShipManager.getShip(shipId);
        if (ship == null) return;

        // Position aus den Schildblöcken und Schiffsblöcken austragen
        ship.getShields().remove(pos);
        ship.getBlocks().remove(pos);

        // Finde und deaktiviere die zugehörige ShieldZone (Generator zerstört)
        for (java.util.Map.Entry<Byte, com.lit.spaceships.ship.domain.ShieldZone> entry : ship.getShieldZones().entrySet()) {
            com.lit.spaceships.ship.domain.ShieldZone z = entry.getValue();
            if (z.generatorPos() != null && z.generatorPos().equals(pos)) {
                ship.setShieldZone(new com.lit.spaceships.ship.domain.ShieldZone(z.id(), null, 0, z.maxEnergy(), Long.MAX_VALUE, false));
                break;
            }
        }

        if (ship.getShields().isEmpty()) {
            // Letzter Generator zerstört: Schild sofort deaktivieren und leeres Mesh an alle Clients senden!
            ship.setShieldActive(false);
            long cooldownEnd = level.getGameTime() + ShipState.SHIELD_COOLDOWN_TICKS;
            ship.setShieldCooldownUntil(cooldownEnd);
            ShieldLifecycleLogger.logShieldToggled(ship.getId(), false);
            PacketDistributor.sendToAllPlayers(new ShipStateSyncPayload(ship.getId(), 0, false,
                    ship.getShieldCooldownRemaining(level.getGameTime()),
                    ship.getMovementCooldownRemaining(level.getGameTime())));
            PacketDistributor.sendToAllPlayers(new ShieldBubbleSyncPacket(ship.getId(), ship.getControllerPos(), java.util.Collections.emptyMap()));
        } else {
            // Noch weitere Generatoren vorhanden: KEINE Neuberechnung der Schildblase!
            // Das Schildsegment des zerstörten Generators fällt aus (Loch entsteht).
            ServerShipManager.syncShieldZoneStates(level, ship);
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

    /**
     * Versucht, dem für die gegebene Position verantwortlichen Schildgenerator
     * Energie abzuziehen. Findet den Generator via VoxelGridCache oder per
     * geometrischer Distanz (für Blöcke außerhalb der Hülle).
     */
    public static boolean tryConsumeShieldEnergyAt(Level level, ShipState ship, BlockPos hitPos, int energyCost, byte fallbackShieldId) {
        if (ship == null || ship.getShieldZones().isEmpty()) return false;

        byte shieldId = 0;

        // 1. Ist es Teil der Huelle? (O(1) Lookup)
        if (ship.getHullVoxelCache() != null) {
            BlockPos localPos = hitPos.subtract(ship.getControllerPos());
            shieldId = ship.getHullVoxelCache().getShieldId(localPos);
        }

        // 2. Fallback: Naechster Generator zum Explosionszentrum
        if (shieldId == 0) {
            shieldId = fallbackShieldId;
        }

        // 3. Energie abziehen, falls moeglich
        if (shieldId > 0) {
            com.lit.spaceships.ship.domain.ShieldZone zone = ship.getShieldZone(shieldId);
            long gameTime = level.getGameTime();
            if (zone != null && !zone.isCollapsed(gameTime) && zone.currentEnergy() >= energyCost) {
                int newEnergy = zone.currentEnergy() - energyCost;
                long cooldown = newEnergy <= 0 ? gameTime + ShipState.SHIELD_COOLDOWN_TICKS : zone.cooldownUntil();
                ship.updateShieldZoneEnergyAndCooldown(shieldId, newEnergy, cooldown);

                if (newEnergy <= 0) {
                    ServerShipManager.syncShieldZoneStates(level, ship);
                }
                return true;
            }
        }
        return false;
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
            List<BlockPos> shipProtectedBlocks = new ArrayList<>();
            
            // Finde naehsten Generator zur Explosionsmitte (O(Generators) einmal pro Explosion statt pro Block)
            byte fallbackShieldId = 0;
            BlockPos explosionCenter = BlockPos.containing(event.getExplosion().center());
            double minSq = Double.MAX_VALUE;
            long gameTime = level.getGameTime();
            for (com.lit.spaceships.ship.domain.ShieldZone zone : ship.getShieldZones().values()) {
                if (!zone.isCollapsed(gameTime) && zone.generatorPos() != null) {
                    double dist = zone.generatorPos().distSqr(explosionCenter);
                    if (dist < minSq) {
                        minSq = dist;
                        fallbackShieldId = zone.id();
                    }
                }
            }

            // 2. Betroffene Blöcke gegen den Schild prüfen
            for (BlockPos affectedBlock : event.getAffectedBlocks()) {
                if (protectedBlocks.contains(affectedBlock)) {
                    continue;
                }

                // 3. Algorithmischer Check gegen die Schildmorphologie
                if (ShieldMorphology.isBlockProtected(ship.getBlocks(), affectedBlock, radius)) {
                    // 4. Energieverbrauch prüfen
                    if (tryConsumeShieldEnergyAt(level, ship, affectedBlock, ENERGY_COST_PER_BLOCK, fallbackShieldId)) {
                        protectedBlocks.add(affectedBlock);
                        shipProtectedBlocks.add(affectedBlock);
                    }
                }
            }

            // Wenn Blöcke dieses Schiffs geschützt wurden: Impact-Welle & Energiestatus senden
            if (!shipProtectedBlocks.isEmpty() && level instanceof ServerLevel serverLevel) {
                BlockPos ctrl = ship.getControllerPos();
                BlockPos hitPos = shipProtectedBlocks.get(0);
                Vec3 localImpact = Vec3.atCenterOf(hitPos.subtract(ctrl));

                // Schicke Einschlagswelle an alle Spieler im Chunk-Bereich
                PacketDistributor.sendToPlayersTrackingChunk(
                        serverLevel,
                        new ChunkPos(ctrl),
                        new ShipImpactEventPayload(ship.getId(), localImpact, 1.0f)
                );

                // Neuer Energie-Status an alle Clients broadcasten
                PacketDistributor.sendToAllPlayers(new ShipStateSyncPayload(
                        ship.getId(),
                        SpaceshipEnergyManager.getTotalAvailableEnergy(level, ship),
                        true,
                        ship.getShieldCooldownRemaining(level.getGameTime()),
                        ship.getMovementCooldownRemaining(level.getGameTime())
                ));
            }
        }

        // 5. Gerettete Blöcke aus der Zerstörungsliste entfernen
        if (!protectedBlocks.isEmpty()) {
            event.getAffectedBlocks().removeAll(protectedBlocks);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        Level level = event.getLevel();
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        // Verteile periodisch Energie auf aktive Schildzonen (hier 1x pro Tick)
        long gameTime = serverLevel.getGameTime();
        for (ShipState ship : ServerShipManager.getShipsInDimension(serverLevel.dimension()).values()) {
            if (ship.isShieldActive() && hasShieldGenerator(ship)) {
                // Merke alte Maske
                long oldMask = ServerShipManager.calculateShieldActiveMask(ship, gameTime);
                
                int distributed = com.lit.spaceships.ship.SpaceshipEnergyManager.distributeEnergyToShields(serverLevel, ship);
                
                long newMask = ServerShipManager.calculateShieldActiveMask(ship, gameTime);
                
                // Falls Energie verteilt wurde oder sich der Masken-Status geändert hat, synchronisiere
                if (distributed > 0 || newMask != oldMask) {
                    ServerShipManager.syncShieldZoneStates(serverLevel, ship);
                }
            }
            // Schließe Telemetrie für diesen Tick ab (Rollover von Puffer zu Last-Drain)
            ship.endTickTelemetry();
        }
    }
}