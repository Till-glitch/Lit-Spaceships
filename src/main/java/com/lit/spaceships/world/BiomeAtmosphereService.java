package com.lit.spaceships.world;

import com.lit.spaceships.LitSpaceships;
import com.lit.spaceships.world.feature.GravityRiftFeature;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Optional;

/**
 * Biome-Atmosphaeren-Dienst: gibt jedem Biom seinen eigenen spuerbaren
 * Charakter — kurze, milde, thematische Statuseffekte mit Action-Bar-Nachricht,
 * damit sich Reisen durch den Void belohnt und abwechslungsreich anfuehlt.
 *
 * <p>Playability-Regeln: Effekte sind kurz (4-15 s), selten (Erwartungswert
 * weit ueber 20 s Pause), stapeln nicht (bereits aktive Effekte werden
 * uebersprungen) und treffen Kreativ-/Zuschauer-Spieler nicht.</p>
 *
 * <ul>
 *   <li><b>Plasma Nebula:</b> "Statische Aufladung" (Leuchten) — der Nebel
 *       haftet am Anzug; "Plasma-Aufwind" (Sanfter Fall) — Aufwinde tragen.</li>
 *   <li><b>Frozen Expanse:</b> "Unterkuehlung" (Langsamkeit) — kurze Kaelte-
 *       Starre im Eiskornado.</li>
 *   <li><b>Void Wastes:</b> "Sensorische Deprivation" (Dunkelheit) — die
 *       Stille drueckt auf die Augen. Bewusst verstärkend, kurz.</li>
 *   <li><b>Deep Space:</b> neutral — die sichere Heimatbasis.</li>
 * </ul>
 */
@EventBusSubscriber(modid = LitSpaceships.MODID)
public final class BiomeAtmosphereService {

    /** Planungsintervall in Ticks (Effekt-Roll alle 1,5 s pro Spieler). */
    public static final int ROLL_INTERVAL_TICKS = 30;

    // Effekt-Chancen pro Roll (deterministisch testbar ueber RandomSource).
    public static final float NEBULA_STATIC_CHANCE = 0.015F;
    public static final float NEBULA_UPDRAFT_CHANCE = 0.020F;
    public static final float FROST_HYPOTHERMIA_CHANCE = 0.020F;
    public static final float WASTES_DREAD_CHANCE = 0.020F;

    public static final ResourceKey<Biome> NEBULA_KEY = ModBiomes.PLASMA_NEBULA;
    public static final ResourceKey<Biome> FROZEN_KEY = ModBiomes.FROZEN_EXPANSE;
    public static final ResourceKey<Biome> WASTES_KEY = ModBiomes.VOID_WASTES;

    /**
     * Ein geplanter Statuseffekt: MobEffekt, Dauer, Verstaerkung und die
     * i18n-Nachricht fuer die Action Bar.
     */
    public record EffectPlan(Holder<MobEffect> effect, int durationTicks, int amplifier, String messageKey) {
        public MobEffectInstance toInstance() {
            return new MobEffectInstance(effect, durationTicks, amplifier);
        }

        public Component toMessage() {
            return Component.translatable(messageKey);
        }
    }

    private BiomeAtmosphereService() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()
                || player.tickCount % ROLL_INTERVAL_TICKS != 0
                || player.isCreative() || player.isSpectator()) {
            return;
        }
        Holder<Biome> biomeHolder = player.level().getBiome(player.blockPosition());
        ResourceKey<Biome> biomeKey = biomeHolder.unwrapKey().orElse(null);
        if (biomeKey == null) {
            return;
        }
        // Epoch 8/9: extreme Biome mit positionsabhaengiger Logik
        if (NEBULA_KEY.equals(biomeKey) || FROZEN_KEY.equals(biomeKey) || WASTES_KEY.equals(biomeKey)) {
            Optional<EffectPlan> plan = planFor(biomeKey, player.getRandom());
            if (plan.isEmpty() || player.hasEffect(plan.get().effect())) {
                return;
            }
            EffectPlan effect = plan.get();
            player.addEffect(effect.toInstance());
            player.displayClientMessage(effect.toMessage(), true);
            return;
        }
        applyExtremeBiomeEffects(player, biomeKey);
    }

    /** Wendet die positionsabhaengigen Effekte der 3 extremen Biome an. */
    private static void applyExtremeBiomeEffects(Player player, ResourceKey<Biome> biomeKey) {
        RandomSource random = player.getRandom();
        if (ModBiomes.GRAVITY_RIFT.equals(biomeKey)) {
            ChunkPos chunk = new ChunkPos(player.blockPosition());
            var spec = GravityRiftFeature.specForCell(
                    Math.floorDiv(chunk.getMinBlockX(), GravityRiftFeature.CELL_SIZE),
                    Math.floorDiv(chunk.getMinBlockZ(), GravityRiftFeature.CELL_SIZE));
            planGravityShear(player.position(), spec, random).ifPresent(plan -> {
                player.push(plan.impulse());
                player.hasImpulse = true;
                if (plan.showAlert()) {
                    player.displayClientMessage(Component.translatable(
                            "biome.lit_spaceships.gravity_shear"), true);
                }
            });
            return;
        }
        if (ModBiomes.STELLAR_CORONA.equals(biomeKey)) {
            boolean protection = isRadiationProtected(player);
            planSolarRadiation(protection, random).ifPresent(plan -> {
                player.hurt(player.damageSources().onFire(), plan.damage());
                for (var slot : new net.minecraft.world.entity.EquipmentSlot[]{
                        net.minecraft.world.entity.EquipmentSlot.HEAD,
                        net.minecraft.world.entity.EquipmentSlot.CHEST,
                        net.minecraft.world.entity.EquipmentSlot.LEGS,
                        net.minecraft.world.entity.EquipmentSlot.FEET}) {
                    var stack = player.getItemBySlot(slot);
                    if (!stack.isEmpty() && !protection) {
                        stack.hurtAndBreak(plan.armorDegrade(), player, slot);
                    }
                }
                player.displayClientMessage(Component.translatable(plan.messageKey()), true);
            });
            return;
        }
        if (ModBiomes.ION_STORM.equals(biomeKey)) {
            boolean metal = isWearingMetalArmor(player);
            planIonStorm(player.isFallFlying(), metal, random).ifPresent(plan -> {
                if (plan.nausea()) {
                    player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
                }
                if (plan.elytraStall()) {
                    // Schub zusammenbrechen lassen (kurzer Stall)
                    player.setDeltaMovement(player.getDeltaMovement().scale(0.25D));
                    player.hasImpulse = true;
                }
                if (plan.lightning()) {
                    var bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(player.level());
                    if (bolt != null) {
                        bolt.setPos(player.position().add(
                                (random.nextDouble() - 0.5D) * 8.0D, 0.0D,
                                (random.nextDouble() - 0.5D) * 8.0D));
                        player.level().addFreshEntity(bolt);
                    }
                }
                player.displayClientMessage(Component.translatable(plan.messageKey()), true);
            });
        }
    }

    // ==================== Epoch 9: Extreme-Biome-Planner ====================

    /** Effektkonstanten (public fuer statistische Tests). */
    public static final float GRAVITY_SHEAR_MESSAGE_CHANCE = 0.10F;
    public static final double GRAVITY_PULL_MAX = 0.06D;
    public static final double GRAVITY_PULL_RANGE = 96.0D;
    public static final float SOLAR_RADIATION_CHANCE = 0.04F;
    public static final float SOLAR_RADIATION_DAMAGE = 1.0F;
    public static final int SOLAR_ARMOR_DEGRADE = 1;
    public static final float ION_NAUSEA_CHANCE = 0.015F;
    public static final float ION_LIGHTNING_CHANCE = 0.008F;
    public static final float ION_ELYTRA_STALL_CHANCE = 0.05F;

    /**
     * Gravity Rift: Gravitationsschub Richtung lokale Akkretionsmitte. Der
     * Impuls waechst mit der Naehe zur Singularitaet; die Warnung erscheint
     * nur bei einem Teil der Impulse (Anti-Spam).
     */
    public static Optional<GravityShearPlan> planGravityShear(Vec3 playerPos,
                                                              GravityRiftFeature.RiftSpec spec,
                                                              RandomSource random) {
        Vec3 center = new Vec3(spec.centerX(), spec.riftY(), spec.centerZ());
        Vec3 delta = center.subtract(playerPos);
        double distance = delta.length();
        if (distance > GRAVITY_PULL_RANGE || distance < 0.001D) {
            return Optional.empty();
        }
        double strength = GRAVITY_PULL_MAX * Math.max(0.3D, 1.0D - distance / GRAVITY_PULL_RANGE);
        Vec3 impulse = delta.normalize().scale(strength);
        boolean showAlert = random.nextFloat() < GRAVITY_SHEAR_MESSAGE_CHANCE;
        return Optional.of(new GravityShearPlan(impulse, showAlert));
    }

    public record GravityShearPlan(Vec3 impulse, boolean showAlert) {
    }

    /**
     * Stellar Corona: Solarstrahlung trifft ungeschuetzte Spieler (kein Gold-
     * oder Netherite-Ruestungsteil) und degradiert freiliegende Ruestung.
     */
    public static Optional<SolarRadiationPlan> planSolarRadiation(boolean protectedByGoldOrNetherite,
                                                                  RandomSource random) {
        if (protectedByGoldOrNetherite) {
            return Optional.empty();
        }
        if (random.nextFloat() >= SOLAR_RADIATION_CHANCE) {
            return Optional.empty();
        }
        return Optional.of(new SolarRadiationPlan(SOLAR_RADIATION_DAMAGE, SOLAR_ARMOR_DEGRADE,
                "biome.lit_spaceships.solar_radiation"));
    }

    public record SolarRadiationPlan(float damage, int armorDegrade, String messageKey) {
    }

    /**
     * Ion Storm: instruments-Scrambling (Uebelkeit), kurzzeitiger Elytra-Stall
     * und Blitzeinschlag bei Metallruestung.
     */
    public static Optional<IonPlan> planIonStorm(boolean fallFlying, boolean wearingMetalArmor,
                                                 RandomSource random) {
        boolean nausea = random.nextFloat() < ION_NAUSEA_CHANCE;
        boolean stall = fallFlying && random.nextFloat() < ION_ELYTRA_STALL_CHANCE;
        boolean lightning = wearingMetalArmor && random.nextFloat() < ION_LIGHTNING_CHANCE;
        if (!nausea && !stall && !lightning) {
            return Optional.empty();
        }
        return Optional.of(new IonPlan(nausea, stall, lightning, "biome.lit_spaceships.ion_interference"));
    }

    public record IonPlan(boolean nausea, boolean elytraStall, boolean lightning, String messageKey) {
    }

    /** Prueft, ob irgendein Ruestungsteil aus Gold oder Netherite besteht. */
    public static boolean isRadiationProtected(Player player) {
        for (var stack : player.getArmorSlots()) {
            if (stack.is(Items.GOLDEN_HELMET) || stack.is(Items.GOLDEN_CHESTPLATE)
                    || stack.is(Items.GOLDEN_LEGGINGS) || stack.is(Items.GOLDEN_BOOTS)
                    || stack.is(Items.NETHERITE_HELMET) || stack.is(Items.NETHERITE_CHESTPLATE)
                    || stack.is(Items.NETHERITE_LEGGINGS) || stack.is(Items.NETHERITE_BOOTS)) {
                return true;
            }
        }
        return false;
    }

    /** Prueft, ob irgendein Ruestungsteil aus Metall (leitet Strom) besteht. */
    public static boolean isWearingMetalArmor(Player player) {
        for (var stack : player.getArmorSlots()) {
            if (!stack.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Reine Entscheidungslogik (deterministisch per RandomSource): liefert den
     * naechsten Atmosphaereneffekt fuer das Biom oder Optional.empty().
     */
    public static Optional<EffectPlan> planFor(ResourceKey<Biome> biome, RandomSource random) {
        if (NEBULA_KEY.equals(biome)) {
            float roll = random.nextFloat();
            if (roll < NEBULA_STATIC_CHANCE) {
                return Optional.of(new EffectPlan(MobEffects.GLOWING, 100, 0,
                        "biome.lit_spaceships.nebula_static"));
            }
            if (roll < NEBULA_STATIC_CHANCE + NEBULA_UPDRAFT_CHANCE) {
                return Optional.of(new EffectPlan(MobEffects.SLOW_FALLING, 300, 0,
                        "biome.lit_spaceships.nebula_updraft"));
            }
            return Optional.empty();
        }
        if (FROZEN_KEY.equals(biome)) {
            if (random.nextFloat() < FROST_HYPOTHERMIA_CHANCE) {
                return Optional.of(new EffectPlan(MobEffects.MOVEMENT_SLOWDOWN, 80, 0,
                        "biome.lit_spaceships.frost_hypothermia"));
            }
            return Optional.empty();
        }
        if (WASTES_KEY.equals(biome)) {
            if (random.nextFloat() < WASTES_DREAD_CHANCE) {
                return Optional.of(new EffectPlan(MobEffects.DARKNESS, 160, 0,
                        "biome.lit_spaceships.wastes_dread"));
            }
            return Optional.empty();
        }
        // Deep Space: neutral
        return Optional.empty();
    }
}
