package com.lit.spaceships.client.state;

import com.lit.spaceships.LitSpaceships;
import com.lit.spaceships.helper.ShieldLifecycleLogger;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Überwacht Dimensionswechsel des lokalen Spielers auf dem Client.
 * Verhindert VBO Zombie-Speicher im VRAM, indem bei jedem Dimensionswechsel
 * der VRAM-Cache geleert wird und Schiffsdaten für die neue Dimension frisch angefordert werden.
 */
@EventBusSubscriber(modid = LitSpaceships.MODID, value = Dist.CLIENT)
public class ClientDimensionLifecycleObserver {

    private static net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> lastClientDimension = null;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> currentDim = mc.level.dimension();
            if (lastClientDimension != null && !lastClientDimension.equals(currentDim)) {
                ShieldLifecycleLogger.logClientReset("Dimensionswechsel von " + lastClientDimension.location() + " nach " + currentDim.location());
                ClientShipManager.clearAllVBOs();
            }
            lastClientDimension = currentDim;
        } else {
            lastClientDimension = null;
        }
    }

    @SubscribeEvent
    public static void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        lastClientDimension = null;
        ClientShipManager.clearAllVBOs();
    }
}
