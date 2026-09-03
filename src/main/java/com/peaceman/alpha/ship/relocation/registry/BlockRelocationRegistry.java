package com.peaceman.alpha.ship.relocation.registry;

import com.peaceman.alpha.registry.ModTags;
import com.peaceman.alpha.ship.relocation.api.IBlockRelocationHandler;
import com.peaceman.alpha.ship.relocation.api.RelocationContext;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Zentrale Registrierungs- und Dispatching-Instanz für IBlockRelocationHandler
 * und Community-Tags (Immunität, Cluster, Container).
 */
public final class BlockRelocationRegistry {

    private static final List<IBlockRelocationHandler> HANDLERS = new CopyOnWriteArrayList<>();
    private static boolean spiLoaded = false;

    private BlockRelocationRegistry() {}

    /**
     * Registriert einen benutzerdefinierten Handler.
     */
    public static void registerHandler(IBlockRelocationHandler handler) {
        if (handler != null && !HANDLERS.contains(handler)) {
            HANDLERS.add(handler);
            HANDLERS.sort(Comparator.comparingInt(IBlockRelocationHandler::getPriority).reversed());
        }
    }

    /**
     * Lädt Handler via Java ServiceLoader, falls vorhanden.
     */
    public static synchronized void loadSpiHandlers() {
        if (!spiLoaded) {
            spiLoaded = true;
            try {
                ServiceLoader<IBlockRelocationHandler> loader = ServiceLoader.load(IBlockRelocationHandler.class);
                for (IBlockRelocationHandler handler : loader) {
                    registerHandler(handler);
                }
            } catch (Exception ignored) {
                // Fallback wenn ClassLoader keinen ServiceLoader unterstützt
            }
        }
    }

    /**
     * Prüft, ob ein BlockState immun gegen Verschiebung ist (z. B. Bedrock, Portale, Command Blocks
     * oder markiert durch #c:relocation_immune).
     */
    public static boolean isImmune(BlockState state) {
        if (state == null || state.isAir()) {
            return false;
        }

        // 1. Tag-Prüfung (#c:relocation_immune / #forge:relocation_immune)
        if (state.is(ModTags.Blocks.RELOCATION_IMMUNE) || state.is(ModTags.Blocks.RELOCATION_IMMUNE_FORGE)) {
            return true;
        }

        // 2. Unzerstörbare oder unverschiebbare Welt-Blöcke
        if (state.is(Blocks.BEDROCK) ||
                state.is(Blocks.END_PORTAL) ||
                state.is(Blocks.END_PORTAL_FRAME) ||
                state.is(Blocks.NETHER_PORTAL) ||
                state.is(Blocks.END_GATEWAY) ||
                state.is(Blocks.COMMAND_BLOCK) ||
                state.is(Blocks.CHAIN_COMMAND_BLOCK) ||
                state.is(Blocks.REPEATING_COMMAND_BLOCK) ||
                state.is(Blocks.STRUCTURE_BLOCK) ||
                state.is(Blocks.JIGSAW) ||
                state.is(Blocks.BARRIER)) {
            return true;
        }

        return false;
    }

    /**
     * Prüft, ob ein Block als zusammenhängender Cluster verschoben werden soll (#c:relocates_as_cluster).
     */
    public static boolean isCluster(BlockState state) {
        if (state == null || state.isAir()) return false;
        return state.is(ModTags.Blocks.RELOCATES_AS_CLUSTER) || state.is(ModTags.Blocks.RELOCATES_AS_CLUSTER_FORGE);
    }

    /**
     * Feuert onPreRelocation auf allen passenden Handlern.
     */
    public static void dispatchPreRelocation(BlockPos pos, BlockState state, BlockEntity be, CompoundTag nbt, RelocationContext context) {
        loadSpiHandlers();
        for (IBlockRelocationHandler handler : HANDLERS) {
            if (handler.shouldHandle(state)) {
                handler.onPreRelocation(pos, state, be, nbt, context);
            }
        }
    }

    /**
     * Feuert onPostRelocation auf allen passenden Handlern.
     */
    public static void dispatchPostRelocation(BlockPos oldPos, BlockPos newPos, BlockState state, BlockEntity be, RelocationContext context) {
        loadSpiHandlers();
        for (IBlockRelocationHandler handler : HANDLERS) {
            if (handler.shouldHandle(state)) {
                handler.onPostRelocation(oldPos, newPos, state, be, context);
            }
        }
    }

    public static List<IBlockRelocationHandler> getHandlers() {
        loadSpiHandlers();
        return new ArrayList<>(HANDLERS);
    }
}
