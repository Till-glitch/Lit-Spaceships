package com.peaceman.alpha.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.peaceman.alpha.block.entity.SpaceshipReactorBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import org.joml.Matrix4f;

public class ReactorBlockEntityRenderer implements BlockEntityRenderer<SpaceshipReactorBlockEntity> {

    public ReactorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(SpaceshipReactorBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        int energy = blockEntity.getEnergyStorage().getEnergyStored();
        int max = blockEntity.getEnergyStorage().getMaxEnergyStored();
        float percentage = max == 0 ? 0 : (float) energy / max;

        // Farbcodierung
        int color = 0x00FF00; // Grün (Voll)
        if (percentage < 0.2f) {
            color = 0xFF0000; // Rot (Kritisch)
        } else if (percentage < 0.5f) {
            color = 0xFFFFAA; // Gelb (Halb)
        }

        String text = String.format(java.util.Locale.ROOT, "%,d FE", energy);
        String pctText = String.format(java.util.Locale.ROOT, "%.1f%%", percentage * 100);

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        poseStack.pushPose();
        
        // Zentriere über dem Block
        poseStack.translate(0.5, 1.5, 0.5);

        float scale = 0.025f;
        
        // Die EntityViewRenderState / GuiTextRenderState Architektur in aktuelleren/zukünftigen NeoForge Versionen
        // erfordert oft vorbereiteten Text, aber der FontRenderer kapselt das zumeist noch in .drawInBatch.
        poseStack.mulPose(mc.gameRenderer.getMainCamera().rotation());
        poseStack.mulPose(com.mojang.math.Axis.XN.rotationDegrees(180));
        poseStack.scale(scale, scale, scale);

        Matrix4f matrix = poseStack.last().pose();
        
        // Hintergrund für Lesbarkeit
        int width1 = font.width(text);
        int width2 = font.width(pctText);
        
        float bgOpacity = mc.options.getBackgroundOpacity(0.25f);
        int bgColor = (int)(bgOpacity * 255.0f) << 24;

        font.drawInBatch(text, -width1 / 2f, 0, color, false, matrix, buffer, Font.DisplayMode.SEE_THROUGH, bgColor, packedLight);
        font.drawInBatch(text, -width1 / 2f, 0, color, false, matrix, buffer, Font.DisplayMode.NORMAL, 0, packedLight);

        font.drawInBatch(pctText, -width2 / 2f, 10, color, false, matrix, buffer, Font.DisplayMode.SEE_THROUGH, bgColor, packedLight);
        font.drawInBatch(pctText, -width2 / 2f, 10, color, false, matrix, buffer, Font.DisplayMode.NORMAL, 0, packedLight);

        poseStack.popPose();
    }
}
