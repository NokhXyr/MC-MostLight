package com.nokhxyr.mostlight.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Bloc lumineux plein. Se relie aux blocs voisins avec le connecteur, comme les bandes LED (chaînes redstone). */
public class CubeLampBlock extends LampBlock {
    public CubeLampBlock(LampType type, DyeColor color, Properties properties) {
        super(type, color, properties);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return placementState(context).setValue(WATERLOGGED, false);
    }

    @Override
    public boolean canPlaceLiquid(@Nullable Player player, BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
        return false;
    }

    @Override
    protected Direction shapeFacing(BlockState state) {
        return Direction.NORTH;
    }

    @Override
    protected void neighborChanged(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, net.minecraft.world.level.block.Block neighbor,
            BlockPos neighborPos, boolean movedByPiston) {
        if (!LightStripBlock.chainNeighborChanged(level, pos)) {
            super.neighborChanged(state, level, pos, neighbor, neighborPos, movedByPiston);
        }
    }

    /** Comme le verre : la face collée à un bloc du même modèle n'est pas dessinée. */
    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacent, Direction side) {
        return adjacent.getBlock() instanceof CubeLampBlock other && other.type() == type() || super.skipRendering(state, adjacent, side);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }
}
