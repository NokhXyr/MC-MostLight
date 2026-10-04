package com.nokhxyr.mostlight.client;

import com.nokhxyr.mostlight.block.LampBlock;
import com.nokhxyr.mostlight.block.LightStripBlock;
import com.nokhxyr.mostlight.block.entity.LampBlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;

/**
 * Rendu des bandes (LED, guirlandes). Les morceaux de ligne sont précalculés dans un repère local (support en y = 0,
 * ligne le long de x, position en z) ; on les tourne ici vers la face et l'axe voulus. Les morceaux à dessiner sont
 * choisis d'après les bandes voisines ({@link LightStripBlock#segments}) : angles, T et croix se raccordent.
 * Les quads tournés sont mis en cache : il n'y a que 18 orientations.
 */
public final class StripBakedModel implements IDynamicBakedModel {
    public static final ModelProperty<int[]> SEGMENTS = new ModelProperty<>();
    /** Indice de bout local quand le repère local va dans le sens inverse de l'axe du monde. */
    private static final int LAST_END = 8;

    /** templates[variante][allumé][position][bout1][bout2] */
    private final BakedModel[][][][][] templates;
    private final boolean sagVariants;
    private final BakedModel fallback;
    private final Map<Long, List<BakedQuad>> cache = new ConcurrentHashMap<>();

    public StripBakedModel(BakedModel[][][][][] templates, boolean sagVariants, BakedModel fallback) {
        this.templates = templates;
        this.sagVariants = sagVariants;
        this.fallback = fallback;
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data) {
        Integer stored = data.get(LampBlockEntity.STRIP_LAYOUT);
        int layout = stored == null ? LampBlockEntity.DEFAULT_STRIP_LAYOUT : stored;
        return data.derive().with(SEGMENTS, LightStripBlock.segments(level, pos, state, layout)).build();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data,
            @Nullable RenderType renderType) {
        if (state == null || side != null || !(state.getBlock() instanceof LightStripBlock)) {
            return state == null ? fallback.getQuads(null, side, rand, data, renderType) : List.of();
        }
        int[] segments = data.get(SEGMENTS);
        if (segments == null) {
            return List.of();
        }
        int lit = state.getValue(LampBlock.LIT) ? 1 : 0;
        List<BakedQuad> quads = new ArrayList<>();
        for (int seg : segments) {
            quads.addAll(segmentQuads(state, seg, lit, rand, renderType));
        }
        return quads;
    }

    /** Couches de rendu rencontrées (cutout, translucide...) : un numéro par couche pour la clé du cache. */
    private static final Map<RenderType, Integer> LAYERS = new ConcurrentHashMap<>();

    private List<BakedQuad> segmentQuads(BlockState state, int seg, int lit, RandomSource rand, @Nullable RenderType renderType) {
        int layer = renderType == null ? 0 : LAYERS.computeIfAbsent(renderType, t -> LAYERS.size() + 1);
        long key = ((long) seg << 8) | ((long) layer << 1) | lit;
        return cache.computeIfAbsent(key, k -> {
            Direction face = LightStripBlock.segmentSide(seg);
            Direction.Axis axis = LightStripBlock.segmentAxis(seg);
            Direction.Axis slotAxis = LightStripBlock.slotAxis(face, axis);
            // repère local : x = u (le long de la ligne), y = n (opposé au support), z = w (axe de position, sens croissant)
            int[] n = vec(face.getOpposite().getNormal());
            int[] w = unit(slotAxis);
            int[] a = unit(axis);
            // u x n = w : on prend u = +a ou -a selon le cas
            int[] cross = cross(a, n);
            boolean flipped = !(cross[0] == w[0] && cross[1] == w[1] && cross[2] == w[2]);
            int[] u = flipped ? new int[] {-a[0], -a[1], -a[2]} : a;
            int from = LightStripBlock.segmentFrom(seg);
            int to = LightStripBlock.segmentTo(seg);
            int i = flipped ? LAST_END - to : from;
            int j = flipped ? LAST_END - from : to;
            // guirlande au mur, ligne horizontale : variante qui pend vers le bas
            int variant = sagVariants && face.getAxis().isHorizontal() && axis != Direction.Axis.Y ? 1 : 0;
            BakedModel template = templates[variant][lit][LightStripBlock.segmentSlot(seg)][i][j];
            if (template == null || renderType != null && !template.getRenderTypes(state, rand, ModelData.EMPTY).contains(renderType)) {
                return List.of();
            }
            List<BakedQuad> out = new ArrayList<>();
            // une guirlande mêle pièces opaques et verre (modèle composite) : seulement la couche demandée
            for (BakedQuad quad : template.getQuads(state, null, rand, ModelData.EMPTY, renderType)) {
                out.add(transform(quad, u, n, w));
            }
            return List.copyOf(out);
        });
    }

    private static int[] vec(net.minecraft.core.Vec3i v) {
        return new int[] {v.getX(), v.getY(), v.getZ()};
    }

    private static int[] unit(Direction.Axis axis) {
        int[] v = new int[3];
        v[axis.ordinal()] = 1;
        return v;
    }

    private static int[] cross(int[] a, int[] b) {
        return new int[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    /** Applique la rotation (colonnes u, n, w) autour du centre du bloc : sommets, normales et direction du quad. */
    private static BakedQuad transform(BakedQuad quad, int[] u, int[] n, int[] w) {
        int[] v = quad.getVertices().clone();
        for (int i = 0; i < 4; i++) {
            int off = i * IQuadTransformer.STRIDE + IQuadTransformer.POSITION;
            float x = Float.intBitsToFloat(v[off]) - 0.5F;
            float y = Float.intBitsToFloat(v[off + 1]) - 0.5F;
            float z = Float.intBitsToFloat(v[off + 2]) - 0.5F;
            v[off] = Float.floatToRawIntBits(u[0] * x + n[0] * y + w[0] * z + 0.5F);
            v[off + 1] = Float.floatToRawIntBits(u[1] * x + n[1] * y + w[1] * z + 0.5F);
            v[off + 2] = Float.floatToRawIntBits(u[2] * x + n[2] * y + w[2] * z + 0.5F);
            int nOff = i * IQuadTransformer.STRIDE + IQuadTransformer.NORMAL;
            int packed = v[nOff];
            if ((packed & 0x00FFFFFF) != 0) {
                int nx = (byte) (packed & 0xFF);
                int ny = (byte) ((packed >> 8) & 0xFF);
                int nz = (byte) ((packed >> 16) & 0xFF);
                int rx = u[0] * nx + n[0] * ny + w[0] * nz;
                int ry = u[1] * nx + n[1] * ny + w[1] * nz;
                int rz = u[2] * nx + n[2] * ny + w[2] * nz;
                v[nOff] = (packed & 0xFF000000) | (rx & 0xFF) | ((ry & 0xFF) << 8) | ((rz & 0xFF) << 16);
            }
        }
        int[] d = vec(quad.getDirection().getNormal());
        Direction dir = Direction.getNearest(
                u[0] * d[0] + n[0] * d[1] + w[0] * d[2],
                u[1] * d[0] + n[1] * d[1] + w[1] * d[2],
                u[2] * d[0] + n[2] * d[1] + w[2] * d[2]);
        return new BakedQuad(v, quad.getTintIndex(), dir, quad.getSprite(), quad.isShade(), quad.hasAmbientOcclusion());
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        return fallback.getRenderTypes(state, rand, data);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return false;
    }

    @Override
    public boolean isGui3d() {
        return fallback.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return fallback.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public TextureAtlasSprite getParticleIcon() {
        return fallback.getParticleIcon();
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        return fallback.getParticleIcon(data);
    }

    @Override
    public ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }
}
