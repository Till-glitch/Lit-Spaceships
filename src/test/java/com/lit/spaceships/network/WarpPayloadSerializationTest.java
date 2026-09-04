package com.lit.spaceships.network;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit-Tests für die symmetrische Serialisierung und Deserialisierung
 * der Warp-Engine Netzwerk-Payloads.
 */
class WarpPayloadSerializationTest {

    @Test
    @DisplayName("WarpActionPayload START_COUNTDOWN serialisiert und deserialisiert fehlerfrei")
    void testWarpActionPayload_StartCountdown() {
        BlockPos enginePos = new BlockPos(42, 64, -108);
        WarpActionPayload original = new WarpActionPayload(enginePos, WarpActionPayload.Action.START_COUNTDOWN);

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        WarpActionPayload.STREAM_CODEC.encode(buf, original);
        WarpActionPayload decoded = WarpActionPayload.STREAM_CODEC.decode(buf);

        assertEquals(original.pos(), decoded.pos());
        assertEquals(original.action(), decoded.action());
        assertEquals(WarpActionPayload.Action.START_COUNTDOWN, decoded.action());
    }

    @Test
    @DisplayName("WarpActionPayload ABORT_COUNTDOWN serialisiert und deserialisiert fehlerfrei")
    void testWarpActionPayload_AbortCountdown() {
        BlockPos enginePos = new BlockPos(-500, 120, 2048);
        WarpActionPayload original = new WarpActionPayload(enginePos, WarpActionPayload.Action.ABORT_COUNTDOWN);

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        WarpActionPayload.STREAM_CODEC.encode(buf, original);
        WarpActionPayload decoded = WarpActionPayload.STREAM_CODEC.decode(buf);

        assertEquals(original.pos(), decoded.pos());
        assertEquals(original.action(), decoded.action());
        assertEquals(WarpActionPayload.Action.ABORT_COUNTDOWN, decoded.action());
    }

    @Test
    @DisplayName("WarpStateSyncPayload serialisiert und deserialisiert alle 8 Felder bit- und datenidentisch")
    void testWarpStateSyncPayload_FullCodec() {
        BlockPos enginePos = new BlockPos(1024, 70, -2048);
        WarpStateSyncPayload original = new WarpStateSyncPayload(
                enginePos,
                100000,
                100000,
                142,
                0,
                true,
                true,
                true
        );

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        WarpStateSyncPayload.STREAM_CODEC.encode(buf, original);
        WarpStateSyncPayload decoded = WarpStateSyncPayload.STREAM_CODEC.decode(buf);

        assertEquals(original.pos(), decoded.pos());
        assertEquals(original.energy(), decoded.energy());
        assertEquals(original.maxEnergy(), decoded.maxEnergy());
        assertEquals(original.countdownTicks(), decoded.countdownTicks());
        assertEquals(original.cooldownRemainingTicks(), decoded.cooldownRemainingTicks());
        assertEquals(original.isCountingDown(), decoded.isCountingDown());
        assertEquals(original.isLinked(), decoded.isLinked());
        assertEquals(original.targetIsSpace(), decoded.targetIsSpace());
    }

    @Test
    @DisplayName("WarpStateSyncPayload verarbeitet Cooldown- und Inaktiv-Status korrekt")
    void testWarpStateSyncPayload_CooldownState() {
        BlockPos enginePos = new BlockPos(0, 0, 0);
        WarpStateSyncPayload original = new WarpStateSyncPayload(
                enginePos,
                25000,
                100000,
                0,
                850,
                false,
                true,
                false
        );

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        WarpStateSyncPayload.STREAM_CODEC.encode(buf, original);
        WarpStateSyncPayload decoded = WarpStateSyncPayload.STREAM_CODEC.decode(buf);

        assertEquals(original.pos(), decoded.pos());
        assertEquals(original.energy(), decoded.energy());
        assertEquals(original.maxEnergy(), decoded.maxEnergy());
        assertEquals(original.countdownTicks(), decoded.countdownTicks());
        assertEquals(original.cooldownRemainingTicks(), decoded.cooldownRemainingTicks());
        assertEquals(original.isCountingDown(), decoded.isCountingDown());
        assertEquals(original.isLinked(), decoded.isLinked());
        assertEquals(original.targetIsSpace(), decoded.targetIsSpace());
    }
}
