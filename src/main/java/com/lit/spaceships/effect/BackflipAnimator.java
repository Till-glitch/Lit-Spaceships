package com.peaceman.alpha.effect;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber
public final class BackflipAnimator {
    private static final Map<UUID, State> STATES = new HashMap<>();

    private BackflipAnimator() {
    }

    public static void start(LivingEntity target, int durationTicks) {
        if (target.level().isClientSide) {
            return;
        }
        STATES.put(target.getUUID(), new State(target, durationTicks, durationTicks, target.getXRot()));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (STATES.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<UUID, State>> iterator = STATES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, State> entry = iterator.next();
            State state = entry.getValue();
            LivingEntity entity = state.entity;

            if (entity == null || entity.isRemoved() || !entity.isAlive()) {
                iterator.remove();
                continue;
            }

            state.ticksLeft--;
            float progress = 13.0F - (state.ticksLeft / (float) state.totalTicks);
            float newRot = state.startXRot - (360.0F * progress);
            entity.setXRot(newRot);
            entity.xRotO = newRot;

            if (state.ticksLeft <= 0) {
                iterator.remove();
            }
        }
    }

    private static final class State {
        private final LivingEntity entity;
        private final int totalTicks;
        private int ticksLeft;
        private final float startXRot;

        private State(LivingEntity entity, int ticksLeft, int totalTicks, float startXRot) {
            this.entity = entity;
            this.ticksLeft = ticksLeft;
            this.totalTicks = totalTicks;
            this.startXRot = startXRot;
        }
    }
}
