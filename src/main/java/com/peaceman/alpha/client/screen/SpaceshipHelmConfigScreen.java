package com.peaceman.alpha.client.screen;

import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.menu.SpaceshipHelmMenu;
import com.peaceman.alpha.network.ShipActionPayload.ActionType;
import com.peaceman.alpha.network.ShipActionPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import com.peaceman.alpha.block.ISpaceshipNode;
import java.util.Optional;
import java.util.UUID;

public class SpaceshipHelmConfigScreen extends AbstractContainerScreen<SpaceshipHelmMenu> {

    private EditBox homeNameInput;

    public SpaceshipHelmConfigScreen(SpaceshipHelmMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 200;
        this.imageHeight = 150;
    }

    @Override
    protected void init() {
        super.init();
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Wegpunkt Steuerung
        this.homeNameInput = new EditBox(this.font, centerX - 40, centerY - 20, 80, 20, Component.literal("Wegpunkt"));
        this.homeNameInput.setValue("Basis");
        this.addRenderableWidget(this.homeNameInput);

        this.addRenderableWidget(Button.builder(Component.literal("Speichern"), button -> {
            sendShipAction(ActionType.SAVE_HOME, 0, this.homeNameInput.getValue());
        }).bounds(centerX - 40, centerY + 5, 80, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Anfliegen"), button -> {
            sendShipAction(ActionType.TP_HOME, 0, this.homeNameInput.getValue());
            this.minecraft.setScreen(null);
        }).bounds(centerX - 40, centerY + 30, 80, 20).build());
    }

    private void sendShipAction(ActionType type, int value, String targetName) {
        if (this.minecraft == null || this.minecraft.level == null) return;
        
        // Versuche ShipId vom BlockEntity zu bekommen
        net.minecraft.world.level.block.entity.BlockEntity be = this.minecraft.level.getBlockEntity(this.getMenu().getBlockPos());
        if (be instanceof ISpaceshipNode node) {
            UUID shipId = node.getShipId();
            if (shipId != null) {
                PacketDistributor.sendToServer(new ShipActionPayload(
                        Optional.of(shipId),
                        this.getMenu().getBlockPos(),
                        type,
                        value,
                        targetName
                ));
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        // Standard GUI Hintergrund
        guiGraphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFFC6C6C6);
        guiGraphics.fill(x + 1, y + 1, x + this.imageWidth - 1, y + this.imageHeight - 1, 0xFF555555);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
    }
}
