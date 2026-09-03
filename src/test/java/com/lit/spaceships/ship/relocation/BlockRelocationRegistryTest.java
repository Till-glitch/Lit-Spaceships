package com.lit.spaceships.ship.relocation;

import com.lit.spaceships.ship.relocation.api.IBlockRelocationHandler;
import com.lit.spaceships.ship.relocation.api.RelocationContext;
import com.lit.spaceships.ship.relocation.registry.BlockRelocationRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BlockRelocationRegistry & Handler-SPI Tests")
class BlockRelocationRegistryTest {

    @Test
    @DisplayName("Immunitäts-Check für Vanilla-Weltblöcke")
    void testImmunityCheck() {
        assertTrue(BlockRelocationRegistry.isImmune(Blocks.BEDROCK.defaultBlockState()), "Bedrock muss immun sein");
        assertTrue(BlockRelocationRegistry.isImmune(Blocks.END_PORTAL.defaultBlockState()), "End-Portal muss immun sein");
        assertTrue(BlockRelocationRegistry.isImmune(Blocks.COMMAND_BLOCK.defaultBlockState()), "Command-Block muss immun sein");
        assertTrue(BlockRelocationRegistry.isImmune(Blocks.BARRIER.defaultBlockState()), "Barrier muss immun sein");

        assertFalse(BlockRelocationRegistry.isImmune(Blocks.STONE.defaultBlockState()), "Stone darf nicht immun sein");
        assertFalse(BlockRelocationRegistry.isImmune(Blocks.OAK_PLANKS.defaultBlockState()), "Oak Planks dürfen nicht immun sein");
        assertFalse(BlockRelocationRegistry.isImmune(Blocks.IRON_BLOCK.defaultBlockState()), "Iron Block darf nicht immun sein");
    }

    @Test
    @DisplayName("Handler-Registrierung, Prioritäts-Sortierung und Lifecycle-Dispatching")
    void testHandlerRegistrationAndDispatch() {
        List<String> executionOrder = new ArrayList<>();

        IBlockRelocationHandler lowPriorityHandler = new IBlockRelocationHandler() {
            @Override
            public boolean shouldHandle(BlockState state) {
                return state.is(Blocks.GOLD_BLOCK);
            }

            @Override
            public void onPreRelocation(BlockPos pos, BlockState state, BlockEntity be, CompoundTag snapshotNbt, RelocationContext context) {
                executionOrder.add("LOW_PRE");
            }

            @Override
            public void onPostRelocation(BlockPos oldPos, BlockPos newPos, BlockState state, BlockEntity be, RelocationContext context) {
                executionOrder.add("LOW_POST");
            }

            @Override
            public int getPriority() {
                return 5;
            }
        };

        IBlockRelocationHandler highPriorityHandler = new IBlockRelocationHandler() {
            @Override
            public boolean shouldHandle(BlockState state) {
                return state.is(Blocks.GOLD_BLOCK);
            }

            @Override
            public void onPreRelocation(BlockPos pos, BlockState state, BlockEntity be, CompoundTag snapshotNbt, RelocationContext context) {
                executionOrder.add("HIGH_PRE");
            }

            @Override
            public void onPostRelocation(BlockPos oldPos, BlockPos newPos, BlockState state, BlockEntity be, RelocationContext context) {
                executionOrder.add("HIGH_POST");
            }

            @Override
            public int getPriority() {
                return 100;
            }
        };

        BlockRelocationRegistry.registerHandler(lowPriorityHandler);
        BlockRelocationRegistry.registerHandler(highPriorityHandler);

        BlockState gold = Blocks.GOLD_BLOCK.defaultBlockState();
        BlockPos pos = new BlockPos(10, 20, 30);

        BlockRelocationRegistry.dispatchPreRelocation(pos, gold, null, null, null);
        assertEquals(List.of("HIGH_PRE", "LOW_PRE"), executionOrder, "Höhere Priorität muss zuerst ausgeführt werden");

        executionOrder.clear();
        BlockRelocationRegistry.dispatchPostRelocation(pos, pos.offset(1, 0, 0), gold, null, null);
        assertEquals(List.of("HIGH_POST", "LOW_POST"), executionOrder, "Höhere Priorität muss auch im Post-Hook zuerst ausgeführt werden");
    }
}
