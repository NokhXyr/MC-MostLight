package com.nokhxyr.mostlight.link;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Variateur : clic = luminosité suivante pour tout le groupe, accroupi + clic = allumer/éteindre. */
public class DimmerSwitchBlock extends LightSwitchBlock {
    public static final MapCodec<DimmerSwitchBlock> CODEC = simpleCodec(DimmerSwitchBlock::new);

    public DimmerSwitchBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void press(BlockState state, Level level, BlockPos pos, Player player, SwitchBlockEntity entity) {
        if (player.isShiftKeyDown()) {
            super.press(state, level, pos, player, entity);
            return;
        }
        int light = LampLinks.cycleBrightness(level, pos, entity.links());
        level.setBlock(pos, state.setValue(ON, true), Block.UPDATE_ALL);
        LampLinks.click(level, pos, true);
        player.displayClientMessage(message("brightness", light), true);
    }
}
