package com.peaceman.alpha.datagen.provider;

import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.registry.ModBlocks;
import com.peaceman.alpha.registry.ModItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider {

    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Alpha.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        // Laser Items als hierarchische Parent-Referenz auf die Block-Basis-Platte
        withExistingParent(ModBlocks.PULSE_LASER.getId().getPath(), modLoc("block/laser_base"));
        withExistingParent(ModBlocks.HEAVY_BEAM.getId().getPath(), modLoc("block/laser_base"));
        withExistingParent(ModBlocks.MINING_LASER.getId().getPath(), modLoc("block/laser_base"));

        // Maschinen Block-Items mit bestehendem Parent
        withExistingParent(ModBlocks.SPACESHIP_CONTROL.getId().getPath(), modLoc("block/spaceship_control"));
        withExistingParent(ModBlocks.SPACESHIP_HELM.getId().getPath(), modLoc("block/spaceship_helm"));
        withExistingParent(ModBlocks.SPACESHIP_REACTOR.getId().getPath(), modLoc("block/spaceship_reactor"));
        withExistingParent(ModBlocks.SPACESHIP_SHIELD.getId().getPath(), modLoc("block/spaceship_shield"));

        // Eigenständige 2D-Items
        basicItem(ModItems.BACKFLIP_TOOL.get());
    }
}
