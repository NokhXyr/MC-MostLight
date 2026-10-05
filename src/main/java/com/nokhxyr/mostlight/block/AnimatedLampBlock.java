package com.nokhxyr.mostlight.block;

import com.nokhxyr.mostlight.block.entity.AnimatedLampBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Lampe à pièces animées (engrenages, lave, ventilateur) : corps précuit, pièces animées par GeckoLib de près. */
public class AnimatedLampBlock extends HorizontalLampBlock {
    public AnimatedLampBlock(LampType type, Properties properties) {
        super(type, properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AnimatedLampBlockEntity(pos, state);
    }
}
