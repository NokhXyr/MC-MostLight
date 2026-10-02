package com.nokhxyr.mostlight.block;

import com.nokhxyr.mostlight.MostLight;
import com.nokhxyr.mostlight.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Base de toutes les lampes : allumage manuel ou redstone, 4 niveaux de luminosité,
 * recoloration avec un colorant et support de l'eau.
 */
public abstract class LampBlock extends Block implements SimpleWaterloggedBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final IntegerProperty BRIGHTNESS = IntegerProperty.create("brightness", 0, 3);
    private static final int[] LIGHT_LEVELS = {15, 12, 9, 6};

    private final LampType type;
    private final DyeColor color;

    protected LampBlock(LampType type, DyeColor color, Properties properties) {
        super(properties);
        this.type = type;
        this.color = color;
        registerDefaultState(stateDefinition.any()
                .setValue(LIT, true)
                .setValue(POWERED, false)
                .setValue(BRIGHTNESS, 0)
                .setValue(WATERLOGGED, false));
    }

    public static int lightLevel(BlockState state) {
        return state.getValue(LIT) ? LIGHT_LEVELS[state.getValue(BRIGHTNESS)] : 0;
    }

    public LampType type() {
        return type;
    }

    public DyeColor color() {
        return color;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT, POWERED, BRIGHTNESS, WATERLOGGED);
    }

    /** État commun à la pose : eau, alimentation redstone. */
    protected BlockState placementState(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        return defaultBlockState()
                .setValue(WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER)
                .setValue(POWERED, level.hasNeighborSignal(pos));
    }

    /** Applique un nouvel état (les lampes hautes le recopient sur l'autre moitié). */
    protected void applyState(Level level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, Block.UPDATE_ALL);
    }

    protected boolean isPowered(Level level, BlockPos pos, BlockState state) {
        return level.hasNeighborSignal(pos);
    }

    /** Remplace la lampe par la même dans une autre couleur. */
    protected void recolor(Level level, BlockPos pos, BlockState state, DyeColor newColor) {
        level.setBlock(pos, ModBlocks.lamp(type, newColor).withPropertiesOf(state), Block.UPDATE_ALL);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockState next;
            if (player.isShiftKeyDown()) {
                next = state.setValue(BRIGHTNESS, (state.getValue(BRIGHTNESS) + 1) % LIGHT_LEVELS.length).setValue(LIT, true);
                player.displayClientMessage(Component.translatable("message." + MostLight.MOD_ID + ".brightness", lightLevel(next)), true);
            } else {
                next = state.cycle(LIT);
            }
            applyState(level, pos, next);
            level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.3F, next.getValue(LIT) ? 0.6F : 0.5F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof DyeItem dye) {
            if (dye.getDyeColor() == color) {
                return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
            }
            if (!level.isClientSide) {
                recolor(level, pos, state, dye.getDyeColor());
                stack.consume(1, player);
                level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        // la main vide allume/éteint ; avec un objet en main on laisse le jeu poser le bloc
        return stack.isEmpty()
                ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                : ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean movedByPiston) {
        if (level.isClientSide) {
            return;
        }
        boolean powered = isPowered(level, pos, state);
        if (powered != state.getValue(POWERED)) {
            applyState(level, pos, state.setValue(POWERED, powered).setValue(LIT, powered));
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
            BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        if (!state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    /** Nom du modèle dont dérive la hitbox de cet état. */
    protected String shapeModel(BlockState state) {
        return type.id();
    }

    protected abstract Direction shapeFacing(BlockState state);

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return LampShapes.get(shapeModel(state), shapeFacing(state), type.placement() == Placement.OMNI);
    }
}
