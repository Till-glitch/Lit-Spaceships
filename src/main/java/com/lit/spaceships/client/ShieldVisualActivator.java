package com.lit.spaceships.client;

import com.lit.spaceships.LitSpaceships;
import com.lit.spaceships.client.state.ClientShipManager;
import com.lit.spaceships.client.state.ClientShipState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = LitSpaceships.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ShieldVisualActivator {

    @SubscribeEvent
    public static void onHitSimulation(PlayerInteractEvent.RightClickBlock event) {
        // Grafische Effekte laufen immer nur auf dem Client!
        if (!event.getLevel().isClientSide()) return;

        // Wir nutzen einen PFEIL als unser Test-Werkzeug
        if (event.getItemStack().is(Items.ARROW)) {
            BlockPos clickPos = event.getPos();
            boolean hitFound = false;

            for (ClientShipState ship : ClientShipManager.getAllShips()) {
                if (ship.getAnchorPos() != null && ship.isShieldActive()) {
                    Vec3 localHit = Vec3.atCenterOf(clickPos.subtract(ship.getAnchorPos()));
                    ship.addImpact(localHit, event.getLevel().getGameTime());
                    event.getEntity().displayClientMessage(
                            Component.translatable(
                                    com.lit.spaceships.registry.ModI18n.Message.SHADER_TEST_HIT,
                                    (int) localHit.x, (int) localHit.y, (int) localHit.z
                            ).withStyle(net.minecraft.ChatFormatting.RED),
                            true
                    );
                    hitFound = true;
                    break;
                }
            }

            if (!hitFound) {
                event.getEntity().displayClientMessage(
                        Component.translatable(
                                com.lit.spaceships.registry.ModI18n.Message.SHADER_TEST_NO_SHIP
                        ).withStyle(net.minecraft.ChatFormatting.YELLOW),
                        true
                );
            }

            event.setCanceled(true); // Verhindert andere Aktionen mit dem Pfeil
        }
    }
}