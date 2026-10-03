package com.nokhxyr.mostlight.crafting;

import com.mojang.serialization.MapCodec;
import com.nokhxyr.mostlight.MostLight;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Type de recette, sérialiseur et menu de l'établi de luminaire. */
public final class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, MostLight.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, MostLight.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MostLight.MOD_ID);

    public static final Supplier<RecipeType<WorkbenchRecipe>> WORKBENCH = TYPES.register("lamp_workbench",
            () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MostLight.MOD_ID, "lamp_workbench")));
    public static final Supplier<RecipeSerializer<WorkbenchRecipe>> SERIALIZER = SERIALIZERS.register("workbench",
            () -> new RecipeSerializer<WorkbenchRecipe>() {
                @Override
                public MapCodec<WorkbenchRecipe> codec() {
                    return WorkbenchRecipe.CODEC;
                }

                @Override
                public StreamCodec<RegistryFriendlyByteBuf, WorkbenchRecipe> streamCodec() {
                    return WorkbenchRecipe.STREAM_CODEC;
                }
            });
    public static final Supplier<MenuType<LampWorkbenchMenu>> WORKBENCH_MENU = MENUS.register("lamp_workbench",
            () -> new MenuType<>(LampWorkbenchMenu::new, FeatureFlags.DEFAULT_FLAGS));

    private ModRecipes() {}
}
