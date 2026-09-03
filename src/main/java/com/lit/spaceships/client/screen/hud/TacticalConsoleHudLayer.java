package com.lit.spaceships.client.screen.hud;

import com.lit.spaceships.client.state.ClientShipState;
import com.lit.spaceships.client.state.ClientShipStateProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;

import java.util.UUID;

public class TacticalConsoleHudLayer implements LayeredDraw.Layer {

    // Wird vom TurretClientInputHandler oder ähnlichem gesetzt, wenn man in einem Turm sitzt
    public static UUID activeTacticalShipId = null;

    @Override
    public void render(GuiGraphics guiGraphics, net.minecraft.client.DeltaTracker deltaTracker) {
        if (activeTacticalShipId == null) return;

        ClientShipState shipState = ClientShipStateProvider.getInstance().getShip(activeTacticalShipId);
        if (shipState == null) return;

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();

        // Rechts Unten - Schild & Cooldowns
        int startX = width - 120;
        int startY = height - 90;

        guiGraphics.fill(startX, startY, startX + 110, startY + 80, 0x55000000);
        guiGraphics.drawString(font, Component.translatable(com.lit.spaceships.registry.ModI18n.Screen.HUD_TACTICAL_HEADER).withStyle(net.minecraft.ChatFormatting.AQUA), startX + 5, startY + 5, 0xFFFFFF);

        long currentTick = mc.level != null ? mc.level.getGameTime() : 0;
        
        // Schild Status
        if (shipState.isShieldActive()) {
            if (shipState.getShieldEnergyPercentage() < 0.2f) {
                guiGraphics.drawString(font, Component.translatable(com.lit.spaceships.registry.ModI18n.Screen.HUD_TACTICAL_SHIELD_CRITICAL).withStyle(net.minecraft.ChatFormatting.RED), startX + 5, startY + 20, 0xFFFFFF);
            } else {
                guiGraphics.drawString(font, Component.translatable(com.lit.spaceships.registry.ModI18n.Screen.HUD_TACTICAL_SHIELD_ACTIVE).withStyle(net.minecraft.ChatFormatting.DARK_AQUA), startX + 5, startY + 20, 0xFFFFFF);
            }
        } else {
            if (shipState.isShieldOnCooldown(currentTick)) {
                long cd = shipState.getShieldCooldownDisplay(currentTick);
                String cdStr = String.format(java.util.Locale.ROOT, "%.1f", (cd / 20.0f));
                guiGraphics.drawString(font, Component.translatable(com.lit.spaceships.registry.ModI18n.Screen.HUD_TACTICAL_SHIELD_OFFLINE).withStyle(net.minecraft.ChatFormatting.RED), startX + 5, startY + 20, 0xFFFFFF);
                guiGraphics.drawString(font, Component.translatable(com.lit.spaceships.registry.ModI18n.Screen.HUD_TACTICAL_REBOOT, cdStr).withStyle(net.minecraft.ChatFormatting.RED), startX + 5, startY + 35, 0xFFFFFF);
            } else {
                guiGraphics.drawString(font, Component.translatable(com.lit.spaceships.registry.ModI18n.Screen.HUD_TACTICAL_SHIELD_DISABLED).withStyle(net.minecraft.ChatFormatting.DARK_RED), startX + 5, startY + 20, 0xFFFFFF);
            }
        }

        guiGraphics.drawString(font, Component.translatable(com.lit.spaceships.registry.ModI18n.Screen.HUD_TACTICAL_ENERGY, (int)(shipState.getShieldEnergyPercentage() * 100)), startX + 5, startY + 50, 0xFFFFFF);
        
        // Fadenkreuz wird durch TurretCrosshairRenderer gezeichnet
        com.lit.spaceships.client.render.TurretCrosshairRenderer.render(guiGraphics, deltaTracker);
    }
}
