package com.peaceman.alpha.client.render;

import com.peaceman.alpha.block.entity.AbstractLaserNodeBlockEntity;
import com.peaceman.alpha.entity.TurretSeatEntity;
import com.peaceman.alpha.ship.combat.aim.AimTransformMath;
import com.peaceman.alpha.ship.combat.aim.GimbalLimits;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class TurretCrosshairRenderer {

    private static final ResourceLocation CROSSHAIR_VALID = ResourceLocation.withDefaultNamespace("textures/gui/sprites/hud/crosshair.png");
    private static final ResourceLocation CROSSHAIR_INVALID = ResourceLocation.withDefaultNamespace("textures/gui/sprites/hud/crosshair_attack_indicator_full.png"); // Vanilla placeholder
    
    public static void render(GuiGraphics guiGraphics, net.minecraft.client.DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Entity vehicle = mc.player.getVehicle();
        if (!(vehicle instanceof TurretSeatEntity turretSeat)) return;

        BlockPos weaponPos = turretSeat.getWeaponPos();
        if (weaponPos == null) return;

        if (!(mc.level.getBlockEntity(weaponPos) instanceof AbstractLaserNodeBlockEntity laserBE)) return;

        // Vector math: Q_ship^-1 * V_world * Q_ship (done in AimTransformMath)
        // Here we just use camera angles to get world vector, then transform to local.
        float yaw = mc.player.getViewYRot(deltaTracker.getGameTimeDeltaPartialTick(true));
        float pitch = mc.player.getViewXRot(deltaTracker.getGameTimeDeltaPartialTick(true));
        
        Vec3 lookVec = AimTransformMath.calculateWorldLookVector(yaw, pitch);
        
        // Assume ship rotation is identity for now if not fetched, but ideally we fetch from ClientShipState.
        // For gimbal limits, the block entity might have `gimbalLimits.isWithinLimits(yaw, pitch)` but we need local yaw/pitch.
        // In this implementation we will just show how the crosshair color/texture modulates.
        
        // Fallback checks (e.g. if the crosshair should be red)
        boolean isAimLocked = false; 
        try {
            java.lang.reflect.Field lockedField = AbstractLaserNodeBlockEntity.class.getDeclaredField("isAimLocked");
            lockedField.setAccessible(true);
            isAimLocked = lockedField.getBoolean(laserBE);
        } catch (Exception e) {
            // Ignore reflection errors for now
        }

        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();
        int centerX = width / 2;
        int centerY = height / 2;

        guiGraphics.pose().pushPose();
        
        // Modulate texture based on Gimbal Limits or Aim Lock (mock check for now)
        // A true gimbal check requires the ship's current rotation from ClientShipState.
        boolean isWithinGimbal = true; // Placeholder for gimbal math

        if (isAimLocked) {
            // Draw locked crosshair (red)
            guiGraphics.setColor(1.0f, 0.0f, 0.0f, 1.0f);
            guiGraphics.blitSprite(CROSSHAIR_VALID, centerX - 7, centerY - 7, 15, 15);
            guiGraphics.drawString(mc.font, "LOCKED", centerX + 10, centerY - 4, 0xFF0000);
            guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        } else if (!isWithinGimbal) {
            // Draw invalid crosshair
            guiGraphics.setColor(0.5f, 0.5f, 0.5f, 1.0f);
            guiGraphics.blitSprite(CROSSHAIR_INVALID, centerX - 7, centerY - 7, 15, 15);
            guiGraphics.drawString(mc.font, "X", centerX - 4, centerY - 4, 0xFF0000);
            guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        } else {
            // Default crosshair (cyan)
            guiGraphics.setColor(0.0f, 1.0f, 1.0f, 1.0f);
            guiGraphics.blitSprite(CROSSHAIR_VALID, centerX - 7, centerY - 7, 15, 15);
            guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        }

        guiGraphics.pose().popPose();
    }
}
