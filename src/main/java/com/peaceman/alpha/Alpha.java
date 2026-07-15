package com.peaceman.alpha;

import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.logging.LogUtils;
import com.peaceman.alpha.client.render.ShieldRenderer;
import com.peaceman.alpha.network.ShieldBubbleSyncPacket;
import com.peaceman.alpha.network.ShipCommandPayload;
import com.peaceman.alpha.registry.ModBlockEntities;
import com.peaceman.alpha.registry.ModBlocks;
import com.peaceman.alpha.registry.ModCreativeTabs;
import com.peaceman.alpha.registry.ModItems;
import com.peaceman.alpha.registry.ModMenuTypes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;
import com.mojang.blaze3d.vertex.MeshData;

@Mod(Alpha.MODID)
public class Alpha {
    public static final String MODID = "peaceman_alpha";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Alpha(IEventBus modEventBus, ModContainer modContainer) {
        // 1. Registriert unsere Netzwerk-Pakete (für die Raumschiff-Steuerung)
        modEventBus.addListener(this::registerNetwork);
        modEventBus.addListener(this::registerCapabilities);

        // 2. Ruft unsere aufgeräumten Register-Klassen auf
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModMenuTypes.register(modEventBus);

    }

    // Kümmert sich darum, dass Client und Server miteinander reden können
    private void registerNetwork(final RegisterPayloadHandlersEvent event) {
        final var registrar = event.registrar("1.0");
        registrar.playToServer(
                ShipCommandPayload.TYPE,
                ShipCommandPayload.STREAM_CODEC,
                ShipCommandPayload::handleData);

        registrar.playToClient(
                ShieldBubbleSyncPacket.TYPE,
                ShieldBubbleSyncPacket.STREAM_CODEC,
                (packet, context) -> {
                    context.enqueueWork(() -> {
                        ShieldRenderer.ClientShieldData data = ShieldRenderer.ACTIVE_CLIENT_SHIELDS
                                .computeIfAbsent(packet.shipId(), id -> new ShieldRenderer.ClientShieldData());

                        data.anchorPoint = packet.anchorPos();
                        data.relativeBubbleBlocks = packet.relativeBubbleBlocks();

                        // 1. Das Einweg-Mesh aus den Daten bauen
                        MeshData meshData = ShieldRenderer.buildShieldMesh(data.relativeBubbleBlocks);

                        // 2. Das Mesh fest in die Grafikkarte (VBO) laden
                        if (meshData != null) {
                            // Wenn schon ein altes Schild existiert, den Speicher erst freigeben! (Memory Leak verhindern)
                            if (data.vertexBuffer != null) {
                                data.vertexBuffer.close();
                            }

                            // Neuen VBO erstellen und Daten hochladen
                            data.vertexBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
                            data.vertexBuffer.bind();
                            data.vertexBuffer.upload(meshData);
                            VertexBuffer.unbind();
                        }
                    });
                }
        );
    }

    private void registerCapabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.SPACESHIP_REACTOR_BE.get(),
                (be, side) -> be.getEnergyStorage()
        );
    }
}