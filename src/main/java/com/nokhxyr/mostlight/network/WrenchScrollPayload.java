package com.nokhxyr.mostlight.network;

import com.nokhxyr.mostlight.MostLight;
import com.nokhxyr.mostlight.item.DesignerWrenchItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Maj + molette avec la clé de décorateur en main : finition sélectionnée suivante / précédente. */
@EventBusSubscriber(modid = MostLight.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public record WrenchScrollPayload(int delta) implements CustomPacketPayload {
    public static final Type<WrenchScrollPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MostLight.MOD_ID, "wrench_scroll"));
    public static final StreamCodec<ByteBuf, WrenchScrollPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(WrenchScrollPayload::new, WrenchScrollPayload::delta);

    @Override
    public Type<WrenchScrollPayload> type() {
        return TYPE;
    }

    @SubscribeEvent
    static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToServer(TYPE, STREAM_CODEC, WrenchScrollPayload::handle);
    }

    private static void handle(WrenchScrollPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ItemStack stack = context.player().getItemInHand(InteractionHand.MAIN_HAND);
            if (stack.getItem() instanceof DesignerWrenchItem) {
                DesignerWrenchItem.scroll(context.player(), stack, payload.delta());
            }
        });
    }
}
