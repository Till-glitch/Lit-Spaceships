package com.lit.spaceships.registry;

import com.lit.spaceships.LitSpaceships;
import com.lit.spaceships.menu.SpaceshipReactorMenu;
import com.lit.spaceships.menu.SpaceshipHelmMenu;
import com.lit.spaceships.menu.SpaceshipShieldMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(BuiltInRegistries.MENU, LitSpaceships.MODID);

    public static final Supplier<MenuType<SpaceshipReactorMenu>> REACTOR_MENU =
            MENUS.register("spaceship_reactor_menu", () -> IMenuTypeExtension.create(SpaceshipReactorMenu::new));

    public static final Supplier<MenuType<SpaceshipHelmMenu>> HELM_MENU =
            MENUS.register("spaceship_helm_menu", () -> IMenuTypeExtension.create(SpaceshipHelmMenu::new));

    public static final Supplier<MenuType<SpaceshipShieldMenu>> SHIELD_MENU =
            MENUS.register("spaceship_shield_menu", () -> IMenuTypeExtension.create(SpaceshipShieldMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
