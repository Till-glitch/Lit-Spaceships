package com.peaceman.alpha.datagen.provider;

import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.registry.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModBlockStateProvider extends BlockStateProvider {

    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, Alpha.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        // Standard Cube-All Block
        simpleBlockWithItem(ModBlocks.EXAMPLE_BLOCK.get(), cubeAll(ModBlocks.EXAMPLE_BLOCK.get()));

        // Directional / Machine Blocks mit Existing Models
        ModelFile controlModel = models().getExistingFile(modLoc("block/spaceship_control"));
        simpleBlock(ModBlocks.SPACESHIP_CONTROL.get(), controlModel);

        ModelFile helmModel = models().getExistingFile(modLoc("block/spaceship_helm"));
        simpleBlock(ModBlocks.SPACESHIP_HELM.get(), helmModel);

        ModelFile reactorModel = models().getExistingFile(modLoc("block/spaceship_reactor"));
        simpleBlock(ModBlocks.SPACESHIP_REACTOR.get(), reactorModel);

        ModelFile shieldModel = models().getExistingFile(modLoc("block/spaceship_shield"));
        simpleBlock(ModBlocks.SPACESHIP_SHIELD.get(), shieldModel);

        // Laser Split-Model Extrahierung (Nur die statische Voxel-Basisplatte)
        ModelFile laserBaseModel = models().getExistingFile(modLoc("block/laser_base"));
        registerLaserBase(ModBlocks.PULSE_LASER.get(), laserBaseModel);
        registerLaserBase(ModBlocks.HEAVY_BEAM.get(), laserBaseModel);
        registerLaserBase(ModBlocks.MINING_LASER.get(), laserBaseModel);
    }

    // Rotiert die Basisplatte basierend auf dem FACING-Property des AbstractLaserNodeBlock
    private void registerLaserBase(Block block, ModelFile baseModel) {
        getVariantBuilder(block).forAllStates(state -> {
            Direction dir = state.getValue(BlockStateProperties.FACING);
            int rotX = 0;
            int rotY = 0;

            // Orthogonales Euler-Angle Mapping für die 3D-Engine
            switch (dir) {
                case UP -> { rotX = 0; rotY = 0; }
                case DOWN -> { rotX = 180; rotY = 0; }
                case NORTH -> { rotX = 90; rotY = 0; }
                case SOUTH -> { rotX = 90; rotY = 180; }
                case WEST -> { rotX = 90; rotY = 270; }
                case EAST -> { rotX = 90; rotY = 90; }
            }

            return ConfiguredModel.builder()
                    .modelFile(baseModel)
                    .rotationX(rotX)
                    .rotationY(rotY)
                    .build();
        });
    }
}
