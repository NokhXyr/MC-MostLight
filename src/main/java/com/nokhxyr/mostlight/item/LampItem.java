package com.nokhxyr.mostlight.item;

import com.nokhxyr.mostlight.MostLight;
import com.nokhxyr.mostlight.block.LampBlock;
import com.nokhxyr.mostlight.block.LampFinish;
import com.nokhxyr.mostlight.block.LightTone;
import com.nokhxyr.mostlight.component.ModComponents;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

public class LampItem extends BlockItem {
    public LampItem(Block block, Properties properties) {
        super(block, properties);
    }

    /** One item per model: the name follows the colour (block.mostlight.table_lamp.red). */
    @Override
    public net.minecraft.network.chat.Component getName(ItemStack stack) {
        return net.minecraft.network.chat.Component.translatable(com.nokhxyr.mostlight.item.ItemColor.nameKey(getDescriptionId(), stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        LampFinish finish = stack.getOrDefault(ModComponents.FINISH.get(), ((LampBlock) getBlock()).type().defaultFinish());
        LightTone tone = stack.getOrDefault(ModComponents.LIGHT_TONE.get(), LightTone.AUTO);
        String ns = MostLight.MOD_ID;
        tooltip.add(Component.translatable("tooltip." + ns + ".finish",
                Component.translatable("finish." + ns + "." + finish.getSerializedName())).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip." + ns + ".tone",
                Component.translatable("tone." + ns + "." + tone.getSerializedName())).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip." + ns + ".toggle").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip." + ns + ".dye").withStyle(ChatFormatting.GRAY));
    }
}
