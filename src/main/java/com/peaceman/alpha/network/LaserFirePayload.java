package com.peaceman.alpha.network;

import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.ship.combat.LaserWeaponTier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Überträgt einen diskreten Laser-Feuerstoß (Pulse-Laser) an Clients zur visuellen Darstellung.
 */
public record LaserFirePayload(
        UUID shooterShipId,
        Vec3 startPos,
        Vec3 endPos,
        LaserWeaponTier tier
) implements CustomPacketPayload {

    public static final Type<LaserFirePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Alpha.MODID, "laser_fire"));

    public static final StreamCodec<FriendlyByteBuf, LaserFirePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeUUID(payload.shooterShipId());
                buf.writeDouble(payload.startPos().x());
                buf.writeDouble(payload.startPos().y());
                buf.writeDouble(payload.startPos().z());
                buf.writeDouble(payload.endPos().x());
                buf.writeDouble(payload.endPos().y());
                buf.writeDouble(payload.endPos().z());
                buf.writeEnum(payload.tier());
            },
            buf -> {
                UUID id = buf.readUUID();
                Vec3 start = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
                Vec3 end = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
                LaserWeaponTier tier = buf.readEnum(LaserWeaponTier.class);
                return new LaserFirePayload(id, start, end, tier);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
