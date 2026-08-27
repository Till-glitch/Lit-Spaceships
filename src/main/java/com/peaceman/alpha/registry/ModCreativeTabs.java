package com.peaceman.alpha.registry;

import com.peaceman.alpha.Alpha;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister
            .create(Registries.CREATIVE_MODE_TAB, Alpha.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS
            .register("example_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable(ModI18n.Tab.MAIN))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    // NEU: Der Raumschiff-Block ist jetzt das Icon des Tabs!
                    .icon(() -> ModItems.SPACESHIP_CONTROL_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        // Das Beispiel-Item ist hier verschwunden
                        output.accept(ModItems.SPACESHIP_CONTROL_ITEM.get());
                        output.accept(ModItems.EXAMPLE_BLOCK_ITEM.get()); // Den alten Beispiel-Block lassen wir vorerst
                                                                          // noch drin
                        output.accept(ModItems.SPACESHIP_HELM_ITEM.get());
                        output.accept(ModItems.SPACESHIP_REACTOR_ITEM.get());
                        output.accept(ModItems.SPACESHIP_SHIELD_ITEM.get());
                        output.accept(ModItems.PULSE_LASER_ITEM.get());
                        output.accept(ModItems.HEAVY_BEAM_ITEM.get());
                        output.accept(ModItems.MINING_LASER_ITEM.get());
                        output.accept(ModItems.BACKFLIP_TOOL.get());

                    }).build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
