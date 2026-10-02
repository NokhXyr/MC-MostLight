package com.nokhxyr.mostlight.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Bande LED : se fixe sur n'importe quelle face, en position basse, milieu ou haute (selon l'endroit visé),
 * à l'horizontale ou à la verticale (accroupi). En position basse/haute la bande touche l'arête du bloc :
 * deux bandes posées sur deux faces voisines forment un angle propre.
 */
public class LightStripBlock extends OmniLampBlock {
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

    public static final EnumProperty<Slot> SLOT = EnumProperty.create("slot", Slot.class);
    /** La bande court le long de l'autre axe de la face (verticale sur un mur). */
    public static final BooleanProperty ROTATED = BooleanProperty.create("rotated");

    public LightStripBlock(LampType type, DyeColor color, Properties properties) {
        super(type, color, properties);
        registerDefaultState(defaultBlockState().setValue(SLOT, Slot.MIDDLE).setValue(ROTATED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SLOT, ROTATED);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }
        Direction facing = state.getValue(FACING);
        Player player = context.getPlayer();
        boolean rotated;
        if (facing.getAxis().isVertical()) {
            // au sol / au plafond : la bande suit le regard du joueur
            Direction look = context.getHorizontalDirection();
            double[] a = LampShapes.rotate(0, 0, 8, facing, true);
            double[] b = LampShapes.rotate(16, 0, 8, facing, true);
            boolean stripAlongX = Math.abs(b[0] - a[0]) > 1;
            rotated = stripAlongX != (look.getAxis() == Direction.Axis.X);
        } else {
            // au mur : horizontale par défaut, verticale en étant accroupi
            rotated = player != null && player.isSecondaryUseActive();
        }
        state = state.setValue(ROTATED, rotated);
        return state.setValue(SLOT, slotFromHit(context, facing, rotated));
    }

    /** Position (basse / milieu / haute) selon l'endroit visé sur la face, dans l'axe perpendiculaire à la bande. */
    private static Slot slotFromHit(BlockPlaceContext context, Direction facing, boolean rotated) {
        // axe "position" du modèle : z pour une bande le long de x, x pour une bande tournée
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

    @Override
    protected String shapeModel(BlockState state) {
        return type().id() + "_" + state.getValue(SLOT).getSerializedName() + (state.getValue(ROTATED) ? "_r" : "");
    }
}
