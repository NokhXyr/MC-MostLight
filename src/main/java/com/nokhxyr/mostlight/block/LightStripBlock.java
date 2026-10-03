package com.nokhxyr.mostlight.block;

import com.nokhxyr.mostlight.MostLightConfig;
import com.nokhxyr.mostlight.block.entity.LampBlockEntity;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Bande LED. Un bloc peut porter une bande sur chacune de ses 6 faces (comme le lichen lumineux) : deux murs qui se
 * rejoignent, un mur et un plafond... forment un angle dans le même bloc. Chaque bande a sa position (basse / milieu /
 * haute, selon l'endroit visé) et son sens (à la verticale sur un mur en étant accroupi), gardés dans la block entity.
 * Avec le connecteur LED, des bandes voisines sont reliées : alimenter l'une allume toute la chaîne.
 */
public class LightStripBlock extends LampBlock {
    public enum Slot implements StringRepresentable {
        LOW("low"), MIDDLE("middle"), HIGH("high");

        private final String id;

        Slot(String id) {
            this.id = id;
        }

        @Override
        public String getSerializedName() {
            return id;
        }
    }

    /** Côté du support de chaque bande : down = bande posée au sol, north = bande sur le mur nord... */
    public static final Map<Direction, BooleanProperty> SIDES = PipeBlock.PROPERTY_BY_DIRECTION;
    private static final Map<Integer, VoxelShape> SHAPES = new ConcurrentHashMap<>();
    /** Évite les mises à jour en cascade pendant qu'une chaîne est recalculée. */
    private static boolean updatingChain;

    public LightStripBlock(LampType type, DyeColor color, Properties properties) {
        super(type, color, properties);
        BlockState state = defaultBlockState();
        for (BooleanProperty side : SIDES.values()) {
            state = state.setValue(side, false);
        }
        registerDefaultState(state.setValue(SIDES.get(Direction.DOWN), true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        SIDES.values().forEach(builder::add);
    }

    public static boolean has(BlockState state, Direction side) {
        return state.getValue(SIDES.get(side));
    }

    private static int sideMask(BlockState state) {
        int mask = 0;
        for (Direction d : Direction.values()) {
            if (has(state, d)) {
                mask |= 1 << d.ordinal();
            }
        }
        return mask;
    }

    // ------------------------------------------------------------------ pose

    /** Côté où fixer la nouvelle bande : la face cliquée, sinon la plus proche du regard qui a un support. */
    private @Nullable Direction sideFor(BlockPlaceContext context, BlockState current) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        List<Direction> candidates = new ArrayList<>();
        if (!context.replacingClickedOnBlock()) {
            candidates.add(context.getClickedFace().getOpposite());
        }
        for (Direction d : context.getNearestLookingDirections()) {
            candidates.add(d);
        }
        for (Direction d : candidates) {
            boolean free = !current.is(this) || !has(current, d);
            if (free && hasSupport(level, pos.relative(d), d.getOpposite())) {
                return d;
            }
        }
        return null;
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        // une bande de même couleur s'ajoute dans le même bloc si une face est libre (angle)
        return context.getItemInHand().is(asItem()) && sideFor(context, state) != null || super.canBeReplaced(state, context);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState current = context.getLevel().getBlockState(context.getClickedPos());
        BlockState base;
        if (current.is(this)) {
            base = current;
        } else {
            base = placementState(context);
            for (BooleanProperty side : SIDES.values()) {
                base = base.setValue(side, false);
            }
        }
        Direction side = sideFor(context, base);
        return side == null ? null : base.setValue(SIDES.get(side), true);
    }

    /** Position et sens de la bande qui vient d'être posée, d'après l'endroit visé (appelé par l'objet). */
    public static void configurePlaced(Level level, BlockPos pos, BlockState before, BlockState after, BlockPlaceContext context) {
        if (!(level.getBlockEntity(pos) instanceof LampBlockEntity lamp)) {
            return;
        }
        for (Direction side : Direction.values()) {
            boolean added = has(after, side) && !(before.getBlock() == after.getBlock() && has(before, side));
            if (added) {
                Direction facing = side.getOpposite();
                boolean rotated;
                if (facing.getAxis().isVertical()) {
                    // au sol / au plafond : la bande suit le regard du joueur
                    double[] a = LampShapes.rotate(0, 0, 8, facing, true);
                    double[] b = LampShapes.rotate(16, 0, 8, facing, true);
                    boolean stripAlongX = Math.abs(b[0] - a[0]) > 1;
                    rotated = stripAlongX != (context.getHorizontalDirection().getAxis() == Direction.Axis.X);
                } else {
                    // au mur : horizontale par défaut, verticale en étant accroupi
                    rotated = context.getPlayer() != null && context.getPlayer().isSecondaryUseActive();
                }
                lamp.setStrip(side, slotFromHit(context, facing, rotated).ordinal(), rotated);
            }
        }
    }

    /** Position (basse / milieu / haute) selon l'endroit visé sur la face, dans l'axe perpendiculaire à la bande. */
    private static Slot slotFromHit(BlockPlaceContext context, Direction facing, boolean rotated) {
        double[] p0 = rotated ? LampShapes.rotate(0, 0, 8, facing, true) : LampShapes.rotate(8, 0, 0, facing, true);
        double[] p1 = rotated ? LampShapes.rotate(16, 0, 8, facing, true) : LampShapes.rotate(8, 0, 16, facing, true);
        Vec3 hit = context.getClickLocation().subtract(Vec3.atLowerCornerOf(context.getClickedPos())).scale(16);
        double[] h = {hit.x, hit.y, hit.z};
        int axis = 0;
        for (int i = 1; i < 3; i++) {
            if (Math.abs(p1[i] - p0[i]) > Math.abs(p1[axis] - p0[axis])) {
                axis = i;
            }
        }
        double t = (h[axis] - p0[axis]) / (p1[axis] - p0[axis]);
        return t < 1.0 / 3 ? Slot.LOW : t < 2.0 / 3 ? Slot.MIDDLE : Slot.HIGH;
    }

    // ------------------------------------------------------------------ supports

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        for (Direction d : Direction.values()) {
            if (has(state, d) && hasSupport(level, pos.relative(d), d.getOpposite())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
            BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        if (!has(state, direction) || hasSupport(level, neighborPos, direction.getOpposite())) {
            return state;
        }
        BlockState without = state.setValue(SIDES.get(direction), false);
        if (sideMask(without) == 0) {
            // dernière bande : le bloc casse normalement (la table de butin rend une bande par face)
            return Blocks.AIR.defaultBlockState();
        }
        if (level instanceof ServerLevel server) {
            popResource(server, pos, new ItemStack(this));
        }
        return without;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        BlockState out = state;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            out = out.setValue(SIDES.get(rotation.rotate(d)), has(state, d));
        }
        return out;
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        BlockState out = state;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            out = out.setValue(SIDES.get(mirror.mirror(d)), has(state, d));
        }
        return out;
    }

    // ------------------------------------------------------------------ forme

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int layout = level.getBlockEntity(pos) instanceof LampBlockEntity lamp ? lamp.stripLayout() : LampBlockEntity.DEFAULT_STRIP_LAYOUT;
        int mask = sideMask(state);
        return SHAPES.computeIfAbsent(mask | (layout << 6), k -> {
            VoxelShape shape = Shapes.empty();
            for (Direction side : Direction.values()) {
                if ((mask & (1 << side.ordinal())) != 0) {
                    shape = Shapes.joinUnoptimized(shape, LampShapes.get(partModel(type().id(), layout, side), side.getOpposite(), true),
                            net.minecraft.world.phys.shapes.BooleanOp.OR);
                }
            }
            return shape.optimize();
        });
    }

    /** Nom du modèle de bande (forme ou rendu) pour la face donnée : light_strip_middle, light_strip_high_r... */
    public static String partModel(String id, int layout, Direction side) {
        Slot slot = Slot.values()[LampBlockEntity.slot(layout, side)];
        return id + "_" + slot.getSerializedName() + (LampBlockEntity.rotated(layout, side) ? "_r" : "");
    }

    @Override
    protected Direction shapeFacing(BlockState state) {
        for (Direction d : Direction.values()) {
            if (has(state, d)) {
                return d.getOpposite();
            }
        }
        return Direction.UP;
    }

    // ------------------------------------------------------------------ chaînes redstone

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean movedByPiston) {
        if (level.isClientSide || updatingChain) {
            return;
        }
        if (level.getBlockEntity(pos) instanceof LampBlockEntity lamp && lamp.connections() != 0) {
            updateChain(level, pos);
        } else {
            super.neighborChanged(state, level, pos, neighbor, neighborPos, movedByPiston);
        }
    }

    /** Bandes reliées à celle-ci (elle comprise), en largeur d'abord, dans la limite réglée dans la config. */
    public static List<BlockPos> chain(Level level, BlockPos start) {
        int limit = MostLightConfig.get(MostLightConfig.LED_CHAIN_LENGTH);
        List<BlockPos> out = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start.immutable());
        seen.add(start.immutable());
        while (!queue.isEmpty() && out.size() < limit) {
            BlockPos pos = queue.poll();
            if (!(level.getBlockEntity(pos) instanceof LampBlockEntity lamp)) {
                continue;
            }
            out.add(pos);
            for (Direction d : Direction.values()) {
                if (!lamp.connected(d)) {
                    continue;
                }
                BlockPos next = pos.relative(d);
                if (!seen.contains(next) && level.isLoaded(next) && level.getBlockState(next).getBlock() instanceof LightStripBlock
                        && level.getBlockEntity(next) instanceof LampBlockEntity other && other.connected(d.getOpposite())) {
                    seen.add(next);
                    queue.add(next);
                }
            }
        }
        return out;
    }

    /** Toute la chaîne suit la redstone : allumée si au moins une de ses bandes est alimentée. */
    public static void updateChain(Level level, BlockPos start) {
        List<BlockPos> chain = chain(level, start);
        boolean powered = false;
        for (BlockPos pos : chain) {
            if (level.hasNeighborSignal(pos)) {
                powered = true;
                break;
            }
        }
        updatingChain = true;
        try {
            for (BlockPos pos : chain) {
                BlockState state = level.getBlockState(pos);
                if (state.getBlock() instanceof LightStripBlock && state.getValue(POWERED) != powered) {
                    // pas de mise à jour des voisins : les bandes n'émettent pas de redstone
                    level.setBlock(pos, state.setValue(POWERED, powered).setValue(LIT, powered), Block.UPDATE_CLIENTS);
                }
            }
        } finally {
            updatingChain = false;
        }
    }
}
