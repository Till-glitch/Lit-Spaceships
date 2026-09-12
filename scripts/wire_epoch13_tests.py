# -*- coding: utf-8 -*-
"""Epoch 13: zusaetzliche Tests fuer die >=230-Exit-Kriterien."""

# --- TelemetryTest Erweiterungen ---
p = 'src/test/java/com/lit/spaceships/world/TelemetryTest.java'
s = open(p, encoding='utf-8').read()
if 'bestSignalTieBreaksByDistance' not in s:
    old = '''    @Test
    @DisplayName("TelemetryData Codec: Feld-Roundtrip via generiertem Codec")'''
    new = '''    @Test
    @DisplayName("bestSignal Tie-Break: gleiche Ausrichtung -> naeheres Signal gewinnt")
    void bestSignalTieBreaksByDistance() {
        Vec3 listener = new Vec3(0, 0, 0);
        Vec3 look = new Vec3(1, 0, 0);
        var near = new Telemetry.BeaconSignal(Telemetry.Frequencies.DISTRESS_CALL, new Vec3(50, 0, 0));
        var far = new Telemetry.BeaconSignal(Telemetry.Frequencies.RESEARCH_BEACON, new Vec3(100, 0, 0));
        var best = Telemetry.bestSignal(listener, look, List.of(far, near));
        assertTrue(best.isPresent());
        assertEquals(near, best.get().signal(), "Gleiche Ausrichtung -> naeheres Signal");
        assertTrue(best.get().distance() < 100.0);
    }

    @Test
    @DisplayName("bestSignal: leere Signalliste -> Optional.empty")
    void bestSignalHandlesEmptyList() {
        assertTrue(Telemetry.bestSignal(Vec3.ZERO, new Vec3(1, 0, 0), List.of()).isEmpty());
    }

    @Test
    @DisplayName("Ping-Pitch ist monoton steigend mit dem Alignment")
    void pingPitchIsMonotonic() {
        float previous = -Float.MAX_VALUE;
        for (int i = -10; i <= 10; i++) {
            float pitch = Telemetry.pingPitch(i / 10.0D);
            assertTrue(pitch > previous, "Pitch muss monoton steigen bei " + i);
            previous = pitch;
        }
    }

    @Test
    @DisplayName("Verschluesselung: unterschiedliche Transponder-IDs -> unterschiedliche Chiffren")
    void encryptionVariesByTransponderId() {
        long coord = 999_999L;
        assertTrue(Telemetry.encryptCoordinate(coord, 1) != Telemetry.encryptCoordinate(coord, 2),
                "Verschiedene IDs muessen verschiedene Chiffren liefern");
    }

    @Test
    @DisplayName("TelemetryData Codec: Feld-Roundtrip via generiertem Codec")'''
    assert old in s
    s = s.replace(old, new)
    open(p, 'w', encoding='utf-8').write(s)
print('Telemetry extended')

# --- ReactorMeltdownLogicTest Erweiterungen ---
p2 = 'src/test/java/com/lit/spaceships/block/ReactorMeltdownLogicTest.java'
s = open(p2, encoding='utf-8').read()
if 'coolingBeyondTwoValvesIsNoOp' not in s:
    old = '''    @Test
    @DisplayName("Rauch: ab Stufe 2 mit wahrscheinlichkeitsbasiertem Auftreten")'''
    new = '''    @Test
    @DisplayName("Kuehlung nach Freischaltung: Zustand bleibt stabil")
    void coolingBeyondTwoValvesIsNoOp() {
        var state = ReactorMeltdownLogic.ReactorState.INITIAL;
        state = ReactorMeltdownLogic.cool(state);
        state = ReactorMeltdownLogic.cool(state);
        var unlocked = state;
        for (int i = 0; i < 5; i++) {
            state = ReactorMeltdownLogic.cool(state);
        }
        assertEquals(unlocked, state, "Nach Freischaltung darf Kuehlung nichts mehr aendern");
        for (int i = 0; i < 100; i++) {
            assertEquals(ReactorMeltdownLogic.ReactorEvent.NONE, ReactorMeltdownLogic.tick(state));
        }
    }

    @Test
    @DisplayName("Valve-Counter: drittes Ventil erhaelt den Counter (kein Overflow)")
    void valveCounterGrowsMonotonically() {
        var state = ReactorMeltdownLogic.ReactorState.INITIAL;
        state = ReactorMeltdownLogic.cool(state);
        state = ReactorMeltdownLogic.cool(state);
        int afterTwo = state.valvesCooled();
        state = ReactorMeltdownLogic.cool(state);
        assertTrue(state.valvesCooled() >= afterTwo, "Counter darf nicht schrumpfen");
    }

    @Test
    @DisplayName("Rauch: ab Stufe 2 mit wahrscheinlichkeitsbasiertem Auftreten")'''
    assert old in s
    s = s.replace(old, new)
    open(p2, 'w', encoding='utf-8').write(s)
print('Reactor extended')

# --- AtmospherePlannerTest Erweiterungen ---
p3 = 'src/test/java/com/lit/spaceships/world/AtmospherePlannerTest.java'
s = open(p3, encoding='utf-8').read()
if 'updraftDurationIsExactlyFifteenSeconds' not in s:
    old = '''    @Test
    @DisplayName("Deep Space bleibt neutral: niemals Effekte")'''
    new = '''    @Test
    @DisplayName("Plasma-Aufwind: exakt 15 Sekunden Dauer")
    void updraftDurationIsExactlyFifteenSeconds() {
        RandomSource random = RandomSource.create(404L);
        for (int i = 0; i < 10_000; i++) {
            var plan = BiomeAtmosphereService.planFor(ModBiomes.PLASMA_NEBULA, random);
            if (plan.isPresent() && plan.get().effect() == MobEffects.SLOW_FALLING) {
                assertEquals(300, plan.get().durationTicks(), "Aufwind = 15s");
                return;
            }
        }
        org.junit.jupiter.api.Assertions.fail("Aufwind musste in 10k Rolls auftreten");
    }

    @Test
    @DisplayName("Alle Biome-Plaene tragen nicht-leere i18n-Schluessel")
    void allPlansCarryMessageKeys() {
        RandomSource random = RandomSource.create(505L);
        for (int i = 0; i < 10_000; i++) {
            var nebula = BiomeAtmosphereService.planFor(ModBiomes.PLASMA_NEBULA, random);
            nebula.ifPresent(plan -> assertFalse(plan.messageKey().isBlank()));
            var frozen = BiomeAtmosphereService.planFor(ModBiomes.FROZEN_EXPANSE, random);
            frozen.ifPresent(plan -> assertTrue(plan.messageKey().contains("frost")));
            var wastes = BiomeAtmosphereService.planFor(ModBiomes.VOID_WASTES, random);
            wastes.ifPresent(plan -> assertTrue(plan.messageKey().contains("wastes")));
        }
    }

    @Test
    @DisplayName("Deep Space bleibt neutral: niemals Effekte")'''
    assert old in s
    s = s.replace(old, new)
    s = s.replace('import static org.junit.jupiter.api.Assertions.assertEquals;',
'''import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;''')
    open(p3, 'w', encoding='utf-8').write(s)
print('Atmosphere extended')
