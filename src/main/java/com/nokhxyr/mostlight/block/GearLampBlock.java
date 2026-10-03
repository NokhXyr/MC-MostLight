package com.nokhxyr.mostlight.block;

import com.nokhxyr.mostlight.block.entity.GearLampBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Lampe à engrenages : le corps est un modèle statique, les engrenages sont animés par GearLampRenderer. */
public class GearLampBlock extends HorizontalLampBlock {
    public GearLampBlock(LampType type, DyeColor color, Properties properties) {
        super(type, color, properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GearLampBlockEntity(pos, state);
    }
}
