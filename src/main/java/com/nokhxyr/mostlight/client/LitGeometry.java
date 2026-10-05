package com.nokhxyr.mostlight.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.neoforged.neoforge.client.model.CompositeModel;
import net.neoforged.neoforge.client.model.ExtraFaceData;
import net.neoforged.neoforge.client.model.IModelBuilder;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import net.neoforged.neoforge.client.model.geometry.UnbakedGeometryHelper;
import org.apache.commons.lang3.mutable.MutableObject;

/**
 * Lit version of a lamp model ({@code mostlight:lit}), built from the unlit one instead of being stored twice:
 * the same pieces, the {@code _off} textures replaced by their {@code _on} versions, the pieces listed in {@code glow}
 * at full brightness without ambient occlusion, and those in {@code flat} without shading. These are the rules the
 * generator used to write the lit models; it lists the pieces for each part (opaque, translucent).
 */
public record LitGeometry(List<Part> parts) implements IUnbakedGeometry<LitGeometry> {
    /** One part of the unlit model (a whole model, or one child of a composite model) and its render type. */
    public record Part(ResourceLocation model, ResourceLocation renderType, Set<Integer> glow, Set<Integer> flat) {}

    private static final ExtraFaceData GLOW = new ExtraFaceData(0xFFFFFFFF, 15, 15, false);

    public static final IGeometryLoader<LitGeometry> LOADER = (JsonObject json, JsonDeserializationContext context) -> {
        List<Part> parts = new ArrayList<>();
        for (JsonElement element : GsonHelper.getAsJsonArray(json, "parts")) {
            JsonObject part = element.getAsJsonObject();
            parts.add(new Part(ResourceLocation.parse(GsonHelper.getAsString(part, "model")),
                    ResourceLocation.parse(GsonHelper.getAsString(part, "render_type")),
                    indices(part, "glow"), indices(part, "flat")));
        }
        return new LitGeometry(parts);
    };

    private static Set<Integer> indices(JsonObject json, String key) {
        Set<Integer> out = new HashSet<>();
        if (json.has(key)) {
            JsonArray array = json.getAsJsonArray(key);
            array.forEach(e -> out.add(e.getAsInt()));
        }
        return out;
    }

    /** {@code mostlight:block/light_off} becomes {@code mostlight:block/light_on}; other textures stay. */
    static Material lit(Material material) {
        String path = material.texture().getPath();
        if (!path.endsWith("_off")) {
            return material;
        }
        return new Material(material.atlasLocation(),
                ResourceLocation.fromNamespaceAndPath(material.texture().getNamespace(), path.substring(0, path.length() - 4) + "_on"));
    }

    @Override
    public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter, IGeometryBakingContext context) {
        for (Part part : parts) {
            modelGetter.apply(part.model()).resolveParents(modelGetter);
        }
    }

    @Override
    public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> sprites,
            ModelState state, ItemOverrides overrides) {
        TextureAtlasSprite particle = sprites.apply(lit(context.getMaterial("particle")));
        CompositeModel.Baked.Builder builder = CompositeModel.Baked.builder(context, particle, overrides, context.getTransforms());
        for (Part part : parts) {
            if (!(baker.getModel(part.model()) instanceof BlockModel model)) {
                continue;
            }
            IModelBuilder<?> layer = IModelBuilder.of(context.useAmbientOcclusion(), context.useBlockLight(), context.isGui3d(),
                    context.getTransforms(), overrides, particle, context.getRenderType(part.renderType()));
            List<BlockElement> elements = model.getElements();
            for (int i = 0; i < elements.size(); i++) {
                BlockElement element = litElement(elements.get(i), part.glow().contains(i), part.flat().contains(i));
                for (Map.Entry<Direction, BlockElementFace> entry : element.faces.entrySet()) {
                    BlockElementFace face = entry.getValue();
                    TextureAtlasSprite sprite = sprites.apply(lit(model.getMaterial(face.texture())));
                    var quad = UnbakedGeometryHelper.bakeElementFace(element, face, sprite, entry.getKey(), state);
                    if (face.cullForDirection() == null) {
                        layer.addUnculledFace(quad);
                    } else {
                        layer.addCulledFace(Direction.rotate(state.getRotation().getMatrix(), face.cullForDirection()), quad);
                    }
                }
            }
            builder.addLayer(layer.build());
        }
        return builder.build();
    }

    /**
     * A copy of the piece with its own copies of the faces: a face points to its piece for its light data, and the
     * faces of the unlit model must keep pointing to the unlit piece.
     */
    private static BlockElement litElement(BlockElement element, boolean glow, boolean flat) {
        Map<Direction, BlockElementFace> faces = new EnumMap<>(Direction.class);
        element.faces.forEach((direction, face) -> faces.put(direction, new BlockElementFace(face.cullForDirection(), face.tintIndex(),
                face.texture(), face.uv(), null, new MutableObject<>())));
        return new BlockElement(element.from, element.to, faces, element.rotation, element.shade && !flat,
                glow ? GLOW : element.getFaceData());
    }
}
