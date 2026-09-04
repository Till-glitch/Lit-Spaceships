package com.lit.spaceships.client;

import com.lit.spaceships.client.screen.SpaceshipControlScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/**
 * Diese Klasse darf NIEMALS vom Server aufgerufen werden.
 * Sie dient als "Puffer" zwischen unserem gemeinsamen Server-Code (Blöcke)
 * und dem reinen Client-Code (Screens, GUI, Rendering).
 */
public class ClientHooks {

    public static void openControlScreen(java.util.UUID shipId, BlockPos pos) {
        Minecraft.getInstance().setScreen(new SpaceshipControlScreen(shipId, pos));
    }

    public static void openWarpEngineScreen(java.util.UUID shipId, BlockPos pos) {
        Minecraft.getInstance().setScreen(new com.lit.spaceships.client.screen.WarpEngineScreen(shipId, pos));
    }
}