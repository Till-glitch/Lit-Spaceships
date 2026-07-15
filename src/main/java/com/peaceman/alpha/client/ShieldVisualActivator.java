package com.peaceman.alpha.client;

import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.client.render.ShieldRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

// WICHTIG: value = Dist.CLIENT sagt dem Spiel, dass dieser Code NUR auf der Grafik-Seite läuft!
@EventBusSubscriber(modid = Alpha.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ShieldVisualActivator {

    @SubscribeEvent
    public static void onBlockClick(PlayerInteractEvent.RightClickBlock event) {
        // Wir wollen nur eingreifen, wenn es auf dem Client (Spieler-Sicht) passiert
        if (!event.getLevel().isClientSide()) return;

        // Wenn du schleichst und mit einem Lohenstaub (Blaze Powder) rechtsklickst...
        if (event.getEntity().isCrouching() && event.getItemStack().is(Items.BLAZE_POWDER)) {

            // ... schalten wir den Shader genau an dieser Position ein!
            //ShieldRenderer.shipAnchorPoint = event.getPos();

            // Kleine Info im Chat für dich
            event.getEntity().displayClientMessage(
                    Component.literal("§b[Shader] §fSchild-Visualisierung an " + event.getPos().toShortString() + " gestartet!"),
                    true
            );

            // Verhindert, dass der Rechtsklick noch andere Dinge tut
            event.setCanceled(true);
        }
    }
    @SubscribeEvent
    public static void onHitSimulation(PlayerInteractEvent.RightClickBlock event) {
        // Grafische Effekte laufen immer nur auf dem Client!
        if (!event.getLevel().isClientSide()) return;

        // Wir nutzen einen PFEIL als unser Test-Werkzeug
        if (event.getItemStack().is(Items.ARROW)) {

            if (event.getEntity().isCrouching()) {
                // 1. Schleich-Klick: Einschlag zurücksetzen (Wellen stoppen)
                ShieldRenderer.lastImpactPos = Vec3.ZERO;
                event.getEntity().displayClientMessage(
                        Component.literal("§e[Shader-Test] §fSchild beruhigt sich wieder."), true);
            } else {
                // 2. Normaler Klick: Einschlag simulieren!
                // Wir nehmen exakt die Mitte des angeklickten Blocks
                Vec3 hitVec = event.getPos().getCenter();
                ShieldRenderer.lastImpactPos = hitVec;
                ShieldRenderer.lastImpactTick = event.getLevel().getGameTime(); // NEU: Startschuss!

                event.getEntity().displayClientMessage(
                        Component.literal("§c[Shader-Test] §fTreffer-Welle ausgelöst!"), true);
            }

            event.setCanceled(true); // Verhindert andere Aktionen mit dem Pfeil
        }
    }
}