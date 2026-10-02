package com.nokhxyr.mostlight.compat;

import com.mojang.logging.LogUtils;
import com.nokhxyr.mostlight.block.LampBlock;
import com.nokhxyr.mostlight.registry.ModBlocks;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.registries.DeferredBlock;
import org.slf4j.Logger;

/**
 * Lumière colorée sous shaders (Iris). Les packs comme Complementary colorent la lumière d'après l'identifiant
 * de matériau que leur block.properties attribue à chaque bloc ; un bloc moddé inconnu éclaire en blanc.
 * On donne donc à chaque lampe allumée l'identifiant que le pack actif a choisi pour une source vanilla de la
 * même couleur. Aucune dépendance au pack : si la source de référence n'est pas listée, rien ne change.
 */
public final class IrisLightColors {
    private static final Logger LOGGER = LogUtils.getLogger();

    private IrisLightColors() {}

    /** Source de lumière vanilla dont la couleur se rapproche le plus de chaque teinte. */
    private static Map<DyeColor, BlockState> references() {
        Map<DyeColor, BlockState> refs = new EnumMap<>(DyeColor.class);
        BlockState endRod = Blocks.END_ROD.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP);
        refs.put(DyeColor.WHITE, endRod);
        refs.put(DyeColor.LIGHT_GRAY, endRod);
        refs.put(DyeColor.GRAY, Blocks.SEA_LANTERN.defaultBlockState());
        refs.put(DyeColor.BLACK, Blocks.AMETHYST_CLUSTER.defaultBlockState());
        refs.put(DyeColor.BROWN, Blocks.LANTERN.defaultBlockState());
        refs.put(DyeColor.RED, Blocks.REDSTONE_BLOCK.defaultBlockState());
        refs.put(DyeColor.ORANGE, Blocks.SHROOMLIGHT.defaultBlockState());
        refs.put(DyeColor.YELLOW, Blocks.OCHRE_FROGLIGHT.defaultBlockState());
        refs.put(DyeColor.LIME, Blocks.VERDANT_FROGLIGHT.defaultBlockState());
        refs.put(DyeColor.GREEN, Blocks.VERDANT_FROGLIGHT.defaultBlockState());
        refs.put(DyeColor.CYAN, Blocks.SOUL_LANTERN.defaultBlockState());
        refs.put(DyeColor.LIGHT_BLUE, Blocks.SOUL_LANTERN.defaultBlockState());
        refs.put(DyeColor.BLUE, Blocks.SEA_LANTERN.defaultBlockState());
        refs.put(DyeColor.PURPLE, Blocks.CRYING_OBSIDIAN.defaultBlockState());
        refs.put(DyeColor.MAGENTA, Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState());
        refs.put(DyeColor.PINK, Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState());
        return refs;
    }

    /** Renvoie une copie de la table d'Iris enrichie des lampes allumées. */
    public static Object2IntMap<BlockState> withLamps(Object2IntMap<BlockState> ids) {
        if (ids == null || ids.isEmpty()) {
            return ids;
        }
        Object2IntMap<BlockState> out = new Object2IntOpenHashMap<>(ids);
        out.defaultReturnValue(ids.defaultReturnValue());
        Map<DyeColor, BlockState> refs = references();
        int added = 0;
        for (DeferredBlock<LampBlock> holder : ModBlocks.all()) {
            LampBlock lamp = holder.get();
            BlockState ref = refs.get(lamp.color());
            if (ref == null || !ids.containsKey(ref)) {
                continue;
            }
            int id = ids.getInt(ref);
            for (BlockState state : lamp.getStateDefinition().getPossibleStates()) {
                // seules les parties qui éclairent prennent l'identifiant (pas la moitié basse d'un lampadaire)
                if (state.getLightEmission() > 0 && !out.containsKey(state)) {
                    out.put(state, id);
                    added++;
                }
            }
        }
        LOGGER.info("[MostLight] lumière colorée Iris : {} états de lampes associés", added);
        return out;
    }
}
