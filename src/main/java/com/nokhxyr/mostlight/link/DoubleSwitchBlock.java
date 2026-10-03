package com.nokhxyr.mostlight.link;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Interrupteur double (pour les ventilateurs lumineux) : la bascule de gauche allume / éteint la lumière des lampes
 * liées, celle de droite met en marche / arrête leurs ventilateurs.
 */
public class DoubleSwitchBlock extends LightSwitchBlock {
    public static final MapCodec<DoubleSwitchBlock> CODEC = simpleCodec(DoubleSwitchBlock::new);
    public static final BooleanProperty FAN = BooleanProperty.create("fan");

    private static final VoxelShape FLOOR = Block.box(3, 0, 4, 13, 2, 12);
    private static final VoxelShape FLOOR_X = Block.box(4, 0, 3, 12, 2, 13);
    private static final VoxelShape CEILING = Block.box(3, 14, 4, 13, 16, 12);
    private static final VoxelShape CEILING_X = Block.box(4, 14, 3, 12, 16, 13);
    private static final VoxelShape NORTH = Block.box(3, 4, 14, 13, 12, 16);
    private static final VoxelShape SOUTH = Block.box(3, 4, 0, 13, 12, 2);
    private static final VoxelShape WEST = Block.box(14, 4, 3, 16, 12, 13);
    private static final VoxelShape EAST = Block.box(0, 4, 3, 2, 12, 13);

    public DoubleSwitchBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FAN, false));
    }

    @Override
    protected MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FAN);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        boolean alongX = state.getValue(FACING).getAxis() == Direction.Axis.Z;
        return switch (state.getValue(FACE)) {
            case FLOOR -> alongX ? FLOOR : FLOOR_X;
            case CEILING -> alongX ? CEILING : CEILING_X;
            case WALL -> switch (state.getValue(FACING)) {
                case SOUTH -> SOUTH;
                case WEST -> WEST;
                case EAST -> EAST;
                default -> NORTH;
            };
        };
    }

    /** Vrai si le clic tombe sur la moitié droite (vue de face) : la bascule du ventilateur. */
    private static boolean rightHalf(BlockState state, BlockPos pos, BlockHitResult hit) {
        Direction facing = state.getValue(FACING);
        // côté de la bascule du ventilateur, tel que dessiné sur le modèle
        Direction right = state.getValue(FACE) == AttachFace.CEILING ? facing.getCounterClockWise() : facing.getClockWise();
        Vec3 offset = hit.getLocation().subtract(Vec3.atCenterOf(pos));
        return offset.x * right.getStepX() + offset.z * right.getStepZ() > 0;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof SwitchBlockEntity entity) {
            if (entity.links().isEmpty()) {
                LampLinks.warnNoLinks(player);
            } else if (rightHalf(state, pos, hit)) {
                boolean on = LampLinks.toggleFans(level, pos, entity.links());
                level.setBlock(pos, state.setValue(FAN, on), Block.UPDATE_ALL);
                LampLinks.click(level, pos, on);
            } else {
                press(state, level, pos, player, entity);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
