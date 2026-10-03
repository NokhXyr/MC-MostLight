package com.nokhxyr.mostlight.compat.jei;

import com.nokhxyr.mostlight.MostLight;
import com.nokhxyr.mostlight.crafting.WorkbenchRecipe;
import com.nokhxyr.mostlight.registry.ModBlocks;
import java.util.Arrays;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/** Catégorie JEI « Établi de luminaire » : matériaux avec quantités, flèche, objet fabriqué (en blanc ; couleur au choix). */
final class WorkbenchCategory extends AbstractRecipeCategory<RecipeHolder<WorkbenchRecipe>> {
    private static final int MAX_MATERIALS = 6;
    private final IDrawableStatic arrow;

    WorkbenchCategory(IGuiHelper gui) {
        super(MostLightJeiPlugin.WORKBENCH, Component.translatable("jei." + MostLight.MOD_ID + ".category.workbench"),
                gui.createDrawableItemLike(ModBlocks.LAMP_WORKBENCH_ITEM.get()), MAX_MATERIALS * 18 + 52, 20);
        this.arrow = gui.getRecipeArrow();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<WorkbenchRecipe> holder, IFocusGroup focuses) {
        WorkbenchRecipe recipe = holder.value();
        List<SizedIngredient> materials = recipe.materials();
        for (int i = 0; i < Math.min(MAX_MATERIALS, materials.size()); i++) {
            SizedIngredient material = materials.get(i);
            List<ItemStack> stacks = Arrays.stream(material.ingredient().getItems())
                    .map(stack -> stack.copyWithCount(material.count())).toList();
            builder.addSlot(RecipeIngredientRole.INPUT, i * 18 + 1, 2).setStandardSlotBackground().addItemStacks(stacks);
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, MAX_MATERIALS * 18 + 34, 2).setOutputSlotBackground().addItemStack(recipe.result());
    }

    @Override
    public void draw(RecipeHolder<WorkbenchRecipe> holder, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        arrow.draw(graphics, MAX_MATERIALS * 18 + 5, 2);
    }
}
