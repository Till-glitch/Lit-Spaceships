package com.peaceman.alpha.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.peaceman.alpha.block.entity.AbstractLaserNodeBlockEntity;
import com.peaceman.alpha.ship.combat.LaserWeaponTier;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;

/**
 * Dynamischer Renderer für ausrichtbare Geschütztürme (Pulse Laser, Heavy Beam,
 * Mining Laser).
 * Unterstützt State-Extraction und dynamische Wand-/Decken-/Boden-Montage via
 * FACING Quaternion-Transformation.
 */
public class TurretBlockEntityRenderer<T extends AbstractLaserNodeBlockEntity> implements BlockEntityRenderer<T> {

    private final BlockRenderDispatcher blockRenderer;

    public TurretBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(T laserBE, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
            int packedLight, int packedOverlay) {
        // 1. Extrahiere den Render-State
        LaserNodeRenderState renderState = LaserNodeRenderState.extract(laserBE, partialTick);

        // 2. Führe submit-Transformation und Rendering durch
        submit(renderState, poseStack, bufferSource, packedLight, packedOverlay);
    }

    /**
     * Führt die Matrizen-Transformationen und das Rendering der Turret-Geometrie
     * aus.
     */
    public void submit(LaserNodeRenderState renderState, PoseStack poseStack, MultiBufferSource bufferSource,
            int packedLight, int packedOverlay) {
        poseStack.pushPose();

        // 1. In die absolute Mitte des Block-Spaces verschieben (für die Wand-Rotation)
        poseStack.translate(0.5D, 0.5D, 0.5D);

        // 2. Das gesamte Koordinatensystem an die Wand rotieren, an der der Block klebt
        if (renderState.facing() != null) {
            poseStack.mulPose(com.peaceman.alpha.ship.combat.aim.AimTransformMath.getRotationForFacing(renderState.facing()));
        }

        // 3. Zurück an den lokalen Nullpunkt des nun rotierten Blocks verschieben
        poseStack.translate(-0.5D, -0.5D, -0.5D);

        // 4. Zum Pivot-Punkt des Turrets verschieben.
        // X und Z sind 0.5 (Mitte). Y ist 0.25 (Da die Laser-Basisplatte 4 Voxel dick
        // ist = 4/16 = 0.25)
        poseStack.translate(0.5D, 0.25D, 0.5D);

        // 5. Jetzt die Schwenk-Kinematik des Geschützes anwenden (180 Grad Offset, da Modell nach Norden zeigt)
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - renderState.getYaw()));
        poseStack.mulPose(Axis.XP.rotationDegrees(-renderState.getPitch()));



        // 6. Zurück-Translation, um das zentrierte Blockbench-Modell zu rendern
        // X und Z um 0.5 zurück. Y bleibt 0, da der Turret in Blockbench bei Y=0
        // anfängt
        poseStack.translate(-0.5D, 0.0D, -0.5D);

        // 7. Rendern des JSON-Modells
        ResourceLocation modelLoc;
        if (renderState.getTier() == LaserWeaponTier.HEAVY_BEAM) {
            modelLoc = ResourceLocation.fromNamespaceAndPath("peaceman_alpha", "block/laser_turret_heavy");
        } else if (renderState.getTier() == LaserWeaponTier.MINING_LASER) {
            modelLoc = ResourceLocation.fromNamespaceAndPath("peaceman_alpha", "block/laser_turret_mining");
        } else {
            modelLoc = ResourceLocation.fromNamespaceAndPath("peaceman_alpha", "block/laser_turret_pulse");
        }

        ModelResourceLocation mrl = new ModelResourceLocation(modelLoc, "standalone");
        BakedModel bakedModel = this.blockRenderer.getBlockModelShaper().getModelManager().getModel(mrl);

        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.cutout());
        this.blockRenderer.getModelRenderer().renderModel(poseStack.last(), vertexConsumer, null, bakedModel, 1.0f,
                1.0f, 1.0f, packedLight, packedOverlay, net.neoforged.neoforge.client.model.data.ModelData.EMPTY, RenderType.cutout());

        poseStack.popPose();
    }
}
