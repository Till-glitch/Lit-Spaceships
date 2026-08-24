package com.peaceman.alpha.client.render;

import com.peaceman.alpha.block.entity.AbstractLaserNodeBlockEntity;
import com.peaceman.alpha.ship.combat.LaserWeaponTier;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class LaserNodeRenderStateTest {

    @Test
    @DisplayName("LaserNodeRenderState extrahiert interpolierte Winkel und Waffen-Tier fehlerfrei")
    void testExtractRenderState() {
        AbstractLaserNodeBlockEntity mockBe = Mockito.mock(AbstractLaserNodeBlockEntity.class);
        when(mockBe.getPrevTargetYaw()).thenReturn(0.0f);
        when(mockBe.getTargetYaw()).thenReturn(90.0f);
        when(mockBe.getPrevTargetPitch()).thenReturn(-10.0f);
        when(mockBe.getTargetPitch()).thenReturn(30.0f);
        when(mockBe.getFacing()).thenReturn(Direction.NORTH);
        when(mockBe.getTier()).thenReturn(LaserWeaponTier.HEAVY_BEAM);

        // Bei partialTick = 0.5f -> Yaw = 45.0f, Pitch = 10.0f
        LaserNodeRenderState state = LaserNodeRenderState.extract(mockBe, 0.5f);

        assertNotNull(state);
        assertEquals(Direction.NORTH, state.getFacing());
        assertEquals(45.0f, state.getYaw(), 0.001f);
        assertEquals(10.0f, state.getPitch(), 0.001f);
        assertEquals(LaserWeaponTier.HEAVY_BEAM, state.getTier());
    }

    @ParameterizedTest
    @EnumSource(Direction.class)
    @DisplayName("LaserNodeRenderState unterstützt alle 6 Montage-Richtungen (FACING)")
    void testExtractRenderState_AllDirections(Direction direction) {
        AbstractLaserNodeBlockEntity mockBe = Mockito.mock(AbstractLaserNodeBlockEntity.class);
        when(mockBe.getPrevTargetYaw()).thenReturn(180.0f);
        when(mockBe.getTargetYaw()).thenReturn(180.0f);
        when(mockBe.getPrevTargetPitch()).thenReturn(0.0f);
        when(mockBe.getTargetPitch()).thenReturn(0.0f);
        when(mockBe.getFacing()).thenReturn(direction);
        when(mockBe.getTier()).thenReturn(LaserWeaponTier.PULSE_LASER);

        LaserNodeRenderState state = LaserNodeRenderState.extract(mockBe, 1.0f);

        assertEquals(direction, state.getFacing());
        assertNotNull(state.getFacing().getRotation(), "Rotation Quaternion für Direction darf nicht null sein");
    }

    @Test
    @DisplayName("LaserNodeRenderState behandelt 180-Grad Winkel-Wrap bei Interpolation deterministisch")
    void testExtractRenderState_AngleWrap() {
        AbstractLaserNodeBlockEntity mockBe = Mockito.mock(AbstractLaserNodeBlockEntity.class);
        when(mockBe.getPrevTargetYaw()).thenReturn(170.0f);
        when(mockBe.getTargetYaw()).thenReturn(-170.0f); // 20 Grad Drehung über den 180°-Wrap
        when(mockBe.getPrevTargetPitch()).thenReturn(0.0f);
        when(mockBe.getTargetPitch()).thenReturn(0.0f);
        when(mockBe.getFacing()).thenReturn(Direction.UP);
        when(mockBe.getTier()).thenReturn(LaserWeaponTier.MINING_LASER);

        LaserNodeRenderState state = LaserNodeRenderState.extract(mockBe, 0.5f);

        // Der kürzeste Interpolations-Schritt zwischen 170° und -170° (=190°) ist 180°
        assertEquals(180.0f, state.getYaw(), 0.001f);
        assertEquals(LaserWeaponTier.MINING_LASER, state.getTier());
    }
}
