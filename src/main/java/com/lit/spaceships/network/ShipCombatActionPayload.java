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

/**
 * Überträgt Kampf- und Waffenauslösebefehle vom Client an den Server.
 */
public record ShipCombatActionPayload(
        Optional<UUID> shipId,
        CombatAction action,
        Optional<BlockPos> weaponPos
) implements CustomPacketPayload {

    public enum CombatAction {
        FIRE_PULSE(0),
        TOGGLE_HEAVY_BEAM(1),
        TOGGLE_MINING_LASER(2),
        FIRE_ALL(3),
        FIRE_SPECIFIC(4);

        private final int id;

        CombatAction(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public static CombatAction fromId(int id) {
            for (CombatAction a : values()) {
                if (a.id == id) return a;
            }
            return FIRE_ALL;
        }
    }

    public static final Type<ShipCombatActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LitSpaceships.MODID, "ship_combat_action"));

    public static final StreamCodec<ByteBuf, CombatAction> COMBAT_ACTION_STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(CombatAction::fromId, CombatAction::getId);

    public static final StreamCodec<ByteBuf, ShipCombatActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), ShipCombatActionPayload::shipId,
            COMBAT_ACTION_STREAM_CODEC, ShipCombatActionPayload::action,
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC), ShipCombatActionPayload::weaponPos,
            ShipCombatActionPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
