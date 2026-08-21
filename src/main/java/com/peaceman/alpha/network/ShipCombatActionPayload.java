package com.peaceman.alpha.network;

import com.peaceman.alpha.Alpha;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * Überträgt Kampf- und Waffenauslösebefehle vom Client an den Server.
 */
public record ShipCombatActionPayload(
        UUID shipId,
        CombatAction action
) implements CustomPacketPayload {

    public enum CombatAction {
        FIRE_PULSE,
        TOGGLE_HEAVY_BEAM,
        TOGGLE_MINING_LASER,
        FIRE_ALL
    }

    public static final Type<ShipCombatActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Alpha.MODID, "ship_combat_action"));

    public static final StreamCodec<FriendlyByteBuf, ShipCombatActionPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeUUID(payload.shipId());
                buf.writeEnum(payload.action());
            },
            buf -> new ShipCombatActionPayload(buf.readUUID(), buf.readEnum(CombatAction.class))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
