package com.lit.spaceships.world;

import com.lit.spaceships.LitSpaceships;
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
        Optional<EffectPlan> plan = planFor(biomeKey, player.getRandom());
        if (plan.isEmpty() || player.hasEffect(plan.get().effect())) {
            return;
        }
        EffectPlan effect = plan.get();
        player.addEffect(effect.toInstance());
        player.displayClientMessage(effect.toMessage(), true);
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
