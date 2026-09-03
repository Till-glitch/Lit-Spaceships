package com.lit.spaceships.tests;

import com.lit.spaceships.LitSpaceships;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

import java.util.ArrayList;
import java.util.List;

// Diese Annotation meldet die Klasse automatisch am globalen Spiel-EventBus an
@EventBusSubscriber(modid = LitSpaceships.MODID)
public class ShieldTestHandler {

    // Hier merken wir uns temporär das Zentrum unseres Schildes
    public static BlockPos shieldCenter = null;
    public static final int SHIELD_RADIUS = 5; // 10 Blöcke in jede Richtung (Sphäre)

    /**
     * 1. Das Schild aktivieren (Mit einem Stock schleichend rechtsklicken)
     */
    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        // Nur auf dem Server ausführen
        if (event.getLevel().isClientSide())
            return;

        // Wenn der Spieler schleicht und einen Stock in der Hand hat
        if (event.getEntity().isCrouching() && event.getItemStack().is(Items.STICK)) {
            shieldCenter = event.getPos();
            event.getEntity().sendSystemMessage(Component
                    .literal("§b[Schild-Test] §fSchild-Zentrum aktiviert bei: " + shieldCenter.toShortString()));
            event.setCanceled(true); // Verhindert, dass der Stock normal benutzt wird
        }
    }

    /**
     * 2. Spieler daran hindern, Blöcke im Schild abzubauen
     */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (shieldCenter == null || event.getPlayer().isCreative())
            return; // Kreativ-Spieler ignorieren wir mal

        // Wir prüfen die mathematische Distanz (Sphäre/Kugel).
        // distSqr (Distanz zum Quadrat) ist für den PC viel schneller zu berechnen als
        // die echte Distanz!
        if (event.getPos().distSqr(shieldCenter) <= (SHIELD_RADIUS * SHIELD_RADIUS)) {
            event.setCanceled(true); // Abbauen abbrechen!
            event.getPlayer().displayClientMessage(Component.translatable(com.lit.spaceships.registry.ModI18n.Message.SHIELD_PROTECTED_BLOCK).withStyle(net.minecraft.ChatFormatting.RED),
                    true);
        }
    }

    /**
     * 3. Explosionen (Creeper, TNT) blockieren
     */
    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (shieldCenter == null)
            return;

        // Die Explosion gibt uns eine Liste aller Blöcke, die sie zerstören will.
        // Wir suchen alle Blöcke heraus, die in unserem Schild liegen...
        List<BlockPos> protectedBlocks = new ArrayList<>();

        for (BlockPos pos : event.getAffectedBlocks()) {
            if (pos.distSqr(shieldCenter) <= (SHIELD_RADIUS * SHIELD_RADIUS)) {
                protectedBlocks.add(pos);
            }
        }

        // ... und streichen sie einfach von der Zerstörungs-Liste der Explosion!
        event.getAffectedBlocks().removeAll(protectedBlocks);
    }
}