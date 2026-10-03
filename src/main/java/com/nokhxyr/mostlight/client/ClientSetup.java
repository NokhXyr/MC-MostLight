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
 * Teinte des modèles : 0 = couleur de la lampe, 1 = diffuseurs, 2 = ampoules, 3 = finition du cadre,
 * 4 / 5 = pièces animées (finition / sans teinte).
 * Finition et teinte de lumière viennent du block entity (bloc posé) ou des composants (objet).
 */
@EventBusSubscriber(modid = MostLight.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    public static int tint(DyeColor color, LampFinish finish, LightTone tone, int index) {
        int rgb = switch (index) {
            case 0 -> color.getTextureDiffuseColor() & 0xFFFFFF;
            case 1, 2 -> tone.tint(color, index);
            // 4 : pièces animées teintées comme la finition, 5 : pièces animées sans teinte (repérables dans les quads)
            case 3, 4 -> finish.color();
            default -> 0xFFFFFF;
        };
        return 0xFF000000 | rgb;
    }

    /** Dernier bloc teinté par ce fil (reconstruction des chunks sur plusieurs fils). */
    private static final class LookCache {
        Object level;
        long pos;
        Object block;
        LampFinish finish;
        LightTone tone;
        int version;
    }

    private static final ThreadLocal<LookCache> LOOK = ThreadLocal.withInitial(LookCache::new);

    @SubscribeEvent
    static void onBlockColors(RegisterColorHandlersEvent.Block event) {
        for (DeferredBlock<LampBlock> holder : ModBlocks.all()) {
            LampBlock block = holder.get();
            event.register((state, level, pos, index) -> {
                if (index < 0) {
                    return -1;
                }
                if (level == null || pos == null) {
                    return tint(block.color(), block.type().defaultFinish(), LightTone.AUTO, index);
                }
                // une lampe a jusqu'à des centaines de faces teintées : la block entity n'est lue qu'une fois par bloc
                LookCache cache = LOOK.get();
                long key = pos.asLong();
                int version = LampBlockEntity.lookVersion;
                if (cache.level != level || cache.pos != key || cache.block != block || cache.version != version) {
                    cache.version = version;
                    cache.level = level;
                    cache.pos = key;
                    cache.block = block;
                    if (level.getBlockEntity(pos) instanceof LampBlockEntity lamp) {
                        cache.finish = lamp.finish();
                        cache.tone = lamp.tone();
                    } else {
                        cache.finish = block.type().defaultFinish();
                        cache.tone = LightTone.AUTO;
                    }
                }
                return tint(block.color(), cache.finish, cache.tone, index);
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
