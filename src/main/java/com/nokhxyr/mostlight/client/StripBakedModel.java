package com.nokhxyr.mostlight.client;

import com.nokhxyr.mostlight.block.LampBlock;
import com.nokhxyr.mostlight.block.LightStripBlock;
import com.nokhxyr.mostlight.block.entity.LampBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

/**
 * Rendu des bandes LED : assemble, pour chaque face portant une bande, le modèle précalculé correspondant à sa
 * position, son sens et son état (allumé / éteint). Les quads gardent leurs indices de teinte et leur lumière émise.
 */
public final class StripBakedModel implements IDynamicBakedModel {
    /** parts[face][position][sens][allumé] */
    private final BakedModel[][][][] parts;
    private final BakedModel fallback;

    public StripBakedModel(BakedModel[][][][] parts) {
        this.parts = parts;
        this.fallback = parts[Direction.DOWN.ordinal()][1][0][1];
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data,
            @Nullable RenderType renderType) {
        if (state == null || !(state.getBlock() instanceof LightStripBlock)) {
            return fallback.getQuads(state, side, rand, data, renderType);
        }
        Integer stored = data.get(LampBlockEntity.STRIP_LAYOUT);
        int layout = stored == null ? LampBlockEntity.DEFAULT_STRIP_LAYOUT : stored;
        int lit = state.getValue(LampBlock.LIT) ? 1 : 0;
        List<BakedQuad> quads = new ArrayList<>();
        for (Direction face : Direction.values()) {
            if (LightStripBlock.has(state, face)) {
                BakedModel part = parts[face.ordinal()][LampBlockEntity.slot(layout, face)][LampBlockEntity.rotated(layout, face) ? 1 : 0][lit];
                quads.addAll(part.getQuads(state, side, rand, data, renderType));
            }
        }
        return quads;
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        return fallback.getRenderTypes(state, rand, data);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return fallback.useAmbientOcclusion();
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
