package com.peaceman.alpha.client.screen;

import com.peaceman.alpha.client.render.ShipHighlightRenderer;
import com.peaceman.alpha.network.ShipActionPayload.ActionType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.UUID;

public class SpaceshipControlScreen extends AbstractSpaceshipScreen {

    public SpaceshipControlScreen(UUID shipId, BlockPos pos) {
        super(Component.literal("Raumschiff Steuerung"), pos);
        this.shipId = shipId;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int btnWidth = 140;
        int btnHeight = 20;
        int btnLeft = centerX - (btnWidth / 2);

        // 1. Schiff erstellen
        this.addRenderableWidget(Button.builder(Component.literal("Schiff erstellen"), button -> {
            sendShipAction(ActionType.CREATE);
        }).bounds(btnLeft, centerY - 45, btnWidth, btnHeight).build());

        // 2. Struktur aktualisieren
        this.addRenderableWidget(Button.builder(Component.literal("Struktur updaten"), button -> {
            sendShipAction(ActionType.UPDATE_BLOCKS);
        }).bounds(btnLeft, centerY - 15, btnWidth, btnHeight).build());

        // 3. Schiff auflösen
        this.addRenderableWidget(Button.builder(Component.literal("Schiff auflösen"), button -> {
            sendShipAction(ActionType.DELETE_SHIP);
        }).bounds(btnLeft, centerY + 15, btnWidth, btnHeight).build());

        // 4. Markierung An/Aus (rein Client-seitig)
        this.addRenderableWidget(Button.builder(Component.literal("Markierung An/Aus"), button -> {
            if (this.minecraft != null && this.minecraft.level != null) {
                ShipHighlightRenderer.toggleHighlight(this.minecraft.level, this.blockPos);
            }
        }).bounds(btnLeft, centerY + 45, btnWidth, btnHeight).build());

        // 5. Schild An/Aus (Sendet Action an Server)
        this.addRenderableWidget(Button.builder(Component.literal("Schild An/Aus"), button -> {
            sendShipAction(ActionType.TOGGLE_SHIELD);
        }).bounds(btnLeft, centerY + 70, btnWidth, btnHeight).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 75, 16777215);
    }
}