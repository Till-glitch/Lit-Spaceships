package com.lit.spaceships.block;

import net.minecraft.util.RandomSource;

/**
 * Pure Reactor-Meltdown-State-Machine (Epoch 12, voll JUnit-testbar).
 *
 * <p>Stufen 0..4 mit je 450 Ticks (90 Sekunden Gesamtcountdown). Zwei
 * Kuehlventile setzen den Countdown zurueck und schalten die Kernkammer frei.
 * Ueberlebt der Reaktor Stufe 4 ohne vollstaendige Kuehlung, explodiert er.</p>
 */
public final class ReactorMeltdownLogic {

    public static final int TICKS_PER_STAGE = 450;
    public static final int MAX_STAGE = 4;
    public static final int VALVES_REQUIRED = 2;
    public static final float EXPLOSION_POWER = 4.0F;

    /**
     * Reaktorzustand: meltdown_stage 0..4, Ticks in der aktuellen Stufe,
     * gezählte Kuehlventile, Loot-Freischaltung.
     */
    public record ReactorState(int stage, int ticksInStage, int valvesCooled, boolean unlocked) {
        public static final ReactorState INITIAL = new ReactorState(0, 0, 0, false);
    }

    /** Event, das der Tick-Handler ausfuehrt. */
    public enum ReactorEvent {
        NONE, STAGE_ADVANCED, EXPLODE
    }

    private ReactorMeltdownLogic() {
    }

    /**
     * Ein Server-Tick: rueckt den Countdown fort. Liefert STAGE_ADVANCED bei
     * Stufenwechsel, EXPLODE wenn Stufe 4 abgelaufen ist.
     */
    public static ReactorEvent tick(ReactorState state) {
        if (state.unlocked()) {
            return ReactorEvent.NONE;
        }
        int ticks = state.ticksInStage() + 1;
        if (ticks < TICKS_PER_STAGE) {
            return ReactorEvent.NONE; // bleibt (implizit: gleiche Stufe)
        }
        if (state.stage() >= MAX_STAGE) {
            return ReactorEvent.EXPLODE;
        }
        return ReactorEvent.STAGE_ADVANCED;
    }

    /** Auswertung nach einem Tick-Event (uebergibt den neuen Zustand). */
    public static ReactorState afterTick(ReactorState state, ReactorEvent event) {
        return switch (event) {
            case NONE -> new ReactorState(state.stage(), state.ticksInStage() + 1,
                    state.valvesCooled(), state.unlocked());
            case STAGE_ADVANCED -> new ReactorState(state.stage() + 1, 0,
                    state.valvesCooled(), state.unlocked());
            case EXPLODE -> state; //爆炸 wird vom Handler ausgefuehrt
        };
    }

    /**
     * Ein Kuehlventil wurde aktiviert: Counter hoch; ab 2 Ventilen Countdown
     * zuruecksetzen und Loot freischalten.
     */
    public static ReactorState cool(ReactorState state) {
        if (state.unlocked()) {
            return state;
        }
        int valves = state.valvesCooled() + 1;
        if (valves >= VALVES_REQUIRED) {
            return new ReactorState(0, 0, valves, true);
        }
        return new ReactorState(state.stage(), 0, valves, false);
    }

    /** Visuelle Stufe fuer den Blockstate (0..4). */
    public static int displayStage(ReactorState state) {
        return Math.min(MAX_STAGE, state.stage());
    }

    /** Deterministischer Partikel-Zufall fuer Tests. */
    public static boolean emitsSmoke(ReactorState state, RandomSource random) {
        return state.stage() >= 2 && random.nextFloat() < 0.25F * state.stage();
    }
}
