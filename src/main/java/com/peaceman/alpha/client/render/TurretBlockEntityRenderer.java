package com.peaceman.alpha.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.peaceman.alpha.block.entity.AbstractLaserNodeBlockEntity;
import com.peaceman.alpha.ship.combat.LaserWeaponTier;
import com.peaceman.alpha.ship.combat.aim.AimTransformMath;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;

/**
 * Dynamischer Renderer für ausrichtbare Geschütztürme (Pulse Laser & Mining Laser).
 * Nutzt hierarchische PoseStack-Transformationen und 180°-Wrap-fähige Interpolation.
 */
public class TurretBlockEntityRenderer<T extends AbstractLaserNodeBlockEntity> implements BlockEntityRenderer<T> {

    public TurretBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(T laserBE, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // 1. Berechne interpolierte Winkel für geschmeidige 60+ FPS Animation
        float renderYaw = AimTransformMath.interpolateAngle(laserBE.getPrevTargetYaw(), laserBE.getTargetYaw(), partialTick);
        float renderPitch = AimTransformMath.interpolateAngle(laserBE.getPrevTargetPitch(), laserBE.getTargetPitch(), partialTick);

        poseStack.pushPose();

        // 2. Verschiebe Matrix ins Blockzentrum
        poseStack.translate(0.5, 0.5, 0.5);

        VertexConsumer solidBuffer = bufferSource.getBuffer(RenderType.solid());

        // 3. Turm-Sockel (Gieren um die vertikale Y-Achse passend zur Blickrichtung)
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-renderYaw));

        // Rendere Sockel-Plattform (Grauer Stahl)
        renderBox(poseStack, solidBuffer, -0.3f, -0.45f, -0.3f, 0.3f, -0.25f, 0.3f, 0.25f, 0.27f, 0.30f, 1.0f, packedLight, packedOverlay);

        // 4. Turm-Geschütz (Neigen um die horizontale X-Achse)
        poseStack.pushPose();
        poseStack.translate(0.0, -0.15, 0.0);
        poseStack.mulPose(Axis.XP.rotationDegrees(renderPitch));

        // Geschützkörper (Dunkelgrau / Graphit)
        renderBox(poseStack, solidBuffer, -0.2f, -0.1f, -0.2f, 0.2f, 0.15f, 0.2f, 0.18f, 0.20f, 0.22f, 1.0f, packedLight, packedOverlay);

        // Doppelläufe (Kanone nach vorne entlang +Z)
        renderBox(poseStack, solidBuffer, -0.15f, -0.05f, -0.1f, -0.05f, 0.05f, 0.7f, 0.35f, 0.38f, 0.42f, 1.0f, packedLight, packedOverlay);
        renderBox(poseStack, solidBuffer, 0.05f, -0.05f, -0.1f, 0.15f, 0.05f, 0.7f, 0.35f, 0.38f, 0.42f, 1.0f, packedLight, packedOverlay);

        // Leuchtende Mündungs-Emissions-Kerne (Farbe je nach Waffen-Tier an der Mündung +Z)
        float r = 0.0f, g = 0.9f, b = 1.0f; // Default Cyan (Pulse)
        if (laserBE.getTier() == LaserWeaponTier.MINING_LASER) {
            r = 1.0f; g = 0.7f; b = 0.0f; // Amber/Gold (Mining)
        } else if (laserBE.getTier() == LaserWeaponTier.HEAVY_BEAM) {
            r = 1.0f; g = 0.1f; b = 0.2f; // Crimson Red (Heavy)
        }

        renderBox(poseStack, solidBuffer, -0.14f, -0.04f, 0.70f, -0.06f, 0.04f, 0.72f, r, g, b, 1.0f, 15728880, packedOverlay);
        renderBox(poseStack, solidBuffer, 0.06f, -0.04f, 0.70f, 0.14f, 0.04f, 0.72f, r, g, b, 1.0f, 15728880, packedOverlay);

        poseStack.popPose(); // Pop Cannon
        poseStack.popPose(); // Pop Swivel
        poseStack.popPose(); // Pop Root
    }

    private void renderBox(PoseStack poseStack, VertexConsumer buffer,
                           float minX, float minY, float minZ,
                           float maxX, float maxY, float maxZ,
                           float r, float g, float b, float a,
                           int packedLight, int packedOverlay) {
        Matrix4f mat = poseStack.last().pose();

        // Front Face (Z-)
        vertex(buffer, mat, minX, minY, minZ, r, g, b, a, packedLight, packedOverlay, 0, 0, -1);
        vertex(buffer, mat, minX, maxY, minZ, r, g, b, a, packedLight, packedOverlay, 0, 0, -1);
        vertex(buffer, mat, maxX, maxY, minZ, r, g, b, a, packedLight, packedOverlay, 0, 0, -1);
        vertex(buffer, mat, maxX, minY, minZ, r, g, b, a, packedLight, packedOverlay, 0, 0, -1);

        // Back Face (Z+)
        vertex(buffer, mat, maxX, minY, maxZ, r, g, b, a, packedLight, packedOverlay, 0, 0, 1);
        vertex(buffer, mat, maxX, maxY, maxZ, r, g, b, a, packedLight, packedOverlay, 0, 0, 1);
        vertex(buffer, mat, minX, maxY, maxZ, r, g, b, a, packedLight, packedOverlay, 0, 0, 1);
        vertex(buffer, mat, minX, minY, maxZ, r, g, b, a, packedLight, packedOverlay, 0, 0, 1);

        // Top Face (Y+)
        vertex(buffer, mat, minX, maxY, minZ, r, g, b, a, packedLight, packedOverlay, 0, 1, 0);
        vertex(buffer, mat, minX, maxY, maxZ, r, g, b, a, packedLight, packedOverlay, 0, 1, 0);
        vertex(buffer, mat, maxX, maxY, maxZ, r, g, b, a, packedLight, packedOverlay, 0, 1, 0);
        vertex(buffer, mat, maxX, maxY, minZ, r, g, b, a, packedLight, packedOverlay, 0, 1, 0);

        // Bottom Face (Y-)
        vertex(buffer, mat, maxX, minY, minZ, r, g, b, a, packedLight, packedOverlay, 0, -1, 0);
        vertex(buffer, mat, maxX, minY, maxZ, r, g, b, a, packedLight, packedOverlay, 0, -1, 0);
        vertex(buffer, mat, minX, minY, maxZ, r, g, b, a, packedLight, packedOverlay, 0, -1, 0);
        vertex(buffer, mat, minX, minY, minZ, r, g, b, a, packedLight, packedOverlay, 0, -1, 0);

        // Left Face (X-)
        vertex(buffer, mat, minX, minY, maxZ, r, g, b, a, packedLight, packedOverlay, -1, 0, 0);
        vertex(buffer, mat, minX, maxY, maxZ, r, g, b, a, packedLight, packedOverlay, -1, 0, 0);
        vertex(buffer, mat, minX, maxY, minZ, r, g, b, a, packedLight, packedOverlay, -1, 0, 0);
        vertex(buffer, mat, minX, minY, minZ, r, g, b, a, packedLight, packedOverlay, -1, 0, 0);

        // Right Face (X+)
        vertex(buffer, mat, maxX, minY, minZ, r, g, b, a, packedLight, packedOverlay, 1, 0, 0);
        vertex(buffer, mat, maxX, maxY, minZ, r, g, b, a, packedLight, packedOverlay, 1, 0, 0);
        vertex(buffer, mat, maxX, maxY, maxZ, r, g, b, a, packedLight, packedOverlay, 1, 0, 0);
        vertex(buffer, mat, maxX, minY, maxZ, r, g, b, a, packedLight, packedOverlay, 1, 0, 0);
    }

    private void vertex(VertexConsumer buffer, Matrix4f mat, float x, float y, float z,
                        float r, float g, float b, float a,
                        int light, int overlay, float nx, float ny, float nz) {
        buffer.addVertex(mat, x, y, z)
                .setColor(r, g, b, a)
                .setUv(0, 0)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(nx, ny, nz);
    }
}
