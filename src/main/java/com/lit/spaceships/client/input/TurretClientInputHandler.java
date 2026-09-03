package com.lit.spaceships.client.input;

import com.lit.spaceships.LitSpaceships;
import com.lit.spaceships.block.entity.AbstractLaserNodeBlockEntity;
import com.lit.spaceships.entity.TurretSeatEntity;
import com.lit.spaceships.network.TurretAimSyncPayload;
import com.lit.spaceships.network.TurretLockTogglePayload;
import com.lit.spaceships.ship.combat.aim.AimAngles;
import com.lit.spaceships.ship.combat.aim.AimTransformMath;
import com.lit.spaceships.ship.combat.aim.GimbalLimits;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Erfasst Mauseingaben und Klicks des Gunners in Echtzeit.
 * - Kontinuierlicher Aim-Sync (Client -> Server) via TurretAimSyncPayload
 * - Klick-Interceptor (Linksklick -> Server) via TurretLockTogglePayload
 */
@EventBusSubscriber(modid = LitSpaceships.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class TurretClientInputHandler {

    private static float lastSentYaw = 0.0f;
    private static float lastSentPitch = 0.0f;
    private static int tickCounter = 0;
    private static boolean wasRiding = false;
    private static final float DELTA_THRESHOLD_DEGREES = 0.5f;
    private static long lastLockToggleTick = 0L;

    private static void triggerLockToggle(BlockPos weaponPos, String source) {
        Minecraft mc = Minecraft.getInstance();
        long currentTick = mc.level != null ? mc.level.getGameTime() : 0L;
        if (currentTick - lastLockToggleTick < 2L && lastLockToggleTick != 0L) {
            return;
        }
        lastLockToggleTick = currentTick;
        com.lit.spaceships.helper.TurretDebugLogger.logClientLockTriggered(weaponPos, source);
        PacketDistributor.sendToServer(new TurretLockTogglePayload(weaponPos));
    }

    /**
     * Interceptet Aktionen (Angriff / Abbauen / Item-Nutzung) des im Geschütz sitzenden Spielers.
     */
    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack() && !event.isUseItem()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (mc.player.getVehicle() instanceof TurretSeatEntity seat) {
            BlockPos weaponPos = seat.getWeaponPos();
            if (weaponPos != null) {
                if (event.isAttack()) {
                    event.setCanceled(true);
                    event.setSwingHand(false);
                    triggerLockToggle(weaponPos, "InteractionKeyMappingTriggered/Attack");
                } else if (event.isUseItem()) {
                    event.setCanceled(true);
                    event.setSwingHand(false);
                    PacketDistributor.sendToServer(new com.lit.spaceships.network.ShipCombatActionPayload(
                            java.util.Optional.ofNullable(seat.getShipId()),
                            com.lit.spaceships.network.ShipCombatActionPayload.CombatAction.FIRE_SPECIFIC,
                            java.util.Optional.of(weaponPos)
                    ));
                }
            }
        }
    }

    /**
     * Fängt rohe Mausklicks (Button 0 = Linksklick) im Client ab, auch bei Klick in die leere Luft / den Himmel.
     */
    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT && event.getAction() == GLFW.GLFW_PRESS) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.player.getVehicle() instanceof TurretSeatEntity seat) {
                BlockPos weaponPos = seat.getWeaponPos();
                if (weaponPos != null) {
                    event.setCanceled(true);
                    triggerLockToggle(weaponPos, "MouseButton.Pre/GLFW_LEFT");
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        if (event.getEntity() != null && event.getEntity().getVehicle() instanceof TurretSeatEntity seat) {
            BlockPos weaponPos = seat.getWeaponPos();
            if (weaponPos != null) {
                triggerLockToggle(weaponPos, "PlayerInteractEvent.LeftClickEmpty");
            }
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntity() != null && event.getEntity().getVehicle() instanceof TurretSeatEntity seat) {
            BlockPos weaponPos = seat.getWeaponPos();
            if (weaponPos != null) {
                event.setCanceled(true);
                triggerLockToggle(weaponPos, "PlayerInteractEvent.LeftClickBlock");
            }
        }
    }

    /**
     * Sendet kontinuierlich alle 2 Ticks oder bei Winkeländerung die Zielausrichtung der Spielerkamera an den Server.
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            com.lit.spaceships.client.screen.hud.TacticalConsoleHudLayer.activeTacticalShipId = null;
            wasRiding = false;
            tickCounter = 0;
            return;
        }

        if (mc.player.getVehicle() instanceof TurretSeatEntity seat) {
            com.lit.spaceships.client.screen.hud.TacticalConsoleHudLayer.activeTacticalShipId = seat.getShipId();
            BlockPos weaponPos = seat.getWeaponPos();
            if (weaponPos == null) return;

            if (mc.level.getBlockEntity(weaponPos) instanceof AbstractLaserNodeBlockEntity laserBE) {
                if (laserBE.isAimLocked()) {
                    // Arretierter Zustand: Keine Kamera-Nachführung
                    wasRiding = true;
                    return;
                }

                GimbalLimits limits = laserBE.getGimbalLimits();

                // 1. Lies Rotation direkt vom lokalen Spieler aus
                float playerYaw = net.minecraft.util.Mth.wrapDegrees(mc.player.getYRot());
                float playerPitch = net.minecraft.util.Mth.clamp(mc.player.getXRot(), -90.0f, 90.0f);

                // 1.5 Wandle die globale/Schiffs-lokale Spieler-Rotation in eine wand-lokale Rotation um!
                net.minecraft.world.phys.Vec3 globalLookVec = AimTransformMath.calculateWorldLookVector(playerYaw, playerPitch);
                org.joml.Vector3f wallLocalVec = globalLookVec.toVector3f();
                
                net.minecraft.core.Direction facing = net.minecraft.core.Direction.UP;
                if (laserBE.getBlockState().hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING)) {
                    facing = laserBE.getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
                }
                
                org.joml.Quaternionf inverseWallRot = com.lit.spaceships.ship.combat.aim.AimTransformMath.getRotationForFacing(facing).conjugate();
                wallLocalVec.rotate(inverseWallRot);
                
                float wallYaw = (float) Math.toDegrees(Math.atan2(-wallLocalVec.x(), wallLocalVec.z()));
                float wallPitch = (float) Math.toDegrees(Math.asin(net.minecraft.util.Mth.clamp(-wallLocalVec.y(), -1.0, 1.0)));

                AimAngles angles = new AimAngles(wallYaw, wallPitch);
                if (limits != null) {
                    angles = limits.clamp(angles);
                }

                // 2. Sofortige lokale Client-Side Prediction (keine Wahrnehmungsverzögerung für den Gunner)
                laserBE.setAimAngles(angles);

                // 3. Kontinuierlicher Sync an den Server (alle 2 Ticks oder bei Delta >= 0.5°)
                tickCounter++;
                float deltaYaw = Math.abs(AimTransformMath.normalizeAngleDelta(angles.yaw(), lastSentYaw));
                float deltaPitch = Math.abs(AimTransformMath.normalizeAngleDelta(angles.pitch(), lastSentPitch));

                if (!wasRiding || deltaYaw >= DELTA_THRESHOLD_DEGREES || deltaPitch >= DELTA_THRESHOLD_DEGREES || tickCounter >= 2) {
                    com.lit.spaceships.helper.TurretDebugLogger.logClientAimSent(weaponPos, angles.yaw(), angles.pitch());
                    PacketDistributor.sendToServer(new TurretAimSyncPayload(weaponPos, angles.yaw(), angles.pitch()));

                    lastSentYaw = angles.yaw();
                    lastSentPitch = angles.pitch();
                    tickCounter = 0;
                    wasRiding = true;
                }
            }
        } else {
            com.lit.spaceships.client.screen.hud.TacticalConsoleHudLayer.activeTacticalShipId = null;
            wasRiding = false;
            tickCounter = 0;
        }
    }
}
