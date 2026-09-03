package com.lit.spaceships.registry;

import com.lit.spaceships.LitSpaceships;
import net.minecraft.core.UUIDUtil;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.UUID;
import java.util.function.Supplier;

public class ModAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, LitSpaceships.MODID);

    public static final Supplier<AttachmentType<UUID>> SHIP_ID = ATTACHMENT_TYPES.register(
            "ship_id",
            () -> AttachmentType.builder(() -> (UUID) null)
                    .serialize(UUIDUtil.CODEC)
                    .build()
    );

    public static void register(IEventBus eventBus) {
        ATTACHMENT_TYPES.register(eventBus);
    }
}
