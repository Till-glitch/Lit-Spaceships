package com.peaceman.alpha.client.render;

import com.peaceman.alpha.entity.TurretSeatEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * Unsichtbarer Renderer für TurretSeatEntity (Dummy-Sitz).
 */
public class TurretSeatRenderer extends EntityRenderer<TurretSeatEntity> {

    public TurretSeatRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(TurretSeatEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        return false;
    }

    @Override
    public ResourceLocation getTextureLocation(TurretSeatEntity entity) {
        return null;
    }
}
