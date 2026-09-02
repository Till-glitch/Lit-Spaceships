package com.peaceman.alpha.client.screen;

import com.peaceman.alpha.client.render.ShipHighlightRenderer;
import com.peaceman.alpha.client.state.ClientShipState;
import com.peaceman.alpha.network.ShipActionPayload.ActionType;
import com.peaceman.alpha.registry.ModI18n;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Modernes Sci-Fi Command- & Lebenszyklus-Terminal für das Raumschiff (240x220).
 * Zeigt detaillierte BFS-Strukturdiagnostik, ein Subsystem-Register (Reaktoren, Schilde, Laser)
 * sowie sichere Lifecycle- und Hüllen-Hervorhebungs-Operationen.
 */
public class SpaceshipControlScreen extends AbstractSpaceshipScreen {

    private final int imageWidth = 240;
    private final int imageHeight = 220;

    private Button createButton;
    private Button updateButton;
    private Button disassembleButton;
    private Button highlightButton;

    private Set<BlockPos> cachedUnboundBlocks = null;
    private long lastScanTime = -100L;

    public SpaceshipControlScreen(UUID shipId, BlockPos pos) {
        super(Component.translatable(ModI18n.Screen.CONTROL_TITLE), pos);
        this.shipId = shipId;
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
        int btnWidth = 108;
        int btnHeight = 20;

        // 1. Schiff erstellen / Binden
        this.createButton = Button.builder(Component.translatable(ModI18n.Screen.CONTROL_BTN_CREATE), button -> {
            sendShipAction(ActionType.CREATE);
            this.cachedUnboundBlocks = null;
        }).bounds(startX + 8, startY + 158, btnWidth, btnHeight).build();
        this.addRenderableWidget(this.createButton);

        // 2. Struktur-Grenzen aktualisieren
        this.updateButton = Button.builder(Component.translatable(ModI18n.Screen.CONTROL_BTN_UPDATE), button -> {
            sendShipAction(ActionType.UPDATE_BLOCKS);
            this.cachedUnboundBlocks = null;
        }).bounds(startX + 124, startY + 158, btnWidth, btnHeight).build();
        this.addRenderableWidget(this.updateButton);

        // 3. Hülle hervorheben (Client-Side Particle Scan)
        this.highlightButton = Button.builder(getHighlightButtonText(), button -> {
            if (this.minecraft != null && this.minecraft.level != null) {
                var clientState = getClientShipState();
                if (clientState != null && !clientState.getRelativeStructureBlocks().isEmpty()) {
                    BlockPos anchor = clientState.getAnchorPos() != null ? clientState.getAnchorPos() : this.blockPos;
                    Set<BlockPos> absoluteBlocks = new HashSet<>();
                    for (BlockPos rel : clientState.getRelativeStructureBlocks()) {
                        absoluteBlocks.add(anchor.offset(rel));
                    }
                    ShipHighlightRenderer.toggleHighlight(absoluteBlocks);
                } else if (this.cachedUnboundBlocks != null && !this.cachedUnboundBlocks.isEmpty()) {
                    ShipHighlightRenderer.toggleHighlight(this.cachedUnboundBlocks);
                } else {
                    ShipHighlightRenderer.toggleHighlight(this.minecraft.level, this.blockPos);
                }
                button.setMessage(getHighlightButtonText());
            }
        }).bounds(startX + 8, startY + 184, btnWidth, btnHeight).build();
        this.addRenderableWidget(this.highlightButton);

        // 4. Schiff auflösen
        this.disassembleButton = Button.builder(Component.translatable(ModI18n.Screen.CONTROL_BTN_DISASSEMBLE), button -> {
            sendShipAction(ActionType.DELETE_SHIP);
            this.cachedUnboundBlocks = null;
            this.shipId = null;
        }).bounds(startX + 124, startY + 184, btnWidth, btnHeight).build();
        this.addRenderableWidget(this.disassembleButton);
    }

    private Component getHighlightButtonText() {
        return ShipHighlightRenderer.isHighlightActive()
                ? Component.translatable(ModI18n.Screen.CONTROL_HIGHLIGHT_ACTIVE).withStyle(ChatFormatting.GREEN)
                : Component.translatable(ModI18n.Screen.CONTROL_HIGHLIGHT_INACTIVE).withStyle(ChatFormatting.GRAY);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        int startX = (this.width - this.imageWidth) / 2;
        int startY = (this.height - this.imageHeight) / 2;

        // 1. Status & Button-Aktivierung berechnen
        var clientState = getClientShipState();
        boolean isBound = (clientState != null && this.shipId != null);

        if (this.createButton != null) this.createButton.active = !isBound;
        if (this.updateButton != null) this.updateButton.active = isBound;
        if (this.disassembleButton != null) this.disassembleButton.active = isBound;
        if (this.highlightButton != null) this.highlightButton.setMessage(getHighlightButtonText());

        // 2. Vollständig deckender Terminal-Hintergrund & Panels (Opaque, kein Durchscheinen des Blurs)
        renderTerminalBackground(guiGraphics, startX, startY);

        // 3. Widgets / Buttons rendern
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // 4. Text-Inhalte & Subsystem-Informationen rendern
        renderTerminalLabels(guiGraphics, startX, startY, clientState, isBound);
    }

    private void renderTerminalBackground(GuiGraphics guiGraphics, int startX, int startY) {
        // Äußerer Rahmen & Terminal-Körper (Solid Dark Emerald / Matrix Obsidian Theme)
        guiGraphics.fill(startX, startY, startX + this.imageWidth, startY + this.imageHeight, 0xFF08120B);
        guiGraphics.fill(startX + 1, startY + 1, startX + this.imageWidth - 1, startY + this.imageHeight - 1, 0xFF0F2417);
        guiGraphics.fill(startX + 2, startY + 2, startX + this.imageWidth - 2, startY + this.imageHeight - 2, 0xFF0B1A10);

        // Header-Balken mit Neon-Grün Akzentlinie
        guiGraphics.fill(startX + 4, startY + 4, startX + this.imageWidth - 4, startY + 22, 0xFF142E1E);
        guiGraphics.fill(startX + 4, startY + 22, startX + this.imageWidth - 4, startY + 23, 0xFF00FF66);

        // Panel A: Struktur-Diagnostik Box (Y: 26..82)
        guiGraphics.fill(startX + 6, startY + 26, startX + this.imageWidth - 6, startY + 82, 0xFF0F2215);
        guiGraphics.renderOutline(startX + 6, startY + 26, this.imageWidth - 12, 56, 0xFF1E4D2B);

        // Panel B: Subsystem-Register Box (Y: 86..150)
        guiGraphics.fill(startX + 6, startY + 86, startX + this.imageWidth - 6, startY + 150, 0xFF0F2215);
        guiGraphics.renderOutline(startX + 6, startY + 86, this.imageWidth - 12, 64, 0xFF1E4D2B);
    }

    private void renderTerminalLabels(GuiGraphics guiGraphics, int startX, int startY, ClientShipState clientState, boolean isBound) {
        // Header Titel
        guiGraphics.drawString(this.font, this.title, startX + 8, startY + 7, 0x00FF66, false);

        // Status Badge
        Component statusBadge = isBound
                ? Component.translatable(ModI18n.Screen.CONTROL_STATUS_BOUND).withStyle(ChatFormatting.GREEN)
                : Component.translatable(ModI18n.Screen.CONTROL_STATUS_UNBOUND).withStyle(ChatFormatting.GOLD);
        int badgeWidth = this.font.width(statusBadge);
        guiGraphics.drawString(this.font, statusBadge, startX + this.imageWidth - 8 - badgeWidth, startY + 7, isBound ? 0x00FF66 : 0xFFA726, false);

        // Panel A: Struktur-Diagnostik Header & Metriken
        guiGraphics.drawString(this.font, Component.translatable(ModI18n.Screen.CONTROL_STRUCTURAL_HEADER), startX + 10, startY + 30, 0x00E676, false);

        // Ermittle aktive oder gescannte Blöcke
        BlockPos anchor = (isBound && clientState != null && clientState.getAnchorPos() != null)
                ? clientState.getAnchorPos()
                : this.blockPos;

        Set<BlockPos> relativeBlocks = null;
        if (isBound && clientState != null && !clientState.getRelativeStructureBlocks().isEmpty()) {
            relativeBlocks = clientState.getRelativeStructureBlocks();
        } else if (this.minecraft != null && this.minecraft.level != null) {
            long now = this.minecraft.level.getGameTime();
            if (this.cachedUnboundBlocks == null || (now - this.lastScanTime >= 20)) {
                this.cachedUnboundBlocks = com.peaceman.alpha.ship.service.ShipScannerService.scan(this.minecraft.level, this.blockPos);
                this.lastScanTime = now;
            }
            if (this.cachedUnboundBlocks != null && !this.cachedUnboundBlocks.isEmpty()) {
                Set<BlockPos> rel = new HashSet<>();
                for (BlockPos abs : this.cachedUnboundBlocks) {
                    rel.add(abs.subtract(this.blockPos));
                }
                relativeBlocks = rel;
                anchor = this.blockPos;
            }
        }

        int totalBlocks = (relativeBlocks != null) ? relativeBlocks.size() : 0;
        int spanX = 1, spanY = 1, spanZ = 1;

        if (relativeBlocks != null && !relativeBlocks.isEmpty()) {
            int minX = 0, maxX = 0, minY = 0, maxY = 0, minZ = 0, maxZ = 0;
            boolean first = true;
            for (BlockPos pos : relativeBlocks) {
                if (first) {
                    minX = maxX = pos.getX();
                    minY = maxY = pos.getY();
                    minZ = maxZ = pos.getZ();
                    first = false;
                } else {
                    if (pos.getX() < minX) minX = pos.getX();
                    if (pos.getX() > maxX) maxX = pos.getX();
                    if (pos.getY() < minY) minY = pos.getY();
                    if (pos.getY() > maxY) maxY = pos.getY();
                    if (pos.getZ() < minZ) minZ = pos.getZ();
                    if (pos.getZ() > maxZ) maxZ = pos.getZ();
                }
            }
            spanX = Math.max(1, maxX - minX + 1);
            spanY = Math.max(1, maxY - minY + 1);
            spanZ = Math.max(1, maxZ - minZ + 1);
        }

        String blockCountStr = String.format(Locale.ROOT, "%,d", totalBlocks);
        String massStr = String.format(Locale.ROOT, "%.1f", (totalBlocks * 1.0f));
        guiGraphics.drawString(this.font, Component.translatable(ModI18n.Screen.CONTROL_STRUCTURAL_BLOCKS, blockCountStr, massStr), startX + 10, startY + 43, 0xEEEEEE, false);
        guiGraphics.drawString(this.font, Component.translatable(ModI18n.Screen.CONTROL_STRUCTURAL_BOUNDS, spanX, spanY, spanZ), startX + 10, startY + 55, 0xA7F3D0, false);
        guiGraphics.drawString(this.font, Component.translatable(ModI18n.Screen.CONTROL_STRUCTURAL_ANCHOR, anchor.getX(), anchor.getY(), anchor.getZ()), startX + 10, startY + 67, 0x81C784, false);

        // Panel B: Subsystem-Register Header & Aufzählung
        guiGraphics.drawString(this.font, Component.translatable(ModI18n.Screen.CONTROL_SUBSYSTEM_HEADER), startX + 10, startY + 90, 0x00E676, false);

        int reactorCount = 0;
        int shieldCount = 0;
        int heavyTurretCount = 0;
        int pulseTurretCount = 0;
        int miningLaserCount = 0;
        int helmCount = 0;

        if (this.minecraft != null && this.minecraft.level != null && relativeBlocks != null && !relativeBlocks.isEmpty()) {
            for (BlockPos rel : relativeBlocks) {
                BlockPos worldPos = anchor.offset(rel);
                var block = this.minecraft.level.getBlockState(worldPos).getBlock();
                if (block instanceof com.peaceman.alpha.block.SpaceshipReactorBlock) {
                    reactorCount++;
                } else if (block instanceof com.peaceman.alpha.block.SpaceshipShieldBlock) {
                    shieldCount++;
                } else if (block instanceof com.peaceman.alpha.block.HeavyBeamBlock) {
                    heavyTurretCount++;
                } else if (block instanceof com.peaceman.alpha.block.PulseLaserBlock) {
                    pulseTurretCount++;
                } else if (block instanceof com.peaceman.alpha.block.MiningLaserBlock) {
                    miningLaserCount++;
                } else if (block instanceof com.peaceman.alpha.block.SpaceshipHelmBlock) {
                    helmCount++;
                }
            }
        }

        int totalTurrets = heavyTurretCount + pulseTurretCount + miningLaserCount;
        guiGraphics.drawString(this.font, Component.translatable(ModI18n.Screen.CONTROL_SUBSYSTEM_CORES, reactorCount, shieldCount), startX + 10, startY + 104, 0xEEEEEE, false);
        guiGraphics.drawString(this.font, Component.translatable(ModI18n.Screen.CONTROL_SUBSYSTEM_WEAPONS, totalTurrets, heavyTurretCount, pulseTurretCount, miningLaserCount), startX + 10, startY + 117, 0xFFA726, false);
        guiGraphics.drawString(this.font, Component.translatable(ModI18n.Screen.CONTROL_SUBSYSTEM_NAV, helmCount), startX + 10, startY + 130, 0x66BB6A, false);
    }
}