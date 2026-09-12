package com.lit.spaceships.world;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * DecompressionService — Hardspace-artige Druckverlust-Simulation (Epoch 9).
 *
 * <p>Zerstoert ein Spieler einen Block des Tags {@code lit_spaceships:pressurized_hull},
 * wird die 5x5x5-Umgebung auf "versiegelte Luft" gescannt: Findet sich eine
 * nennenswerte Luft-Kaverne hinter dem Block, bricht Dekompression aus —
 * 60 Ticks lang werden Entities und Items mit einem Sog-Vektor Richtung Bruch
 * hinausgetragen (Outward-Impuls zur Oeffnung hin).</p>
 *
 * <p>Tick-sicher: aktive Dekompressionen werden in einer statischen Queue
 * verarbeitet (ServerTickEvent.Post), niemals ueber Chunk-Grenzen geladen.</p>
 */
@EventBusSubscriber(modid = "lit_spaceships")
public final class DecompressionService {

    /** Block-Tag pressurisierter Huellenbloecke. */
    public static final TagKey<net.minecraft.world.level.block.Block> PRESSURIZED_HULL =
            TagKey.create(net.minecraft.core.registries.Registries.BLOCK,
                    ResourceLocation.fromNamespaceAndPath("lit_spaceships", "pressurized_hull"));

    /** Scan-Volumen um den zerstoerten Block (5x5x5 => Radius 2). */
    public static final int SCAN_RADIUS = 2;
    /** Mindestanzahl Luftbloecke im Scanvolumen fuer "Kaverne vorhanden". */
    public static final int MIN_CAVITY_AIR = 6;
    /** Dauer des Sogs in Ticks. */
    public static final int IMPULSE_DURATION_TICKS = 60;
    /** Sog-Staerke pro Tick (Bloecke/Tick). */
    public static final double PULL_STRENGTH = 0.045D;
    /** Wirkradius um die Bruchstelle. */
    public static final double AFFECTED_RADIUS = 6.0D;

    private static final List<ActiveDecompression> ACTIVE = new LinkedList<>();

    private record ActiveDecompression(Vec3 breachPos, long tickEnd) {
    }

    private DecompressionService() {
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Level level = (Level) event.getLevel();
        if (level.isClientSide()) {
            return;
        }
        BlockState state = event.getState();
        if (!state.is(PRESSURIZED_HULL)) {
            return;
        }
        BlockPos breachPos = event.getPos();
        if (countCavityAir(level, breachPos) < MIN_CAVITY_AIR) {
            return;
        }
        // Dekompression: 60 Ticks Sog Richtung Bruch
        ACTIVE.add(new ActiveDecompression(Vec3.atCenterOf(breachPos),
                level.getGameTime() + IMPULSE_DURATION_TICKS));
    }

    @SubscribeEvent
    public static void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        Iterator<ActiveDecompression> iterator = ACTIVE.iterator();
        while (iterator.hasNext()) {
            ActiveDecompression active = iterator.next();
            if (event.getServer().overworld().getGameTime() > active.tickEnd()) {
                iterator.remove();
                continue;
            }
            applyOutwardImpulse(event.getServer().overworld(), active.breachPos());
        }
    }

    /**
     * Zaehlt Luftbloecke im 5x5x5-Scanvolumen hinter der Bruchstelle.
     * Public fuer GameTests.
     */
    public static int countCavityAir(Level level, BlockPos breachPos) {
        int air = 0;
        for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx++) {
            for (int dy = -SCAN_RADIUS; dy <= SCAN_RADIUS; dy++) {
                for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz++) {
                    BlockPos pos = breachPos.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir() || state.is(Blocks.WATER)) {
                        air++;
                    }
                }
            }
        }
        return air;
    }

    /**
     * Wendet den Auswaerts-Sog auf alle Entities/Items nahe der Bruchstelle an.
     * Der Impuls zeigt von der Kaverne Richtung Bruch (nach draussen).
     * Public fuer GameTests.
     */
    public static void applyOutwardImpulse(Level level, Vec3 breachPos) {
        List<Entity> entities = level.getEntities((Entity) null,
                net.minecraft.world.phys.AABB.ofSize(breachPos, AFFECTED_RADIUS * 2, AFFECTED_RADIUS * 2, AFFECTED_RADIUS * 2),
                entity -> !entity.isSpectator());
        for (Entity entity : entities) {
            // Richtung: von der Entity zur Bruchstelle (hinausgetragen werden)
            Vec3 pull = breachPos.subtract(entity.position()).normalize().scale(PULL_STRENGTH);
            entity.setDeltaMovement(entity.getDeltaMovement().add(pull));
            entity.hasImpulse = true;
            if (entity instanceof ItemEntity item) {
                item.setPickUpDelay(10); // Item wird "mitgerissen" statt sofort eingesammelt
            }
        }
    }
}
