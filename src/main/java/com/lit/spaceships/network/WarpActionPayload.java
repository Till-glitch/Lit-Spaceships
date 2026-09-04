package com.lit.spaceships.network;

import com.lit.spaceships.LitSpaceships;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record WarpActionPayload(
        BlockPos pos,
        Action action
) implements CustomPacketPayload {

    public static final Type<WarpActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "warp_action"));

    public enum Action {
        START_COUNTDOWN(0),
        ABORT_COUNTDOWN(1);

        private final int id;

        Action(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public static Action fromId(int id) {
            for (Action a : values()) {
                if (a.id == id) return a;
            }
            return START_COUNTDOWN;
        }
    }

    public static final StreamCodec<ByteBuf, Action> ACTION_STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(Action::fromId, Action::getId);

    public static final StreamCodec<ByteBuf, WarpActionPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, WarpActionPayload::pos,
            ACTION_STREAM_CODEC, WarpActionPayload::action,
            WarpActionPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
