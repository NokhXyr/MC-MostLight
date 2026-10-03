package com.nokhxyr.mostlight.crafting;

import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * Recette de l'établi de luminaire : grille 3 x 3 comme l'établi vanilla, mais un type de recette à part, pour que
 * les lampes ne se fabriquent qu'à cet établi. Largeur et hauteur 0 = recette sans forme.
 */
public interface WorkbenchRecipe extends Recipe<CraftingInput> {
    int width();

    int height();

    @Override
    default RecipeType<?> getType() {
        return ModRecipes.WORKBENCH.get();
    }

    /** Pas de livre de recettes vanilla pour ce type (JEI les affiche dans sa propre catégorie). */
    @Override
    default boolean isSpecial() {
        return true;
    }

    @Override
    RecipeSerializer<?> getSerializer();
}
