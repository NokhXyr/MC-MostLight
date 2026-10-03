package com.nokhxyr.mostlight.registry;

import com.nokhxyr.mostlight.MostLight;
import com.nokhxyr.mostlight.block.AnimatedLampBlock;
import com.nokhxyr.mostlight.block.entity.AnimatedLampBlockEntity;
import com.nokhxyr.mostlight.block.entity.LampBlockEntity;
import com.nokhxyr.mostlight.link.SwitchBlockEntity;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MostLight.MOD_ID);

    public static final Supplier<BlockEntityType<LampBlockEntity>> LAMP = BLOCK_ENTITIES.register("lamp",
            () -> BlockEntityType.Builder.of(LampBlockEntity::new, ModBlocks.all().stream().map(holder -> (Block) holder.get())
                    .filter(block -> !(block instanceof AnimatedLampBlock)).toArray(Block[]::new)).build(null));

    /** Lampes à engrenages : block entity à part, la seule avec un rendu animé (les autres restent statiques). */
    public static final Supplier<BlockEntityType<AnimatedLampBlockEntity>> ANIMATED_LAMP = BLOCK_ENTITIES.register("animated_lamp",
            () -> BlockEntityType.Builder.of(AnimatedLampBlockEntity::new, ModBlocks.all().stream().map(holder -> (Block) holder.get())
                    .filter(block -> block instanceof AnimatedLampBlock).toArray(Block[]::new)).build(null));

    public static final Supplier<BlockEntityType<SwitchBlockEntity>> SWITCH = BLOCK_ENTITIES.register("switch",
            () -> BlockEntityType.Builder.of(SwitchBlockEntity::new, ModBlocks.allSwitches().toArray(Block[]::new)).build(null));

    private ModBlockEntities() {}
}
