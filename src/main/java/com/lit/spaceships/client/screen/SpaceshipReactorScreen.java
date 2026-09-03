package com.lit.spaceships.client.screen;

import com.lit.spaceships.menu.SpaceshipReactorMenu;
import com.lit.spaceships.network.ShipActionPayload;
import com.lit.spaceships.registry.ModI18n;
import com.lit.spaceships.ship.domain.PowerPriority;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Modernes Crimson-Sci-Fi-Terminal für das Reaktor- und Energie-Management.
 * Visualisiert in Echtzeit:
 * 1. Lokalen und schiffsweiten Energiespeicher (bis 1.000.000+ FE).
 * 2. Echtzeit-Flussmetriken (Generierung, Gesamtlast, Netto-Durchsatz).
 * 3. Taktische Energie-Priorisierung (Ausgeglichen, Schilde, Waffen, Antrieb).
 * 4. Reaktor-Betriebsstatus.
 */
public class SpaceshipReactorScreen extends AbstractContainerScreen<SpaceshipReactorMenu> {

    public SpaceshipReactorScreen(SpaceshipReactorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 240;
        this.imageHeight = 190;
    }

    @Override
    protected void init() {
        super.init();

        int startX = (this.width - this.imageWidth) / 2;
        int startY = (this.height - this.imageHeight) / 2;

        // Button für Taktische Energie-Priorisierung (Zyklisch umschaltbar)
        this.addRenderableWidget(Button.builder(
                getPriorityButtonText(),
                button -> {
                    UUID shipId = this.menu.getBlockEntity().getShipId();
                    if (shipId != null) {
                        PacketDistributor.sendToServer(new ShipActionPayload(
                                Optional.of(shipId),
                                this.menu.getBlockEntity().getBlockPos(),
                                ShipActionPayload.ActionType.CYCLE_POWER_PRIORITY,
                                0,
                                ""
                        ));
                    }
                }
        ).bounds(startX + 10, startY + 162, this.imageWidth - 20, 20).build());
    }

    private Component getPriorityButtonText() {
        PowerPriority priority = this.menu.getPowerPriority();
        String key = switch (priority) {
            case SHIELDS_FIRST -> ModI18n.Screen.REACTOR_PRIORITY_SHIELDS;
            case WEAPONS_FIRST -> ModI18n.Screen.REACTOR_PRIORITY_WEAPONS;
            case ENGINES_FIRST -> ModI18n.Screen.REACTOR_PRIORITY_ENGINES;
            default -> ModI18n.Screen.REACTOR_PRIORITY_BALANCED;
        };
        return Component.translatable(ModI18n.Screen.REACTOR_PRIORITY_BTN, Component.translatable(key));
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

        // 1. Äußerer Rahmen & Terminal-Hintergrund (Dark Obsidian / Crimson)
        guiGraphics.fill(left, top, left + this.imageWidth, top + this.imageHeight, 0xFF0E080A);
        guiGraphics.fill(left + 1, top + 1, left + this.imageWidth - 1, top + this.imageHeight - 1, 0xFF1D0E13);
        guiGraphics.fill(left + 2, top + 2, left + this.imageWidth - 2, top + this.imageHeight - 2, 0xFF14090D);

        // 2. Header-Balken mit Crimson-Akzentlinie
        guiGraphics.fill(left + 4, top + 4, left + this.imageWidth - 4, top + 22, 0xFF240F16);
        guiGraphics.fill(left + 4, top + 22, left + this.imageWidth - 4, top + 23, 0xFFFF1744); // Neon-Crimson

        // 3. Panel A: Energiespeicher & Kapazität (Y: 26..74)
        guiGraphics.fill(left + 6, top + 26, left + this.imageWidth - 6, top + 74, 0xFF1A0B10);
        guiGraphics.renderOutline(left + 6, top + 26, this.imageWidth - 12, 48, 0xFF3D1B26);

        // 4. Panel B: Energie-Flussmetriken & Lastverteilung (Y: 78..124)
        guiGraphics.fill(left + 6, top + 78, left + this.imageWidth - 6, top + 124, 0xFF1A0B10);
        guiGraphics.renderOutline(left + 6, top + 78, this.imageWidth - 12, 46, 0xFF3D1B26);

        // 5. Panel C: Energie-Priorität & Status-Matrix (Y: 128..158)
        guiGraphics.fill(left + 6, top + 128, left + this.imageWidth - 6, top + 158, 0xFF1A0B10);
        guiGraphics.renderOutline(left + 6, top + 128, this.imageWidth - 12, 30, 0xFF3D1B26);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int currentEnergy = this.menu.getCurrentEnergy();
        int maxEnergy = this.menu.getMaxEnergy();
        int totalEnergy = this.menu.getTotalShipEnergy();
        int totalMaxEnergy = this.menu.getTotalShipMaxEnergy();
        int genRate = this.menu.getGenerationRate();
        int consRate = this.menu.getConsumptionRate();
        int netThroughput = this.menu.getNetThroughput();
        int opStatus = this.menu.getOperationalStatus();
        int reactorCount = this.menu.getReactorCount();
        int shieldDrain = this.menu.getShieldDrainRate();
        int weaponDrain = this.menu.getWeaponDrainRate();
        int engineDrain = this.menu.getEngineDrainRate();
        PowerPriority priority = this.menu.getPowerPriority();

        // --- HEADER ---
        guiGraphics.drawString(this.font, this.title, 8, 8, 0xFFFFFF, false);

        // --- PANEL A: STORAGE GAUGE ---
        Component statusBadge = switch (opStatus) {
            case 1 -> Component.translatable(ModI18n.Screen.REACTOR_STATUS_HIGH_LOAD);
            case 2 -> Component.translatable(ModI18n.Screen.REACTOR_STATUS_CRITICAL);
            case 3 -> Component.translatable(ModI18n.Screen.REACTOR_STATUS_STANDBY);
            case 4 -> Component.translatable(ModI18n.Screen.REACTOR_STATUS_UNLINKED);
            default -> Component.translatable(ModI18n.Screen.REACTOR_STATUS_OPTIMAL);
        };
        int statusColor = switch (opStatus) {
            case 1 -> 0xFFFF9100; // Amber
            case 2 -> 0xFFFF1744; // Crimson
            case 3 -> 0xFF00E5FF; // Standby Cyan
            case 4 -> 0xFF888888; // Unlinked Grey
            default -> 0xFF00FF66; // Optimal Green
        };
        guiGraphics.drawString(this.font, statusBadge, 10, 30, statusColor, false);

        // Progress Bar
        int barX = 10;
        int barY = 42;
        int barW = this.imageWidth - 20;
        int barH = 12;

        guiGraphics.fill(barX, barY, barX + barW, barY + barH, 0xFF0A0507);
        guiGraphics.renderOutline(barX, barY, barW, barH, 0xFF3D1B26);

        if (maxEnergy > 0 && currentEnergy > 0) {
            float fillRatio = Math.clamp((float) currentEnergy / (float) maxEnergy, 0.0f, 1.0f);
            int filledWidth = (int) (fillRatio * (barW - 2));
            int barFillColor = fillRatio > 0.5f ? 0xFFFF1744 : (fillRatio > 0.2f ? 0xFFFF9100 : 0xFFFF3344);
            guiGraphics.fill(barX + 1, barY + 1, barX + 1 + filledWidth, barY + barH - 1, barFillColor);
        }

        float localPct = maxEnergy > 0 ? ((float) currentEnergy / (float) maxEnergy) * 100.0f : 0.0f;
        Component localText = Component.translatable(
                ModI18n.Screen.REACTOR_STORAGE_LOCAL,
                String.format(Locale.ROOT, "%,d", currentEnergy),
                String.format(Locale.ROOT, "%,d", maxEnergy),
                String.format(Locale.ROOT, "%.1f", localPct)
        );
        guiGraphics.drawString(this.font, localText, 10, 58, 0xEEEEEE, false);

        if (reactorCount > 1) {
            Component gridText = Component.translatable(
                    ModI18n.Screen.REACTOR_STORAGE_GRID,
                    String.format(Locale.ROOT, "%,d", totalEnergy),
                    String.format(Locale.ROOT, "%,d", totalMaxEnergy),
                    reactorCount
            );
            int gridW = this.font.width(gridText);
            guiGraphics.drawString(this.font, gridText, this.imageWidth - gridW - 10, 30, 0x8899A6, false);
        }

        // --- PANEL B: ENERGY FLOW METRICS ---
        Component genText = Component.translatable(ModI18n.Screen.REACTOR_GENERATION, String.format(Locale.ROOT, "%,d", genRate));
        guiGraphics.drawString(this.font, genText, 10, 82, 0xFF00FFCC, false);

        String sign = netThroughput > 0 ? "+" : "";
        Component netText = Component.translatable(ModI18n.Screen.REACTOR_NET_FLOW, sign + String.format(Locale.ROOT, "%,d", netThroughput));
        int netColor = netThroughput > 0 ? 0xFF00FF66 : (netThroughput < 0 ? 0xFFFF3344 : 0xFF8899A6);
        int netW = this.font.width(netText);
        guiGraphics.drawString(this.font, netText, this.imageWidth - netW - 10, 82, netColor, false);

        Component consText = Component.translatable(ModI18n.Screen.REACTOR_CONSUMPTION, String.format(Locale.ROOT, "%,d", consRate));
        guiGraphics.drawString(this.font, consText, 10, 95, 0xFFFFAB00, false);

        Component breakdownText = Component.translatable(
                ModI18n.Screen.REACTOR_DRAIN_BREAKDOWN,
                String.format(Locale.ROOT, "%,d", engineDrain),
                String.format(Locale.ROOT, "%,d", shieldDrain),
                String.format(Locale.ROOT, "%,d", weaponDrain)
        );
        guiGraphics.drawString(this.font, breakdownText, 10, 108, 0x8899A6, false);

        // --- PANEL C: POWER PRIORITY MATRIX ---
        int shldPct = (int) (priority.getShieldShare() * 100);
        int wpnPct = (int) (priority.getWeaponShare() * 100);
        int engPct = (int) (priority.getEngineShare() * 100);
        Component ratioText = Component.translatable(ModI18n.Screen.REACTOR_ALLOCATION_RATIO, engPct, shldPct, wpnPct);
        guiGraphics.drawString(this.font, ratioText, 10, 132, 0xFFFFFF, false);

        String focusKey = switch (priority) {
            case SHIELDS_FIRST -> ModI18n.Screen.REACTOR_FOCUS_SHIELDS;
            case WEAPONS_FIRST -> ModI18n.Screen.REACTOR_FOCUS_WEAPONS;
            case ENGINES_FIRST -> ModI18n.Screen.REACTOR_FOCUS_ENGINES;
            default -> ModI18n.Screen.REACTOR_FOCUS_BALANCED;
        };
        Component hintText = Component.translatable(focusKey);
        guiGraphics.drawString(this.font, hintText, 10, 144, 0x8899A6, false);
    }
}
