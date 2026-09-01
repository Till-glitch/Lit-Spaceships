package com.peaceman.alpha.client.screen;

import com.peaceman.alpha.menu.SpaceshipShieldMenu;
import com.peaceman.alpha.network.ShipActionPayload;
import com.peaceman.alpha.registry.ModI18n;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Optional;
import java.util.UUID;

/**
 * Modernes Sci-Fi-Terminal für den Raumschiff-Schildgenerator.
 * Visualisiert in Echtzeit:
 * 1. Reaktor-Leistungsfluss (FE/t).
 * 2. Zonen-Status & Integrität (inkl. Kollaps-Countdown).
 * 3. Voronoi-Sektorabdeckung & räumliche Hüllen-Bounding-Box.
 */
public class SpaceshipShieldScreen extends AbstractContainerScreen<SpaceshipShieldMenu> {

    public SpaceshipShieldScreen(SpaceshipShieldMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 220;
        this.imageHeight = 190;
    }

    @Override
    protected void init() {
        super.init();

        int startX = (this.width - this.imageWidth) / 2;
        int startY = (this.height - this.imageHeight) / 2;

        this.addRenderableWidget(Button.builder(
                Component.translatable(ModI18n.Screen.SHIELD_TOGGLE),
                (button) -> {
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
                }
        ).bounds(startX + (this.imageWidth - 140) / 2, startY + 164, 140, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;

        // 1. Äußerer Rahmen & Terminal-Hintergrund
        guiGraphics.fill(left, top, left + this.imageWidth, top + this.imageHeight, 0xFF0B0E14);
        guiGraphics.fill(left + 1, top + 1, left + this.imageWidth - 1, top + this.imageHeight - 1, 0xFF141A24);
        guiGraphics.fill(left + 2, top + 2, left + this.imageWidth - 2, top + this.imageHeight - 2, 0xFF10141D);

        // 2. Header-Balken
        guiGraphics.fill(left + 4, top + 4, left + this.imageWidth - 4, top + 22, 0xFF182030);
        guiGraphics.fill(left + 4, top + 22, left + this.imageWidth - 4, top + 23, 0xFF00E5FF); // Neon-Cyan Akzentlinie

        // 3. Panel A: Status & Puffer
        guiGraphics.fill(left + 6, top + 26, left + this.imageWidth - 6, top + 74, 0xFF161B26);
        guiGraphics.renderOutline(left + 6, top + 26, this.imageWidth - 12, 48, 0xFF283244);

        // 4. Panel B: Reaktor Power Flow
        guiGraphics.fill(left + 6, top + 78, left + this.imageWidth - 6, top + 108, 0xFF161B26);
        guiGraphics.renderOutline(left + 6, top + 78, this.imageWidth - 12, 30, 0xFF283244);

        // 5. Panel C: Voronoi Sektor Telemetrie
        guiGraphics.fill(left + 6, top + 112, left + this.imageWidth - 6, top + 158, 0xFF161B26);
        guiGraphics.renderOutline(left + 6, top + 112, this.imageWidth - 12, 46, 0xFF283244);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int currentEnergy = this.menu.getCurrentEnergy();
        int maxEnergy = this.menu.getMaxEnergy();
        int deficit = this.menu.getEnergyDeficit();
        boolean isEnabled = this.menu.isShieldActive();
        int chargeRate = this.menu.getChargeRate();
        int cdRemaining = this.menu.getCooldownRemainingTicks();
        int sectorId = this.menu.getSectorId();
        int totalSectors = this.menu.getTotalZonesCount();
        int assignedBlocks = this.menu.getAssignedVoxelCount();
        int totalBlocks = this.menu.getTotalShipVoxelCount();
        float coverageRatio = this.menu.getCoverageRatio();
        int spanX = this.menu.getSpanX();
        int spanY = this.menu.getSpanY();
        int spanZ = this.menu.getSpanZ();

        // --- HEADER ---
        guiGraphics.drawString(this.font, this.title, 8, 8, 0xFFFFFF, false);
        if (sectorId > 0) {
            Component sectorText = Component.translatable(ModI18n.Screen.SHIELD_SECTOR_ID, sectorId, totalSectors > 0 ? totalSectors : 1);
            int secWidth = this.font.width(sectorText);
            guiGraphics.drawString(this.font, sectorText, this.imageWidth - secWidth - 8, 8, 0x00E5FF, false);
        }

        // --- PANEL A: STATUS & BUFFER ---
        Component statusBadge;
        int statusColor;

        if (cdRemaining > 0) {
            double cdSeconds = cdRemaining / 20.0;
            statusBadge = Component.translatable(ModI18n.Screen.SHIELD_STATUS_RECHARGE_CD, String.format("%.1f", cdSeconds));
            statusColor = 0xFFFF3333; // Alarm-Rot
        } else if (!isEnabled) {
            statusBadge = Component.translatable(ModI18n.Screen.SHIELD_STATUS_OFFLINE);
            statusColor = 0xFFFFAA00; // Warn-Orange
        } else if (currentEnergy <= 0) {
            statusBadge = Component.translatable(ModI18n.Screen.SHIELD_STATUS_COLLAPSED);
            statusColor = 0xFFFF2222; // Durchschlag-Rot
        } else if (currentEnergy >= maxEnergy && maxEnergy > 0) {
            statusBadge = Component.translatable(ModI18n.Screen.SHIELD_STATUS_OPTIMAL);
            statusColor = 0xFF00FF66; // Optimal-Grün
        } else {
            statusBadge = Component.translatable(ModI18n.Screen.SHIELD_STATUS_CHARGING);
            statusColor = 0xFF00E5FF; // Lade-Cyan
        }

        guiGraphics.drawString(this.font, statusBadge, 10, 30, statusColor, false);

        // Energy Bar
        int barX = 10;
        int barY = 42;
        int barW = this.imageWidth - 20;
        int barH = 12;

        guiGraphics.fill(barX, barY, barX + barW, barY + barH, 0xFF0A0D14);
        guiGraphics.renderOutline(barX, barY, barW, barH, 0xFF283244);

        if (maxEnergy > 0 && currentEnergy > 0) {
            float fillRatio = Math.clamp((float) currentEnergy / (float) maxEnergy, 0.0f, 1.0f);
            int filledWidth = (int) (fillRatio * (barW - 2));
            int barFillColor = (cdRemaining > 0 || currentEnergy <= 0) ? 0xFFFF3333 : (fillRatio >= 1.0f ? 0xFF00FF66 : 0xFF00BFFF);
            guiGraphics.fill(barX + 1, barY + 1, barX + 1 + filledWidth, barY + barH - 1, barFillColor);
        }

        // Energy Values Text
        float energyPct = maxEnergy > 0 ? ((float) currentEnergy / maxEnergy) * 100.0f : 0.0f;
        String energyValueStr = String.format("%,d / %,d FE (%.1f%%)", currentEnergy, maxEnergy, energyPct);
        int energyStrW = this.font.width(energyValueStr);
        guiGraphics.drawString(this.font, energyValueStr, (this.imageWidth - energyStrW) / 2, 58, 0xDDDDDD, false);

        // --- PANEL B: REACTOR POWER ROUTING ---
        Component flowText = Component.translatable(
                ModI18n.Screen.SHIELD_POWER_FLOW,
                String.format("%,d", chargeRate)
        );
        int flowColor = chargeRate > 0 ? 0xFF00FFCC : (deficit > 0 ? 0xFFFFCC00 : 0xFF8899A6);
        guiGraphics.drawString(this.font, flowText, 10, 83, flowColor, false);

        String deficitStr = deficit > 0 ? String.format("Ladebedarf: %,d FE", deficit) : "Voll aufgeladen";
        guiGraphics.drawString(this.font, deficitStr, 10, 95, 0x8899A6, false);

        // --- PANEL C: VORONOI SECTOR COVERAGE ---
        Component coverageText = Component.translatable(
                ModI18n.Screen.SHIELD_COVERAGE_VOXELS,
                assignedBlocks,
                totalBlocks,
                coverageRatio
        );
        guiGraphics.drawString(this.font, coverageText, 10, 116, 0xFFFFFF, false);

        Component spanText = Component.translatable(
                ModI18n.Screen.SHIELD_COVERAGE_BOUNDS,
                spanX, spanY, spanZ
        );
        guiGraphics.drawString(this.font, spanText, 10, 128, 0x00E5FF, false);

        Component spanCoordsText = Component.translatable(
                ModI18n.Screen.SHIELD_COVERAGE_SPAN,
                this.menu.getMinRelX(), this.menu.getMinRelY(), this.menu.getMinRelZ(),
                this.menu.getMaxRelX(), this.menu.getMaxRelY(), this.menu.getMaxRelZ()
        );
        guiGraphics.drawString(this.font, spanCoordsText, 10, 140, 0x8899A6, false);
    }
}

