package com.nokhxyr.mostlight.block;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Tige lumineuse (style barre de l'End). Debout comme une barre de l'End au sol et au plafond ; couchée le long de la
 * surface comme une bande LED :
 * <ul>
 *   <li>au mur : couchée à l'horizontale, à la verticale en étant accroupi ;</li>
 *   <li>au sol / au plafond : debout (pendue au plafond), couchée dans l'axe du regard en étant accroupi.</li>
 * </ul>
 */
public class RodLampBlock extends OmniLampBlock {
    public static final BooleanProperty LYING = BooleanProperty.create("lying");
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    private final Map<BlockState, VoxelShape> lyingShapes = new ConcurrentHashMap<>();

    public RodLampBlock(LampType type, Properties properties) {
        super(type, properties);
        registerDefaultState(defaultBlockState().setValue(LYING, false).setValue(AXIS, Direction.Axis.Y));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LYING, AXIS);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }
        Direction facing = state.getValue(FACING);
        Player player = context.getPlayer();
        boolean sneaking = player != null && player.isSecondaryUseActive();
        if (facing.getAxis().isHorizontal()) {
            Direction.Axis along = sneaking ? Direction.Axis.Y : facing.getClockWise().getAxis();
            return state.setValue(LYING, true).setValue(AXIS, along);
        }
        if (sneaking) {
            return state.setValue(LYING, true).setValue(AXIS, context.getHorizontalDirection().getAxis());
        }
        return state.setValue(LYING, false).setValue(AXIS, Direction.Axis.Y);
    }

    private static boolean lying(BlockState state) {
        return state.getValue(LYING) && state.getValue(AXIS) != state.getValue(FACING).getAxis();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!lying(state)) {
            return super.getShape(state, level, pos, context);
        }
        return lyingShapes.computeIfAbsent(state, s -> LampShapes.lying(type().id(), s.getValue(FACING), s.getValue(AXIS)));
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        BlockState out = super.rotate(state, rotation);
        Direction.Axis axis = state.getValue(AXIS);
        if (axis != Direction.Axis.Y && (rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90)) {
            out = out.setValue(AXIS, axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
        }
        return out;
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
