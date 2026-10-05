package com.nokhxyr.mostlight.component;

import com.mojang.serialization.Codec;
import com.nokhxyr.mostlight.MostLight;
import com.nokhxyr.mostlight.block.LampFinish;
import com.nokhxyr.mostlight.block.LightTone;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.registries.DeferredRegister;
import io.netty.buffer.ByteBuf;

/** Finition et teinte de lumière portées par l'objet (copiées depuis/vers le bloc). */
public final class ModComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MostLight.MOD_ID);

    public static final Supplier<DataComponentType<LampFinish>> FINISH = COMPONENTS.register("finish",
            () -> enumComponent(StringRepresentable.fromEnum(LampFinish::values), LampFinish.values()));
    public static final Supplier<DataComponentType<LightTone>> LIGHT_TONE = COMPONENTS.register("light_tone",
            () -> enumComponent(StringRepresentable.fromEnum(LightTone::values), LightTone.values()));

    /**
     * Most links an item or a switch can hold: the highest maxLinks the config allows. Also the size limit when reading
     * one from a save or a packet, so a forged item (creative inventory, modified client) cannot carry millions.
     */
    public static final int MAX_LINKS = 1024;

    /** Lampes liées à un interrupteur ou une télécommande (positions absolues). */
    public static final Supplier<DataComponentType<List<BlockPos>>> LINKS = COMPONENTS.register("links",
            () -> DataComponentType.<List<BlockPos>>builder()
                    .persistent(BlockPos.CODEC.listOf(0, MAX_LINKS))
                    .networkSynchronized(BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_LINKS)))
                    .build());

    private ModComponents() {}

    private static <E extends Enum<E>> DataComponentType<E> enumComponent(Codec<E> codec, E[] values) {
        StreamCodec<ByteBuf, E> stream = ByteBufCodecs.VAR_INT.map(i -> values[i], Enum::ordinal);
        return DataComponentType.<E>builder().persistent(codec).networkSynchronized(stream).build();
    }
}
