package com.nokhxyr.mostlight.block;

import com.nokhxyr.mostlight.MostLight;
import com.nokhxyr.mostlight.block.entity.LampBlockEntity;
import com.nokhxyr.mostlight.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
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
 * recoloration avec un colorant, finition et teinte de lumière (block entity), support de l'eau.
 */
public abstract class LampBlock extends Block implements SimpleWaterloggedBlock, EntityBlock {
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

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LampBlockEntity(pos, state);
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

    /** Allume ou éteint (utilisé par les interrupteurs liés). */
    public void setLit(Level level, BlockPos pos, BlockState state, boolean lit) {
        if (state.getValue(LIT) != lit) {
            applyState(level, pos, state.setValue(LIT, lit));
        }
    }

    /** Règle la luminosité (0 = la plus forte) et allume. */
    public void setBrightness(Level level, BlockPos pos, BlockState state, int brightness) {
        applyState(level, pos, state.setValue(BRIGHTNESS, Math.floorMod(brightness, LIGHT_LEVELS.length)).setValue(LIT, true));
    }

    protected boolean isPowered(Level level, BlockPos pos, BlockState state) {
        return level.hasNeighborSignal(pos);
    }

    /** Change finition et teinte (les lampes hautes le recopient sur l'autre moitié). */
    public void setLook(Level level, BlockPos pos, BlockState state, LampFinish finish, LightTone tone) {
        if (level.getBlockEntity(pos) instanceof LampBlockEntity lamp) {
            lamp.setLook(finish, tone);
        }
    }

    /** Remplace le bloc par la même lampe dans une autre couleur en gardant finition et teinte. */
    protected static void replaceKeepingLook(Level level, BlockPos pos, BlockState state, Block target) {
        LampFinish finish = null;
        LightTone tone = null;
        if (level.getBlockEntity(pos) instanceof LampBlockEntity lamp) {
            finish = lamp.finish();
            tone = lamp.tone();
        }
        level.setBlock(pos, target.withPropertiesOf(state), Block.UPDATE_ALL);
        if (finish != null && level.getBlockEntity(pos) instanceof LampBlockEntity lamp) {
            lamp.setLook(finish, tone);
        }
    }

    protected void recolor(Level level, BlockPos pos, BlockState state, DyeColor newColor) {
        replaceKeepingLook(level, pos, state, ModBlocks.lamp(type, newColor));
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
        // la main vide allume/éteint ; avec un objet en main on laisse l'objet agir (pose, clé...)
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

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        ItemStack stack = super.getCloneItemStack(level, pos, state);
        if (level.getBlockEntity(pos) instanceof LampBlockEntity lamp) {
            stack.applyComponents(lamp.collectComponents());
        }
        return stack;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT) || state.getValue(WATERLOGGED)) {
            return;
        }
        double[][] points = LampShapes.flames(shapeModel(state));
        if (points == null) {
            return;
        }
        boolean omni = type.placement() == Placement.OMNI;
        for (double[] p : points) {
            boolean big = p[3] > 0;
            if (random.nextInt(big ? 2 : 4) != 0) {
                continue;
            }
            double[] r = LampShapes.rotate(p[0], p[1], p[2], shapeFacing(state), omni);
            double x = pos.getX() + r[0] / 16.0;
            double y = pos.getY() + r[1] / 16.0;
            double z = pos.getZ() + r[2] / 16.0;
            level.addParticle(big ? ParticleTypes.FLAME : ParticleTypes.SMALL_FLAME, x, y, z, 0, big ? 0.01 : 0, 0);
            if (random.nextInt(big ? 3 : 8) == 0) {
                level.addParticle(ParticleTypes.SMOKE, x, y + 0.05, z, 0, 0.02, 0);
            }
        }
    }

    /** Nom du modèle dont dérivent la hitbox et les flammes de cet état. */
    protected String shapeModel(BlockState state) {
        return type.id();
    }

    protected abstract Direction shapeFacing(BlockState state);

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return LampShapes.get(shapeModel(state), shapeFacing(state), type.placement() == Placement.OMNI);
    }
}
