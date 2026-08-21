package com.peaceman.alpha.ship;

import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.ship.domain.ShipState;
import com.peaceman.alpha.ship.service.ServerShipManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ExplosionEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = Alpha.MODID, bus = EventBusSubscriber.Bus.GAME)
public class SpaceshipShieldHandler {

    public static final int ENERGY_COST_PER_BLOCK = 50;
    public static final int SHIELD_RADIUS = 5;

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
            if (!ship.isShieldActive() || ship.getShields().isEmpty()) {
                continue;
            }

            // 2. Betroffene Blöcke gegen den Schild prüfen
            for (BlockPos affectedBlock : event.getAffectedBlocks()) {
                if (protectedBlocks.contains(affectedBlock)) {
                    continue;
                }

                // 3. Algorithmischer Check gegen die Schildmorphologie
                if (ShieldMorphology.isBlockProtected(ship.getBlocks(), affectedBlock, SHIELD_RADIUS)) {
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