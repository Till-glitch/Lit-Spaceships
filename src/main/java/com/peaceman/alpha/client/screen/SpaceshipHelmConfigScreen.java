package com.peaceman.alpha.client.screen;

import com.peaceman.alpha.block.ISpaceshipNode;
import com.peaceman.alpha.client.state.ClientShipState;
import com.peaceman.alpha.client.state.ClientShipStateProvider;
import com.peaceman.alpha.menu.SpaceshipHelmMenu;
import com.peaceman.alpha.network.ShipActionPayload;
import com.peaceman.alpha.network.ShipActionPayload.ActionType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Optional;
import java.util.UUID;

public class SpaceshipHelmConfigScreen extends AbstractContainerScreen<SpaceshipHelmMenu> {

    private EditBox homeNameInput;
    private EditBox jumpDistanceInput;

    private Button saveHomeBtn;
    private Button jumpHomeBtn;
    private Button jumpForwardBtn;
    private Button jumpUpBtn;
    private Button jumpDownBtn;

    public SpaceshipHelmConfigScreen(SpaceshipHelmMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 260;
        this.imageHeight = 220;
    }

    @Override
    protected void init() {
        super.init();

        int left = (this.width - this.imageWidth) / 2;
        int top = (this.height - this.imageHeight) / 2;

        // 1. Wegpunkt-Steuerung
        this.homeNameInput = new EditBox(this.font, left + 15, top + 92, 100, 18, Component.translatable(com.peaceman.alpha.registry.ModI18n.Screen.HELM_WAYPOINT_INPUT));
        this.homeNameInput.setValue("Basis");
        this.addRenderableWidget(this.homeNameInput);

        this.saveHomeBtn = Button.builder(Component.translatable(com.peaceman.alpha.registry.ModI18n.Screen.HELM_BTN_SAVE), button -> {
            sendShipAction(ActionType.SAVE_HOME, 0, this.homeNameInput.getValue());
        }).bounds(left + 120, top + 92, 60, 18).build();
        this.addRenderableWidget(this.saveHomeBtn);

        this.jumpHomeBtn = Button.builder(Component.translatable(com.peaceman.alpha.registry.ModI18n.Screen.HELM_BTN_FLYTO), button -> {
            sendShipAction(ActionType.TP_HOME, 0, this.homeNameInput.getValue());
            this.minecraft.setScreen(null);
        }).bounds(left + 185, top + 92, 60, 18).build();
        this.addRenderableWidget(this.jumpHomeBtn);

        // 2. Manueller Distanz-Sprung
        this.jumpDistanceInput = new EditBox(this.font, left + 15, top + 138, 60, 18, Component.translatable(com.peaceman.alpha.registry.ModI18n.Screen.HELM_DISTANCE_INPUT));
        this.jumpDistanceInput.setValue("50");
        this.jumpDistanceInput.setFilter(s -> s.matches("\\d*"));
        this.addRenderableWidget(this.jumpDistanceInput);

        this.jumpForwardBtn = Button.builder(Component.translatable(com.peaceman.alpha.registry.ModI18n.Screen.HELM_BTN_FORWARD), button -> {
            int dist = parseDistance();
            if (dist > 0) {
                sendShipAction(ActionType.MOVE_FORWARD, dist, "");
                this.minecraft.setScreen(null);
            }
        }).bounds(left + 80, top + 138, 55, 18).build();
        this.addRenderableWidget(this.jumpForwardBtn);

        this.jumpUpBtn = Button.builder(Component.translatable(com.peaceman.alpha.registry.ModI18n.Screen.HELM_BTN_UP), button -> {
            int dist = parseDistance();
            if (dist > 0) {
                sendShipAction(ActionType.MOVE_UP, dist, "");
                this.minecraft.setScreen(null);
            }
        }).bounds(left + 140, top + 138, 50, 18).build();
        this.addRenderableWidget(this.jumpUpBtn);

        this.jumpDownBtn = Button.builder(Component.translatable(com.peaceman.alpha.registry.ModI18n.Screen.HELM_BTN_DOWN), button -> {
            int dist = parseDistance();
            if (dist > 0) {
                sendShipAction(ActionType.MOVE_DOWN, dist, "");
                this.minecraft.setScreen(null);
            }
        }).bounds(left + 195, top + 138, 50, 18).build();
        this.addRenderableWidget(this.jumpDownBtn);
    }

    private int parseDistance() {
        try {
            return Integer.parseInt(this.jumpDistanceInput.getValue());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private UUID getShipId() {
        if (this.minecraft == null || this.minecraft.level == null) return null;
        net.minecraft.world.level.block.entity.BlockEntity be = this.minecraft.level.getBlockEntity(this.getMenu().getBlockPos());
        if (be instanceof ISpaceshipNode node) {
            return node.getShipId();
        }
        return null;
    }

    private ClientShipState getClientShipState() {
        UUID id = getShipId();
        return id != null ? ClientShipStateProvider.getInstance().getShip(id) : null;
    }

    private void sendShipAction(ActionType type, int value, String targetName) {
        UUID shipId = getShipId();
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

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int left = (this.width - this.imageWidth) / 2;
        int top = (this.height - this.imageHeight) / 2;

        ClientShipState clientState = getClientShipState();
        int currentEnergy = clientState != null ? clientState.getCurrentEnergy() : 0;
        int blockCount = clientState != null ? Math.max(1, clientState.getRelativeStructureBlocks().size()) : 1;
        int costPerMeter = blockCount * 10;
        int maxJumpDistance = costPerMeter > 0 ? (currentEnergy / costPerMeter) : 0;

        int inputDist = parseDistance();
        int jumpCost = inputDist * costPerMeter;
        boolean hasEnoughEnergy = currentEnergy >= jumpCost && jumpCost > 0;

        long currentTick = this.minecraft != null && this.minecraft.level != null ? this.minecraft.level.getGameTime() : 0L;
        boolean onWarpCooldown = clientState != null && clientState.isMovementOnCooldown(currentTick);

        // Aktualisiere Button-Status
        if (this.jumpForwardBtn != null) this.jumpForwardBtn.active = !onWarpCooldown && hasEnoughEnergy;
        if (this.jumpUpBtn != null) this.jumpUpBtn.active = !onWarpCooldown && hasEnoughEnergy;
        if (this.jumpDownBtn != null) this.jumpDownBtn.active = !onWarpCooldown && hasEnoughEnergy;
        if (this.jumpHomeBtn != null) this.jumpHomeBtn.active = !onWarpCooldown;

        // 1. Telemetrie & Energie / Reichweite (Oben)
        guiGraphics.drawString(this.font, Component.translatable(com.peaceman.alpha.registry.ModI18n.Screen.HELM_SHIP_SIZE, blockCount), left + 15, top + 22, 0xAAAAAA, false);
        guiGraphics.drawString(this.font, Component.translatable(com.peaceman.alpha.registry.ModI18n.Screen.HELM_REACTOR_ENERGY, String.format("%,d", currentEnergy)), left + 15, top + 34, 0xAAAAAA, false);
        guiGraphics.drawString(this.font, Component.translatable(com.peaceman.alpha.registry.ModI18n.Screen.HELM_FLIGHT_COST, costPerMeter), left + 15, top + 46, 0xAAAAAA, false);
        guiGraphics.drawString(this.font, Component.translatable(com.peaceman.alpha.registry.ModI18n.Screen.HELM_MAX_JUMP, String.format("%,d", maxJumpDistance)), left + 15, top + 58, 0x55FF55, false);

        // Trennlinien
        guiGraphics.hLine(left + 12, left + this.imageWidth - 14, top + 74, 0xFF444444);
        guiGraphics.drawString(this.font, Component.translatable(com.peaceman.alpha.registry.ModI18n.Screen.HELM_WAYPOINT_NAV), left + 15, top + 78, 0xFFFFAA, false);

        guiGraphics.hLine(left + 12, left + this.imageWidth - 14, top + 118, 0xFF444444);
        guiGraphics.drawString(this.font, Component.translatable(com.peaceman.alpha.registry.ModI18n.Screen.HELM_MANUAL_JUMP), left + 15, top + 124, 0xFFFFAA, false);

        // 3. Kosten-Kalkulation für manuelle Distanz (Unten)
        if (inputDist > 0) {
            if (hasEnoughEnergy) {
                guiGraphics.drawString(this.font, Component.translatable(com.peaceman.alpha.registry.ModI18n.Screen.HELM_COST_READY, inputDist, String.format("%,d", jumpCost)), left + 15, top + 164, 0x55FF55, false);
            } else {
                int missing = jumpCost - currentEnergy;
                guiGraphics.drawString(this.font, Component.translatable(com.peaceman.alpha.registry.ModI18n.Screen.HELM_COST_MISSING, String.format("%,d", jumpCost), String.format("%,d", missing)), left + 15, top + 164, 0xFF5555, false);
            }
        } else {
            guiGraphics.drawString(this.font, Component.translatable(com.peaceman.alpha.registry.ModI18n.Screen.HELM_ENTER_DISTANCE), left + 15, top + 164, 0x888888, false);
        }

        // Cooldown-Warnung
        if (onWarpCooldown) {
            double cdSeconds = clientState.getMovementCooldownDisplay(currentTick) / 20.0;
            guiGraphics.drawCenteredString(this.font, Component.translatable(com.peaceman.alpha.registry.ModI18n.Screen.HELM_WARP_COOLDOWN, cdSeconds), left + this.imageWidth / 2, top + 185, 0xFF5555);
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Sci-Fi Dark Palette Panel
        guiGraphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF10141C);
        guiGraphics.fill(x + 1, y + 1, x + this.imageWidth - 1, y + this.imageHeight - 1, 0xFF1A2230);
        guiGraphics.fill(x + 2, y + 2, x + this.imageWidth - 2, y + this.imageHeight - 2, 0xFF0D121B);

        // Header Balken
        guiGraphics.fill(x + 5, y + 5, x + this.imageWidth - 5, y + 18, 0xFF162A45);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, Component.translatable(com.peaceman.alpha.registry.ModI18n.Screen.HELM_SCREEN_TITLE), 12, 6, 0x55FFFF, false);
    }
}
