package com.nokhxyr.mostlight.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * Recette de l'établi de luminaire (catalogue, comme l'établi de Refurbished ou de Decocraft) : une liste de
 * matériaux avec quantités, prélevés dans l'inventaire du joueur, et l'onglet du catalogue où elle apparaît.
 */
public record WorkbenchRecipe(String category, List<SizedIngredient> materials, ItemStack result) implements Recipe<RecipeInput> {
    public static final MapCodec<WorkbenchRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.optionalFieldOf("category", "tools").forGetter(WorkbenchRecipe::category),
            SizedIngredient.NESTED_CODEC.listOf().fieldOf("ingredients").forGetter(WorkbenchRecipe::materials),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(WorkbenchRecipe::result)
    ).apply(i, WorkbenchRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, WorkbenchRecipe> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, WorkbenchRecipe::category,
            SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), WorkbenchRecipe::materials,
            ItemStack.STREAM_CODEC, WorkbenchRecipe::result,
            WorkbenchRecipe::new);

    /** Pas de grille : la recette se choisit dans le catalogue, jamais par placement d'objets. */
    @Override
    public boolean matches(RecipeInput input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> out = NonNullList.create();
        materials.forEach(m -> out.add(m.ingredient()));
        return out;
    }

    /** Pas de livre de recettes vanilla (JEI les affiche dans sa propre catégorie). */
    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.WORKBENCH.get();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.SERIALIZER.get();
    }
}
