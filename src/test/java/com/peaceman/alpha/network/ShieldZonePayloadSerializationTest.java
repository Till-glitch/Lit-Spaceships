package com.peaceman.alpha.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ShieldZonePayloadSerializationTest {

    @Test
    @DisplayName("ShieldZoneStatePayload sollte 64-Bit Maske und 64-Byte Energie-Array serialisieren und verlustfrei dekodieren")
    void testSerializationAndBitmaskIntegrity() {
        UUID shipId = UUID.randomUUID();
        // 64-Bit Test-Maske mit alternierenden Zonen
        long activeMask = 0xAAAAAAAAAAAAAAAAL ^ 0x0F0F0F0F0F0F0F0FL;

        byte[] energies = new byte[64];
        for (int i = 0; i < 64; i++) {
            energies[i] = (byte) (i * 3);
        }

        ShieldZoneStatePayload payload = new ShieldZoneStatePayload(shipId, activeMask, energies);

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        ShieldZoneStatePayload.STREAM_CODEC.encode(buffer, payload);

        // Byte-Größen-Verifikation (~72-88 Bytes)
        int serializedBytes = buffer.readableBytes();
        assertTrue(serializedBytes < 100, "Paketgröße muss unter 100 Bytes liegen, war aber: " + serializedBytes);

        // Dekodierung
        ShieldZoneStatePayload decoded = ShieldZoneStatePayload.STREAM_CODEC.decode(buffer);

        assertEquals(shipId, decoded.shipId());
        assertEquals(activeMask, decoded.activeMask(), "Die 64-Bit Zonen-Bitmaske muss nach der Dekodierung bit-identisch sein");
        assertArrayEquals(energies, decoded.zoneEnergies(), "Das Energie-Array muss nach der Dekodierung byte-identisch sein");
    }

    @Test
    @DisplayName("64-Bit Bit-Operationen sollten einzelne Zonen 1 bis 64 präzise maskieren")
    void testBitmaskZoneExtraction() {
        long mask = 0L;

        // Schalte Zonen 1, 7, 32, 64 ein
        mask |= (1L << (1 - 1));   // Zone 1 -> Bit 0
        mask |= (1L << (7 - 1));   // Zone 7 -> Bit 6
        mask |= (1L << (32 - 1));  // Zone 32 -> Bit 31
        mask |= (1L << (64 - 1));  // Zone 64 -> Bit 63

        // Prüfe Zonen
        assertTrue((mask & (1L << 0)) != 0, "Zone 1 aktiv");
        assertTrue((mask & (1L << 6)) != 0, "Zone 7 aktiv");
        assertTrue((mask & (1L << 31)) != 0, "Zone 32 aktiv");
        assertTrue((mask & (1L << 63)) != 0, "Zone 64 aktiv");

        assertFalse((mask & (1L << 1)) != 0, "Zone 2 inaktiv");
        assertFalse((mask & (1L << 30)) != 0, "Zone 31 inaktiv");
        assertFalse((mask & (1L << 62)) != 0, "Zone 63 inaktiv");
    }
}
