package com.peaceman.alpha.client.render;

import org.joml.Quaternionf;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.client.state.ClientShipManager;
import com.peaceman.alpha.client.state.ClientShipState;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.joml.Matrix4f;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public class ShieldRenderer {

    // Uniforms für Animationen (global / Fallback)
    public static Vec3 lastImpactPos = Vec3.ZERO;
    public static float shieldEnergyPercentage = 1.0f;
    public static long lastImpactTick = -1000L;

    private static ShaderInstance hexShieldShader;

    @EventBusSubscriber(modid = Alpha.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModClientEvents {
        @SubscribeEvent
        public static void onRegisterShaders(RegisterShadersEvent event) throws IOException {
            event.registerShader(
                    new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Alpha.MODID, "hex_shield"), DefaultVertexFormat.POSITION_TEX),
                    shaderInstance -> hexShieldShader = shaderInstance
            );
        }
    }

    @EventBusSubscriber(modid = Alpha.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
    public static class ForgeClientEvents {

        @SubscribeEvent
        public static void onRenderLevelStage(RenderLevelStageEvent event) {
            // 1. Phasen-Validierung: Limitierung auf die Translucent-Stage
            if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
            if (hexShieldShader == null) return;

            Collection<ClientShipState> ships = ClientShipManager.getAllShips();
            if (ships.isEmpty()) return;

            // 2. Extraktion der Kern-Objekte
            Minecraft mc = Minecraft.getInstance();
            PoseStack poseStack = event.getPoseStack();
            Camera camera = event.getCamera();
            Vec3 cameraPos = camera.getPosition();

            Matrix4f projection = event.getProjectionMatrix();

            // Render-Pipeline Setup für Volumen-Rendering
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull(); // Backface Culling deaktiviert
            RenderSystem.setShader(() -> hexShieldShader);

            for (ClientShipState shipState : ships) {
                if (!shipState.isShieldActive() || shipState.getAnchorPos() == null || shipState.getShieldMesh() == null) {
                    continue;
                }

                BlockPos anchor = shipState.getAnchorPos();

                // 1. Nur rendern, wenn der Chunk auf dem Client geladen ist
                if (mc.level == null || !mc.level.isLoaded(anchor)) {
                    continue;
                }

                // 2. Maximaler Render-Abstand basierend auf der Render-Distanz des Spielers
                double maxRenderDist = (mc.options.getEffectiveRenderDistance() + 2) * 16.0;
                double distSq = anchor.distToCenterSqr(cameraPos.x, cameraPos.y, cameraPos.z);
                if (distSq > maxRenderDist * maxRenderDist) {
                    continue;
                }

                VertexBuffer mesh = shipState.getShieldMesh();

                // 3. State-Sicherung
                poseStack.pushPose();

                // 4. Extraktion und Invertierung der Kamera-Rotation
                Quaternionf cameraRotation = camera.rotation();
                Quaternionf inverseCamRot = new Quaternionf(cameraRotation).invert();

                // 5. Applikation der Ent-Drehung auf den PoseStack
                poseStack.mulPose(inverseCamRot);

                // 6. Berechnung und Applikation der relativen World-Translation
                double deltaX = anchor.getX() - cameraPos.x;
                double deltaY = anchor.getY() - cameraPos.y;
                double deltaZ = anchor.getZ() - cameraPos.z;
                poseStack.translate(deltaX, deltaY, deltaZ);

                // 7. Extraktion der konsistenten Matrix
                Matrix4f modelView = poseStack.last().pose();

                // 8. Eigene Uniforms füttern
                if (hexShieldShader.safeGetUniform("HexModelViewMat") != null) {
                    hexShieldShader.safeGetUniform("HexModelViewMat").set(modelView);
                }
                if (hexShieldShader.safeGetUniform("HexProjMat") != null) {
                    hexShieldShader.safeGetUniform("HexProjMat").set(projection);
                }

                // 9. Draw Call ausführen
                mesh.bind();
                mesh.drawWithShader(modelView, projection, hexShieldShader);
                VertexBuffer.unbind();

                // 10. State-Wiederherstellung
                poseStack.popPose();
            }

            // 11. Globales Cleanup
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
        }
    }

    /**
     * Baut ein effizientes Mesh aus den Voxel-Daten.
     * Nur die Seiten, die nach außen zeigen, werden gezeichnet!
     */
    public static MeshData buildShieldMesh(Set<BlockPos> relativeBlocks) {
        if (relativeBlocks == null || relativeBlocks.isEmpty()) return null;

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        for (BlockPos pos : relativeBlocks) {

            // Jeden Voxel prüfen
            for (Direction dir : Direction.values()) {
                // Nur Seiten zeichnen, die an Luft grenzen!
                if (!relativeBlocks.contains(pos.relative(dir))) {
                    drawFace(bufferbuilder, pos, dir);
                }
            }
        }

        try {
            return bufferbuilder.buildOrThrow();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Hilfsfunktion, um eine einzelne Voxel-Seite mit korrekten UVs zu zeichnen
     */
    /**
     * Hilfsfunktion, um eine einzelne Voxel-Seite mit korrekten UVs zu zeichnen.
     * Nutzt .endVertex(), um Speicherlecks und Textur-Verschiebungen zu verhindern.
     */
    /**
     * Hilfsfunktion, um eine einzelne Voxel-Seite mit korrekten UVs zu zeichnen (Minecraft 1.21+).
     */
    private static void drawFace(VertexConsumer buffer, BlockPos pos, Direction dir) {
        float x1 = pos.getX(); float y1 = pos.getY(); float z1 = pos.getZ();
        float x2 = x1 + 1.0f;  float y2 = y1 + 1.0f;  float z2 = z1 + 1.0f;

        // UV-Koordinaten basierend auf der Position
        float scale = 0.1f;

        switch (dir) {
            case DOWN -> {
                buffer.addVertex(x1, y1, z2).setUv(x1 * scale, z2 * scale);
                buffer.addVertex(x1, y1, z1).setUv(x1 * scale, z1 * scale);
                buffer.addVertex(x2, y1, z1).setUv(x2 * scale, z1 * scale);
                buffer.addVertex(x2, y1, z2).setUv(x2 * scale, z2 * scale);
            }
            case UP -> {
                buffer.addVertex(x1, y2, z1).setUv(x1 * scale, z1 * scale);
                buffer.addVertex(x1, y2, z2).setUv(x1 * scale, z2 * scale);
                buffer.addVertex(x2, y2, z2).setUv(x2 * scale, z2 * scale);
                buffer.addVertex(x2, y2, z1).setUv(x2 * scale, z1 * scale);
            }
            case NORTH -> {
                buffer.addVertex(x2, y2, z1).setUv(x2 * scale, y2 * scale);
                buffer.addVertex(x2, y1, z1).setUv(x2 * scale, y1 * scale);
                buffer.addVertex(x1, y1, z1).setUv(x1 * scale, y1 * scale);
                buffer.addVertex(x1, y2, z1).setUv(x1 * scale, y2 * scale);
            }
            case SOUTH -> {
                buffer.addVertex(x1, y2, z2).setUv(x1 * scale, y2 * scale);
                buffer.addVertex(x1, y1, z2).setUv(x1 * scale, y1 * scale);
                buffer.addVertex(x2, y1, z2).setUv(x2 * scale, y1 * scale);
                buffer.addVertex(x2, y2, z2).setUv(x2 * scale, y2 * scale);
            }
            case WEST -> {
                buffer.addVertex(x1, y2, z1).setUv(z1 * scale, y2 * scale);
                buffer.addVertex(x1, y1, z1).setUv(z1 * scale, y1 * scale);
                buffer.addVertex(x1, y1, z2).setUv(z2 * scale, y1 * scale);
                buffer.addVertex(x1, y2, z2).setUv(z2 * scale, y2 * scale);
            }
            case EAST -> {
                buffer.addVertex(x2, y2, z2).setUv(z2 * scale, y2 * scale);
                buffer.addVertex(x2, y1, z2).setUv(z2 * scale, y1 * scale);
                buffer.addVertex(x2, y1, z1).setUv(z1 * scale, y1 * scale);
                buffer.addVertex(x2, y2, z1).setUv(z1 * scale, y2 * scale);
            }
        }
    }
}