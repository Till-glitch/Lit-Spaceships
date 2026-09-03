package com.peaceman.alpha.helper;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@EventBusSubscriber(modid = "peaceman_alpha")
public class AutoOpEvent {

    @SubscribeEvent
    public static void onServerStart(ServerStartedEvent event) {
        // Das simuliert die Eingabe in die Server-Konsole direkt beim Start
        // Falls du in der Testumgebung einen anderen Namen als "Till" nutzt, passe ihn
        // hier an
        event.getServer().getCommands().performPrefixedCommand(
                event.getServer().createCommandSourceStack(),
                "op Dev");
    }
}