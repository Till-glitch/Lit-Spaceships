package com.peaceman.alpha.client.screen;

import com.peaceman.alpha.menu.SpaceshipShieldMenu;
import com.peaceman.alpha.network.ShipActionPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Optional;
import java.util.UUID;

public class SpaceshipShieldScreen extends AbstractContainerScreen<SpaceshipShieldMenu> {

    public SpaceshipShieldScreen(SpaceshipShieldMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 100;
    }

    @Override
    protected void init() {
        super.init();
        
        int startX = (this.width - this.imageWidth) / 2;
        int startY = (this.height - this.imageHeight) / 2;

        this.addRenderableWidget(Button.builder(Component.translatable(com.peaceman.alpha.registry.ModI18n.Screen.SHIELD_TOGGLE), (button) -> {
            UUID shipId = this.menu.getBlockEntity().getShipId();
            if (shipId != null) {
                PacketDistributor.sendToServer(new ShipActionPayload(
                        Optional.of(shipId),
                        this.menu.getBlockEntity().getBlockPos(),
                        ShipActionPayload.ActionType.TOGGLE_SHIELD_ZONE,
                        0,
                        ""
                ));
            }
        }).bounds(startX + (this.imageWidth - 100) / 2, startY + 70, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int leftPos = this.leftPos;
        int topPos = this.topPos;

        // Background
        guiGraphics.fill(leftPos, topPos, leftPos + this.imageWidth, topPos + this.imageHeight, 0xFFC6C6C6);
        guiGraphics.fill(leftPos + 2, topPos + 2, leftPos + this.imageWidth - 2, topPos + this.imageHeight - 2, 0xFF333333);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int currentEnergy = this.menu.getCurrentEnergy();
        int maxEnergy = this.menu.getMaxEnergy();
        int deficit = this.menu.getEnergyDeficit();
        boolean isActive = this.menu.isShieldActive();

        // Title
        guiGraphics.drawCenteredString(this.font, this.title, this.imageWidth / 2, 6, 0xFFFFFF);

        // Status Text
        Component statusText = Component.translatable(
                com.peaceman.alpha.registry.ModI18n.Screen.SHIELD_STATUS,
                Component.translatable(isActive ? com.peaceman.alpha.registry.ModI18n.Screen.SHIELD_ACTIVE : com.peaceman.alpha.registry.ModI18n.Screen.SHIELD_INACTIVE)
        );
        guiGraphics.drawCenteredString(this.font, statusText, this.imageWidth / 2, 20, isActive ? 0xFF00FF00 : 0xFFFF0000);

        // Energy / Deficit Text
        Component energyText = Component.translatable(
                com.peaceman.alpha.registry.ModI18n.Screen.SHIELD_ENERGY,
                String.format("%,d", currentEnergy),
                String.format("%,d", deficit)
        );
        guiGraphics.drawCenteredString(this.font, energyText, this.imageWidth / 2, 35, 0xFFFFFF);

        // --- PROGRESS BAR ---
        int barWidth = 120;
        int barHeight = 12;
        int startX = (this.imageWidth - barWidth) / 2;
        int startY = 50;

        // Bar background
        guiGraphics.fill(startX, startY, startX + barWidth, startY + barHeight, 0xFF000000);

        if (maxEnergy > 0) {
            float fillPercentage = (float) currentEnergy / maxEnergy;
            int filledWidth = (int) (fillPercentage * barWidth);
            guiGraphics.fill(startX + 1, startY + 1, startX + filledWidth, startY + barHeight - 1, 0xFF00BFFF); // Deep Sky Blue
        }
    }
}
