package com.peaceman.alpha.world.environment;

import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.item.SpaceSuitItem;
import com.peaceman.alpha.registry.ModItems;
import com.peaceman.alpha.ship.domain.ShipState;
import com.peaceman.alpha.ship.service.ServerShipManager;
import com.peaceman.alpha.world.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Überwacht Weltraum-Umweltbedingungen (Zero-G / Schwerelosigkeit und tödliches Vakuum)
 * für alle Lebewesen in der Weltraum-Dimension.
 */
@EventBusSubscriber(modid = Alpha.MODID)
public class SpaceEnvironmentService {

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        Level level = entity.level();

        if (level.isClientSide()) return;
        if (!ModDimensions.SPACE_LEVEL.equals(level.dimension())) return;

        // 1. Schwerelosigkeit (Zero-G): Verleiht kontinuierlichen Slow Falling Drift-Effekt
        entity.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, false, false, false));

        // 2. Vakuum- & Asphyxiations-Prüfung alle 20 Ticks (1 Sekunde)
        if (entity.tickCount % 20 == 0) {
            if (!isProtectedFromVacuum(entity)) {
                // Tödlicher Erstickungsschaden
                entity.hurt(entity.damageSources().drown(), 2.0f);

                if (entity instanceof ServerPlayer player && entity.tickCount % 40 == 0) {
                    player.displayClientMessage(
                            Component.literal("§c⚠ WARNUNG: TÖDLICHES VAKUUM! Kein Sauerstoff!"),
                            true
                    );
                }
            }
        }
    }

    /**
     * Prüft, ob ein Lebewesen vor dem Vakuum geschützt ist:
     * 1. Creative/Spectator-Modus (Spieler)
     * 2. Angelegter Raumanzug (Space Suit Helm oder volles Set)
     * 3. Innerhalb eines aktiven Raumschiffs (Hülle oder Schildblase)
     */
    public static boolean isProtectedFromVacuum(LivingEntity entity) {
        if (entity == null) return false;

        // 1. Creative / Spectator
        if (entity instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return true;
        }

        // 2. Raumanzug (Mindestens Raumanzug-Helm schützt die Atemwege)
        ItemStack helmet = entity.getItemBySlot(EquipmentSlot.HEAD);
        if (!helmet.isEmpty() && helmet.getItem() instanceof SpaceSuitItem) {
            return true;
        }

        // 3. Innerhalb eines Raumschiffs mit Hülle oder aktiver Schildblase
        BlockPos pos = entity.blockPosition();
        for (ShipState ship : ServerShipManager.getShipsInDimension(ModDimensions.SPACE_LEVEL).values()) {
            if (ship.isInsideHull(pos) || ship.isInsideShield(pos)) {
                return true;
            }
        }

        return false;
    }
}
