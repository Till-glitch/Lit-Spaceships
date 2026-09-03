package com.lit.spaceships.client.render;

import com.lit.spaceships.block.entity.AbstractLaserNodeBlockEntity;
import com.lit.spaceships.entity.TurretSeatEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class TurretCrosshairRenderer {

    private static final ResourceLocation CROSSHAIR_SPRITE = ResourceLocation.withDefaultNamespace("hud/crosshair");
    
    public static void render(GuiGraphics guiGraphics, net.minecraft.client.DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Entity vehicle = mc.player.getVehicle();
        if (!(vehicle instanceof TurretSeatEntity turretSeat)) return;

        BlockPos weaponPos = turretSeat.getWeaponPos();
        if (weaponPos == null) return;

        if (!(mc.level.getBlockEntity(weaponPos) instanceof AbstractLaserNodeBlockEntity laserBE)) return;

        boolean isAimLocked = laserBE.isAimLocked();

        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();
        int centerX = width / 2;
        int centerY = height / 2;

        guiGraphics.pose().pushPose();

        if (isAimLocked) {
            // Draw locked crosshair (Rot)
            guiGraphics.setColor(1.0f, 0.2f, 0.2f, 1.0f);
            guiGraphics.blitSprite(CROSSHAIR_SPRITE, centerX - 7, centerY - 7, 15, 15);
            guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            
            // Status-Text
            guiGraphics.drawString(mc.font, "§c[LOCKED]", centerX + 12, centerY - 4, 0xFF5555, true);
            guiGraphics.drawString(mc.font, String.format("§7Yaw: %.1f° Pitch: %.1f°", laserBE.getTargetYaw(), laserBE.getTargetPitch()), centerX + 12, centerY + 6, 0xAAAAAA, true);
        } else {
            // Active Freelook (Cyan)
            guiGraphics.setColor(0.2f, 0.8f, 1.0f, 1.0f);
            guiGraphics.blitSprite(CROSSHAIR_SPRITE, centerX - 7, centerY - 7, 15, 15);
            guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            
            // Status-Text
            guiGraphics.drawString(mc.font, "§b[FREELOOK]", centerX + 12, centerY - 4, 0x55FFFF, true);
        }

        guiGraphics.pose().popPose();
    }
}
