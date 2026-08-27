package com.peaceman.alpha.client.screen;

import com.peaceman.alpha.menu.SpaceshipReactorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class SpaceshipReactorScreen extends AbstractContainerScreen<SpaceshipReactorMenu> {

    public SpaceshipReactorScreen(SpaceshipReactorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 80; // Kleineres Fenster
        // Keine Label-Y-Hacks mehr nötig!
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick); // Dunkelt die Spielwelt ab
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        // renderTooltip komplett entfernt
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int leftPos = this.leftPos;
        int topPos = this.topPos;

        // Hintergrund-Rahmen
        guiGraphics.fill(leftPos, topPos, leftPos + this.imageWidth, topPos + this.imageHeight, 0xFFC6C6C6);
        guiGraphics.fill(leftPos + 2, topPos + 2, leftPos + this.imageWidth - 2, topPos + this.imageHeight - 2, 0xFF333333);
    }

    // HIER kommt jetzt dein Vordergrund rein (relative Koordinaten!)
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Absichtlich KEIN super.renderLabels(), das killt den Standard-Text und das Inventar-Label

        int currentEnergy = this.menu.getCurrentEnergy();
        int maxEnergy = this.menu.getMaxEnergy();

        // Optional: Titel schön zentriert in Weiß ganz oben
        guiGraphics.drawCenteredString(this.font, this.title, this.imageWidth / 2, 6, 0xFFFFFF);

        // Energie-Text (Kein topPos mehr nötig, 25 ist relativ zur GUI-Kante)
        Component energyText = Component.translatable(
                com.peaceman.alpha.registry.ModI18n.Screen.REACTOR_ENERGY,
                String.format("%,d", currentEnergy)
        );
        guiGraphics.drawCenteredString(this.font, energyText, this.imageWidth / 2, 25, 0xFFFFFF);

        // --- LADEBALKEN ---
        int barWidth = 120;
        int barHeight = 12;
        int startX = (this.imageWidth - barWidth) / 2;
        int startY = 40;

        // Balken Hintergrund
        guiGraphics.fill(startX, startY, startX + barWidth, startY + barHeight, 0xFF000000);

        if (maxEnergy > 0) {
            float fillPercentage = (float) currentEnergy / maxEnergy;
            int filledWidth = (int) (fillPercentage * barWidth);
            guiGraphics.fill(startX + 1, startY + 1, startX + filledWidth, startY + barHeight - 1, 0xFF00FF00);
        }
    }
}