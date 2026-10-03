package com.nokhxyr.mostlight.block;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Construit les hitbox à partir des boîtes générées, tournées comme dans les blockstates :
 * modèles horizontaux dessinés vers le nord, modèles omnidirectionnels dessinés vers le haut.
 */
public final class LampShapes {
    private static final Map<String, VoxelShape> CACHE = new ConcurrentHashMap<>();

    private LampShapes() {}

    public static VoxelShape get(String model, Direction facing, boolean omni) {
        return CACHE.computeIfAbsent(model + "/" + facing.getName() + "/" + omni, k -> build(model, facing, omni));
    }

    /** Points de flamme du modèle (orientation de référence), ou null. */
    public static double @Nullable [][] flames(String model) {
        return GeneratedShapes.FLAMES.get(model);
    }

    /** Engrenage animé d'une lampe : modèle (block/...), centre en pixels, vitesse relative (signe = sens). */
    public record Gear(String model, double cx, double cy, double cz, double speed) {}

    public static java.util.List<Gear> gears(String lamp) {
        return GeneratedShapes.GEARS.getOrDefault(lamp, java.util.List.of());
    }

    /** Animation GeckoLib d'une lampe : nom de l'animation, déclencheur « lit » (allumée) ou « fan » (ventilateur). */
    public record Animation(String name, String trigger) {}

    public static @Nullable Animation animation(String lamp) {
        return GeneratedShapes.ANIMATIONS.get(lamp);
    }

    public static java.util.Collection<java.util.List<Gear>> allGears() {
        return GeneratedShapes.GEARS.values();
    }

    private static VoxelShape build(String model, Direction facing, boolean omni) {
        double[][] boxes = GeneratedShapes.BOXES.get(model);
        if (boxes == null) {
            return Shapes.block();
        }
        // fusion sans optimisation intermédiaire (Shapes.or ré-optimise à chaque boîte), une seule à la fin
        VoxelShape shape = Shapes.empty();
        for (double[] b : boxes) {
            double[] p1 = rotate(b[0], b[1], b[2], facing, omni);
            double[] p2 = rotate(b[3], b[4], b[5], facing, omni);
            shape = Shapes.joinUnoptimized(shape, Block.box(
                    Math.min(p1[0], p2[0]), Math.min(p1[1], p2[1]), Math.min(p1[2], p2[2]),
                    Math.max(p1[0], p2[0]), Math.max(p1[1], p2[1]), Math.max(p1[2], p2[2])), BooleanOp.OR);
        }
        return shape.optimize();
    }

    /** Vrai si la lampe a un modèle couché (tiges posées le long d'une surface). */
    public static boolean hasLying(String lamp) {
        return GeneratedShapes.BOXES.containsKey(lamp + "_lying");
    }

    /**
     * Rotation de blockstate d'une tige couchée : modèle (0 = le long de x, 1 = le long de z), quarts de tour en x puis
     * en y. Même recherche que lyingRotation dans tools/generate.mjs.
     */
    public static int[] lyingRotation(Direction facing, Direction.Axis axis) {
        int[][] lines = {{1, 0, 0}, {0, 0, 1}};
        for (int m = 0; m < 2; m++) {
            for (int rx = 0; rx < 4; rx++) {
                for (int ry = 0; ry < 4; ry++) {
                    int[] up = turn(new int[] {0, 1, 0}, rx, ry);
                    int[] line = turn(lines[m].clone(), rx, ry);
                    if (up[0] == facing.getStepX() && up[1] == facing.getStepY() && up[2] == facing.getStepZ()
                            && Math.abs(line[axis.ordinal()]) == 1) {
                        return new int[] {m, rx, ry};
                    }
                }
            }
        }
        return new int[] {0, 0, 0};
    }

    private static int[] turn(int[] v, int rx, int ry) {
        for (int i = 0; i < rx; i++) {
            v = new int[] {v[0], v[2], -v[1]};
        }
        for (int i = 0; i < ry; i++) {
            v = new int[] {-v[2], v[1], v[0]};
        }
        return v;
    }

    /** Hitbox d'une tige couchée sur la face {@code facing}, le long de {@code axis}. */
    public static VoxelShape lying(String lamp, Direction facing, Direction.Axis axis) {
        return CACHE.computeIfAbsent(lamp + "/lying/" + facing.getName() + "/" + axis.getName(), k -> {
            int[] r = lyingRotation(facing, axis);
            double[][] boxes = GeneratedShapes.BOXES.get(lamp + (r[0] == 0 ? "_lying" : "_lying_z"));
            if (boxes == null) {
                return Shapes.block();
            }
            VoxelShape shape = Shapes.empty();
            for (double[] b : boxes) {
                double[] p1 = turnPoint(b[0], b[1], b[2], r[1], r[2]);
                double[] p2 = turnPoint(b[3], b[4], b[5], r[1], r[2]);
                shape = Shapes.joinUnoptimized(shape, Block.box(
                        Math.min(p1[0], p2[0]), Math.min(p1[1], p2[1]), Math.min(p1[2], p2[2]),
                        Math.max(p1[0], p2[0]), Math.max(p1[1], p2[1]), Math.max(p1[2], p2[2])), BooleanOp.OR);
            }
            return shape.optimize();
        });
    }

    private static double[] turnPoint(double x, double y, double z, int rx, int ry) {
        double[] v = {x - 8, y - 8, z - 8};
        for (int i = 0; i < rx; i++) {
            v = new double[] {v[0], v[2], -v[1]};
        }
        for (int i = 0; i < ry; i++) {
            v = new double[] {-v[2], v[1], v[0]};
        }
        return new double[] {v[0] + 8, v[1] + 8, v[2] + 8};
    }

    /** Tourne un point (en pixels) comme le blockstate tourne le modèle. */
    public static double[] rotate(double x, double y, double z, Direction facing, boolean omni) {
        if (omni) {
            if (facing == Direction.UP) {
                return new double[] {x, y, z};
            }
            if (facing == Direction.DOWN) {
                return new double[] {x, 16 - y, 16 - z};
            }
            // rotation x=90 du blockstate : le haut du modèle pointe vers le nord
            double ny = z;
            z = 16 - y;
            y = ny;
        }
        // rotation y du blockstate : 90° par quart de tour dans le sens horaire (nord -> est)
        int turns = (facing.get2DDataValue() + 2) % 4;
        for (int i = 0; i < turns; i++) {
            double nx = 16 - z;
            z = x;
            x = nx;
        }
        return new double[] {x, y, z};
    }
}
