package com.lit.spaceships.block;

import net.minecraft.SharedConstants;
import net.minecraft.util.RandomSource;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Epoch 12: Reactor-Meltdown-State-Machine (pure Logik) — Stufenfortschritt,
 * Kuehlreset, Explosionsausloesung und 90-Sekunden-Gesamtzeit.
 */
class ReactorMeltdownLogicTest {

    @BeforeAll
    static void initMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    @DisplayName("Countdown: 4 Stufen x 450 Ticks = 90 Sekunden Gesamtzeit")
    void totalCountdownIs90Seconds() {
        assertEquals(450, ReactorMeltdownLogic.TICKS_PER_STAGE);
        assertEquals(4, ReactorMeltdownLogic.MAX_STAGE);
        assertEquals(1800, ReactorMeltdownLogic.TICKS_PER_STAGE * ReactorMeltdownLogic.MAX_STAGE,
                "90 Sekunden bei 20 Ticks/s");
    }

    @Test
    @DisplayName("Stufenfortschritt: NONE bis Stufenende, dann STAGE_ADVANCED")
    void stageAdvancesEvery450Ticks() {
        var state = ReactorMeltdownLogic.ReactorState.INITIAL;
        for (int i = 0; i < 449; i++) {
            var event = ReactorMeltdownLogic.tick(state);
            assertEquals(ReactorMeltdownLogic.ReactorEvent.NONE, event);
            state = ReactorMeltdownLogic.afterTick(state, event);
        }
        assertEquals(ReactorMeltdownLogic.ReactorEvent.STAGE_ADVANCED, ReactorMeltdownLogic.tick(state));
        state = ReactorMeltdownLogic.afterTick(state, ReactorMeltdownLogic.ReactorEvent.STAGE_ADVANCED);
        assertEquals(1, state.stage());
        assertEquals(0, state.ticksInStage());
    }

    @Test
    @DisplayName("Explosion: Stufe 4 + 450 Ticks ohne Kuehlung loest EXPLODE aus")
    void meltdownExplodesAtStage4End() {
        var state = ReactorMeltdownLogic.ReactorState.INITIAL;
        // 4 Stufen durchlaufen
        for (int stage = 0; stage < 4; stage++) {
            for (int t = 0; t < 449; t++) {
                state = ReactorMeltdownLogic.afterTick(state, ReactorMeltdownLogic.tick(state));
            }
            state = ReactorMeltdownLogic.afterTick(state, ReactorMeltdownLogic.ReactorEvent.STAGE_ADVANCED);
        }
        assertEquals(4, state.stage());
        // Stufe 4 ablaufen lassen: 449 Ticks NONE, dann EXPLODE beim 450.
        for (int t = 0; t < 449; t++) {
            assertEquals(ReactorMeltdownLogic.ReactorEvent.NONE, ReactorMeltdownLogic.tick(state));
            state = ReactorMeltdownLogic.afterTick(state, ReactorMeltdownLogic.ReactorEvent.NONE);
        }
        assertEquals(ReactorMeltdownLogic.ReactorEvent.EXPLODE, ReactorMeltdownLogic.tick(state));
    }

    @Test
    @DisplayName("Kuehlung: 2 Ventile resetten Countdown + schalten Loot frei")
    void twoValvesResetAndUnlock() {
        var state = ReactorMeltdownLogic.ReactorState.INITIAL;
        // Erstes Ventil: nur Counter hoch (kein Reset, keine Freischaltung)
        state = ReactorMeltdownLogic.cool(state);
        assertEquals(1, state.valvesCooled());
        assertFalse(state.unlocked());
        // Zweites Ventil: voller Reset + Freischaltung
        state = ReactorMeltdownLogic.cool(state);
        assertEquals(2, state.valvesCooled());
        assertTrue(state.unlocked());
        assertEquals(0, state.stage());
        assertEquals(0, state.ticksInStage());
        // Nach Freischaltung: kein weiterer Countdown, keine Explosion
        for (int i = 0; i < 5000; i++) {
            assertEquals(ReactorMeltdownLogic.ReactorEvent.NONE, ReactorMeltdownLogic.tick(state));
            state = ReactorMeltdownLogic.afterTick(state, ReactorMeltdownLogic.ReactorEvent.NONE);
        }
    }

    @Test
    @DisplayName("Erstes Ventil resettet den Stufen-Timer (Teilkuehlung)")
    void firstValveResetsStageTimer() {
        var state = new ReactorMeltdownLogic.ReactorState(2, 300, 0, false);
        state = ReactorMeltdownLogic.cool(state);
        assertEquals(0, state.ticksInStage(), "Teilkuehlung resettet den Stufen-Timer");
        assertEquals(2, state.stage(), "Stufe bleibt bei Teilkuehlung");
        assertFalse(state.unlocked());
    }

    @Test
    @DisplayName("displayStage: 0..4 geklemmt")
    void displayStageClamped() {
        assertEquals(0, ReactorMeltdownLogic.displayStage(ReactorMeltdownLogic.ReactorState.INITIAL));
        assertEquals(4, ReactorMeltdownLogic.displayStage(
                new ReactorMeltdownLogic.ReactorState(4, 0, 0, false)));
    }

    @Test
    @DisplayName("Rauch: ab Stufe 2 mit wahrscheinlichkeitsbasiertem Auftreten")
    void smokeEmissionScalesWithStage() {
        var stage1 = new ReactorMeltdownLogic.ReactorState(1, 0, 0, false);
        var stage4 = new ReactorMeltdownLogic.ReactorState(4, 0, 0, false);
        RandomSource random = RandomSource.create(9L);
        for (int i = 0; i < 100; i++) {
            assertTrue(!ReactorMeltdownLogic.emitsSmoke(stage1, random), "Stufe 1 raucht nicht");
        }
        int smoke = 0;
        for (int i = 0; i < 100; i++) {
            if (ReactorMeltdownLogic.emitsSmoke(stage4, random)) smoke++;
        }
        assertTrue(smoke > 50, "Stufe 4 raucht haeufig (100% erwartet, war " + smoke + ")");
    }
}
