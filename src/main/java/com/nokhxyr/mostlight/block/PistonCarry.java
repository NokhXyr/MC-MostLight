package com.nokhxyr.mostlight.block;

import com.nokhxyr.mostlight.block.entity.LampBlockEntity;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonStructureResolver;

/**
 * Lampes poussées par un piston. Le piston vanilla refuse les blocs à block entity, et il ne transporte pas leurs
 * données : on autorise les lampes (PistonBaseBlockMixin) et on garde leurs données (finition, teinte, bandes,
 * liaisons) le temps du mouvement, rendues à la lampe qui apparaît à l'arrivée.
 */
public final class PistonCarry {
    /** Le mouvement d'un piston dure 2 ticks ; au-delà, une donnée en attente n'appartient plus à personne. */
    private static final long MAX_AGE = 6;

    /** Data of a moving lamp, the block it belongs to and when it left. */
    private record Pending(CompoundTag tag, net.minecraft.world.level.block.Block block, long time) {}

    private static final Map<ResourceKey<Level>, Map<BlockPos, Pending>> PENDING = new ConcurrentHashMap<>();

    private PistonCarry() {}

    /** Mêmes limites que le piston vanilla (hauteur du monde, bordure), sans le refus des block entities. */
    public static boolean canPush(Level level, BlockPos pos, Direction movement) {
        if (pos.getY() < level.getMinBuildHeight() || pos.getY() > level.getMaxBuildHeight() - 1 || !level.getWorldBorder().isWithinBounds(pos)) {
            return false;
        }
        if (movement == Direction.DOWN && pos.getY() == level.getMinBuildHeight()) {
            return false;
        }
        return movement != Direction.UP || pos.getY() != level.getMaxBuildHeight() - 1;
    }

    /** Juste avant que le piston déplace ses blocs : les données de chaque lampe poussée, rangées à sa case d'arrivée. */
    public static void capture(Level level, BlockPos piston, Direction facing, boolean extending) {
        if (level.isClientSide) {
            return;
        }
        PistonStructureResolver resolver = new PistonStructureResolver(level, piston, facing, extending);
        if (!resolver.resolve()) {
            return;
        }
        Direction move = resolver.getPushDirection();
        Map<BlockPos, Pending> pending = PENDING.computeIfAbsent(level.dimension(), k -> new HashMap<>());
        long now = level.getGameTime();
        pending.values().removeIf(p -> now - p.time() > MAX_AGE);
        for (BlockPos pos : resolver.getToPush()) {
            if (level.getBlockEntity(pos) instanceof LampBlockEntity lamp) {
                pending.put(pos.relative(move).immutable(), new Pending(lamp.saveWithoutMetadata(level.registryAccess()), lamp.getBlockState().getBlock(), now));
            }
        }
    }

    /** Appelé quand la block entity d'une lampe arrive dans le monde : reprend les données transportées, s'il y en a. */
    public static void restore(LampBlockEntity lamp) {
        Level level = lamp.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        Map<BlockPos, Pending> pending = PENDING.get(level.dimension());
        Pending carried = pending == null ? null : pending.remove(lamp.getBlockPos());
        // only onto the lamp that was moved: a block placed there meanwhile does not inherit its data
        if (carried != null && level.getGameTime() - carried.time() <= MAX_AGE && carried.block() == lamp.getBlockState().getBlock()) {
            lamp.loadWithComponents(carried.tag(), level.registryAccess());
            lamp.afterMove();
        }
    }
}
