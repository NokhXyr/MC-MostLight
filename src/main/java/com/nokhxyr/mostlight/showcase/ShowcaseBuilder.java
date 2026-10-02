package com.nokhxyr.mostlight.showcase;

import com.mojang.logging.LogUtils;
import com.nokhxyr.mostlight.block.HorizontalLampBlock;
import com.nokhxyr.mostlight.block.LampBlock;
import com.nokhxyr.mostlight.block.LampCategory;
import com.nokhxyr.mostlight.block.LampFinish;
import com.nokhxyr.mostlight.block.LampType;
import com.nokhxyr.mostlight.block.LightTone;
import com.nokhxyr.mostlight.block.OmniLampBlock;
import com.nokhxyr.mostlight.block.Placement;
import com.nokhxyr.mostlight.block.TallLampBlock;
import com.nokhxyr.mostlight.link.LightSwitchBlock;
import com.nokhxyr.mostlight.link.SwitchBlockEntity;
import com.nokhxyr.mostlight.registry.ModBlocks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.slf4j.Logger;

/**
 * Construit une galerie de toutes les lampes : une rangée par modèle (16 couleurs),
 * puis des rangées de finitions, de teintes de lumière et une démo d'interrupteurs.
 * Les lampes regardent vers le nord : on les admire depuis le nord.
 */
public final class ShowcaseBuilder {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** Assez d'espace pour placer la caméra devant chaque rangée. */
    public static final int ROW_SPACING = 9;
    /** Une rangée fait 16 blocs (20 avec les interrupteurs de la démo). */
    public static final int COLUMN_SPACING = 24;

    /** Position de caméra (yaw 0 = regarde vers le sud) et début de la rangée filmée (null pour la vue d'ensemble). */
    public record View(double x, double y, double z, float yaw, float pitch, BlockPos row) {}

    /** Vrai si la lampe vérifiée appartient à la rangée filmée. */
    public static boolean inRow(Check check, BlockPos row) {
        int dx = check.pos().getX() - row.getX();
        return Math.abs(check.pos().getZ() - row.getZ()) <= 2 && dx >= -3 && dx <= 18;
    }

    /** Lampe posée à vérifier : position du bloc qui éclaire, lumière attendue. */
    public record Check(BlockPos pos, String label, int expectedLight) {}

    /** views : galerie (vue d'ensemble + rangées) ; closeups : un gros plan par modèle. */
    public record Result(List<View> views, List<View> closeups, List<Check> checks) {}

    private record Entry(LampType type, DyeColor color, LampFinish finish, LightTone tone) {}

    private ShowcaseBuilder() {}

    /**
     * Construit la galerie centrée sur {@code center} : une colonne par catégorie (une rangée par modèle,
     * 16 couleurs), plus une colonne finitions / teintes / interrupteurs. L'ensemble reste compact pour que
     * tous les chunks restent chargés pendant que le moteur de lumière travaille.
     */
    public static Result build(ServerLevel level, BlockPos center) {
        List<List<List<Entry>>> columns = new ArrayList<>();
        for (LampCategory category : LampCategory.values()) {
            List<List<Entry>> column = new ArrayList<>();
            for (LampType type : LampType.values()) {
                if (type.category() == category) {
                    List<Entry> row = new ArrayList<>();
                    for (DyeColor color : DyeColor.values()) {
                        row.add(new Entry(type, color, type.defaultFinish(), LightTone.AUTO));
                    }
                    column.add(row);
                }
            }
            columns.add(column);
        }
        List<List<Entry>> extras = new ArrayList<>();
        // finitions : chaque lampe dans les 10 finitions
        for (LampType type : new LampType[] {LampType.CHANDELIER, LampType.TABLE_LAMP, LampType.WALL_LANTERN,
                LampType.TIFFANY_LAMP, LampType.STREET_LAMP, LampType.FAN_LIGHT}) {
            List<Entry> row = new ArrayList<>();
            for (LampFinish finish : LampFinish.values()) {
                row.add(new Entry(type, DyeColor.RED, finish, LightTone.AUTO));
            }
            extras.add(row);
        }
        // teintes de lumière : 3 couleurs x 5 teintes
        for (LampType type : new LampType[] {LampType.LAMP_BLOCK, LampType.GLOBE_PENDANT, LampType.FLOOR_LAMP}) {
            List<Entry> row = new ArrayList<>();
            for (DyeColor color : new DyeColor[] {DyeColor.WHITE, DyeColor.ORANGE, DyeColor.BLUE}) {
                for (LightTone tone : LightTone.values()) {
                    row.add(new Entry(type, color, type.defaultFinish(), tone));
                }
            }
            extras.add(row);
        }
        columns.add(extras);

        int maxRows = columns.stream().mapToInt(List::size).max().orElse(0) + 1;
        BlockPos origin = center.offset(-columns.size() * COLUMN_SPACING / 2, 0, -maxRows * ROW_SPACING / 2);
        int ground = origin.getY();
        // sol sombre pour faire ressortir l'éclairage
        for (int x = -6; x < columns.size() * COLUMN_SPACING; x++) {
            for (int z = -8; z < maxRows * ROW_SPACING; z++) {
                level.setBlock(origin.offset(x, -1, z), Blocks.POLISHED_DEEPSLATE.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }

        List<Check> checks = new ArrayList<>();
        List<View> views = new ArrayList<>();
        List<View> closeups = new ArrayList<>();
        views.add(new View(center.getX(), ground + 45, origin.getZ() - 30, 0, 42, null));
        for (int c = 0; c < columns.size(); c++) {
            List<List<Entry>> column = columns.get(c);
            for (int r = 0; r < column.size(); r++) {
                BlockPos rowStart = origin.offset(c * COLUMN_SPACING, 0, r * ROW_SPACING);
                List<Entry> row = column.get(r);
                for (int i = 0; i < row.size(); i++) {
                    place(level, rowStart.east(i), row.get(i), checks);
                }
                views.add(rowView(rowStart, ground));
                if (c < columns.size() - 1) {
                    closeups.add(closeView(rowStart, row.get(0).type(), ground));
                }
            }
        }
        BlockPos demo = origin.offset((columns.size() - 1) * COLUMN_SPACING, 0, extras.size() * ROW_SPACING);
        buildSwitchDemo(level, demo, checks);
        views.add(rowView(demo, ground));

        LOGGER.info("[MostLight showcase] {} colonnes, {} lampes posées, {} vues", columns.size(), checks.size(), views.size());
        return new Result(views, closeups, checks);
    }

    /** Gros plan de trois quarts sur la première lampe de la rangée (blanche), visée en son centre. */
    private static View closeView(BlockPos rowStart, LampType type, int ground) {
        double centerY = switch (type.placement()) {
            case HANGING -> ground + 2.45;
            case WALL, OMNI -> ground + 1.5;
            case TALL -> ground + 1.1;
            case STANDING -> type.category() == LampCategory.TABLE ? ground + 1.45 : ground + 0.5;
            case CUBE -> ground + 0.5;
        };
        double tx = rowStart.getX() + 0.5, tz = rowStart.getZ() + 0.5;
        double distance = type.placement() == Placement.TALL ? 3.4 : 2.0;
        double cx = tx - distance * 0.55, cz = tz - distance, eye = centerY + distance * 0.25;
        float yaw = (float) Math.toDegrees(Math.atan2(-(tx - cx), tz - cz));
        float pitch = (float) Math.toDegrees(Math.atan2(eye - centerY, Math.hypot(tx - cx, tz - cz)));
        // la position téléportée est celle des pieds : les yeux sont 1,62 bloc plus haut
        return new View(cx, eye - 1.62, cz, yaw, pitch, rowStart);
    }

    /** Caméra à hauteur d'yeux, face à la rangée qui commence en {@code rowStart}. */
    private static View rowView(BlockPos rowStart, int ground) {
        return new View(rowStart.getX() + 8, ground + 1.4, rowStart.getZ() - 3.6, 0, 10, rowStart);
    }

    private static void place(ServerLevel level, BlockPos pos, Entry entry, List<Check> checks) {
        LampBlock block = ModBlocks.lamp(entry.type(), entry.color());
        Placement placement = entry.type().placement();
        BlockPos lampPos = pos;
        BlockPos lightPos = pos;
        switch (placement) {
            case HANGING -> {
                level.setBlock(pos.above(3), Blocks.DARK_OAK_PLANKS.defaultBlockState(), Block.UPDATE_ALL);
                lampPos = lightPos = pos.above(2);
                level.setBlock(lampPos, block.defaultBlockState().setValue(HorizontalLampBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);
            }
            case WALL, OMNI -> {
                for (int y = 0; y < 3; y++) {
                    level.setBlock(pos.offset(0, y, 1), Blocks.SMOOTH_QUARTZ.defaultBlockState(), Block.UPDATE_ALL);
                }
                lampPos = lightPos = pos.above();
                BlockState state = placement == Placement.WALL
                        ? block.defaultBlockState().setValue(HorizontalLampBlock.FACING, Direction.NORTH)
                        : block.defaultBlockState().setValue(OmniLampBlock.FACING, Direction.NORTH);
                level.setBlock(lampPos, state, Block.UPDATE_ALL);
            }
            case STANDING -> {
                if (entry.type().category() == LampCategory.TABLE) {
                    level.setBlock(pos, Blocks.STRIPPED_SPRUCE_WOOD.defaultBlockState(), Block.UPDATE_ALL);
                    lampPos = lightPos = pos.above();
                }
                level.setBlock(lampPos, block.defaultBlockState().setValue(HorizontalLampBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);
            }
            case TALL -> {
                BlockState lower = block.defaultBlockState().setValue(TallLampBlock.FACING, Direction.NORTH);
                level.setBlock(pos, lower, Block.UPDATE_ALL);
                level.setBlock(pos.above(), lower.setValue(TallLampBlock.HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
                lightPos = pos.above();
            }
            case CUBE -> level.setBlock(pos, block.defaultBlockState(), Block.UPDATE_ALL);
        }
        BlockState placed = level.getBlockState(lampPos);
        if (placed.getBlock() instanceof LampBlock lamp) {
            lamp.setLook(level, lampPos, placed, entry.finish(), entry.tone());
        }
        String label = entry.type().id() + "/" + entry.color().getSerializedName() + "/" + entry.finish().getSerializedName()
                + "/" + entry.tone().getSerializedName();
        checks.add(new Check(lightPos, label, 15));
    }

    /** Huit suspensions liées à un interrupteur, huit lampes de table liées à un variateur. */
    private static void buildSwitchDemo(ServerLevel level, BlockPos start, List<Check> checks) {
        List<BlockPos> pendants = new ArrayList<>();
        List<BlockPos> tables = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            BlockPos p = start.offset(i, 0, 0);
            level.setBlock(p.above(3), Blocks.DARK_OAK_PLANKS.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(p.above(2), ModBlocks.lamp(LampType.PENDANT_LAMP, DyeColor.values()[i]).defaultBlockState()
                    .setValue(HorizontalLampBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);
            pendants.add(p.above(2));
            BlockPos t = start.offset(i + 8, 0, 0);
            level.setBlock(t, Blocks.STRIPPED_SPRUCE_WOOD.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(t.above(), ModBlocks.lamp(LampType.TABLE_LAMP, DyeColor.values()[i + 8]).defaultBlockState()
                    .setValue(HorizontalLampBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);
            tables.add(t.above());
        }
        for (BlockPos p : pendants) {
            checks.add(new Check(p, "switch_demo/pendant", 15));
        }
        for (BlockPos t : tables) {
            checks.add(new Check(t, "switch_demo/table", 15));
        }
        placeSwitch(level, start.offset(-2, 1, 0), ModBlocks.LIGHT_SWITCH.get(), pendants);
        placeSwitch(level, start.offset(17, 1, 0), ModBlocks.DIMMER_SWITCH.get(), tables);
    }

    private static void placeSwitch(ServerLevel level, BlockPos pos, LightSwitchBlock block, List<BlockPos> links) {
        level.setBlock(pos.below(), Blocks.SMOOTH_QUARTZ.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(pos.south(), Blocks.SMOOTH_QUARTZ.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(pos, block.defaultBlockState()
                .setValue(LightSwitchBlock.FACE, AttachFace.WALL)
                .setValue(LightSwitchBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof SwitchBlockEntity entity) {
            entity.setLinks(links);
        }
    }

    /** Vérifie que chaque lampe a survécu et éclaire. Renvoie le nombre d'échecs. */
    public static int verify(ServerLevel level, List<Check> checks) {
        int failures = 0;
        for (Check check : checks) {
            BlockState state = level.getBlockState(check.pos());
            int light = level.getBrightness(LightLayer.BLOCK, check.pos());
            int around = level.getBrightness(LightLayer.BLOCK, check.pos().north());
            boolean ok = state.getBlock() instanceof LampBlock && state.getLightEmission() == check.expectedLight()
                    && light == check.expectedLight() && around >= check.expectedLight() - 2;
            if (!ok) {
                failures++;
                LOGGER.warn("[MostLight showcase] ÉCHEC {} en {} : bloc={}, émission={}, lumière={}, devant={}",
                        check.label(), check.pos().toShortString(), state.getBlock(), state.getLightEmission(), light, around);
            }
        }
        return failures;
    }
}
