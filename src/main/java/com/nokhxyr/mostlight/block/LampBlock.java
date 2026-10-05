package com.nokhxyr.mostlight.block;

import com.nokhxyr.mostlight.MostLight;
import com.nokhxyr.mostlight.block.entity.LampBlockEntity;
import com.nokhxyr.mostlight.registry.ModBlocks;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
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
    /**
     * 0 = off, 1 to 4 = on at one of the 4 brightness steps (1 = brightest). One property instead of lit + brightness
     * (+ powered, now in the block entity) keeps the number of block states down: every state of every lamp block
     * costs memory, startup time and room in shader and renderer ID maps.
     */
    public static final IntegerProperty LIGHT = IntegerProperty.create("light", 0, 4);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    /** One block per lamp model; its 16 colours are a property (same model, tinted). Switches share it. */
    public static final EnumProperty<DyeColor> COLOR = EnumProperty.create("color", DyeColor.class);
    private static final int[] LIGHT_LEVELS = {15, 12, 9, 6};
    /** Number of brightness steps. */
    public static final int STEPS = LIGHT_LEVELS.length;

    private final LampType type;
    /** Hitbox par état (identité) : évite de recalculer la clé du modèle à chaque collision. */
    private final Map<BlockState, VoxelShape> shapes = new ConcurrentHashMap<>();

    protected LampBlock(LampType type, Properties properties) {
        super(properties);
        this.type = type;
        registerDefaultState(stateDefinition.any()
                .setValue(LIGHT, 1)
                .setValue(WATERLOGGED, false)
                .setValue(COLOR, DyeColor.WHITE));
    }

    /**
     * Petites lampes (murales, suspendues, LED, fixées sur une face) : on peut les viser mais pas s'y cogner, comme les
     * appliques de Supplementaries. Moins de calculs de collision pour les entités, rien qui accroche la tête du joueur.
     * Les projectiles les touchent toujours.
     */
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Placement placement = type.placement();
        boolean small = placement == Placement.WALL || placement == Placement.HANGING || placement == Placement.OMNI;
        if (small && !(context instanceof net.minecraft.world.phys.shapes.EntityCollisionContext entity
                && entity.getEntity() instanceof net.minecraft.world.entity.projectile.Projectile)) {
            return net.minecraft.world.phys.shapes.Shapes.empty();
        }
        return super.getCollisionShape(state, level, pos, context);
    }

    public static int lightLevel(BlockState state) {
        int light = state.getValue(LIGHT);
        return light == 0 ? 0 : LIGHT_LEVELS[light - 1];
    }

    public static boolean isLit(BlockState state) {
        return state.getValue(LIGHT) > 0;
    }

    /** Brightness step of a lit lamp (0 = brightest); 0 when off. */
    public static int brightness(BlockState state) {
        return Math.max(0, state.getValue(LIGHT) - 1);
    }

    /** The same lamp, on at brightness step {@code brightness}. */
    public static BlockState lit(BlockState state, int brightness) {
        return state.setValue(LIGHT, Math.floorMod(brightness, STEPS) + 1);
    }

    /** Where this lamp keeps its remembered brightness and redstone signal (tall lamps: the lower half). */
    protected BlockPos memoryPos(BlockPos pos, BlockState state) {
        return pos;
    }

    public @org.jetbrains.annotations.Nullable LampBlockEntity memory(BlockGetter level, BlockPos pos, BlockState state) {
        return level.getBlockEntity(memoryPos(pos, state)) instanceof LampBlockEntity lamp ? lamp : null;
    }

    /** Brightness step the lamp is at, or comes back on at when off. */
    public int rememberedBrightness(BlockGetter level, BlockPos pos, BlockState state) {
        if (isLit(state)) {
            return brightness(state);
        }
        LampBlockEntity memory = memory(level, pos, state);
        return memory == null ? 0 : memory.brightness();
    }

    /** The lamp switched on (at its remembered brightness) or off (remembering its brightness). */
    public BlockState withLit(Level level, BlockPos pos, BlockState state, boolean on) {
        if (on == isLit(state)) {
            return state;
        }
        if (on) {
            return lit(state, rememberedBrightness(level, pos, state));
        }
        LampBlockEntity memory = memory(level, pos, state);
        if (memory != null) {
            memory.setBrightness(brightness(state));
        }
        return state.setValue(LIGHT, 0);
    }

    public LampType type() {
        return type;
    }

    public static DyeColor color(BlockState state) {
        return state.getValue(COLOR);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIGHT, WATERLOGGED, COLOR);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LampBlockEntity(pos, state);
    }

    /**
     * Vrai si le bloc {@code support} peut porter une lampe sur sa face {@code face} : pas besoin d'une face pleine,
     * tout bloc ayant une forme convient (escalier à l'envers, dalle, tête de joueur, barrière, vitre...).
     */
    public static boolean hasSupport(LevelReader level, BlockPos support, Direction face) {
        BlockState state = level.getBlockState(support);
        if (state.isAir()) {
            return false;
        }
        return state.isFaceSturdy(level, support, face, SupportType.CENTER)
                || !state.getCollisionShape(level, support).isEmpty()
                || !state.getShape(level, support).isEmpty() && !state.canBeReplaced();
    }

    /** État commun à la pose : eau. Le signal redstone présent est mémorisé par setPlacedBy (block entity). */
    protected BlockState placementState(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        return defaultBlockState().setValue(WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER);
    }

    /** Placed by a player: remember the redstone signal it is placed in, without switching it. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @org.jetbrains.annotations.Nullable net.minecraft.world.entity.LivingEntity placer,
            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            LampBlockEntity memory = memory(level, pos, state);
            if (memory != null) {
                memory.setPowered(isPowered(level, pos, state));
            }
        }
    }

    /**
     * Applique un nouvel état (les lampes hautes le recopient sur l'autre moitié).
     * Light, brightness, power or fan change. A lamp emits no redstone and keeps its shape, so neighbors are not
     * notified (no neighborChanged cascade through packed lamps). Clients, shape updates (observers) and the light
     * engine still see the change.
     */
    protected void applyState(Level level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
    }

    /** Allume ou éteint (utilisé par les interrupteurs liés). */
    public void setLit(Level level, BlockPos pos, BlockState state, boolean lit) {
        if (isLit(state) != lit) {
            applyState(level, pos, withLit(level, pos, state, lit));
        }
    }

    /** Règle la luminosité (0 = la plus forte) et allume. */
    public void setBrightness(Level level, BlockPos pos, BlockState state, int brightness) {
        int step = Math.floorMod(brightness, STEPS);
        LampBlockEntity memory = memory(level, pos, state);
        if (memory != null) {
            memory.setBrightness(step);
        }
        applyState(level, pos, lit(state, step));
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

    /** New colour: only the property changes, the block and its block entity (finish, tone, links) stay. */
    protected void recolor(Level level, BlockPos pos, BlockState state, DyeColor newColor) {
        applyState(level, pos, state.setValue(COLOR, newColor));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockState next;
            if (player.isShiftKeyDown()) {
                int step = (rememberedBrightness(level, pos, state) + 1) % STEPS;
                LampBlockEntity memory = memory(level, pos, state);
                if (memory != null) {
                    memory.setBrightness(step);
                }
                next = lit(state, step);
                player.displayClientMessage(Component.translatable("message." + MostLight.MOD_ID + ".brightness", lightLevel(next)), true);
            } else {
                next = withLit(level, pos, state, !isLit(state));
            }
            applyState(level, pos, next);
            level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.3F, isLit(next) ? 0.6F : 0.5F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof DyeItem dye) {
            if (dye.getDyeColor() == color(state)) {
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
        LampBlockEntity memory = memory(level, pos, state);
        boolean powered = isPowered(level, pos, state);
        if (memory != null && powered != memory.powered()) {
            // a change of signal switches the lamp; a steady signal leaves manual toggles alone
            memory.setPowered(powered);
            applyState(level, pos, withLit(level, pos, state, powered));
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
            BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        if (!state.canSurvive(level, pos)) {
            // tombe au tick suivant, avec son objet : aussi quand un piston ou une contraption la pose sans support
            // (le jeu recalcule alors la forme du bloc et un retour à l'air ne rendrait rien)
            level.scheduleTick(pos, this, 1);
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected void tick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        ItemStack stack = com.nokhxyr.mostlight.item.ItemColor.with(super.getCloneItemStack(level, pos, state), color(state));
        if (level.getBlockEntity(pos) instanceof LampBlockEntity lamp) {
            stack.applyComponents(lamp.collectComponents());
        }
        return stack;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!isLit(state) || state.getValue(WATERLOGGED)) {
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
        VoxelShape shape = shapes.get(state);
        if (shape == null) {
            shape = LampShapes.get(shapeModel(state), shapeFacing(state), type.placement() == Placement.OMNI);
            shapes.put(state, shape);
        }
        return shape;
    }
}
