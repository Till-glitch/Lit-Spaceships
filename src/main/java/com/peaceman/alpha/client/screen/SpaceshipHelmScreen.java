package com.peaceman.alpha.client.screen;

import com.peaceman.alpha.helper.TickScheduler;
import com.peaceman.alpha.network.ShipActionPayload.ActionType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;


public class SpaceshipHelmScreen extends AbstractSpaceshipScreen {

    private EditBox distanceInput;
    private EditBox homeNameInput;

    // Der Konstruktor braucht nur noch die BlockPos.
    // Titel und Pos werden an die abstrakte Klasse übergeben!
    public SpaceshipHelmScreen(net.minecraft.core.BlockPos pos) {
        super(Component.literal("Raumschiff Navigation"), pos);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init(); // WICHTIG: Holt im Hintergrund direkt die aktuellste UUID über die Basisklasse!

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        java.util.function.Supplier<Integer> getDist = () -> {
            try {
                return Integer.parseInt(this.distanceInput.getValue());
            } catch (NumberFormatException e) {
                return 1;
            }
        };

        // --- LINKE SEITE: FLUG-STEUERUNG ---
        int leftCol = centerX - 120;

        this.distanceInput = new EditBox(this.font, leftCol, centerY - 20, 80, 20, Component.literal("Distanz"));
        this.distanceInput.setValue("5");
        this.addRenderableWidget(this.distanceInput);

        this.addRenderableWidget(Button.builder(Component.literal("Hoch"), button -> {
            sendShipAction(ActionType.MOVE_UP, getDist.get(), "");
        }).bounds(leftCol, centerY + 5, 80, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Runter"), button -> {
            TickScheduler.runAfterSeconds(5, () -> {
                sendShipAction(ActionType.MOVE_DOWN, getDist.get(), "");
            });
        }).bounds(leftCol, centerY + 30, 80, 20).build());


        // --- MITTE: RICHTUNGS-STEUERUNG (WASD) ---
        this.addRenderableWidget(Button.builder(Component.literal("W"), button -> {
            sendShipAction(ActionType.MOVE_FORWARD, getDist.get(), "");
        }).bounds(centerX - 17, centerY - 20, 35, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("A"), button -> {
            sendShipAction(ActionType.MOVE_LEFT, getDist.get(), "");
        }).bounds(centerX - 57, centerY + 5, 35, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("S"), button -> {
            sendShipAction(ActionType.MOVE_BACKWARD, getDist.get(), "");
        }).bounds(centerX - 17, centerY + 5, 35, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("D"), button -> {
            sendShipAction(ActionType.MOVE_RIGHT, getDist.get(), "");
        }).bounds(centerX + 23, centerY + 5, 35, 20).build());


        // --- RECHTE SEITE: NAVIGATION ---
        int rightCol = centerX + 80;

        this.homeNameInput = new EditBox(this.font, rightCol, centerY - 20, 80, 20, Component.literal("Wegpunkt"));
        this.homeNameInput.setValue("Basis");
        this.addRenderableWidget(this.homeNameInput);

        this.addRenderableWidget(Button.builder(Component.literal("Speichern"), button -> {
            sendShipAction(ActionType.SAVE_HOME, 0, this.homeNameInput.getValue());
        }).bounds(rightCol, centerY + 5, 80, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Anfliegen"), button -> {
            sendShipAction(ActionType.TP_HOME, 0, this.homeNameInput.getValue());
            this.minecraft.setScreen(null); // GUI nach dem Klick schließen
        }).bounds(rightCol, centerY + 30, 80, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawCenteredString(this.font, this.title, centerX, centerY - 60, 16777215);
        guiGraphics.drawCenteredString(this.font, Component.literal("Manuell"), centerX - 80, centerY - 35, 10526880);
        guiGraphics.drawCenteredString(this.font, Component.literal("Wegpunkte"), centerX + 120, centerY - 35, 10526880);
    }
}