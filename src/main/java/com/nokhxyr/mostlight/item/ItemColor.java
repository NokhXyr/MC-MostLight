package com.nokhxyr.mostlight.item;

import com.nokhxyr.mostlight.block.LampBlock;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;

/**
 * Colour of a lamp or switch item. It is kept in the vanilla {@code block_state} component, which the game applies to
 * the placed block on its own. White, the default, is stored as no colour at all, so that every white item stacks.
 */
public final class ItemColor {
    private ItemColor() {}

    public static DyeColor of(ItemStack stack) {
        BlockItemStateProperties properties = stack.get(DataComponents.BLOCK_STATE);
        DyeColor color = properties == null ? null : properties.get(LampBlock.COLOR);
        return color == null ? DyeColor.WHITE : color;
    }

    /** The same stack in another colour. */
    public static ItemStack with(ItemStack stack, DyeColor color) {
        BlockItemStateProperties properties = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY);
        java.util.Map<String, String> values = new java.util.HashMap<>(properties.properties());
        if (color == DyeColor.WHITE) {
            values.remove(LampBlock.COLOR.getName());
        } else {
            values.put(LampBlock.COLOR.getName(), color.getSerializedName());
        }
        if (values.isEmpty()) {
            stack.remove(DataComponents.BLOCK_STATE);
        } else {
            stack.set(DataComponents.BLOCK_STATE, new BlockItemStateProperties(values));
        }
        return stack;
    }

    /** Translation key of the item in its colour: {@code block.mostlight.table_lamp.red}. */
    public static String nameKey(String descriptionId, ItemStack stack) {
        return descriptionId + "." + of(stack).getSerializedName();
    }
}
