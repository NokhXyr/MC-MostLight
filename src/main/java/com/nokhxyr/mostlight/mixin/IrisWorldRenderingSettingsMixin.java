package com.nokhxyr.mostlight.mixin;

import com.nokhxyr.mostlight.compat.IrisLightColors;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Compatibilité Iris optionnelle : ignorée si Iris n'est pas installé (@Pseudo). */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings", remap = false)
public class IrisWorldRenderingSettingsMixin {
    @ModifyVariable(method = "setBlockStateIds", at = @At("HEAD"), argsOnly = true, remap = false)
    private Object2IntMap<BlockState> mostlight$addLampLightColors(Object2IntMap<BlockState> ids) {
        return IrisLightColors.withLamps(ids);
    }
}
