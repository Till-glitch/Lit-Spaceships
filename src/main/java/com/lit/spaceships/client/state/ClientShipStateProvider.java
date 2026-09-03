package com.lit.spaceships.client.state;

import java.util.Collection;
import java.util.UUID;

/**
 * Gewährleistet als Singleton oder Thread-sicherer Service den exklusiven, kollisionsfreien
 * Lesezugriff des Render-Threads auf die von der Netzwerkschicht empfangenen Zustandsdaten.
 * Leitet die Aufrufe an den zugrundeliegenden ClientShipManager weiter.
 */
public class ClientShipStateProvider {

    private static final ClientShipStateProvider INSTANCE = new ClientShipStateProvider();

    private ClientShipStateProvider() {
    }

    public static ClientShipStateProvider getInstance() {
        return INSTANCE;
    }

    /**
     * Liefert den aktuellen visuellen State eines spezifischen Schiffs.
     */
    public ClientShipState getShip(UUID shipId) {
        return ClientShipManager.getShip(shipId);
    }

    /**
     * Liefert die States aller aktuell vom Client getrackten Schiffe.
     */
    public Collection<ClientShipState> getAllShips() {
        return ClientShipManager.getAllShips();
    }
}
