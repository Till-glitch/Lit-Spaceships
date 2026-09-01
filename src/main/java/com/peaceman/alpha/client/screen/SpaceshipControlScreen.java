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
        super(Component.translatable(com.peaceman.alpha.registry.ModI18n.Screen.CONTROL_TITLE), pos);
        this.shipId = shipId;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private Button shieldButton;

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int btnWidth = 140;
        int btnHeight = 20;
        int btnLeft = centerX - (btnWidth / 2);

        // 1. Schiff erstellen
        this.addRenderableWidget(Button.builder(Component.translatable(com.peaceman.alpha.registry.ModI18n.Screen.CONTROL_BTN_CREATE), button -> {
            sendShipAction(ActionType.CREATE);
        }).bounds(btnLeft, centerY - 60, btnWidth, btnHeight).build());

        // 2. Struktur aktualisieren
        this.addRenderableWidget(Button.builder(Component.translatable(com.peaceman.alpha.registry.ModI18n.Screen.CONTROL_BTN_UPDATE), button -> {
            sendShipAction(ActionType.UPDATE_BLOCKS);
        }).bounds(btnLeft, centerY - 35, btnWidth, btnHeight).build());

        // 3. Schiff auflösen
        this.addRenderableWidget(Button.builder(Component.translatable(com.peaceman.alpha.registry.ModI18n.Screen.CONTROL_BTN_DISASSEMBLE), button -> {
            sendShipAction(ActionType.DELETE_SHIP);
        }).bounds(btnLeft, centerY - 10, btnWidth, btnHeight).build());

        // 4. Markierung An/Aus (rein Client-seitig)
        this.addRenderableWidget(Button.builder(Component.translatable(com.peaceman.alpha.registry.ModI18n.Screen.CONTROL_BTN_HIGHLIGHT), button -> {
            if (this.minecraft != null && this.minecraft.level != null) {
                ShipHighlightRenderer.toggleHighlight(this.minecraft.level, this.blockPos);
            }
        }).bounds(btnLeft, centerY + 15, btnWidth, btnHeight).build());

        // 5. Schild An/Aus (Sendet Action an Server)
        this.shieldButton = Button.builder(Component.translatable(com.peaceman.alpha.registry.ModI18n.Screen.CONTROL_BTN_SHIELD), button -> {
            sendShipAction(ActionType.TOGGLE_SHIELD);
        }).bounds(btnLeft, centerY + 40, btnWidth, btnHeight).build();
        this.addRenderableWidget(this.shieldButton);

        // 6. Rotations-Buttons (90° CCW / 90° CW)
        int halfBtnWidth = (btnWidth - 4) / 2;
        this.addRenderableWidget(Button.builder(Component.translatable(com.peaceman.alpha.registry.ModI18n.Screen.CONTROL_BTN_ROTATE_CCW), button -> {
            sendShipAction(ActionType.ROTATE_CCW);
        }).bounds(btnLeft, centerY + 65, halfBtnWidth, btnHeight).build());

        this.addRenderableWidget(Button.builder(Component.translatable(com.peaceman.alpha.registry.ModI18n.Screen.CONTROL_BTN_ROTATE_CW), button -> {
            sendShipAction(ActionType.ROTATE_CW);
        }).bounds(btnLeft + halfBtnWidth + 4, centerY + 65, halfBtnWidth, btnHeight).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        // Cooldown- & Status-Prüfung für Schild-Button
        var clientState = getClientShipState();
        if (clientState != null && this.shieldButton != null) {
            long gameTime = getClientGameTime();
            long shieldCd = clientState.getShieldCooldownDisplay(gameTime);
            if (shieldCd > 0) {
                this.shieldButton.active = false;
                double seconds = shieldCd / 20.0;
                this.shieldButton.setMessage(Component.translatable(com.peaceman.alpha.registry.ModI18n.Screen.CONTROL_SHIELD_COOLDOWN, seconds));
                guiGraphics.drawCenteredString(this.font,
                        Component.translatable(com.peaceman.alpha.registry.ModI18n.Screen.CONTROL_SHIELD_COOLDOWN, seconds),
                        this.width / 2, this.height / 2 + 95, 0xFF5555);
            } else {
                this.shieldButton.active = true;
                this.shieldButton.setMessage(Component.translatable(clientState.isShieldActive() ? com.peaceman.alpha.registry.ModI18n.Screen.CONTROL_SHIELD_ACTIVE : com.peaceman.alpha.registry.ModI18n.Screen.CONTROL_SHIELD_INACTIVE));
            }
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 75, 16777215);
    }
}