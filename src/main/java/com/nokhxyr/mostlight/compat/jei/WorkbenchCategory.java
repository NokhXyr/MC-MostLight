package com.nokhxyr.mostlight.compat.jei;

import com.nokhxyr.mostlight.MostLight;
import com.nokhxyr.mostlight.crafting.WorkbenchRecipe;
import com.nokhxyr.mostlight.registry.ModBlocks;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Catégorie JEI « Établi de luminaire » : grille 3 x 3, flèche, résultat (comme la catégorie de l'établi vanilla). */
final class WorkbenchCategory extends AbstractRecipeCategory<RecipeHolder<WorkbenchRecipe>> {
    private final ICraftingGridHelper grid;
    private final IDrawableStatic arrow;

    WorkbenchCategory(IGuiHelper gui) {
        super(MostLightJeiPlugin.WORKBENCH, Component.translatable("jei." + MostLight.MOD_ID + ".category.workbench"),
                gui.createDrawableItemLike(ModBlocks.LAMP_WORKBENCH_ITEM.get()), 116, 54);
        this.grid = gui.createCraftingGridHelper();
        this.arrow = gui.getRecipeArrow();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<WorkbenchRecipe> holder, IFocusGroup focuses) {
        WorkbenchRecipe recipe = holder.value();
        RegistryAccess registries = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.registryAccess() : RegistryAccess.EMPTY;
        grid.createAndSetIngredients(builder, recipe.getIngredients(), recipe.width(), recipe.height());
        grid.createAndSetOutputs(builder, List.of(recipe.getResultItem(registries)));
    }

    @Override
    public void draw(RecipeHolder<WorkbenchRecipe> holder, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        arrow.draw(graphics, 61, 19);
    }
}
