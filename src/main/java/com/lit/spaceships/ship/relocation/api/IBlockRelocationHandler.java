package com.lit.spaceships.ship.relocation.api;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Service Provider Interface (SPI) zur Erweiterung der Relokations-Logik
 * für spezifische Mod-Blöcke oder komplexe Multiblöcke (z. B. Mekanism, Create, AE2).
 */
public interface IBlockRelocationHandler {

    /**
     * Gibt an, ob dieser Handler für den spezifizierten BlockState zuständig ist.
     */
    boolean shouldHandle(BlockState state);

    /**
     * Lifecycle-Hook: Wird vor dem Löschen und Verschieben des Blocks aufgerufen.
     * Ermöglicht das Pausieren von Controllern, Abstraktion von Daten oder Speichern von flüchtigen NBTs.
     */
    default void onPreRelocation(BlockPos pos, BlockState state, BlockEntity be, CompoundTag snapshotNbt, RelocationContext context) {}

    /**
     * Lifecycle-Hook: Wird nach der vollständigen Platzierung des Blocks am Zielort aufgerufen.
     * Ermöglicht das Re-Formieren von Multiblöcken, Neuverbinden von Netzwerken oder Reaktivieren von Ticks.
     */
    default void onPostRelocation(BlockPos oldPos, BlockPos newPos, BlockState state, BlockEntity be, RelocationContext context) {}

    /**
     * Priorität des Handlers (höhere Werte werden zuerst ausgeführt). Standard: 0.
     */
    default int getPriority() {
        return 0;
    }
}
