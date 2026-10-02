package com.nokhxyr.mostlight.client;

import com.nokhxyr.mostlight.MostLight;
import com.nokhxyr.mostlight.block.LampBlock;
import com.nokhxyr.mostlight.block.LampFinish;
import com.nokhxyr.mostlight.block.LightTone;
import com.nokhxyr.mostlight.block.entity.LampBlockEntity;
import com.nokhxyr.mostlight.component.ModComponents;
import com.nokhxyr.mostlight.registry.ModBlocks;
import net.minecraft.world.item.DyeColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * Teinte des modèles : 0 = couleur de la lampe, 1 = diffuseurs, 2 = ampoules, 3 = finition du cadre.
 * Finition et teinte de lumière viennent du block entity (bloc posé) ou des composants (objet).
 */
@EventBusSubscriber(modid = MostLight.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    static int tint(DyeColor color, LampFinish finish, LightTone tone, int index) {
        int rgb = switch (index) {
            case 0 -> color.getTextureDiffuseColor() & 0xFFFFFF;
            case 1, 2 -> tone.tint(color, index);
            case 3 -> finish.color();
            default -> 0xFFFFFF;
        };
        return 0xFF000000 | rgb;
    }

    @SubscribeEvent
    static void onBlockColors(RegisterColorHandlersEvent.Block event) {
        for (DeferredBlock<LampBlock> holder : ModBlocks.all()) {
            LampBlock block = holder.get();
            event.register((state, level, pos, index) -> {
                if (index < 0) {
                    return -1;
                }
                LampFinish finish = block.type().defaultFinish();
                LightTone tone = LightTone.AUTO;
                if (level != null && pos != null && level.getBlockEntity(pos) instanceof LampBlockEntity lamp) {
                    finish = lamp.finish();
                    tone = lamp.tone();
                }
                return tint(block.color(), finish, tone, index);
            }, block);
        }
    }

    @SubscribeEvent
    static void onItemColors(RegisterColorHandlersEvent.Item event) {
        for (DeferredBlock<LampBlock> holder : ModBlocks.all()) {
            LampBlock block = holder.get();
            event.register((stack, index) -> index < 0 ? -1 : tint(block.color(),
                    stack.getOrDefault(ModComponents.FINISH.get(), block.type().defaultFinish()),
                    stack.getOrDefault(ModComponents.LIGHT_TONE.get(), LightTone.AUTO), index), block);
        }
    }
}
