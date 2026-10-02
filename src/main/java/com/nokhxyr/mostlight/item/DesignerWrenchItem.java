package com.nokhxyr.mostlight.item;

import com.nokhxyr.mostlight.MostLight;
import com.nokhxyr.mostlight.block.LampBlock;
import com.nokhxyr.mostlight.block.LampFinish;
import com.nokhxyr.mostlight.block.LightTone;
import com.nokhxyr.mostlight.block.entity.LampBlockEntity;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Clic droit : finition suivante ; accroupi : teinte de lumière suivante. */
public class DesignerWrenchItem extends Item {
    public DesignerWrenchItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof LampBlock lamp) || !(level.getBlockEntity(pos) instanceof LampBlockEntity entity)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            Player player = context.getPlayer();
            boolean toneMode = player != null && player.isSecondaryUseActive();
            LampFinish finish = toneMode ? entity.finish() : entity.finish().next();
            LightTone tone = toneMode ? entity.tone().next() : entity.tone();
            lamp.setLook(level, pos, state, finish, tone);
            level.playSound(null, pos, SoundEvents.SPYGLASS_USE, SoundSource.BLOCKS, 0.8F, toneMode ? 1.4F : 1.0F);
            if (player != null) {
                Component value = toneMode
                        ? Component.translatable("tone." + MostLight.MOD_ID + "." + tone.getSerializedName())
                        : Component.translatable("finish." + MostLight.MOD_ID + "." + finish.getSerializedName());
                player.displayClientMessage(Component.translatable("message." + MostLight.MOD_ID + (toneMode ? ".tone" : ".finish"), value), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip." + MostLight.MOD_ID + ".wrench").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip." + MostLight.MOD_ID + ".wrench_sneak").withStyle(ChatFormatting.GRAY));
    }
}
