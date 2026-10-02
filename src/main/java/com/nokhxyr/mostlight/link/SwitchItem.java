package com.nokhxyr.mostlight.link;

import com.nokhxyr.mostlight.block.LampBlock;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

/** Interrupteur en main : accroupi + clic droit sur une lampe pour la lier, clic normal pour poser. */
public class SwitchItem extends BlockItem {
    private final String usageKey;

    public SwitchItem(Block block, Properties properties, String usageKey) {
        super(block, properties);
        this.usageKey = usageKey;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && player.isSecondaryUseActive()
                && context.getLevel().getBlockState(context.getClickedPos()).getBlock() instanceof LampBlock) {
            if (!context.getLevel().isClientSide) {
                player.displayClientMessage(LampLinks.toggleLink(context.getItemInHand(), context.getClickedPos()), true);
            }
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
        return super.useOn(context);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return !LampLinks.get(stack).isEmpty();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        LampLinks.appendTooltip(stack, tooltip, usageKey);
    }
}
