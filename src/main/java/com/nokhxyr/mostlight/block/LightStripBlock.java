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
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Bande (LED, guirlande). Un bloc peut porter une bande sur chacune de ses 6 faces (comme le lichen lumineux). Chaque
 * bande a un sens (au mur : horizontale, ou verticale en étant accroupi ; au sol et au plafond : dans l'axe du regard)
 * et une position (basse / milieu / haute, selon l'endroit visé), gardés dans la block entity.
 * <p>
 * Le rendu relie les bandes comme un fil de redstone : continuité sur le même mur, angle intérieur (deux bandes du même
 * bloc), angle extérieur (autour de l'arête du support), en L, en T ou en croix. Avec le connecteur LED, des bandes
 * voisines sont aussi reliées pour la redstone : alimenter l'une allume toute la chaîne.
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
    /** Coordonnée (en pixels, le long de l'axe de position) des positions basse, milieu, haute. */
    public static final double[] NODES = {1.5, 8, 14.5};
    private static final Map<Long, VoxelShape> SHAPES = new ConcurrentHashMap<>();
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

    /** Guirlande (plus épaisse, pend) plutôt que bande plate. */
    private boolean garland() {
        return type() != LampType.LIGHT_STRIP;
    }

    // ------------------------------------------------------------------ sens et position

    /** Axe le long duquel court la bande fixée côté {@code side} : au mur horizontale (verticale si tournée), au sol x (z si tournée). */
    public static Direction.Axis lineAxis(Direction side, boolean rotated) {
        if (side.getAxis().isHorizontal()) {
            return rotated ? Direction.Axis.Y : side.getAxis() == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        }
        return rotated ? Direction.Axis.Z : Direction.Axis.X;
    }

    /** Axe de la position (basse / milieu / haute) : le troisième axe, ni celui de la face ni celui de la bande. */
    public static Direction.Axis slotAxis(Direction side, Direction.Axis line) {
        for (Direction.Axis axis : Direction.Axis.values()) {
            if (axis != side.getAxis() && axis != line) {
                return axis;
            }
        }
        return Direction.Axis.Y;
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
                boolean rotated = side.getAxis().isHorizontal()
                        // au mur : horizontale par défaut, verticale en étant accroupi
                        ? context.getPlayer() != null && context.getPlayer().isSecondaryUseActive()
                        // au sol / au plafond : la bande suit le regard du joueur
                        : context.getHorizontalDirection().getAxis() == Direction.Axis.Z;
                Direction.Axis b = slotAxis(side, lineAxis(side, rotated));
                double hit = context.getClickLocation().get(b) - context.getClickedPos().get(b);
                int slot = hit < 1.0 / 3 ? 0 : hit < 2.0 / 3 ? 1 : 2;
                lamp.setStrip(side, slot, rotated);
            }
        }
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
        boolean garland = garland();
        return SHAPES.computeIfAbsent((long) mask | ((long) layout << 6) | (garland ? 1L << 40 : 0), k -> {
            VoxelShape shape = Shapes.empty();
            for (Direction side : Direction.values()) {
                if ((mask & (1 << side.ordinal())) != 0) {
                    shape = Shapes.joinUnoptimized(shape, lineShape(side, layout, garland), BooleanOp.OR);
                }
            }
            return shape.optimize();
        });
    }

    /** Boîte de la bande fixée côté {@code side} : toute la longueur du bloc, à sa position. */
    private static VoxelShape lineShape(Direction side, int layout, boolean garland) {
        Direction.Axis line = lineAxis(side, LampBlockEntity.rotated(layout, side));
        Direction.Axis b = slotAxis(side, line);
        double c = NODES[LampBlockEntity.slot(layout, side)];
        double half = garland ? 2.6 : 1.5;
        double thick = garland ? 4 : 1;
        double[] min = new double[3];
        double[] max = new double[3];
        min[line.ordinal()] = 0;
        max[line.ordinal()] = 16;
        min[b.ordinal()] = Math.max(0, c - half);
        max[b.ordinal()] = Math.min(16, c + half);
        boolean positive = side.getAxisDirection() == Direction.AxisDirection.POSITIVE;
        min[side.getAxis().ordinal()] = positive ? 16 - thick : 0;
        max[side.getAxis().ordinal()] = positive ? 16 : thick;
        return Block.box(min[0], min[1], min[2], max[0], max[1], max[2]);
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

    // ------------------------------------------------------------------ liaisons visuelles (angles, T, croix)

    /**
     * Morceau de bande à dessiner, codé en int : face (côté du support), axe de la ligne, position (0..2) et deux
     * bouts parmi -ext, 0, trim, 1.5, 8, 14.5, 16-trim, 16, 16+ext (indices 0..8, le long de l'axe, sens croissant).
     */
    public static int segment(Direction side, Direction.Axis axis, int slot, int from, int to) {
        return side.ordinal() | axis.ordinal() << 3 | slot << 5 | from << 7 | to << 11;
    }

    public static Direction segmentSide(int seg) {
        return Direction.values()[seg & 7];
    }

    public static Direction.Axis segmentAxis(int seg) {
        return Direction.Axis.values()[(seg >> 3) & 3];
    }

    public static int segmentSlot(int seg) {
        return (seg >> 5) & 3;
    }

    public static int segmentFrom(int seg) {
        return (seg >> 7) & 15;
    }

    public static int segmentTo(int seg) {
        return (seg >> 11) & 15;
    }

    /** Bande voisine qui se raccorde : dans le même bloc (angle intérieur), sur le même mur, ou autour de l'arête. */
    private record Partner(int kind, Direction side, Direction.Axis axis, int slot) {
        static final int INNER = 0;
        static final int FLAT = 1;
        static final int OUTER = 2;
    }

    /** Deux bandes parallèles côte à côte (même axe, pas dans le prolongement) ne se relient pas. */
    private static boolean joins(Direction.Axis ours, Direction.Axis theirs, Direction toward) {
        return ours != theirs || ours == toward.getAxis();
    }

    private static int layoutAt(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof LampBlockEntity lamp ? lamp.stripLayout() : LampBlockEntity.DEFAULT_STRIP_LAYOUT;
    }

    private static @Nullable Partner partner(BlockGetter level, BlockPos pos, BlockState state, int layout, LampType type,
            Direction side, Direction.Axis axis, Direction toward) {
        if (has(state, toward)) {
            Direction.Axis theirs = lineAxis(toward, LampBlockEntity.rotated(layout, toward));
            if (joins(axis, theirs, toward)) {
                return new Partner(Partner.INNER, toward, theirs, LampBlockEntity.slot(layout, toward));
            }
        }
        BlockPos next = pos.relative(toward);
        BlockState flat = level.getBlockState(next);
        if (flat.getBlock() instanceof LightStripBlock other && other.type() == type && has(flat, side)) {
            int l = layoutAt(level, next);
            Direction.Axis theirs = lineAxis(side, LampBlockEntity.rotated(l, side));
            if (joins(axis, theirs, toward)) {
                return new Partner(Partner.FLAT, side, theirs, LampBlockEntity.slot(l, side));
            }
        }
        BlockPos around = next.relative(side);
        BlockState outer = level.getBlockState(around);
        Direction back = toward.getOpposite();
        if (outer.getBlock() instanceof LightStripBlock other && other.type() == type && has(outer, back)
                && !(flat.getBlock() instanceof LightStripBlock && has(flat, side))) {
            int l = layoutAt(level, around);
            Direction.Axis theirs = lineAxis(back, LampBlockEntity.rotated(l, back));
            if (joins(axis, theirs, toward)) {
                return new Partner(Partner.OUTER, back, theirs, LampBlockEntity.slot(l, back));
            }
        }
        return null;
    }

    /** Indice du bout de la ligne du côté {@code toward} selon la bande qui s'y raccorde. */
    private static int end(@Nullable Partner partner, Direction side, Direction toward) {
        boolean positive = toward.getAxisDirection() == Direction.AxisDirection.POSITIVE;
        if (partner != null && partner.kind() == Partner.INNER && side.ordinal() > toward.ordinal()) {
            // angle intérieur : une des deux bandes s'arrête contre l'autre au lieu de la traverser
            return positive ? 6 : 2;
        }
        if (partner != null && partner.kind() == Partner.OUTER && side.ordinal() < partner.side().ordinal()) {
            // angle extérieur : une des deux bandes déborde pour boucher le coin de l'arête
            return positive ? 8 : 0;
        }
        return positive ? 7 : 1;
    }

    /** Tous les morceaux des bandes du bloc, raccordés aux bandes voisines. */
    public static int[] segments(BlockGetter level, BlockPos pos, BlockState state, int layout) {
        if (!(state.getBlock() instanceof LightStripBlock strip)) {
            return new int[0];
        }
        LampType type = strip.type();
        List<Integer> out = new ArrayList<>();
        for (Direction side : Direction.values()) {
            if (!has(state, side)) {
                continue;
            }
            Direction.Axis axis = lineAxis(side, LampBlockEntity.rotated(layout, side));
            Direction.Axis b = slotAxis(side, axis);
            int c = LampBlockEntity.slot(layout, side);
            Direction neg = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE);
            Direction pos2 = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
            Direction bNeg = Direction.fromAxisAndDirection(b, Direction.AxisDirection.NEGATIVE);
            Direction bPos = Direction.fromAxisAndDirection(b, Direction.AxisDirection.POSITIVE);
            Partner pn = partner(level, pos, state, layout, type, side, axis, neg);
            Partner pp = partner(level, pos, state, layout, type, side, axis, pos2);
            Partner qn = partner(level, pos, state, layout, type, side, axis, bNeg);
            Partner qp = partner(level, pos, state, layout, type, side, axis, bPos);
            boolean arms = qn != null || qp != null;
            int node = qn != null ? qn.slot() : qp != null ? qp.slot() : 1;
            // ligne principale coupée au nœud ; en L, la moitié sans suite disparaît
            if (!arms || pn != null || pp == null) {
                out.add(segment(side, axis, c, end(pn, side, neg), 3 + node));
            }
            if (!arms || pp != null || pn == null) {
                out.add(segment(side, axis, c, 3 + node, end(pp, side, pos2)));
            }
            // bras perpendiculaires vers les bandes qui arrivent de côté
            if (qn != null) {
                out.add(segment(side, b, qn.slot(), end(qn, side, bNeg), 3 + c));
            }
            if (qp != null) {
                out.add(segment(side, b, qp.slot(), 3 + c, end(qp, side, bPos)));
            }
        }
        return out.stream().mapToInt(Integer::intValue).toArray();
    }

    // ------------------------------------------------------------------ chaînes redstone

    /** Blocs qui se relient en chaîne avec le connecteur : bandes et blocs lumineux pleins. */
    public static boolean chainable(BlockState state) {
        return state.getBlock() instanceof LightStripBlock || state.getBlock() instanceof CubeLampBlock;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean movedByPiston) {
        if (!chainNeighborChanged(level, pos)) {
            super.neighborChanged(state, level, pos, neighbor, neighborPos, movedByPiston);
        }
    }

    /** Bloc relié à une chaîne : toute la chaîne est recalculée (vrai), sinon comportement normal (faux). */
    public static boolean chainNeighborChanged(Level level, BlockPos pos) {
        if (level.isClientSide || updatingChain) {
            // rien à faire côté client ; pendant un recalcul de chaîne, on ignore les échos
            return true;
        }
        if (level.getBlockEntity(pos) instanceof LampBlockEntity lamp && lamp.connections() != 0) {
            updateChain(level, pos);
            return true;
        }
        return false;
    }

    /** Blocs reliés à celui-ci (lui compris), en largeur d'abord, dans la limite réglée dans la config. */
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
                if (!seen.contains(next) && level.isLoaded(next) && chainable(level.getBlockState(next))
                        && level.getBlockEntity(next) instanceof LampBlockEntity other && other.connected(d.getOpposite())) {
                    seen.add(next);
                    queue.add(next);
                }
            }
        }
        return out;
    }

    /** Toute la chaîne suit la redstone : allumée si au moins un de ses blocs est alimenté. */
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
                if (chainable(state) && state.getValue(POWERED) != powered) {
                    // pas de mise à jour des voisins : ces blocs n'émettent pas de redstone
                    level.setBlock(pos, state.setValue(POWERED, powered).setValue(LIT, powered), Block.UPDATE_CLIENTS);
                }
            }
        } finally {
            updatingChain = false;
        }
    }

    /** Point au milieu de la première bande du bloc (pour dessiner les liaisons), en coordonnées du monde. */
    public static Vec3 anchor(BlockState state, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        if (state.getBlock() instanceof LightStripBlock) {
            for (Direction d : Direction.values()) {
                if (has(state, d)) {
                    return center.add(Vec3.atLowerCornerOf(d.getNormal()).scale(0.4));
                }
            }
        }
        return center;
    }
}
