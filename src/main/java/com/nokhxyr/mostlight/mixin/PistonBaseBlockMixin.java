package com.nokhxyr.mostlight.mixin;

import com.nokhxyr.mostlight.block.LampBlock;
import com.nokhxyr.mostlight.block.PistonCarry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Les pistons poussent et tirent les lampes, en gardant leurs données (voir PistonCarry). */
@Mixin(PistonBaseBlock.class)
public abstract class PistonBaseBlockMixin {
    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true, require = 1)
    private static void mostlight$pushLamps(BlockState state, Level level, BlockPos pos, Direction movement, boolean allowDestroy,
            Direction pistonFacing, CallbackInfoReturnable<Boolean> cir) {
        if (state.getBlock() instanceof LampBlock && state.getPistonPushReaction() == PushReaction.NORMAL) {
            cir.setReturnValue(PistonCarry.canPush(level, pos, movement));
        }
    }

    // where vanilla builds its own resolver: when pulling, the piston head is already gone and no longer blocks the way
    @Inject(method = "moveBlocks", at = @At(value = "NEW", target = "net/minecraft/world/level/block/piston/PistonStructureResolver"), require = 1)
    private void mostlight$carryLamps(Level level, BlockPos pos, Direction facing, boolean extending, CallbackInfoReturnable<Boolean> cir) {
        PistonCarry.capture(level, pos, facing, extending);
    }
}
