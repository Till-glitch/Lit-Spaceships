package com.lit.spaceships;

import com.lit.spaceships.network.ShipMovementRequestPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ShipMovementRequestPayloadTest {

    @Test
    public void testSerializationAndDeserialization() {
        // Arrange
        UUID shipId = UUID.randomUUID();
        float forward = 1.0f;
        float left = -0.5f;
        float up = 0.5f;
        ShipMovementRequestPayload originalPayload = new ShipMovementRequestPayload(shipId, forward, left, up);

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        // Act
        ShipMovementRequestPayload.STREAM_CODEC.encode(buffer, originalPayload);
        ShipMovementRequestPayload decodedPayload = ShipMovementRequestPayload.STREAM_CODEC.decode(buffer);

        // Assert
        assertNotNull(decodedPayload);
        assertEquals(shipId, decodedPayload.shipId());
        assertEquals(forward, decodedPayload.impulseForward());
        assertEquals(left, decodedPayload.impulseLeft());
        assertEquals(up, decodedPayload.impulseUp());
    }
}
