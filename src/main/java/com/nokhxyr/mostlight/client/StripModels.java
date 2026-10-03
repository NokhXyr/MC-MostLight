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
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * Charge les morceaux de bande précalculés de chaque type de bande (face x 3 positions x 2 sens x allumé/éteint x
 * 4 combinaisons de bouts raccourcis, générés par tools/generate.mjs) et remplace le modèle de chaque état de
 * bande par un {@link StripBakedModel}.
 */
@EventBusSubscriber(modid = MostLight.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class StripModels {
    private static final String[] SLOTS = {"low", "middle", "high"};

    private StripModels() {}

    private static ModelResourceLocation part(LampType type, Direction face, int slot, boolean rotated, boolean lit, int trim) {
        String name = "block/" + type.id() + "_part/" + face.getName() + "_" + SLOTS[slot] + (rotated ? "_r" : "") + "_t" + trim + (lit ? "_on" : "");
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(MostLight.MOD_ID, name));
    }

    @SubscribeEvent
    static void register(ModelEvent.RegisterAdditional event) {
        for (LampType type : LampType.values()) {
            if (!type.isStrip()) {
                continue;
            }
            for (Direction face : Direction.values()) {
                for (int slot = 0; slot < 3; slot++) {
                    for (int r = 0; r < 2; r++) {
                        for (int l = 0; l < 2; l++) {
                            for (int trim = 0; trim < 4; trim++) {
                                event.register(part(type, face, slot, r == 1, l == 1, trim));
                            }
                        }
                    }
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
            BakedModel[][][][][] parts = new BakedModel[6][3][2][2][4];
            boolean complete = true;
            for (Direction face : Direction.values()) {
                for (int slot = 0; slot < 3; slot++) {
                    for (int r = 0; r < 2; r++) {
                        for (int l = 0; l < 2; l++) {
                            for (int trim = 0; trim < 4; trim++) {
                                BakedModel model = models.get(part(type, face, slot, r == 1, l == 1, trim));
                                if (model == null) {
                                    complete = false;
                                } else {
                                    parts[face.ordinal()][slot][r][l][trim] = model;
                                }
                            }
                        }
                    }
                }
            }
            if (!complete) {
                continue; // ressources incomplètes : rendu par défaut
            }
            StripBakedModel strip = new StripBakedModel(parts);
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
