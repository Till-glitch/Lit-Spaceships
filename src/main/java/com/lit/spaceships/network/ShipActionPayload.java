package com.lit.spaceships.network;

import com.lit.spaceships.LitSpaceships;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.UUID;

public record ShipActionPayload(
        Optional<UUID> shipId,
        BlockPos pos,
        ActionType actionType,
        int value,
        String targetName
) implements CustomPacketPayload {

    public static final Type<ShipActionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "ship_action"));

    public enum ActionType {
        CREATE(0),
        UPDATE_BLOCKS(1),
        DELETE_SHIP(2),
        TOGGLE_SHIELD(3),
        MOVE_UP(4),
        MOVE_DOWN(5),
        MOVE_FORWARD(6),
        MOVE_BACKWARD(7),
        MOVE_LEFT(8),
        MOVE_RIGHT(9),
        SAVE_HOME(10),
        TP_HOME(11),
        TOGGLE_SHIELD_ZONE(12),
        CYCLE_POWER_PRIORITY(13),
        SET_POWER_PRIORITY(14),
        ROTATE_CW(15),
        ROTATE_CCW(16);

        private final int id;

        ActionType(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public static ActionType fromId(int id) {
            for (ActionType type : values()) {
                if (type.id == id) {
                    return type;
                }
            }
            return CREATE;
        }
    }

    public static final StreamCodec<ByteBuf, ActionType> ACTION_TYPE_STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(ActionType::fromId, ActionType::getId);

    public static final StreamCodec<ByteBuf, ShipActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), ShipActionPayload::shipId,
            BlockPos.STREAM_CODEC, ShipActionPayload::pos,
            ACTION_TYPE_STREAM_CODEC, ShipActionPayload::actionType,
            ByteBufCodecs.VAR_INT, ShipActionPayload::value,
            ByteBufCodecs.STRING_UTF8, ShipActionPayload::targetName,
            ShipActionPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
