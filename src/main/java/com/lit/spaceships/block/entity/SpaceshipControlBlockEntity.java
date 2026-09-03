package com.lit.spaceships.block.entity;

import com.lit.spaceships.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class SpaceshipControlBlockEntity extends AbstractSpaceshipNodeBlockEntity {

    public SpaceshipControlBlockEntity(BlockPos pos, BlockState state) {
        // Hier übergibst du den Typ aus deiner Registry an die abstrakte Elternklasse
        super(ModBlockEntities.SPACESHIP_CONTROL_BE.get(), pos, state);

    }

    // FERTIG! Die gesamte UUID-Logik, das Speichern und Synchronisieren
    // ist automatisch vorhanden. Hier kommt nur noch Code rein, der
    // WIRKLICH nur für den Control-Block gedacht ist.
}