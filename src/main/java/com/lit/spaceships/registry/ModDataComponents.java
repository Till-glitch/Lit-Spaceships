package com.lit.spaceships.registry;

import com.lit.spaceships.LitSpaceships;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Data Components (Epoch 10): Telemetrie-Payloads fuer Items — verschluesselte
 * Koordinaten, Frequenz-Schluessel und Transponder-IDs (Signaldiscs, Flight
 * Recorder, Access Cipher).
 */
public class ModDataComponents {

    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, LitSpaceships.MODID);

    /**
     * Telemetrie-Payload: XOR-verschluesselte Zielkoordinate (deterministisch
     * per Transponder-ID entschluesselbar), Frequenz-Schluessel, Transponder-ID.
     */
    public record TelemetryData(long encryptedCoordinate, String frequency, int transponderId) {

        public static final Codec<TelemetryData> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.LONG.fieldOf("encrypted_coordinate").forGetter(TelemetryData::encryptedCoordinate),
                        Codec.STRING.fieldOf("frequency").forGetter(TelemetryData::frequency),
                        Codec.INT.fieldOf("transponder_id").forGetter(TelemetryData::transponderId)
                ).apply(instance, TelemetryData::new));

        public static final StreamCodec<io.netty.buffer.ByteBuf, TelemetryData> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_LONG, TelemetryData::encryptedCoordinate,
                        ByteBufCodecs.STRING_UTF8, TelemetryData::frequency,
                        ByteBufCodecs.VAR_INT, TelemetryData::transponderId,
                        TelemetryData::new);
    }

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<TelemetryData>> TELEMETRY_DATA =
            DATA_COMPONENTS.register("telemetry_data", () -> DataComponentType.<TelemetryData>builder()
                    .persistent(TelemetryData.CODEC)
                    .networkSynchronized(TelemetryData.STREAM_CODEC)
                    .build());

    public static void register(IEventBus eventBus) {
        DATA_COMPONENTS.register(eventBus);
    }
}
