package com.lit.spaceships.block;

import com.lit.spaceships.world.DecompressionService;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Alloy Cut-Point (Epoch 12): hochhaarter Legierungsblock mit geringer
 * Sprengresistenz. Wenn ALLE Cut-Points in einem 3x3x3-Panel zerstoert sind,
 * fallen konzentrierte Schrott-Items (konzentrierte Legierungserde).
 */
public class CutPointBlock extends Block {

    /** Panel-Radius in dem nach Schwestern gesucht wird. */
    public static final int PANEL_RADIUS = 1;
    /** Bonus-Schrott wenn das Panel vollstaendig geschnitten ist. */
    public static final int CONCENTRATED_SCRAP_COUNT = 6;

    public CutPointBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state,
                                        net.minecraft.world.entity.player.Player player) {
        if (!level.isClientSide()) {
            // Panel gilt als geschnitten, wenn keine weiteren Cut-Points in 3x3x3 uebrig sind
            int remaining = 0;
            for (int dx = -PANEL_RADIUS; dx <= PANEL_RADIUS; dx++) {
                for (int dy = -PANEL_RADIUS; dy <= PANEL_RADIUS; dy++) {
                    for (int dz = -PANEL_RADIUS; dz <= PANEL_RADIUS; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        if (level.getBlockState(pos.offset(dx, dy, dz)).getBlock() instanceof CutPointBlock) {
                            remaining++;
                        }
                    }
                }
            }
            if (remaining == 0) {
                popResource(level, pos, new ItemStack(
                        net.minecraft.world.item.Items.IRON_INGOT, CONCENTRATED_SCRAP_COUNT));
                popResource(level, pos, new ItemStack(
                        net.minecraft.world.item.Items.NETHERITE_SCRAP, 1));
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
