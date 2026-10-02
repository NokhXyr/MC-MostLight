package com.nokhxyr.mostlight.client;

import com.nokhxyr.mostlight.MostLight;
import com.nokhxyr.mostlight.block.LampBlock;
import com.nokhxyr.mostlight.registry.ModBlocks;
import net.minecraft.world.item.DyeColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.registries.DeferredBlock;

/** Teinte les modèles : index 0 = couleur du corps, index 1 = lumière (couleur éclaircie). */
@EventBusSubscriber(modid = MostLight.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    static int tint(DyeColor color, int index) {
        int rgb = color.getTextureDiffuseColor() & 0xFFFFFF;
        if (index == 1) {
            rgb = (lighten(rgb >> 16 & 0xFF) << 16) | (lighten(rgb >> 8 & 0xFF) << 8) | lighten(rgb & 0xFF);
        }
        return 0xFF000000 | rgb;
    }

    private static int lighten(int channel) {
        return channel + (255 - channel) * 35 / 100;
    }

    @SubscribeEvent
    static void onBlockColors(RegisterColorHandlersEvent.Block event) {
        for (DeferredBlock<LampBlock> holder : ModBlocks.all()) {
            DyeColor color = holder.get().color();
            event.register((state, level, pos, index) -> index >= 0 ? tint(color, index) : -1, holder.get());
        }
    }

    @SubscribeEvent
    static void onItemColors(RegisterColorHandlersEvent.Item event) {
        for (DeferredBlock<LampBlock> holder : ModBlocks.all()) {
            DyeColor color = holder.get().color();
            event.register((stack, index) -> index >= 0 ? tint(color, index) : -1, holder.get());
        }
    }
}
