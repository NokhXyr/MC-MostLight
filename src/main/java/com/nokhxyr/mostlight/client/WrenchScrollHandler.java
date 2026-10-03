package com.nokhxyr.mostlight.client;

import com.nokhxyr.mostlight.MostLight;
import com.nokhxyr.mostlight.item.DesignerWrenchItem;
import com.nokhxyr.mostlight.network.WrenchScrollPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Maj + molette avec la clé de décorateur : change la finition sélectionnée au lieu de changer d'emplacement. */
@EventBusSubscriber(modid = MostLight.MOD_ID, value = Dist.CLIENT)
public final class WrenchScrollHandler {
    private WrenchScrollHandler() {}

    @SubscribeEvent
    static void onScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || !mc.player.isShiftKeyDown()
                || !(mc.player.getMainHandItem().getItem() instanceof DesignerWrenchItem) || event.getScrollDeltaY() == 0) {
            return;
        }
        event.setCanceled(true);
        PacketDistributor.sendToServer(new WrenchScrollPayload(event.getScrollDeltaY() > 0 ? -1 : 1));
    }
}
