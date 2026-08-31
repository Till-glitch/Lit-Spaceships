package com.peaceman.alpha.client.state;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.peaceman.alpha.client.render.ShieldRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
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
    private net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension = net.minecraft.world.level.Level.OVERWORLD;

    // Cooldown-Ticks (Rest-Ticks zum Zeitpunkt des letzten Syncs, dekrementiert auf Client-Seite)
    private long shieldCooldownRemainingTicks = 0L;
    private long movementCooldownRemainingTicks = 0L;
    private long lastSyncClientTick = 0L;

    // Zero-Allocation Ring-Buffer für O(1) Multi-Impact Updates
    public static final int MAX_IMPACTS = 4;
    private final Vec3[] impactPositions = new Vec3[MAX_IMPACTS];
    private final long[] impactTickTimes = new long[MAX_IMPACTS];
    private int impactCursor = 0;
    private float shieldEnergyPercentage = 1.0f;
    private int currentEnergy = 0;
    private volatile long activeMask = ~0L; // Standardmäßig alle 64 Zonen aktiv

    public ClientShipState(UUID shipId) {
        this.shipId = shipId;
        for (int i = 0; i < MAX_IMPACTS; i++) {
            this.impactTickTimes[i] = -1000L;
            this.impactPositions[i] = Vec3.ZERO;
        }
    }

    public long getActiveMask() {
        return activeMask;
    }

    public void setActiveMask(long activeMask) {
        this.activeMask = activeMask;
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

    public net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> getDimension() {
        return dimension;
    }

    public void setDimension(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension) {
        this.dimension = dimension != null ? dimension : net.minecraft.world.level.Level.OVERWORLD;
    }

    public Set<BlockPos> getRelativeStructureBlocks() {
        return relativeStructureBlocks;
    }

    public void setRelativeStructureBlocks(Set<BlockPos> relativeStructureBlocks) {
        this.relativeStructureBlocks = relativeStructureBlocks != null ? relativeStructureBlocks : Collections.emptySet();
    }

    public void removeStructureBlocks(Collection<BlockPos> removedBlocks) {
        if (removedBlocks == null || removedBlocks.isEmpty() || this.relativeStructureBlocks.isEmpty()) {
            return;
        }
        java.util.Set<BlockPos> updated = new java.util.HashSet<>(this.relativeStructureBlocks);
        BlockPos anchor = this.anchorPos != null ? this.anchorPos : BlockPos.ZERO;
        for (BlockPos pos : removedBlocks) {
            updated.remove(pos);
            updated.remove(pos.subtract(anchor));
        }
        this.relativeStructureBlocks = updated;
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

    public int getCurrentEnergy() {
        return currentEnergy;
    }

    public void setCurrentEnergy(int currentEnergy) {
        this.currentEnergy = Math.max(0, currentEnergy);
    }

    public float getShieldEnergyPercentage() {
        return shieldEnergyPercentage;
    }

    public void setShieldEnergyPercentage(float shieldEnergyPercentage) {
        this.shieldEnergyPercentage = Math.clamp(shieldEnergyPercentage, 0.0f, 1.0f);
    }

    /**
     * Fügt einen neuen Einschlagspunkt im Local-Space in den Ring-Buffer ein.
     */
    public void addImpact(Vec3 localPos, long currentClientTick) {
        this.impactPositions[this.impactCursor] = localPos != null ? localPos : Vec3.ZERO;
        this.impactTickTimes[this.impactCursor] = currentClientTick;
        this.impactCursor = (this.impactCursor + 1) % MAX_IMPACTS;
    }

    /**
     * Bindet die Shader-Uniforms für Energie, Zeit und die 4 Multi-Impact-Vektoren vor dem Draw-Call.
     */
    public void setupShaderUniforms(ShaderInstance shader, long currentTick, float partialTicks) {
        if (shader == null) return;

        float exactTime = currentTick + partialTicks;

        // 1. Globale Energie- und Zeit-Uniforms übermitteln
        if (shader.getUniform("u_EnergyLevel") != null) {
            shader.getUniform("u_EnergyLevel").set(this.shieldEnergyPercentage);
        }
        if (shader.getUniform("u_GameTime") != null) {
            shader.getUniform("u_GameTime").set(exactTime / 20.0f);
        }
        if (shader.getUniform("u_ActiveMaskLow") != null) {
            shader.getUniform("u_ActiveMaskLow").set((int) (this.activeMask & 0xFFFFFFFFL));
        }
        if (shader.getUniform("u_ActiveMaskHigh") != null) {
            shader.getUniform("u_ActiveMaskHigh").set((int) ((this.activeMask >>> 32) & 0xFFFFFFFFL));
        }

        // 2. Die 4 Impact-Vektoren aus dem Ring-Buffer binden
        for (int i = 0; i < MAX_IMPACTS; i++) {
            String uniformName = "u_Impact" + i;
            long hitTick = this.impactTickTimes[i];

            float timeSinceHit = -1.0f; // < 0.0 bedeutet inaktiv im GLSL

            // Wenn der Treffer nicht älter als 100 Ticks (5 Sekunden) ist
            if (hitTick > 0 && (currentTick - hitTick) < 100) {
                timeSinceHit = (exactTime - hitTick) / 20.0f;
            }

            if (shader.getUniform(uniformName) != null) {
                Vec3 pos = this.impactPositions[i] != null ? this.impactPositions[i] : Vec3.ZERO;
                shader.getUniform(uniformName).set(
                        (float) pos.x,
                        (float) pos.y,
                        (float) pos.z,
                        timeSinceHit
                );
            }
        }
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
