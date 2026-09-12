package com.lit.spaceships.block;

import com.lit.spaceships.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;


/**
 * Reactor-Meltdown BlockEntity (Epoch 12): haelt den Zustand der
 * {@link ReactorMeltdownLogic}-State-Machine, tickt serverseitig und loest bei
 * Versagen eine Explosion aus. Nach erfolgreicher Kuehlung wird die
 * reactor_core_salvage-Loot-Table freigeschaltet und ausgegeben.
 */
public class UnstableReactorBlockEntity extends BlockEntity {

    public static final ResourceKey<net.minecraft.world.level.storage.loot.LootTable> CORE_LOOT =
            ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                    ResourceLocation.fromNamespaceAndPath("lit_spaceships", "chests/reactor_core_salvage"));

    private ReactorMeltdownLogic.ReactorState state = ReactorMeltdownLogic.ReactorState.INITIAL;

    public UnstableReactorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.UNSTABLE_REACTOR_BE.get(), pos, state);
    }

    public ReactorMeltdownLogic.ReactorState state() {
        return state;
    }

    public int displayStage() {
        return ReactorMeltdownLogic.displayStage(state);
    }

    public void cool() {
        ReactorMeltdownLogic.ReactorState before = state;
        state = ReactorMeltdownLogic.cool(state);
        setChanged();
        if (!before.unlocked() && state.unlocked() && level instanceof ServerLevel serverLevel) {
            unlockCoreChamber(serverLevel);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState blockState,
                                  UnstableReactorBlockEntity reactor) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        var event = ReactorMeltdownLogic.tick(reactor.state);
        if (event == ReactorMeltdownLogic.ReactorEvent.NONE) {
            return;
        }
        reactor.state = ReactorMeltdownLogic.afterTick(reactor.state, event);
        reactor.setChanged();
        if (event == ReactorMeltdownLogic.ReactorEvent.EXPLODE) {
            level.removeBlockEntity(pos);
            level.removeBlock(pos, false);
            level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    ReactorMeltdownLogic.EXPLOSION_POWER, Level.ExplosionInteraction.BLOCK);
        }
    }

    /** Schaltet die Kernkammer frei: platziert eine Loot-Kiste ueber dem Reaktor. */
    private void unlockCoreChamber(ServerLevel level) {
        BlockPos chamberPos = worldPosition.above();
        level.setBlock(chamberPos, net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState(), 3);
        if (level.getBlockEntity(chamberPos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
            chest.setLootTable(CORE_LOOT, level.getRandom().nextLong());
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Stage", state.stage());
        tag.putInt("TicksInStage", state.ticksInStage());
        tag.putInt("ValvesCooled", state.valvesCooled());
        tag.putBoolean("Unlocked", state.unlocked());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.state = new ReactorMeltdownLogic.ReactorState(
                tag.getInt("Stage"), tag.getInt("TicksInStage"),
                tag.getInt("ValvesCooled"), tag.getBoolean("Unlocked"));
    }
}
