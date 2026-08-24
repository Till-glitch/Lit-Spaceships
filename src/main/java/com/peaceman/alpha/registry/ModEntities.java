package com.peaceman.alpha.registry;

import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.entity.TurretSeatEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Alpha.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<TurretSeatEntity>> TURRET_SEAT =
            ENTITIES.register("turret_seat", () -> EntityType.Builder.<TurretSeatEntity>of(TurretSeatEntity::new, MobCategory.MISC)
                    .sized(0.01f, 0.01f)
                    .noSave()
                    .fireImmune()
                    .build("turret_seat"));

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}
