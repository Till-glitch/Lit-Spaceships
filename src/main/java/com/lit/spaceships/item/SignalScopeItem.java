package com.lit.spaceships.item;

import com.lit.spaceships.block.entity.BeaconBlockEntity;
import com.lit.spaceships.world.Telemetry;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Signalscope (Epoch 10): richtungsabhaengiges Telemetrie-Handgeraet.
 * Rechtsklick scannt 512 Bloecke nach Transponder-Beacons; der Alignment-Wert
 * (L̂ · T̂) moduliert Ping-Pitch und die HUD-Wellenform; die Action-Bar
 * zeigt Frequenz, Richtung und Entfernung.
 */
public class SignalScopeItem extends Item {

    /** Chunk-Scan-Radius (512 Bloecke / 16 + Puffer). */
    public static final int CHUNK_SCAN_RADIUS = 33;

    public SignalScopeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            Vec3 listener = player.getEyePosition();
            Vec3 look = player.getLookAngle();
            List<Telemetry.BeaconSignal> signals = scanWorld(level, listener);

            Optional<Telemetry.TelemetryReading> reading =
                    Telemetry.bestSignal(listener, look, signals, Telemetry.MAX_RANGE);

            if (reading.isPresent()) {
                Telemetry.TelemetryReading r = reading.get();
                level.playSound(null, player.blockPosition(),
                        SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS,
                        0.8F, r.pingPitch());
                player.displayClientMessage(Component.translatable(
                        "item.lit_spaceships.signalscope.signal",
                        r.signal().frequency().toString(),
                        Math.round((r.alignment() + 1.0D) * 50.0D),
                        (int) r.distance()), true);
            } else {
                level.playSound(null, player.blockPosition(),
                        SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 0.6F, 0.5F);
                player.displayClientMessage(Component.translatable(
                        "item.lit_spaceships.signalscope.none"), true);
            }
            player.getCooldowns().addCooldown(this, 20);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /**
     * Sammelt alle Transponder-Signale geladener Chunks innerhalb der Reichweite.
     * Chunk-Budget: nur bereits geladene Chunks im Scan-Quadrat werden gelesen.
     * Public fuer GameTests.
     */
    public static List<Telemetry.BeaconSignal> scanWorld(Level level, Vec3 listener) {
        List<Telemetry.BeaconSignal> signals = new ArrayList<>();
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return signals;
        }
        int listenerChunkX = net.minecraft.core.BlockPos.containing(listener).getX() >> 4;
        int listenerChunkZ = net.minecraft.core.BlockPos.containing(listener).getZ() >> 4;
        for (int cx = listenerChunkX - CHUNK_SCAN_RADIUS; cx <= listenerChunkX + CHUNK_SCAN_RADIUS; cx++) {
            for (int cz = listenerChunkZ - CHUNK_SCAN_RADIUS; cz <= listenerChunkZ + CHUNK_SCAN_RADIUS; cz++) {
                LevelChunk chunk = serverLevel.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (net.minecraft.core.BlockPos bePos : chunk.getBlockEntitiesPos()) {
                    if (chunk.getBlockEntity(bePos) instanceof BeaconBlockEntity beacon
                            && bePos.getCenter().distanceTo(listener) <= Telemetry.MAX_RANGE) {
                        signals.add(new Telemetry.BeaconSignal(beacon.frequency(),
                                Vec3.atCenterOf(bePos)));
                    }
                }
            }
        }
        return signals;
    }
}
