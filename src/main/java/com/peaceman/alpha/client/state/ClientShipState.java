package com.peaceman.alpha.client.state;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.peaceman.alpha.client.render.ShieldRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

/**
 * Repräsentiert den rein visuellen Zustand eines Raumschiffs auf dem logischen Client.
 * Hält den kompilierten VertexBuffer im VRAM sowie Daten für Shader-Uniforms.
 */
public class ClientShipState implements AutoCloseable {

    private final UUID shipId;
    private BlockPos anchorPos;
    private Set<BlockPos> relativeBubbleBlocks = Collections.emptySet();
    private Set<BlockPos> relativeStructureBlocks = Collections.emptySet();
    private VertexBuffer shieldMesh;
    private boolean isShieldActive = true;
    private boolean isDisposed = false;

    // Cooldown-Ticks (Rest-Ticks zum Zeitpunkt des letzten Syncs, dekrementiert auf Client-Seite)
    private long shieldCooldownRemainingTicks = 0L;
    private long movementCooldownRemainingTicks = 0L;
    private long lastSyncClientTick = 0L;

    public ClientShipState(UUID shipId) {
        this.shipId = shipId;
    }

    public UUID getShipId() {
        return shipId;
    }

    public BlockPos getAnchorPos() {
        return anchorPos;
    }

    public void setAnchorPos(BlockPos anchorPos) {
        this.anchorPos = anchorPos;
    }

    public Set<BlockPos> getRelativeBubbleBlocks() {
        return relativeBubbleBlocks;
    }

    public Set<BlockPos> getRelativeStructureBlocks() {
        return relativeStructureBlocks;
    }

    public void setRelativeStructureBlocks(Set<BlockPos> relativeStructureBlocks) {
        this.relativeStructureBlocks = relativeStructureBlocks != null ? relativeStructureBlocks : Collections.emptySet();
    }

    public VertexBuffer getShieldMesh() {
        return shieldMesh;
    }

    public boolean isShieldActive() {
        return isShieldActive;
    }

    public void setShieldActive(boolean shieldActive) {
        isShieldActive = shieldActive;
    }

    public boolean isDisposed() {
        return isDisposed;
    }

    public long getShieldCooldownRemainingTicks() {
        return shieldCooldownRemainingTicks;
    }

    public long getMovementCooldownRemainingTicks() {
        return movementCooldownRemainingTicks;
    }

    /**
     * Aktualisiert Cooldown-Werte bei empfangenem Server-Sync.
     */
    public void updateCooldowns(long shieldCooldownTicks, long movementCooldownTicks, long clientTick) {
        this.shieldCooldownRemainingTicks = shieldCooldownTicks;
        this.movementCooldownRemainingTicks = movementCooldownTicks;
        this.lastSyncClientTick = clientTick;
    }

    /**
     * Berechnet die aktuellen Rest-Ticks unter Berücksichtigung der seit dem letzten Sync vergangenen Client-Ticks.
     */
    public long getShieldCooldownDisplay(long currentClientTick) {
        long elapsed = currentClientTick - lastSyncClientTick;
        return Math.max(0L, shieldCooldownRemainingTicks - elapsed);
    }

    public long getMovementCooldownDisplay(long currentClientTick) {
        long elapsed = currentClientTick - lastSyncClientTick;
        return Math.max(0L, movementCooldownRemainingTicks - elapsed);
    }

    public boolean isShieldOnCooldown(long currentClientTick) {
        return getShieldCooldownDisplay(currentClientTick) > 0;
    }

    public boolean isMovementOnCooldown(long currentClientTick) {
        return getMovementCooldownDisplay(currentClientTick) > 0;
    }

    /**
     * Baut das Voxel-Mesh für die Schildblase und lädt es direkt in den VRAM (VertexBuffer).
     * Bestehender VRAM-Speicher wird ordnungsgemäß freigegeben.
     */
    public synchronized void updateMesh(Set<BlockPos> relativeBlocks) {
        if (isDisposed) return;
        this.relativeBubbleBlocks = relativeBlocks != null ? relativeBlocks : Collections.emptySet();

        if (this.shieldMesh != null) {
            this.shieldMesh.close();
            this.shieldMesh = null;
        }

        if (this.relativeBubbleBlocks.isEmpty()) {
            return;
        }

        MeshData meshData = ShieldRenderer.buildShieldMesh(this.relativeBubbleBlocks);
        if (meshData != null) {
            this.shieldMesh = new VertexBuffer(VertexBuffer.Usage.STATIC);
            this.shieldMesh.bind();
            this.shieldMesh.upload(meshData);
            VertexBuffer.unbind();
        }
    }

    /**
     * Gibt native OpenGL-Ressourcen (VBO) thread-sicher frei (Blueprint 2).
     */
    public synchronized void dispose() {
        if (isDisposed) return;
        isDisposed = true;

        if (this.shieldMesh != null) {
            VertexBuffer meshToClose = this.shieldMesh;
            this.shieldMesh = null;

            if (RenderSystem.isOnRenderThread()) {
                meshToClose.close();
            } else {
                RenderSystem.recordRenderCall(meshToClose::close);
            }
        }
    }

    @Override
    public void close() {
        dispose();
    }
}
