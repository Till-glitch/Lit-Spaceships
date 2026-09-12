package com.lit.spaceships.world;

import com.lit.spaceships.registry.ModDataComponents;
import net.minecraft.SharedConstants;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Epoch 10: Vektor-Mathe, Alignment, Daempfung und Codec-Roundtrip der
 * Telemetrie-Engine.
 */
class TelemetryTest {

    @BeforeAll
    static void initMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    @DisplayName("Alignment: identisch=+1, gegenueber=-1, 90 Grad=0")
    void alignmentIsNormalizedDotProduct() {
        Vec3 look = new Vec3(1, 0, 0);
        assertEquals(1.0, Telemetry.alignment(look, new Vec3(5, 0, 0)), 1e-9, "Direkt anvisiert");
        assertEquals(-1.0, Telemetry.alignment(look, new Vec3(-3, 0, 0)), 1e-9, "Hinter dem Ruecken");
        assertEquals(0.0, Telemetry.alignment(look, new Vec3(0, 0, 7)), 1e-9, "90 Grad seitlich");
        assertEquals(0.0, Telemetry.alignment(look, Vec3.ZERO), 1e-9, "Nulvektor sicher behandelt");
    }

    @Test
    @DisplayName("Ping-Pitch: -1 -> 0.5, 0 -> 0.9, +1 -> 1.3")
    void pingPitchMapsAlignmentLinear() {
        assertEquals(0.5F, Telemetry.pingPitch(-1.0), 1e-6);
        assertEquals(0.9F, Telemetry.pingPitch(0.0), 1e-6);
        assertEquals(1.3F, Telemetry.pingPitch(1.0), 1e-6);
    }

    @Test
    @DisplayName("Daempfung: linear bis 0 an MAX_RANGE, nie negativ")
    void attenuationIsLinearAndClamped() {
        assertEquals(1.0, Telemetry.attenuation(0.0), 1e-9);
        assertEquals(0.5, Telemetry.attenuation(Telemetry.MAX_RANGE / 2), 1e-9);
        assertEquals(0.0, Telemetry.attenuation(Telemetry.MAX_RANGE), 1e-9);
        assertEquals(0.0, Telemetry.attenuation(Telemetry.MAX_RANGE + 100), 1e-9);
    }

    @Test
    @DisplayName("bestSignal: waehlt bestes Alignment in Reichweite, ignoriert zu weite Signale")
    void bestSignalPicksStrongestAlignedSignal() {
        Vec3 listener = new Vec3(0, 0, 0);
        Vec3 look = new Vec3(1, 0, 0);

        // Direkt vor dem Spieler (Alignment 1) aber weit weg
        var far = new Telemetry.BeaconSignal(Telemetry.Frequencies.DISTRESS_CALL, new Vec3(400, 0, 0));
        // Seitlich (Alignment 0) aber nah
        var nearSide = new Telemetry.BeaconSignal(Telemetry.Frequencies.RESEARCH_BEACON, new Vec3(0, 0, 10));
        // Hinter dem Ruecken (Alignment -1)
        var behind = new Telemetry.BeaconSignal(Telemetry.Frequencies.ANOMALOUS_RELIC, new Vec3(-50, 0, 0));

        Optional<Telemetry.TelemetryReading> best =
                Telemetry.bestSignal(listener, look, List.of(far, nearSide, behind));

        assertTrue(best.isPresent());
        assertEquals(Telemetry.Frequencies.DISTRESS_CALL, best.get().signal().frequency(),
                "Bestes Alignment gewinnt (selbst bei grosser Distanz)");
        assertEquals(1.0, best.get().alignment(), 1e-9);

        // Zu weite Signale werden gefiltert
        assertTrue(Telemetry.bestSignal(listener, look,
                List.of(new Telemetry.BeaconSignal(Telemetry.Frequencies.DISTRESS_CALL, new Vec3(600, 0, 0))),
                Telemetry.MAX_RANGE).isEmpty());
    }

    @Test
    @DisplayName("Wellenformbreite: 4 px Basis bis 60 px bei perfektem Empfang")
    void waveformWidthBounded() {
        assertEquals(4, Telemetry.waveformWidth(-1.0, 1.0), "Kein Alignment: Basisbreite");
        assertEquals(60, Telemetry.waveformWidth(1.0, 1.0), "Perfekter Empfang: volle Breite");
        assertEquals(4, Telemetry.waveformWidth(1.0, 0.0), "Keine Daempfung: Basisbreite");
    }

    @Test
    @DisplayName("XOR-Verschluesselung: deterministisch und involutiv")
    void encryptionIsDeterministicAndInvolutory() {
        long coordinate = 5_000_000_000L;
        int transponderId = 42;
        long encrypted = Telemetry.encryptCoordinate(coordinate, transponderId);
        assertEquals(encrypted, Telemetry.encryptCoordinate(coordinate, transponderId));
        assertEquals(coordinate, Telemetry.decryptCoordinate(encrypted, transponderId));
        // Andere Transponder-ID -> anderer Schluessel
        assertTrue(Telemetry.decryptCoordinate(encrypted, 43) != coordinate || encrypted == coordinate);
    }

    @Test
    @DisplayName("Frequenz-Kanaele: drei distinkte Register-IDs im Mod-Namespace")
    void frequencyChannelsAreRegistered() {
        assertEquals("lit_spaceships", Telemetry.Frequencies.DISTRESS_CALL.getNamespace());
        assertTrue(Telemetry.Frequencies.DISTRESS_CALL.getPath().contains("distress"));
        assertTrue(Telemetry.Frequencies.RESEARCH_BEACON.getPath().contains("research"));
        assertTrue(Telemetry.Frequencies.ANOMALOUS_RELIC.getPath().contains("anomalous"));
    }

    @Test
    @DisplayName("TelemetryData Codec: Feld-Roundtrip via generiertem Codec")
    @SuppressWarnings("unchecked")
    void telemetryDataCodecRoundTrip() {
        var data = new ModDataComponents.TelemetryData(
                Telemetry.encryptCoordinate(123456789L, 7),
                Telemetry.Frequencies.RESEARCH_BEACON.toString(), 7);

        var encoded = ModDataComponents.TelemetryData.CODEC.encodeStart(
                net.minecraft.nbt.NbtOps.INSTANCE, data);
        assertTrue(encoded.result().isPresent(), "Encoding muss erfolgreich sein");

        var decoded = ModDataComponents.TelemetryData.CODEC.parse(
                net.minecraft.nbt.NbtOps.INSTANCE, encoded.result().get());
        assertTrue(decoded.result().isPresent(), "Decoding muss erfolgreich sein");
        assertEquals(data, decoded.result().get(), "Roundtrip muss identisch sein");
        assertEquals(123456789L,
                Telemetry.decryptCoordinate(decoded.result().get().encryptedCoordinate(), 7));
    }
}
