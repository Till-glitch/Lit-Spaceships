package com.peaceman.alpha.registry;

import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.menu.SpaceshipReactorMenu;
import com.peaceman.alpha.menu.SpaceshipHelmMenu;
import com.peaceman.alpha.menu.SpaceshipShieldMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(BuiltInRegistries.MENU, Alpha.MODID);

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
