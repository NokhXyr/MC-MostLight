package com.nokhxyr.mostlight.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;

/** Recette en forme de l'établi de luminaire (même format JSON que crafting_shaped : pattern, key, result). */
public record WorkbenchShapedRecipe(String group, ShapedRecipePattern pattern, ItemStack result) implements WorkbenchRecipe {
    public static final MapCodec<WorkbenchShapedRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(WorkbenchShapedRecipe::group),
            ShapedRecipePattern.MAP_CODEC.forGetter(WorkbenchShapedRecipe::pattern),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(WorkbenchShapedRecipe::result)
    ).apply(i, WorkbenchShapedRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, WorkbenchShapedRecipe> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, WorkbenchShapedRecipe::group,
            ShapedRecipePattern.STREAM_CODEC, WorkbenchShapedRecipe::pattern,
            ItemStack.STREAM_CODEC, WorkbenchShapedRecipe::result,
            WorkbenchShapedRecipe::new);

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return pattern.matches(input);
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= pattern.width() && height >= pattern.height();
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return pattern.ingredients();
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public int width() {
        return pattern.width();
    }

    @Override
    public int height() {
        return pattern.height();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.SHAPED.get();
    }
}
