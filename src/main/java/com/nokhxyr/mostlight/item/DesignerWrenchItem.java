package com.nokhxyr.mostlight.item;

import com.nokhxyr.mostlight.MostLight;
import com.nokhxyr.mostlight.component.ModComponents;
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

/**
 * Clé de décorateur. Maj + molette : choisir la finition (gardée dans la clé). Clic droit sur une lampe : appliquer la
 * finition choisie (sans choix : finition suivante). Accroupi + clic droit : teinte de lumière suivante.
 */
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
            LampFinish selected = context.getItemInHand().get(ModComponents.FINISH.get());
            LampFinish finish = toneMode ? entity.finish() : selected != null ? selected : entity.finish().next();
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

    /** Finition sélectionnée suivante (delta 1) ou précédente (-1), annoncée au joueur. */
    public static void scroll(Player player, ItemStack stack, int delta) {
        LampFinish[] all = LampFinish.values();
        LampFinish current = stack.get(ModComponents.FINISH.get());
        int index = current == null ? (delta > 0 ? 0 : all.length - 1) : Math.floorMod(current.ordinal() + delta, all.length);
        stack.set(ModComponents.FINISH.get(), all[index]);
        player.displayClientMessage(Component.translatable("message." + MostLight.MOD_ID + ".wrench_selected",
                Component.translatable("finish." + MostLight.MOD_ID + "." + all[index].getSerializedName())).withStyle(ChatFormatting.GOLD), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.25F, 1.6F);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        LampFinish selected = stack.get(ModComponents.FINISH.get());
        tooltip.add(Component.translatable("tooltip." + MostLight.MOD_ID + ".wrench_selected", selected == null
                ? Component.translatable("tooltip." + MostLight.MOD_ID + ".wrench_next")
                : Component.translatable("finish." + MostLight.MOD_ID + "." + selected.getSerializedName())).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip." + MostLight.MOD_ID + ".wrench_scroll").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip." + MostLight.MOD_ID + ".wrench").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip." + MostLight.MOD_ID + ".wrench_sneak").withStyle(ChatFormatting.GRAY));
    }
}
