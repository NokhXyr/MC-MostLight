package com.nokhxyr.mostlight.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
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
import net.minecraft.world.level.Level;

/** Recette sans forme de l'établi de luminaire (teintures : lampe + colorant). Même format que crafting_shapeless. */
public record WorkbenchShapelessRecipe(String group, NonNullList<Ingredient> ingredients, ItemStack result) implements WorkbenchRecipe {
    public static final MapCodec<WorkbenchShapelessRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(WorkbenchShapelessRecipe::group),
            Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients").flatXmap(list -> {
                if (list.isEmpty() || list.size() > 9) {
                    return DataResult.error(() -> "1 à 9 ingrédients attendus");
                }
                return DataResult.success(NonNullList.of(Ingredient.EMPTY, list.toArray(Ingredient[]::new)));
            }, DataResult::success).forGetter(WorkbenchShapelessRecipe::ingredients),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(WorkbenchShapelessRecipe::result)
    ).apply(i, WorkbenchShapelessRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, WorkbenchShapelessRecipe> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, WorkbenchShapelessRecipe::group,
            Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()).map(
                    list -> NonNullList.of(Ingredient.EMPTY, list.toArray(Ingredient[]::new)), list -> list),
            WorkbenchShapelessRecipe::ingredients,
            ItemStack.STREAM_CODEC, WorkbenchShapelessRecipe::result,
            WorkbenchShapelessRecipe::new);

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.ingredientCount() != ingredients.size()) {
            return false;
        }
        if (input.size() == 1 && ingredients.size() == 1) {
            return ingredients.getFirst().test(input.getItem(0));
        }
        return input.stackedContents().canCraft(this, null);
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= ingredients.size();
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return ingredients;
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public int width() {
        return 0;
    }

    @Override
    public int height() {
        return 0;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.SHAPELESS.get();
    }
}
