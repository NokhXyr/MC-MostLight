package com.nokhxyr.mostlight.block;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Ventilateur lumineux : 1er clic = lumière, 2e clic = lumière + ventilateur, 3e clic = tout éteint (accroupi :
 * luminosité). L'interrupteur double commande la lumière et le ventilateur séparément.
 */
public class FanLampBlock extends AnimatedLampBlock {
    public static final BooleanProperty FAN = BooleanProperty.create("fan");

    public FanLampBlock(LampType type, DyeColor color, Properties properties) {
        super(type, color, properties);
        registerDefaultState(defaultBlockState().setValue(FAN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FAN);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            return super.useWithoutItem(state, level, pos, player, hit);
        }
        if (!level.isClientSide) {
            BlockState next;
            if (!isLit(state)) {
                next = withLit(level, pos, state, true).setValue(FAN, false);
            } else if (!state.getValue(FAN)) {
                next = state.setValue(FAN, true);
            } else {
                next = withLit(level, pos, state, false).setValue(FAN, false);
            }
            applyState(level, pos, next);
            level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.3F, next.getValue(FAN) ? 0.8F : isLit(next) ? 0.6F : 0.5F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Met le ventilateur en marche ou à l'arrêt (interrupteur double). */
    public void setFan(Level level, BlockPos pos, BlockState state, boolean on) {
        if (state.getValue(FAN) != on) {
            applyState(level, pos, state.setValue(FAN, on));
        }
    }
}
