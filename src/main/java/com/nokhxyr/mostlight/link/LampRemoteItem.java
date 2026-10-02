package com.nokhxyr.mostlight.link;

import com.nokhxyr.mostlight.MostLight;
import com.nokhxyr.mostlight.block.LampBlock;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Télécommande : accroupi + clic droit sur une lampe pour la lier,
 * clic droit dans le vide pour tout allumer/éteindre, accroupi dans le vide pour la luminosité.
 */
public class LampRemoteItem extends Item {
    public LampRemoteItem(Properties properties) {
        super(properties);
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
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        List<net.minecraft.core.BlockPos> links = LampLinks.get(stack);
        if (!level.isClientSide) {
            if (links.isEmpty()) {
                LampLinks.warnNoLinks(player);
            } else if (player.isSecondaryUseActive()) {
                int light = LampLinks.cycleBrightness(level, player.blockPosition(), links);
                player.displayClientMessage(Component.translatable("message." + MostLight.MOD_ID + ".brightness", light), true);
                LampLinks.click(level, player.blockPosition(), true);
            } else {
                boolean on = LampLinks.toggleAll(level, player.blockPosition(), links);
                LampLinks.click(level, player.blockPosition(), on);
            }
        }
        player.getCooldowns().addCooldown(this, 5);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return !LampLinks.get(stack).isEmpty();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        LampLinks.appendTooltip(stack, tooltip, "remote_usage");
    }
}
