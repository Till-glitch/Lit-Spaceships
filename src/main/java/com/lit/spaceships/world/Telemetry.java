package com.lit.spaceships.world;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Telemetry-Engine (Epoch 10): reine Signalverarbeitung fuer das Signalscope.
 * Kein Zustand, keine Weltzugriffe — vollstaendig JUnit-testbar.
 *
 * <p>Physik: Das Scope misst den Richtungs-Schwerpunkt aller Empfaenge.
 * Der Alignment-Wert ist der normierte Skalarprodukt-Winkel
 * {@code L̂ · T̂ in [-1, 1]} zwischen Blickvektor und Zielvektor; daraus
 * ergeben sich Ping-Pitch (0.5..1.3) und HUD-Wellenformbreite (4..60 px).</p>
 */
public final class Telemetry {

    /** Maximale Empfangsreichweite des Scopes. */
    public static final double MAX_RANGE = 512.0D;

    /** Transponder-Frequenz-Kanaele (Register-IDs). */
    public static final class Frequencies {
        public static final ResourceLocation DISTRESS_CALL =
                ResourceLocation.fromNamespaceAndPath("lit_spaceships", "distress_call");
        public static final ResourceLocation RESEARCH_BEACON =
                ResourceLocation.fromNamespaceAndPath("lit_spaceships", "research_beacon");
        public static final ResourceLocation ANOMALOUS_RELIC =
                ResourceLocation.fromNamespaceAndPath("lit_spaceships", "anomalous_relic");

        private Frequencies() {
        }
    }

    /** Ein empfangbarer Transponder: Frequenz + Weltosition. */
    public record BeaconSignal(ResourceLocation frequency, Vec3 position) {
    }

    /** Auswertung eines Scans. */
    public record TelemetryReading(BeaconSignal signal, double alignment, double distance,
                                   double attenuation, float pingPitch, int waveformWidth) {
    }

    private Telemetry() {
    }

    /**
     * Normiertes Skalarprodukt zwischen Blickvektor und Zielrichtung in [-1, 1].
     * +1 = direkt anvisiert, 0 = 90°, -1 = exakt hinter dem Ruecken.
     */
    public static double alignment(Vec3 lookVec, Vec3 targetVec) {
        double lookLength = lookVec.length();
        double targetLength = targetVec.length();
        if (lookLength < 1.0E-6D || targetLength < 1.0E-6D) {
            return 0.0D;
        }
        return lookVec.dot(targetVec) / (lookLength * targetLength);
    }

    /**
     * Distanz-Daempfung in [0, 1]: 1 direkt daneben, 0 an der Grenze von
     * {@code MAX_RANGE}. Jenseits der Range 0.
     */
    public static double attenuation(double distance) {
        if (distance >= MAX_RANGE) {
            return 0.0D;
        }
        return Math.max(0.0D, 1.0D - distance / MAX_RANGE);
    }

    /** Ping-Pitch:_alignment -1..1 -> Pitch 0.5..1.3 (Tieffrequenz bei Fehlausrichtung). */
    public static float pingPitch(double alignment) {
        return (float) (0.5D + (alignment + 1.0D) * 0.4D);
    }

    /** HUD-Wellenformbreite: 4 px Basis + bis zu 56 px bei perfektem Empfang. */
    public static int waveformWidth(double alignment, double attenuation) {
        double strength = Math.max(0.0D, alignment) * attenuation;
        return (int) Math.round(4.0D + strength * 56.0D);
    }

    /**
     * Scan: waehlt aus {@code signals} das staerkste Empfaenge innerhalb der
     * Reichweite. Sortierung: Alignment absteigend, bei Gleichstand Distanz
     * aufsteigend.
     */
    public static Optional<TelemetryReading> bestSignal(Vec3 listenerPos, Vec3 lookVec,
                                                        List<BeaconSignal> signals) {
        return bestSignal(listenerPos, lookVec, signals, MAX_RANGE);
    }

    /** Reichweiten-parametrisierte Variante (GameTests mit kleinen Entfernungen). */
    public static Optional<TelemetryReading> bestSignal(Vec3 listenerPos, Vec3 lookVec,
                                                        List<BeaconSignal> signals, double maxRange) {
        TelemetryReading best = null;
        for (BeaconSignal signal : signals) {
            Vec3 toTarget = signal.position().subtract(listenerPos);
            double distance = toTarget.length();
            if (distance > maxRange || distance < 0.5D) {
                continue; // eigene Position oder jenseits der Reichweite
            }
            double a = alignment(lookVec, toTarget);
            double att = attenuation(distance) * (maxRange / MAX_RANGE);
            TelemetryReading reading = new TelemetryReading(signal, a, distance, att,
                    pingPitch(a), waveformWidth(a, att));
            if (best == null || reading.alignment() > best.alignment()
                    || (reading.alignment() == best.alignment()
                        && reading.distance() < best.distance())) {
                best = reading;
            }
        }
        return Optional.ofNullable(best);
    }

    /**
     * Verschluesselte Koordinaten: deterministisches XOR-Obfuscation fuer
     * Flight-Recorder-/Transponder-Disc-Items (Epoch 13 nutzt dieselbe Logik).
     */
    public static long encryptCoordinate(long coordinate, int transponderId) {
        long key = transponderId * 0x5DEECE66DL ^ 0x2A5F3B17L;
        return coordinate ^ key;
    }

    public static long decryptCoordinate(long encrypted, int transponderId) {
        return encryptCoordinate(encrypted, transponderId); // XOR ist involutiv
    }

    /** Codec-faehige Positions-Serialisierung fuer Data Components. */
    public static final Codec<Vec3> VEC3_CODEC = Codec.DOUBLE.listOf().xmap(
            list -> new Vec3(list.get(0), list.get(1), list.get(2)),
            vec -> List.of(vec.x, vec.y, vec.z));
}
