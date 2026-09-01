package com.peaceman.alpha.client.screen.hud;

import com.peaceman.alpha.client.input.SpaceshipClientInputHandler;
import com.peaceman.alpha.client.state.ClientShipState;
import com.peaceman.alpha.client.state.ClientShipStateProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;

public class SpaceshipHelmHudOverlay implements LayeredDraw.Layer {

    @Override
    public void render(GuiGraphics guiGraphics, net.minecraft.client.DeltaTracker deltaTracker) {
        if (SpaceshipClientInputHandler.activeHelmShipId == null) return;

        ClientShipState shipState = ClientShipStateProvider.getInstance().getShip(SpaceshipClientInputHandler.activeHelmShipId);
        if (shipState == null) return;

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();

        // Zentrum Unten
        int startX = width / 2 - 50;
        int startY = height - 70;

        // HUD Background
        guiGraphics.fill(startX - 10, startY - 10, startX + 110, startY + 40, 0x55000000);

        // Pilot Info
        guiGraphics.drawString(font, Component.translatable(com.peaceman.alpha.registry.ModI18n.Screen.HUD_HELM_HEADER).withStyle(net.minecraft.ChatFormatting.AQUA), startX, startY, 0xFFFFFF);

        long currentTick = mc.level != null ? mc.level.getGameTime() : 0;
        if (shipState.isMovementOnCooldown(currentTick)) {
            long remainingTicks = shipState.getMovementCooldownDisplay(currentTick);
            guiGraphics.drawString(font, Component.translatable(com.peaceman.alpha.registry.ModI18n.Screen.HUD_HELM_WARP_COOLDOWN, (remainingTicks / 20.0f)).withStyle(net.minecraft.ChatFormatting.RED), startX, startY + 12, 0xFFFFFF);
        } else {
            guiGraphics.drawString(font, Component.translatable(com.peaceman.alpha.registry.ModI18n.Screen.HUD_HELM_READY).withStyle(net.minecraft.ChatFormatting.GREEN), startX, startY + 12, 0xFFFFFF);
        }

        // Home Waypoint Info & Controls
        guiGraphics.drawString(font, Component.translatable(com.peaceman.alpha.registry.ModI18n.Screen.HUD_HELM_CONTROLS).withStyle(net.minecraft.ChatFormatting.GRAY), startX - 35, startY + 24, 0xFFFFFF);
    }
}
