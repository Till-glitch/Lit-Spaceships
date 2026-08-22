package com.peaceman.alpha.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.peaceman.alpha.Alpha;
import com.peaceman.alpha.client.state.ClientLaserState;
import com.peaceman.alpha.client.state.ClientShipManager;
import com.peaceman.alpha.client.state.ClientShipState;
import com.peaceman.alpha.ship.combat.LaserWeaponTier;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.UUID;

/**
 * Blaze3D Client-Renderer für Laserstrahlen.
 * Rendert volumetrisch leuchtende Billboard-Strahlen mit additiver Farbüberlagerung.
 */
@EventBusSubscriber(modid = Alpha.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class LaserBeamRenderer {

    public static void addPulseBeam(UUID shooterShipId, Vec3 startPos, Vec3 endPos, LaserWeaponTier tier) {
        ClientLaserState.addPulse(shooterShipId, startPos, endPos, tier);
    }

    public static void setContinuousBeam(UUID shooterShipId, BlockPos weaponPos, boolean isFiring, LaserWeaponTier tier) {
        ClientLaserState.setContinuousBeam(shooterShipId, weaponPos, isFiring, tier);
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        long now = System.currentTimeMillis();
        ClientLaserState.cleanExpired(now);

        var pulses = ClientLaserState.getActivePulses();
        var continuous = ClientLaserState.getActiveContinuousBeams();

        if (pulses.isEmpty() && continuous.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;

        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();

        // 1. Additives Blend-Setup für leuchtende Sci-Fi-Strahlen
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        poseStack.pushPose();
        Matrix4f matrix = poseStack.last().pose();

        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        boolean hasDrawn = false;

        // 2. Pulse-Laser rendern (mit Zeit-Fade-Out)
        for (var pulse : pulses) {
            float progress = pulse.getProgress(now);
            float alpha = (1.0f - progress) * pulse.tier().getColorA();
            if (alpha <= 0.01f) continue;

            drawBeam(buffer, matrix, cameraPos, pulse.startPos(), pulse.endPos(), pulse.tier(), alpha);
            hasDrawn = true;
        }

        // 3. Kontinuierliche Strahlen rendern (Client-Side-Prediction)
        for (var entry : continuous.values()) {
            ClientShipState ship = ClientShipManager.getShip(entry.shooterShipId());
            if (ship == null || ship.isDisposed() || ship.getAnchorPos() == null || (ship.getDimension() != null && !ship.getDimension().equals(level.dimension()))) continue;

            BlockPos weaponWorldPos = ship.getAnchorPos().offset(entry.relativeWeaponPos());
            Direction facing = Direction.NORTH;
            if (level.isLoaded(weaponWorldPos)) {
                BlockState state = level.getBlockState(weaponWorldPos);
                if (state.hasProperty(BlockStateProperties.FACING)) {
                    facing = state.getValue(BlockStateProperties.FACING);
                }
            }

            Vec3 dir = Vec3.atLowerCornerOf(facing.getNormal()).normalize();
            Vec3 start = Vec3.atCenterOf(weaponWorldPos).add(dir.scale(0.55));
            Vec3 maxTarget = start.add(dir.scale(entry.tier().getMaxRange()));

            // Raycast gegen Vanilla-Terrain / Blöcke: Strahl stoppt direkt auf der Block-Oberfläche
            net.minecraft.world.phys.BlockHitResult blockHit = level.clip(new net.minecraft.world.level.ClipContext(
                    start, maxTarget, net.minecraft.world.level.ClipContext.Block.COLLIDER,
                    net.minecraft.world.level.ClipContext.Fluid.NONE,
                    net.minecraft.world.phys.shapes.CollisionContext.empty()
            ));

            Vec3 end = (blockHit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) ? blockHit.getLocation() : maxTarget;
            double closestDistSq = start.distanceToSqr(end);

            // Prüfung gegen Hüllenblöcke anderer Schiffe
            for (ClientShipState otherShip : ClientShipManager.getAllShips()) {
                if (otherShip == null || otherShip.isDisposed() || otherShip.getShipId().equals(entry.shooterShipId()) || otherShip.getAnchorPos() == null) {
                    continue;
                }

                BlockPos otherAnchor = otherShip.getAnchorPos();
                for (BlockPos relPos : otherShip.getRelativeStructureBlocks()) {
                    BlockPos worldVoxel = otherAnchor.offset(relPos);
                    net.minecraft.world.phys.AABB voxelBox = new net.minecraft.world.phys.AABB(worldVoxel);
                    java.util.Optional<Vec3> hit = voxelBox.clip(start, end);
                    if (hit.isPresent()) {
                        double dSq = start.distanceToSqr(hit.get());
                        if (dSq < closestDistSq) {
                            closestDistSq = dSq;
                            end = hit.get();
                        }
                    }
                }
            }

            // Oszillierendes leichtes Pulsieren des Dauerstrahls
            float pulseMod = (float) (0.85 + 0.15 * Math.sin(now * 0.02));
            drawBeam(buffer, matrix, cameraPos, start, end, entry.tier(), entry.tier().getColorA() * pulseMod);
            hasDrawn = true;
        }

        if (hasDrawn) {
            MeshData meshData = buffer.build();
            if (meshData != null) {
                BufferUploader.drawWithShader(meshData);
            }
        }

        poseStack.popPose();

        // 4. Render-Status sauber zurücksetzen
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
    }

    private static void drawBeam(BufferBuilder buffer, Matrix4f matrix, Vec3 cameraPos, Vec3 startWorld, Vec3 endWorld, LaserWeaponTier tier, float alpha) {
        Vec3 start = startWorld.subtract(cameraPos);
        Vec3 end = endWorld.subtract(cameraPos);
        Vec3 dir = end.subtract(start).normalize();

        Vec3 toCam = start.scale(-1.0).normalize();
        Vec3 side1 = dir.cross(toCam).normalize();
        if (side1.lengthSqr() < 0.001) {
            side1 = dir.cross(new Vec3(0, 1, 0)).normalize();
        }
        Vec3 side2 = dir.cross(side1).normalize();

        float glowRadius = tier == LaserWeaponTier.HEAVY_BEAM ? 0.35f : (tier == LaserWeaponTier.MINING_LASER ? 0.20f : 0.28f);
        float coreRadius = glowRadius * 0.28f;

        float r = tier.getColorR();
        float g = tier.getColorG();
        float b = tier.getColorB();

        // Äußerer farbiger Leucht-Zylinder (Glow)
        drawQuad(buffer, matrix, start, end, side1, glowRadius, r, g, b, alpha * 0.65f);
        drawQuad(buffer, matrix, start, end, side2, glowRadius, r, g, b, alpha * 0.65f);

        // Innerer weiß-heißer Kern
        drawQuad(buffer, matrix, start, end, side1, coreRadius, 1.0f, 1.0f, 1.0f, alpha * 0.95f);
        drawQuad(buffer, matrix, start, end, side2, coreRadius, 1.0f, 1.0f, 1.0f, alpha * 0.95f);
    }

    private static void drawQuad(BufferBuilder buffer, Matrix4f matrix, Vec3 start, Vec3 end, Vec3 normal, float radius, float r, float g, float b, float a) {
        Vec3 offset = normal.scale(radius);

        float x1 = (float) (start.x - offset.x);
        float y1 = (float) (start.y - offset.y);
        float z1 = (float) (start.z - offset.z);

        float x2 = (float) (start.x + offset.x);
        float y2 = (float) (start.y + offset.y);
        float z2 = (float) (start.z + offset.z);

        float x3 = (float) (end.x + offset.x);
        float y3 = (float) (end.y + offset.y);
        float z3 = (float) (end.z + offset.z);

        float x4 = (float) (end.x - offset.x);
        float y4 = (float) (end.y - offset.y);
        float z4 = (float) (end.z - offset.z);

        buffer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a);
        buffer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a);
        buffer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a);
        buffer.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a);
    }
}
