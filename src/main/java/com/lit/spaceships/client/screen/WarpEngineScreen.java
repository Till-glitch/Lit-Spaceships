package com.lit.spaceships.client.screen;

import com.lit.spaceships.network.WarpActionPayload;
import com.lit.spaceships.network.WarpStateSyncPayload;
import com.lit.spaceships.registry.ModI18n;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Locale;
import java.util.UUID;

/**
 * Sci-Fi Steuer- und Telemetrie-Terminal für den Warpantrieb (240x210).
 * Farbschema: Light-Blue / Neon-Cyan mit animierten Energie-Balken,
 * Countdown-Visualisierung und Abort-Kontrollen.
 */
public class WarpEngineScreen extends AbstractSpaceshipScreen {

    private final int imageWidth = 240;
    private final int imageHeight = 210;

    private Button warpButton;

    // Lokaler synchronisierter Zustand
    private int currentEnergy = 0;
    private int maxEnergy = 100000;
    private int countdownTicks = 0;
    private int cooldownRemainingTicks = 0;
    private boolean isCountingDown = false;
    private boolean isLinked = false;
    private boolean targetIsSpace = true;

    public WarpEngineScreen(UUID shipId, BlockPos pos) {
        super(Component.translatable(ModI18n.Screen.WARP_TITLE), pos);
        this.shipId = shipId;
    }

    public BlockPos getBlockPos() {
        return this.blockPos;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();

        int startX = (this.width - this.imageWidth) / 2;
        int startY = (this.height - this.imageHeight) / 2;

        this.warpButton = Button.builder(getButtonText(), button -> {
            if (this.isCountingDown) {
                PacketDistributor.sendToServer(new WarpActionPayload(this.blockPos, WarpActionPayload.Action.ABORT_COUNTDOWN));
            } else {
                PacketDistributor.sendToServer(new WarpActionPayload(this.blockPos, WarpActionPayload.Action.START_COUNTDOWN));
            }
        }).bounds(startX + 12, startY + 168, this.imageWidth - 24, 24).build();

        this.addRenderableWidget(this.warpButton);
        updateButtonState();
    }

    public void updateState(WarpStateSyncPayload payload) {
        this.currentEnergy = payload.energy();
        this.maxEnergy = payload.maxEnergy();
        this.countdownTicks = payload.countdownTicks();
        this.cooldownRemainingTicks = payload.cooldownRemainingTicks();
        this.isCountingDown = payload.isCountingDown();
        this.isLinked = payload.isLinked();
        this.targetIsSpace = payload.targetIsSpace();

        updateButtonState();
    }

    private void updateButtonState() {
        if (this.warpButton != null) {
            this.warpButton.setMessage(getButtonText());
            if (this.isCountingDown) {
                this.warpButton.active = true;
            } else {
                this.warpButton.active = this.isLinked && this.currentEnergy >= this.maxEnergy && this.cooldownRemainingTicks <= 0;
            }
        }
    }

    private Component getButtonText() {
        if (this.isCountingDown) {
            double secondsLeft = this.countdownTicks / 20.0;
            return Component.translatable(ModI18n.Screen.WARP_ABORT).append(String.format(Locale.ROOT, " (%.1fs)", secondsLeft));
        }
        return Component.translatable(ModI18n.Screen.WARP_ENGAGE);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        int startX = (this.width - this.imageWidth) / 2;
        int startY = (this.height - this.imageHeight) / 2;

        // Terminal Hintergrund (Dunkelblau-Schwarz mit Cyan Outline)
        guiGraphics.fill(startX, startY, startX + this.imageWidth, startY + this.imageHeight, 0xEE0A121D);
        guiGraphics.renderOutline(startX, startY, this.imageWidth, this.imageHeight, 0xFF00D4FF);

        // Header
        guiGraphics.drawString(this.font, this.title, startX + 12, startY + 12, 0xFF00E5FF, false);

        // Sub-Header: Ziel-Dimension
        Component destComponent = Component.translatable(ModI18n.Screen.WARP_DESTINATION,
                this.targetIsSpace
                        ? Component.translatable(ModI18n.Screen.WARP_DEST_SPACE)
                        : Component.translatable(ModI18n.Screen.WARP_DEST_OVERWORLD));
        guiGraphics.drawString(this.font, destComponent, startX + 12, startY + 28, 0xFF8BE9FD, false);

        // --- PANEL A: STATUS BADGE ---
        Component statusBadge;
        int statusColor;

        if (!this.isLinked) {
            statusBadge = Component.translatable(ModI18n.Screen.WARP_STATUS_UNLINKED);
            statusColor = 0xFFFF5555;
        } else if (this.isCountingDown) {
            double sec = this.countdownTicks / 20.0;
            statusBadge = Component.translatable(ModI18n.Screen.WARP_STATUS_COUNTDOWN, String.format(Locale.ROOT, "%.1f", sec));
            statusColor = 0xFF50FA7B;
        } else if (this.cooldownRemainingTicks > 0) {
            double cdSec = this.cooldownRemainingTicks / 20.0;
            statusBadge = Component.translatable(ModI18n.Screen.WARP_STATUS_COOLDOWN, String.format(Locale.ROOT, "%.1f", cdSec));
            statusColor = 0xFFFFB86C;
        } else if (this.currentEnergy >= this.maxEnergy) {
            statusBadge = Component.translatable(ModI18n.Screen.WARP_STATUS_READY);
            statusColor = 0xFF00FFCC;
        } else {
            statusBadge = Component.translatable(ModI18n.Screen.WARP_STATUS_CHARGING);
            statusColor = 0xFF00BFFF;
        }

        guiGraphics.drawString(this.font, statusBadge, startX + 12, startY + 46, statusColor, false);

        // --- PANEL B: ENERGIE PUFFER (LIGHT BLUE GAUGE) ---
        int barX = startX + 12;
        int barY = startY + 62;
        int barW = this.imageWidth - 24;
        int barH = 14;

        guiGraphics.fill(barX, barY, barX + barW, barY + barH, 0xFF060B12);
        guiGraphics.renderOutline(barX, barY, barW, barH, 0xFF1E3A5F);

        float fillRatio = this.maxEnergy > 0 ? Math.clamp((float) this.currentEnergy / (float) this.maxEnergy, 0.0f, 1.0f) : 0.0f;
        int filledWidth = (int) (fillRatio * (barW - 2));

        if (filledWidth > 0) {
            int fillColor = this.isCountingDown ? 0xFF00FFCC : (fillRatio >= 1.0f ? 0xFF00E5FF : 0xFF3399FF);
            guiGraphics.fill(barX + 1, barY + 1, barX + 1 + filledWidth, barY + barH - 1, fillColor);
        }

        String energyStr = String.format(Locale.ROOT, "%,d / %,d FE (%.1f%%)", this.currentEnergy, this.maxEnergy, fillRatio * 100.0f);
        int strW = this.font.width(energyStr);
        guiGraphics.drawString(this.font, energyStr, startX + (this.imageWidth - strW) / 2, barY + 18, 0xFFE0F7FA, false);

        // --- PANEL C: TELEMETRIE DETAILS ---
        int detailsY = startY + 100;
        guiGraphics.fill(barX, detailsY, barX + barW, detailsY + 56, 0x880E1B2B);
        guiGraphics.renderOutline(barX, detailsY, barW, 56, 0xFF1E3A5F);

        String shipInfo = this.shipId != null ? "Vessel: #" + this.shipId.toString().substring(0, 8) : "Vessel: Unlinked";
        guiGraphics.drawString(this.font, shipInfo, barX + 8, detailsY + 8, 0xFFA0C4FF, false);

        String coordInfo = String.format(Locale.ROOT, "Origin: [%d, %d, %d]", this.blockPos.getX(), this.blockPos.getY(), this.blockPos.getZ());
        guiGraphics.drawString(this.font, coordInfo, barX + 8, detailsY + 22, 0xFFA0C4FF, false);

        String safetyInfo = "Safety Protocol: Adaptive Spiral Collision Avoidance [ON]";
        guiGraphics.drawString(this.font, safetyInfo, barX + 8, detailsY + 36, 0xFF00FFCC, false);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}
