package com.peaceman.alpha.client.screen; // Passe das Package an deine Struktur an

import com.peaceman.alpha.network.ShipActionPayload;
import com.peaceman.alpha.network.ShipActionPayload.ActionType;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Optional;
import java.util.UUID;

public abstract class AbstractSpaceshipScreen extends Screen {

    protected final BlockPos blockPos;
    protected UUID shipId;

    protected AbstractSpaceshipScreen(Component title, BlockPos blockPos) {
        super(title);
        this.blockPos = blockPos;
    }

    @Override
    protected void init() {
        super.init();
        // Aktualisiert die ID automatisch, sobald sich IRGENDEIN Spaceship-Screen öffnet
        updateShipIdFromBlock();
    }

    // Die Methode, die sich immer den neuesten Stand holt
    protected void updateShipIdFromBlock() {
        if (this.minecraft != null && this.minecraft.level != null) {
            // Prüfung auf ISpaceshipNode
            if (this.minecraft.level.getBlockEntity(this.blockPos) instanceof com.peaceman.alpha.block.ISpaceshipNode node) {
                this.shipId = node.getShipId();
            }
        }
    }

    /**
     * Sendet eine typisierte Aktion an den Server.
     */
    protected void sendShipAction(ActionType actionType, int value, String targetName) {
        updateShipIdFromBlock(); // Zieht sich live die aktuellste UUID
        Optional<UUID> optionalShipId = Optional.ofNullable(this.shipId);
        PacketDistributor.sendToServer(new ShipActionPayload(optionalShipId, this.blockPos, actionType, value, targetName));
    }

    protected void sendShipAction(ActionType actionType) {
        sendShipAction(actionType, 0, "");
    }

    protected void sendCombatAction(com.peaceman.alpha.network.ShipCombatActionPayload.CombatAction combatAction) {
        updateShipIdFromBlock();
        if (this.shipId != null) {
            PacketDistributor.sendToServer(new com.peaceman.alpha.network.ShipCombatActionPayload(this.shipId, combatAction));
        }
    }

    protected com.peaceman.alpha.client.state.ClientShipState getClientShipState() {
        if (this.shipId == null) {
            updateShipIdFromBlock();
        }
        return this.shipId != null ? com.peaceman.alpha.client.state.ClientShipManager.getShip(this.shipId) : null;
    }

    protected long getClientGameTime() {
        return this.minecraft != null && this.minecraft.level != null ? this.minecraft.level.getGameTime() : 0L;
    }
}