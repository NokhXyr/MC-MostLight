package com.nokhxyr.mostlight.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.Nullable;

/** Lampe orientée horizontalement : suspendue, murale ou posée. */
public class HorizontalLampBlock extends LampBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public HorizontalLampBlock(LampType type, DyeColor color, Properties properties) {
        super(type, color, properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState base = placementState(context);
        if (type().placement() == Placement.WALL) {
            for (Direction dir : context.getNearestLookingDirections()) {
                if (dir.getAxis().isHorizontal()) {
                    BlockState state = base.setValue(FACING, dir.getOpposite());
                    if (state.canSurvive(context.getLevel(), context.getClickedPos())) {
                        return state;
                    }
                }
            }
            return null;
        }
        BlockState state = base.setValue(FACING, context.getHorizontalDirection().getOpposite());
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return switch (type().placement()) {
            case HANGING -> Block.canSupportCenter(level, pos.above(), Direction.DOWN);
            case WALL -> {
                Direction facing = state.getValue(FACING);
                BlockPos wall = pos.relative(facing.getOpposite());
                yield level.getBlockState(wall).isFaceSturdy(level, wall, facing);
            }
            default -> Block.canSupportCenter(level, pos.below(), Direction.UP);
        };
    }

    @Override
    protected Direction shapeFacing(BlockState state) {
        return state.getValue(FACING);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
