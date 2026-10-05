package com.nokhxyr.mostlight.compat;

import com.mojang.logging.LogUtils;
import com.nokhxyr.mostlight.block.LampBlock;
import com.nokhxyr.mostlight.registry.ModBlocks;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.registries.DeferredBlock;
import org.slf4j.Logger;

/**
 * Lumière colorée sous shaders (Iris). Les packs comme Complementary colorent la lumière d'après l'identifiant
 * de matériau que leur block.properties attribue à chaque bloc ; un bloc moddé inconnu éclaire en blanc.
 * On donne donc à chaque lampe allumée l'identifiant que le pack actif a choisi pour une source vanilla de la
 * même couleur. Aucune dépendance au pack : si la source de référence n'est pas listée, rien ne change.
 *
 * <p>Le même identifiant règle aussi le rendu du bloc : pour une froglight, une lanterne ou une lanterne des âmes,
 * Solas et d'autres packs font briller toute la texture d'après sa couleur (abat-jour, cadre, pied compris).
 * On prend donc d'abord la bougie allumée de la même couleur, quand le pack lui donne un identifiant à elle :
 * c'est là que ces packs rangent les lampes colorées des autres mods, avec une lumière de la bonne couleur et
 * presque aucun effet sur la texture.
 */
public final class IrisLightColors {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Galerie de test seulement : true = sources vanilla uniquement, sans les bougies (comparaison). */
    public static boolean vanillaOnly;

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

    /** Bougie allumée de chaque couleur. */
    private static BlockState candle(DyeColor color) {
        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.withDefaultNamespace(color.getSerializedName() + "_candle"));
        return block.defaultBlockState().setValue(CandleBlock.LIT, true);
    }

    /**
     * Complementary (Reimagined, Unbound and packs built on them) colours its light from the vanilla sources without
     * lighting up whole textures, but leaves coloured candles warm unless an extra option is on: keep the vanilla
     * references there. Other packs (Solas...) make those sources glow over the whole texture: candles are better.
     */
    private static boolean prefersVanillaSources() {
        String pack = currentPackName();
        return pack != null && pack.toLowerCase(java.util.Locale.ROOT).contains("complementary");
    }

    /** Name of the active shader pack (Iris is an optional dependency: reflection). */
    private static @org.jetbrains.annotations.Nullable String currentPackName() {
        try {
            Object name = Class.forName("net.irisshaders.iris.Iris").getMethod("getCurrentPackName").invoke(null);
            return name == null ? null : name.toString();
        } catch (ReflectiveOperationException | LinkageError e) {
            return null;
        }
    }

    /** Renvoie une copie de la table d'Iris enrichie des lampes allumées. */
    public static Object2IntMap<BlockState> withLamps(Object2IntMap<BlockState> ids) {
        if (ids == null || ids.isEmpty()) {
            return ids;
        }
        Object2IntMap<BlockState> out = new Object2IntOpenHashMap<>(ids);
        out.defaultReturnValue(ids.defaultReturnValue());
        Map<DyeColor, BlockState> refs = references();
        // bougies colorées seulement si le pack les distingue de la bougie simple (sinon lumière sans couleur)
        BlockState plain = Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true);
        boolean candles = !vanillaOnly && !prefersVanillaSources();
        for (DyeColor color : candles ? DyeColor.values() : new DyeColor[0]) {
            BlockState candle = candle(color);
            if (ids.containsKey(candle) && (!ids.containsKey(plain) || ids.getInt(candle) != ids.getInt(plain))) {
                refs.put(color, candle);
            }
        }
        int added = 0;
        for (DeferredBlock<LampBlock> holder : ModBlocks.all()) {
            LampBlock lamp = holder.get();
            for (BlockState state : lamp.getStateDefinition().getPossibleStates()) {
                // the colour is a property: each state takes the reference of its own colour
                BlockState ref = refs.get(LampBlock.color(state));
                if (ref == null || !ids.containsKey(ref)) {
                    continue;
                }
                // seules les parties qui éclairent prennent l'identifiant (pas la moitié basse d'un lampadaire)
                if (state.getLightEmission() > 0 && !out.containsKey(state)) {
                    out.put(state, ids.getInt(ref));
                    added++;
                }
            }
        }
        LOGGER.info("[MostLight] lumière colorée Iris : {} états de lampes associés ({})", added,
                candles ? "bougies colorées" : "sources vanilla");
        return out;
    }
}
