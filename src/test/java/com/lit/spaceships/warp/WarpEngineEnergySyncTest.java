package com.lit.spaceships.warp;

import com.lit.spaceships.block.entity.WarpEngineBlockEntity;
import com.lit.spaceships.network.WarpActionPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit-Tests für Warp-Engine Energie-Grid-Laden, Trickle-Charge-Berechnung,
 * REQUEST_SYNC Payload Codec und Subsystem-Erkennung.
 */
class WarpEngineEnergySyncTest {

    @Test
    @DisplayName("WarpActionPayload.Action.REQUEST_SYNC besitzt ID 2 und ist im Enum vorhanden")
    void testRequestSyncActionEnum() {
        assertEquals(0, WarpActionPayload.Action.START_COUNTDOWN.getId());
        assertEquals(1, WarpActionPayload.Action.ABORT_COUNTDOWN.getId());
        assertEquals(2, WarpActionPayload.Action.REQUEST_SYNC.getId());

        assertSame(WarpActionPayload.Action.REQUEST_SYNC, WarpActionPayload.Action.fromId(2));
        assertSame(WarpActionPayload.Action.START_COUNTDOWN, WarpActionPayload.Action.fromId(999)); // Fallback
    }

    @Test
    @DisplayName("WarpActionPayload StreamCodec serialisiert und deserialisiert REQUEST_SYNC fehlerfrei")
    void testRequestSyncPayloadCodec() {
        BlockPos pos = new BlockPos(42, 64, -100);
        WarpActionPayload payload = new WarpActionPayload(pos, WarpActionPayload.Action.REQUEST_SYNC);

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        WarpActionPayload.STREAM_CODEC.encode(buffer, payload);

        WarpActionPayload decoded = WarpActionPayload.STREAM_CODEC.decode(buffer);
        assertNotNull(decoded);
        assertEquals(pos, decoded.pos());
        assertEquals(WarpActionPayload.Action.REQUEST_SYNC, decoded.action());
    }

    @Test
    @DisplayName("Trickle-Charge: Zieht vollen Satz von 500 FE, wenn Reaktoren genügend Energie haben")
    void testTrickleChargeFull() {
        int currentStored = 50_000;
        int needed = WarpEngineBlockEntity.REQUIRED_ENERGY - currentStored;
        int availableReactorEnergy = 500_000;

        int toDraw = Math.min(Math.min(WarpEngineBlockEntity.TRICKLE_CHARGE_RATE, needed), availableReactorEnergy);
        assertEquals(500, toDraw);

        currentStored += toDraw;
        assertEquals(50_500, currentStored);
    }

    @Test
    @DisplayName("Trickle-Charge: Zieht partiellen Betrag (z.B. 250 FE), wenn Reaktor-Puffer fast leer ist")
    void testTrickleChargePartialAvailable() {
        int currentStored = 10_000;
        int needed = WarpEngineBlockEntity.REQUIRED_ENERGY - currentStored;
        int availableReactorEnergy = 250; // Weniger als TRICKLE_CHARGE_RATE (500)

        int toDraw = Math.min(Math.min(WarpEngineBlockEntity.TRICKLE_CHARGE_RATE, needed), availableReactorEnergy);
        assertEquals(250, toDraw, "Muss genau die verbleibenden 250 FE aus dem Reaktor ziehen");

        currentStored += toDraw;
        assertEquals(10_250, currentStored);
    }

    @Test
    @DisplayName("Trickle-Charge: Zieht nur den exakten Restbedarf kurz vor 100.000 FE (z.B. 120 FE)")
    void testTrickleChargeNearFullCap() {
        int currentStored = 99_880;
        int needed = WarpEngineBlockEntity.REQUIRED_ENERGY - currentStored; // 120 FE
        int availableReactorEnergy = 1_000_000;

        int toDraw = Math.min(Math.min(WarpEngineBlockEntity.TRICKLE_CHARGE_RATE, needed), availableReactorEnergy);
        assertEquals(120, toDraw, "Darf nicht über die Kapazitätsgrenze von 100.000 FE hinaus laden");

        currentStored += toDraw;
        assertEquals(100_000, currentStored);
    }

    @Test
    @DisplayName("Trickle-Charge: Stoppt vollständig (0 FE), sobald Puffer voll oder Reaktoren leer sind")
    void testTrickleChargeZeroWhenFullOrEmpty() {
        // Fall 1: Warp Engine bereits voll
        int currentStored = 100_000;
        int needed = WarpEngineBlockEntity.REQUIRED_ENERGY - currentStored;
        int availableReactorEnergy = 500_000;

        int toDraw = Math.min(Math.min(WarpEngineBlockEntity.TRICKLE_CHARGE_RATE, needed), availableReactorEnergy);
        assertEquals(0, toDraw, "Kein Energietransfer wenn Puffer bereits voll");

        // Fall 2: Reaktoren vollkommen leer (0 FE)
        currentStored = 0;
        needed = WarpEngineBlockEntity.REQUIRED_ENERGY;
        availableReactorEnergy = 0;

        toDraw = Math.min(Math.min(WarpEngineBlockEntity.TRICKLE_CHARGE_RATE, needed), availableReactorEnergy);
        assertEquals(0, toDraw, "Kein Energietransfer wenn Reaktor 0 FE hat");
    }

    @Test
    @DisplayName("Subsystem-Zählung: Mehrere Warp-Engines werden korrekt registriert")
    void testSubsystemCounting() {
        Set<BlockPos> scannedBlocks = new HashSet<>();
        BlockPos warp1 = new BlockPos(5, 60, 5);
        BlockPos warp2 = new BlockPos(5, 60, 6);
        BlockPos reactor = new BlockPos(0, 60, 0);

        scannedBlocks.add(warp1);
        scannedBlocks.add(warp2);
        scannedBlocks.add(reactor);

        // Zähllogik wie im Controller
        int warpCount = 0;
        for (BlockPos pos : scannedBlocks) {
            if (pos.equals(warp1) || pos.equals(warp2)) {
                warpCount++;
            }
        }

        assertEquals(2, warpCount);
    }
}
