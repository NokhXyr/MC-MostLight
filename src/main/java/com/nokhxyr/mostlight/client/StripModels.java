package com.nokhxyr.mostlight.client;

import com.nokhxyr.mostlight.MostLight;
import com.nokhxyr.mostlight.block.LampBlock;
import com.nokhxyr.mostlight.block.LampType;
import com.nokhxyr.mostlight.block.LightStripBlock;
import com.nokhxyr.mostlight.registry.ModBlocks;
import java.util.Map;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * Charge les morceaux de ligne précalculés de chaque type de bande (tools/generate.mjs : 3 positions x 18 paires de
 * bouts x allumé/éteint, et pour les guirlandes deux variantes : pendante vers le bas au mur « _w », ailleurs « _y »)
 * et remplace le modèle de chaque état de bande par un {@link StripBakedModel}.
 */
@EventBusSubscriber(modid = MostLight.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class StripModels {
    private static final int[] NODES = {3, 4, 5};
    private static final int[] ENDS = {0, 1, 2, 6, 7, 8};

    private StripModels() {}

    /** Les deux variantes de pente (guirlandes) ou une seule (bandes LED). */
    private static String[] variants(LampType type) {
        return type == LampType.LIGHT_STRIP ? new String[] {""} : new String[] {"_y", "_w"};
    }

    private static ModelResourceLocation segment(LampType type, int slot, int i, int j, String variant, boolean lit) {
        String name = "block/" + type.id() + "_seg/" + slot + "_" + i + "_" + j + variant + (lit ? "_on" : "");
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(MostLight.MOD_ID, name));
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

    @SubscribeEvent
    static void register(ModelEvent.RegisterAdditional event) {
        for (LampType type : LampType.values()) {
            if (!type.isStrip()) {
                continue;
            }
            for (String variant : variants(type)) {
                for (int slot = 0; slot < 3; slot++) {
                    int s = slot;
                    pairs((i, j) -> {
                        event.register(segment(type, s, i, j, variant, false));
                        event.register(segment(type, s, i, j, variant, true));
                    });
                }
            }
        }
    }

    @SubscribeEvent
    static void bake(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        for (LampType type : LampType.values()) {
            if (!type.isStrip()) {
                continue;
            }
            String[] variants = variants(type);
            BakedModel[][][][][] templates = new BakedModel[2][2][3][9][9];
            boolean[] complete = {true};
            for (int v = 0; v < variants.length; v++) {
                for (int l = 0; l < 2; l++) {
                    for (int slot = 0; slot < 3; slot++) {
                        int vv = v, ll = l, s = slot;
                        pairs((i, j) -> {
                            BakedModel model = models.get(segment(type, s, i, j, variants[vv], ll == 1));
                            if (model == null) {
                                complete[0] = false;
                            } else {
                                templates[vv][ll][s][i][j] = model;
                            }
                        });
                    }
                }
            }
            if (!complete[0]) {
                continue; // ressources incomplètes : rendu par défaut
            }
            if (variants.length == 1) {
                templates[1] = templates[0];
            }
            BakedModel fallback = null;
            for (DyeColor color : DyeColor.values()) {
                LampBlock block = ModBlocks.lamp(type, color);
                if (!(block instanceof LightStripBlock)) {
                    continue;
                }
                for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                    ModelResourceLocation location = BlockModelShaper.stateToModelLocation(state);
                    if (fallback == null) {
                        fallback = models.get(location);
                    }
                }
            }
            if (fallback == null) {
                continue;
            }
            StripBakedModel strip = new StripBakedModel(templates, variants.length > 1, fallback);
            for (DyeColor color : DyeColor.values()) {
                LampBlock block = ModBlocks.lamp(type, color);
                if (block instanceof LightStripBlock) {
                    for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                        models.put(BlockModelShaper.stateToModelLocation(state), strip);
                    }
                }
            }
        }
    }
}
