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

    // Uniforms und Animationszustände für den Hex-Shield Shader
    private Vec3 lastImpactPos = Vec3.ZERO;
    private float shieldEnergyPercentage = 1.0f;
    private long lastImpactTick = -1000L;

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

    public Vec3 getLastImpactPos() {
        return lastImpactPos;
    }

    public void setLastImpactPos(Vec3 lastImpactPos) {
        this.lastImpactPos = lastImpactPos;
    }

    public float getShieldEnergyPercentage() {
        return shieldEnergyPercentage;
    }

    public void setShieldEnergyPercentage(float shieldEnergyPercentage) {
        this.shieldEnergyPercentage = shieldEnergyPercentage;
    }

    public long getLastImpactTick() {
        return lastImpactTick;
    }

    public void setLastImpactTick(long lastImpactTick) {
        this.lastImpactTick = lastImpactTick;
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
