package com.lit.spaceships.helper;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber // Registriert die Klasse automatisch bei NeoForge
public class TickScheduler {

    private static final List<DelayedTask> TASKS = new ArrayList<>();

    private record DelayedTask(int targetTick, Runnable action) {
    }

    private static int currentServerTicks = 0;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        currentServerTicks++;

        // Wir gehen die Liste rückwärts durch, um sicher zu entfernen
        for (int i = TASKS.size() - 1; i >= 0; i--) {
            DelayedTask task = TASKS.get(i);
            if (currentServerTicks >= task.targetTick) {
                task.action.run();
                TASKS.remove(i);
            }
        }
    }

    /**
     * Führt einen Code-Block nach einer Verzögerung aus.
     * 
     * @param seconds Verzögerung in Sekunden
     * @param action  Der auszuführende Code (Lambda)
     */
    public static void runAfterSeconds(int seconds, Runnable action) {
        int ticks = seconds * 20; // 20 Ticks = 1 Sekunde
        TASKS.add(new DelayedTask(currentServerTicks + ticks, action));
    }
}
