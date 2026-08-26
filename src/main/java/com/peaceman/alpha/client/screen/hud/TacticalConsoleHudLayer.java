package com.peaceman.alpha.client.screen.hud;

import com.peaceman.alpha.client.state.ClientShipState;
import com.peaceman.alpha.client.state.ClientShipStateProvider;
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
        guiGraphics.drawString(font, Component.literal("§b--- Tactical ---"), startX + 5, startY + 5, 0xFFFFFF);

        long currentTick = mc.level != null ? mc.level.getGameTime() : 0;
        
        // Schild Status
        if (shipState.isShieldActive()) {
            if (shipState.getShieldEnergyPercentage() < 0.2f) {
                guiGraphics.drawString(font, Component.literal("§cSchilde: Kritisch"), startX + 5, startY + 20, 0xFFFFFF);
            } else {
                guiGraphics.drawString(font, Component.literal("§3Schilde: Aktiv"), startX + 5, startY + 20, 0xFFFFFF);
            }
        } else {
            if (shipState.isShieldOnCooldown(currentTick)) {
                long cd = shipState.getShieldCooldownDisplay(currentTick);
                guiGraphics.drawString(font, Component.literal("§cSchilde: Offline"), startX + 5, startY + 20, 0xFFFFFF);
                guiGraphics.drawString(font, Component.literal("§cReboot: " + (cd / 20.0f) + "s"), startX + 5, startY + 35, 0xFFFFFF);
            } else {
                guiGraphics.drawString(font, Component.literal("§4Schilde: Deaktiviert"), startX + 5, startY + 20, 0xFFFFFF);
            }
        }

        guiGraphics.drawString(font, Component.literal("Energie: " + (int)(shipState.getShieldEnergyPercentage() * 100) + "%"), startX + 5, startY + 50, 0xFFFFFF);
        
        // Fadenkreuz wird durch TurretCrosshairRenderer gezeichnet
        com.peaceman.alpha.client.render.TurretCrosshairRenderer.render(guiGraphics, deltaTracker);
    }
}
