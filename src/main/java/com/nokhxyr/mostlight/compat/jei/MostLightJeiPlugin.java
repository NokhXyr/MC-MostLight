package com.nokhxyr.mostlight.compat.jei;

import com.nokhxyr.mostlight.MostLight;
import com.nokhxyr.mostlight.block.LampFinish;
import com.nokhxyr.mostlight.block.LampShapes;
import com.nokhxyr.mostlight.block.LampType;
import com.nokhxyr.mostlight.block.LightTone;
import com.nokhxyr.mostlight.component.ModComponents;
import com.nokhxyr.mostlight.item.ItemColor;
import com.nokhxyr.mostlight.registry.ModBlocks;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import com.nokhxyr.mostlight.crafting.ModRecipes;
import com.nokhxyr.mostlight.crafting.WorkbenchRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.crafting.RecipeHolder;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Compatibilité JEI (optionnelle : chargée seulement si JEI est installé) : catégorie « Établi de luminaire » avec
 * toutes les recettes du mod (matériaux et quantités), variantes de finition / teinte de lumière
 * comme objets distincts, fiche d'explication pour chaque lampe, outil et interrupteur.
 */
@JeiPlugin
public class MostLightJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(MostLight.MOD_ID, "jei_plugin");
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static final RecipeType<RecipeHolder<WorkbenchRecipe>> WORKBENCH =
            RecipeType.create(MostLight.MOD_ID, "lamp_workbench", (Class) RecipeHolder.class);

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new WorkbenchCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(WORKBENCH, ModBlocks.LAMP_WORKBENCH_ITEM.get());
    }

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    /** Couleur, finition et teinte distinguent les variantes d'une même lampe. */
    private static final ISubtypeInterpreter<ItemStack> LOOK = new ISubtypeInterpreter<>() {
        @Override
        public @Nullable Object getSubtypeData(ItemStack stack, UidContext context) {
            LampFinish finish = stack.get(ModComponents.FINISH.get());
            LightTone tone = stack.get(ModComponents.LIGHT_TONE.get());
            return List.of(ItemColor.of(stack).getSerializedName(), String.valueOf(finish), String.valueOf(tone));
        }

        @Override
        public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
            return ItemColor.of(stack).getSerializedName() + "/" + stack.get(ModComponents.FINISH.get()) + "/" + stack.get(ModComponents.LIGHT_TONE.get());
        }
    };

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        for (LampType type : LampType.values()) {
            registration.registerSubtypeInterpreter(ModBlocks.item(type), LOOK);
        }
        for (ModBlocks.SwitchKind kind : ModBlocks.SwitchKind.values()) {
            registration.registerSubtypeInterpreter(ModBlocks.switchItem(kind), LOOK);
        }
    }

    private static Component info(String key) {
        return Component.translatable("jei." + MostLight.MOD_ID + "." + key);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (Minecraft.getInstance().level != null) {
            registration.addRecipes(WORKBENCH, Minecraft.getInstance().level.getRecipeManager().getAllRecipesFor(ModRecipes.WORKBENCH.get()));
        }
        registration.addIngredientInfo(new ItemStack(ModBlocks.LAMP_WORKBENCH_ITEM.get()), VanillaTypes.ITEM_STACK, info("workbench"));
        List<ItemStack> lamps = new ArrayList<>();
        List<ItemStack> strips = new ArrayList<>();
        List<ItemStack> fans = new ArrayList<>();
        List<ItemStack> animated = new ArrayList<>();
        for (LampType type : LampType.values()) {
            LampShapes.Animation animation = LampShapes.animation(type.id());
            for (DyeColor color : DyeColor.values()) {
                ItemStack stack = ModBlocks.stack(type, color);
                if (type.isStrip()) {
                    strips.add(stack);
                } else if (animation != null && "fan".equals(animation.trigger())) {
                    fans.add(stack);
                } else if (animation != null) {
                    animated.add(stack);
                } else {
                    lamps.add(stack);
                }
            }
        }
        registration.addItemStackInfo(lamps, info("lamp"), info("lamp_look"));
        registration.addItemStackInfo(strips, info("lamp"), info("strip"), info("strip_chain"));
        registration.addItemStackInfo(fans, info("lamp"), info("fan"));
        registration.addItemStackInfo(animated, info("lamp"), info("animated"));
        registration.addIngredientInfo(new ItemStack(ModBlocks.DESIGNER_WRENCH.get()), VanillaTypes.ITEM_STACK, info("wrench"));
        registration.addIngredientInfo(new ItemStack(ModBlocks.LED_CONNECTOR.get()), VanillaTypes.ITEM_STACK, info("connector"));
        registration.addIngredientInfo(new ItemStack(ModBlocks.LAMP_REMOTE.get()), VanillaTypes.ITEM_STACK, info("remote"));
        for (ModBlocks.SwitchKind kind : ModBlocks.SwitchKind.values()) {
            List<ItemStack> switches = new ArrayList<>();
            for (DyeColor color : DyeColor.values()) {
                switches.add(ModBlocks.switchStack(kind, color));
            }
            registration.addItemStackInfo(switches, info("switch_link"), info(kind.id));
        }
    }
}
