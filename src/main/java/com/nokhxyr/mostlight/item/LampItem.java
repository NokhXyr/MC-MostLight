package com.nokhxyr.mostlight.item;

import com.nokhxyr.mostlight.MostLight;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

public class LampItem extends BlockItem {
    private static final String[] HINTS = {"toggle", "brightness", "dye"};

    public LampItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        for (String key : HINTS) {
            tooltip.add(Component.translatable("tooltip." + MostLight.MOD_ID + "." + key).withStyle(ChatFormatting.GRAY));
        }
    }
}
