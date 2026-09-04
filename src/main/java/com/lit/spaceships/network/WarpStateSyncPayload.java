package com.lit.spaceships.network;

import com.lit.spaceships.LitSpaceships;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record WarpStateSyncPayload(
        BlockPos pos,
        int energy,
        int maxEnergy,
        int countdownTicks,
        int cooldownRemainingTicks,
        boolean isCountingDown,
        boolean isLinked,
        boolean targetIsSpace
) implements CustomPacketPayload {

    public static final Type<WarpStateSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "warp_state_sync"));

    public static final StreamCodec<ByteBuf, WarpStateSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                BlockPos.STREAM_CODEC.encode(buf, payload.pos());
                ByteBufCodecs.VAR_INT.encode(buf, payload.energy());
                ByteBufCodecs.VAR_INT.encode(buf, payload.maxEnergy());
                ByteBufCodecs.VAR_INT.encode(buf, payload.countdownTicks());
                ByteBufCodecs.VAR_INT.encode(buf, payload.cooldownRemainingTicks());
                ByteBufCodecs.BOOL.encode(buf, payload.isCountingDown());
                ByteBufCodecs.BOOL.encode(buf, payload.isLinked());
                ByteBufCodecs.BOOL.encode(buf, payload.targetIsSpace());
            },
            buf -> new WarpStateSyncPayload(
                    BlockPos.STREAM_CODEC.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf)
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
