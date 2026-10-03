package com.nokhxyr.mostlight.item;

import com.nokhxyr.mostlight.MostLight;
import com.nokhxyr.mostlight.block.LightStripBlock;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Bande LED : après la pose, règle la position et le sens de la bande ajoutée (plusieurs par bloc possibles). */
public class LightStripItem extends LampItem {
    public LightStripItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        BlockState before = context.getLevel().getBlockState(pos);
        InteractionResult result = super.place(context);
        if (result.consumesAction()) {
            LightStripBlock.configurePlaced(context.getLevel(), pos, before, context.getLevel().getBlockState(pos), context);
        }
        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip." + MostLight.MOD_ID + ".strip").withStyle(ChatFormatting.GRAY));
    }
}
