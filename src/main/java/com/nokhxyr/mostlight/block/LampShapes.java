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
