package com.lit.spaceships.block.entity;

import com.lit.spaceships.registry.ModBlockEntities;
import com.lit.spaceships.world.Telemetry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Transponder-Beacon BlockEntity (Epoch 10): haelt seine Frequenz
 * (DISTRESS_CALL / RESEARCH_BEACON / ANOMALOUS_RELIC) und tickt serverseitig
 * als sichtbarer Hinweis (gelegentlicher Ping-Sound) — das Signalscope scannt
 * genau diese BlockEntities innerhalb von 512 Bloecken.
 */
public class BeaconBlockEntity extends BlockEntity {

    /** Default-Frequenz: Notruf. */
    public static final ResourceLocation DEFAULT_FREQUENCY = Telemetry.Frequencies.DISTRESS_CALL;

    /** Ping-Intervall in Ticks (alle 7 Sekunden ein hooerbarer Hinweis). */
    public static final int PING_INTERVAL_TICKS = 140;

    private ResourceLocation frequency = DEFAULT_FREQUENCY;

    public BeaconBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BEACON_BE.get(), pos, state);
    }

    public ResourceLocation frequency() {
        return frequency;
    }

    public void setFrequency(ResourceLocation frequency) {
        this.frequency = frequency;
        setChanged();
    }

    /** Server-Tick: gelegentlicher Hinweis-Ping (diegetisches Sonar). */
    public static void serverTick(Level level, BlockPos pos, BlockState state, BeaconBlockEntity beacon) {
        if (level.getGameTime() % PING_INTERVAL_TICKS == 0) {
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BIT.value(),
                    net.minecraft.sounds.SoundSource.BLOCKS, 0.6F,
                    0.7F + level.random.nextFloat() * 0.3F);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("Frequency", frequency.toString());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.frequency = ResourceLocation.tryParse(tag.getString("Frequency")) != null
                ? ResourceLocation.parse(tag.getString("Frequency"))
                : DEFAULT_FREQUENCY;
    }

}
