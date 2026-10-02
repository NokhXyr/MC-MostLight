package com.nokhxyr.mostlight.link;

import com.mojang.serialization.MapCodec;
import com.nokhxyr.mostlight.MostLight;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Interrupteur lié à des lampes : chaque clic allume ou éteint tout le groupe. */
public class LightSwitchBlock extends FaceAttachedHorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<LightSwitchBlock> CODEC = simpleCodec(LightSwitchBlock::new);
    public static final BooleanProperty ON = BlockStateProperties.POWERED;

    private static final VoxelShape FLOOR = Block.box(4, 0, 4, 12, 2, 12);
    private static final VoxelShape CEILING = Block.box(4, 14, 4, 12, 16, 12);
    private static final VoxelShape NORTH = Block.box(4, 4, 14, 12, 12, 16);
    private static final VoxelShape SOUTH = Block.box(4, 4, 0, 12, 12, 2);
    private static final VoxelShape WEST = Block.box(14, 4, 4, 16, 12, 12);
    private static final VoxelShape EAST = Block.box(0, 4, 4, 2, 12, 12);

    public LightSwitchBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FACE, AttachFace.WALL).setValue(ON, false));
    }

    @Override
    protected MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FACE, ON);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SwitchBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACE)) {
            case FLOOR -> FLOOR;
            case CEILING -> CEILING;
            case WALL -> switch (state.getValue(FACING)) {
                case SOUTH -> SOUTH;
                case WEST -> WEST;
                case EAST -> EAST;
                default -> NORTH;
            };
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof SwitchBlockEntity entity) {
            if (entity.links().isEmpty()) {
                LampLinks.warnNoLinks(player);
            } else {
                press(state, level, pos, player, entity);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Action du bouton ; le variateur la redéfinit. */
    protected void press(BlockState state, Level level, BlockPos pos, Player player, SwitchBlockEntity entity) {
        boolean on = LampLinks.toggleAll(level, pos, entity.links());
        level.setBlock(pos, state.setValue(ON, on), Block.UPDATE_ALL);
        LampLinks.click(level, pos, on);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        ItemStack stack = super.getCloneItemStack(level, pos, state);
        if (level.getBlockEntity(pos) instanceof SwitchBlockEntity entity) {
            stack.applyComponents(entity.collectComponents());
        }
        return stack;
    }

    protected static Component message(String key, Object... args) {
        return Component.translatable("message." + MostLight.MOD_ID + "." + key, args);
    }
}
