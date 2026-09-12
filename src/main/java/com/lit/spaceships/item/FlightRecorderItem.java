package com.lit.spaceships.item;

import com.lit.spaceships.world.Telemetry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Flight Recorder (Epoch 13): Space-Archaeologie-Item. Rechtsklick entschlüsselt
 * den Bordcomputer eines gefallenen Schiffs und zeigt übersetzte Log-Einträge
 * mit Hinweisen auf die Megastrukturen (Koordinaten-Ketten über Zellen-Specs).
 */
public class FlightRecorderItem extends Item {

    /** Log-Narrativen (i18n-Schluessel, en/de). */
    private static final List<String> LOG_KEYS = List.of(
            "item.lit_spaceships.flight_recorder.log1",
            "item.lit_spaceships.flight_recorder.log2",
            "item.lit_spaceships.flight_recorder.log3",
            "item.lit_spaceships.flight_recorder.log4",
            "item.lit_spaceships.flight_recorder.log5");

    public FlightRecorderItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            RandomSource random = level.getRandom();
            int logIndex = random.nextInt(LOG_KEYS.size());

            // Koordinaten-Hinweis: Richtung zum naechsten Megastruktur-Zellzentrum
            String clueKey = pickClueKey(random);
            player.sendSystemMessage(Component.translatable(
                    "item.lit_spaceships.flight_recorder.header").withStyle(ChatFormatting.GOLD));
            player.sendSystemMessage(Component.translatable(LOG_KEYS.get(logIndex))
                    .withStyle(ChatFormatting.GRAY));
            player.sendSystemMessage(Component.translatable(clueKey,
                    clueCoordinate(random)).withStyle(ChatFormatting.AQUA));
            player.sendSystemMessage(Component.translatable(
                    "item.lit_spaceships.flight_recorder.decrypted").withStyle(ChatFormatting.GREEN));

            level.playSound(null, player.blockPosition(),
                    net.minecraft.sounds.SoundEvents.NOTE_BLOCK_XYLOPHONE.value(),
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 0.6F);
            player.getCooldowns().addCooldown(this, 40);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** Namensbasierter Hinweis (deterministisch pro Log) auf die Ziel-Region. */
    private static String pickClueKey(RandomSource random) {
        return switch (random.nextInt(4)) {
            case 0 -> "item.lit_spaceships.flight_recorder.clue_freighter";
            case 1 -> "item.lit_spaceships.flight_recorder.clue_relay";
            case 2 -> "item.lit_spaceships.flight_recorder.clue_collector";
            default -> "item.lit_spaceships.flight_recorder.clue_outpost";
        };
    }

    /** Verschlüsselte Koordinate als Zell-Hinweis (XOR-Kette über die ID 13). */
    public static long clueCoordinate(RandomSource random) {
        return Telemetry.encryptCoordinate(random.nextInt(2048), 13);
    }
}
