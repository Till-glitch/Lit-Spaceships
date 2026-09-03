package com.peaceman.alpha.network;

import com.peaceman.alpha.Alpha;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Ephemeres Netzwerk-Event für Schildeinschläge (Kollisionen, Projektile, Explosionen).
 * Überträgt die Einschlagposition im Local-Space des Raumschiffs zur Shader-Berechnung.
 */
public record ShipImpactEventPayload(
        UUID shipId,
        Vec3 impactPos,
        float force
) implements CustomPacketPayload {

    public static final Type<ShipImpactEventPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Alpha.MODID, "ship_impact_event"));

    public static final StreamCodec<ByteBuf, ShipImpactEventPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, ShipImpactEventPayload::shipId,
            ByteBufCodecs.DOUBLE, p -> p.impactPos().x(),
            ByteBufCodecs.DOUBLE, p -> p.impactPos().y(),
            ByteBufCodecs.DOUBLE, p -> p.impactPos().z(),
            ByteBufCodecs.FLOAT, ShipImpactEventPayload::force,
            (uuid, x, y, z, force) -> new ShipImpactEventPayload(uuid, new Vec3(x, y, z), force)
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
