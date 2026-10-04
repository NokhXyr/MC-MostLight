package com.nokhxyr.mostlight.client;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.nokhxyr.mostlight.MostLight;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;

/**
 * Modèles dynamiques des lampes, déclarés dans les blockstates par un « loader » :
 * <ul>
 *   <li>{@code mostlight:strip} : bandes LED et guirlandes, assemblées à partir des morceaux de ligne précalculés ;</li>
 *   <li>{@code mostlight:animated} : lampes animées, dont le modèle du chunk perd les pièces que GeckoLib anime.</li>
 * </ul>
 * Passer par des loaders (plutôt que remplacer des modèles déjà cuits) laisse le jeu cuire ces modèles à la demande :
 * ça marche aussi avec ModernFix et ses ressources dynamiques, qui ignorent les remplacements après coup.
 */
@EventBusSubscriber(modid = MostLight.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class LampModelLoaders {
    private static final int[] NODES = {3, 4, 5};
    private static final int[] ENDS = {0, 1, 2, 6, 7, 8};

    private LampModelLoaders() {}

    @SubscribeEvent
    static void register(ModelEvent.RegisterGeometryLoaders event) {
        event.register(ResourceLocation.fromNamespaceAndPath(MostLight.MOD_ID, "strip"), StripGeometry.LOADER);
        event.register(ResourceLocation.fromNamespaceAndPath(MostLight.MOD_ID, "animated"), AnimatedGeometry.LOADER);
    }

    private static ResourceLocation block(String path) {
        return ResourceLocation.fromNamespaceAndPath(MostLight.MOD_ID, "block/" + path);
    }

    /** Une bande : les morceaux {@code <lamp>_seg/<position>_<bout>_<bout><variante>[_on]} et le modèle d'objet. */
    record StripGeometry(String lamp, List<String> variants) implements IUnbakedGeometry<StripGeometry> {
        static final IGeometryLoader<StripGeometry> LOADER = (JsonObject json, JsonDeserializationContext context) -> {
            List<String> variants = new ArrayList<>();
            if (json.has("variants")) {
                json.getAsJsonArray("variants").forEach(v -> variants.add(v.getAsString()));
            } else {
                variants.add("");
            }
            return new StripGeometry(GsonHelper.getAsString(json, "lamp"), variants);
        };

        private ResourceLocation segment(int slot, int i, int j, String variant, boolean lit) {
            return block(lamp + "_seg/" + slot + "_" + i + "_" + j + variant + (lit ? "_on" : ""));
        }

        private interface PairVisitor {
            void visit(int i, int j);
        }

        private static void pairs(PairVisitor visitor) {
            for (int node : NODES) {
                for (int end : ENDS) {
                    if (end < node) {
                        visitor.visit(end, node);
                    } else {
                        visitor.visit(node, end);
                    }
                }
            }
        }

        @Override
        public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter, IGeometryBakingContext context) {
            modelGetter.apply(block(lamp + "_on")).resolveParents(modelGetter);
            for (String variant : variants) {
                for (int slot = 0; slot < 3; slot++) {
                    for (boolean lit : new boolean[] {false, true}) {
                        int s = slot;
                        pairs((i, j) -> modelGetter.apply(segment(s, i, j, variant, lit)).resolveParents(modelGetter));
                    }
                }
            }
        }

        @Override
        public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> sprites,
                ModelState state, ItemOverrides overrides) {
            BakedModel[][][][][] templates = new BakedModel[2][2][3][9][9];
            for (int v = 0; v < variants.size(); v++) {
                for (int l = 0; l < 2; l++) {
                    for (int slot = 0; slot < 3; slot++) {
                        int vv = v, ll = l, s = slot;
                        pairs((i, j) -> templates[vv][ll][s][i][j] =
                                baker.bake(segment(s, i, j, variants.get(vv), ll == 1), BlockModelRotation.X0_Y0, sprites));
                    }
                }
            }
            if (variants.size() == 1) {
                templates[1] = templates[0];
            }
            BakedModel fallback = baker.bake(block(lamp + "_on"), BlockModelRotation.X0_Y0, sprites);
            return new StripBakedModel(templates, variants.size() > 1, fallback);
        }
    }

    /** Une lampe animée : le modèle {@code model}, tourné comme le demande le blockstate, sans ses pièces animées de près. */
    record AnimatedGeometry(ResourceLocation model) implements IUnbakedGeometry<AnimatedGeometry> {
        static final IGeometryLoader<AnimatedGeometry> LOADER = (JsonObject json, JsonDeserializationContext context) ->
                new AnimatedGeometry(ResourceLocation.parse(GsonHelper.getAsString(json, "model")));

        @Override
        public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter, IGeometryBakingContext context) {
            modelGetter.apply(model).resolveParents(modelGetter);
        }

        @Override
        public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> sprites,
                ModelState state, ItemOverrides overrides) {
            return new AnimatedLampClient.ChunkModel(baker.bake(model, state, sprites));
        }
    }
}
