package com.nokhxyr.mostlight.block;

import com.nokhxyr.mostlight.block.entity.LampBlockEntity;
import com.nokhxyr.mostlight.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

/** Lampe de sol haute de deux blocs ; seule la moitié haute éclaire. */
public class TallLampBlock extends LampBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    public TallLampBlock(LampType type, DyeColor color, Properties properties) {
        super(type, color, properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(HALF, DoubleBlockHalf.LOWER));
    }

    public static int lightLevel(BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER ? LampBlock.lightLevel(state) : 0;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, HALF);
    }

    private static BlockPos otherHalf(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
    }

    /** Vrai si l'état est l'autre moitié d'une lampe du même type (couleur indifférente pendant la recoloration). */
    private boolean isPartner(BlockState other, DoubleBlockHalf expectedHalf) {
        return other.getBlock() instanceof TallLampBlock lamp && lamp.type() == type() && other.getValue(HALF) == expectedHalf;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() >= level.getMaxBuildHeight() - 1 || !level.getBlockState(pos.above()).canBeReplaced(context)) {
            return null;
        }
        BlockState state = placementState(context)
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(POWERED, level.hasNeighborSignal(pos) || level.hasNeighborSignal(pos.above()));
        return state.canSurvive(level, pos) ? state : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        BlockPos above = pos.above();
        level.setBlock(above, state.setValue(HALF, DoubleBlockHalf.UPPER)
                .setValue(WATERLOGGED, level.getFluidState(above).getType() == Fluids.WATER), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof LampBlockEntity lower && level.getBlockEntity(above) instanceof LampBlockEntity upper) {
            upper.setLook(lower.finish(), lower.tone());
        }
    }

    @Override
    public void setLook(Level level, BlockPos pos, BlockState state, LampFinish finish, LightTone tone) {
        super.setLook(level, pos, state, finish, tone);
        BlockPos other = otherHalf(pos, state);
        if (level.getBlockState(other).getBlock() instanceof TallLampBlock) {
            super.setLook(level, other, level.getBlockState(other), finish, tone);
        }
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            return isPartner(level.getBlockState(pos.below()), DoubleBlockHalf.LOWER);
        }
        return hasSupport(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
            BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        DoubleBlockHalf half = state.getValue(HALF);
        if (direction.getAxis() == Direction.Axis.Y && (half == DoubleBlockHalf.LOWER) == (direction == Direction.UP)) {
            DoubleBlockHalf expected = half == DoubleBlockHalf.LOWER ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER;
            return isPartner(neighborState, expected) ? state : Blocks.AIR.defaultBlockState();
        }
        if (half == DoubleBlockHalf.LOWER && direction == Direction.DOWN && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // en créatif, casser le haut ne doit pas faire tomber l'objet du bas
        if (!level.isClientSide && player.isCreative() && state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            BlockPos below = pos.below();
            BlockState lower = level.getBlockState(below);
            if (isPartner(lower, DoubleBlockHalf.LOWER)) {
                BlockState replacement = lower.getValue(WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
                level.setBlock(below, replacement, Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                level.levelEvent(player, LevelEvent.PARTICLES_DESTROY_BLOCK, below, Block.getId(lower));
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void applyState(Level level, BlockPos pos, BlockState state) {
        super.applyState(level, pos, state);
        BlockPos other = otherHalf(pos, state);
        BlockState otherState = level.getBlockState(other);
        if (otherState.getBlock() instanceof TallLampBlock) {
            level.setBlock(other, otherState
                    .setValue(LIT, state.getValue(LIT))
                    .setValue(POWERED, state.getValue(POWERED))
                    .setValue(BRIGHTNESS, state.getValue(BRIGHTNESS)), Block.UPDATE_ALL);
        }
    }

    @Override
    protected boolean isPowered(Level level, BlockPos pos, BlockState state) {
        return level.hasNeighborSignal(pos) || level.hasNeighborSignal(otherHalf(pos, state));
    }

    @Override
    protected void recolor(Level level, BlockPos pos, BlockState state, DyeColor newColor) {
        Block target = ModBlocks.lamp(type(), newColor);
        BlockPos other = otherHalf(pos, state);
        BlockState otherState = level.getBlockState(other);
        replaceKeepingLook(level, pos, state, target);
        if (otherState.getBlock() instanceof TallLampBlock) {
            replaceKeepingLook(level, other, otherState, target);
        }
    }

    @Override
    protected String shapeModel(BlockState state) {
        return type().id() + "_" + state.getValue(HALF).getSerializedName();
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
