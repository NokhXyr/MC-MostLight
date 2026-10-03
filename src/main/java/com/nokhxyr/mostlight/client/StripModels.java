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
 * Charge les morceaux de bande LED précalculés (une face x 3 positions x 2 sens x allumé/éteint, générés par
 * tools/generate.mjs) et remplace le modèle de chaque état de bande par un {@link StripBakedModel}.
 */
@EventBusSubscriber(modid = MostLight.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class StripModels {
    private static final String[] SLOTS = {"low", "middle", "high"};

    private StripModels() {}

    private static ModelResourceLocation part(Direction face, int slot, boolean rotated, boolean lit) {
        String name = "block/light_strip_part/" + face.getName() + "_" + SLOTS[slot] + (rotated ? "_r" : "") + (lit ? "_on" : "");
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(MostLight.MOD_ID, name));
    }

    @SubscribeEvent
    static void register(ModelEvent.RegisterAdditional event) {
        for (Direction face : Direction.values()) {
            for (int slot = 0; slot < 3; slot++) {
                for (boolean rotated : new boolean[] {false, true}) {
                    for (boolean lit : new boolean[] {false, true}) {
                        event.register(part(face, slot, rotated, lit));
                    }
                }
            }
        }
    }

    @SubscribeEvent
    static void bake(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        BakedModel[][][][] parts = new BakedModel[6][3][2][2];
        for (Direction face : Direction.values()) {
            for (int slot = 0; slot < 3; slot++) {
                for (int r = 0; r < 2; r++) {
                    for (int l = 0; l < 2; l++) {
                        BakedModel model = models.get(part(face, slot, r == 1, l == 1));
                        if (model == null) {
                            return; // ressources incomplètes : on garde le rendu par défaut
                        }
                        parts[face.ordinal()][slot][r][l] = model;
                    }
                }
            }
        }
        StripBakedModel strip = new StripBakedModel(parts);
        for (DyeColor color : DyeColor.values()) {
            LampBlock block = ModBlocks.lamp(LampType.LIGHT_STRIP, color);
            if (block instanceof LightStripBlock) {
                for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                    models.put(BlockModelShaper.stateToModelLocation(state), strip);
                }
            }
        }
    }
}
