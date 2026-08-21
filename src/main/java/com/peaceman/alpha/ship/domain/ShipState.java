package com.peaceman.alpha.ship.domain;

import com.peaceman.alpha.block.entity.SpaceshipReactorBlockEntity;
import com.peaceman.alpha.block.entity.SpaceshipShieldBlockEntity;
import com.peaceman.alpha.network.ShieldBubbleSyncPacket;
import com.peaceman.alpha.ship.ShieldMorphology;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

/**
 * Reines Server-Domain-DTO für den logischen Zustand eines Raumschiffs.
 * Enthält keinerlei Render-Daten (wie shieldBubble) oder Client-Abhängigkeiten.
 */
public class ShipState {

    private final UUID id;
    private BlockPos controllerPos;
    private Set<BlockPos> blocks;
    private final Map<String, BlockPos> homes;
    private List<BlockPos> reactors = new ArrayList<>();
    private List<BlockPos> shields = new ArrayList<>();
    private boolean isShieldActive = false;

    // Konstruktor für ein neues Schiff
    public ShipState(BlockPos controllerPos, Set<BlockPos> blocks) {
        this.id = UUID.randomUUID();
        this.controllerPos = controllerPos;
        this.blocks = blocks != null ? blocks : new HashSet<>();
        this.homes = new HashMap<>();
    }

    // Konstruktor für geladene Schiffe aus dem Savegame
    public ShipState(UUID id, BlockPos controllerPos, Set<BlockPos> blocks, Map<String, BlockPos> homes, List<BlockPos> reactors, List<BlockPos> shields, boolean isShieldActive) {
        this.id = id;
        this.controllerPos = controllerPos;
        this.blocks = blocks != null ? blocks : new HashSet<>();
        this.homes = homes != null ? homes : new HashMap<>();
        if (reactors != null) this.reactors.addAll(reactors);
        if (shields != null) this.shields.addAll(shields);
        this.isShieldActive = isShieldActive;
    }

    public UUID getId() {
        return id;
    }

    public BlockPos getControllerPos() {
        return controllerPos;
    }

    public void setControllerPos(BlockPos controllerPos) {
        this.controllerPos = controllerPos;
    }

    public Set<BlockPos> getBlocks() {
        return blocks;
    }

    public Set<BlockPos> getImmutableBlockSnapshot() {
        return Set.copyOf(this.blocks);
    }

    public void setBlocksRaw(Set<BlockPos> blocks) {
        this.blocks = blocks;
    }

    public List<BlockPos> getReactors() {
        return reactors;
    }

    public void setReactors(List<BlockPos> reactors) {
        this.reactors = reactors;
    }

    public List<BlockPos> getShields() {
        return shields;
    }

    public void setShields(List<BlockPos> shields) {
        this.shields = shields;
    }

    public Map<String, BlockPos> getHomes() {
        return homes;
    }

    public void addHome(String name, BlockPos pos) {
        this.homes.put(name, pos);
    }

    public boolean isShieldActive() {
        return isShieldActive;
    }

    public void setShieldActive(boolean shieldActive) {
        this.isShieldActive = shieldActive;
    }

    public void toggleShieldActive() {
        this.isShieldActive = !this.isShieldActive;
    }

    /**
     * Aktualisiert die Blockstruktur, kategorisiert Funktionsblöcke (Reaktoren, Schilde)
     * und triggert bei Bedarf die Netzwerk-Synchronisation der Schildblase.
     */
    public void setBlocks(Set<BlockPos> newBlocks, Level level) {
        this.blocks = newBlocks != null ? newBlocks : new HashSet<>();
        this.reactors.clear();
        this.shields.clear();

        for (BlockPos pos : this.blocks) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SpaceshipReactorBlockEntity) {
                this.reactors.add(pos);
            } else if (be instanceof SpaceshipShieldBlockEntity) {
                this.shields.add(pos);
            }
        }

        syncShieldBubbleToClients(level);
    }

    /**
     * Berechnet die Schildblase asynchron auf Virtual Threads und synchronisiert sie direkt
     * als Netzwerkpaket an die Clients, ohne sie permanent im Server-Zustand zu speichern.
     */
    public void syncShieldBubbleToClients(Level level) {
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            com.peaceman.alpha.ship.service.ShipMorphologyService.calculateAndSyncShieldAsync(this, serverLevel, 5);
        }
    }
}
