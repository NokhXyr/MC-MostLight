package com.nokhxyr.mostlight.crafting;

import com.nokhxyr.mostlight.block.LampBlock;
import com.nokhxyr.mostlight.link.LightSwitchBlock;
import com.nokhxyr.mostlight.registry.ModBlocks;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.jetbrains.annotations.Nullable;

/**
 * Fabrication à l'établi de luminaire : catalogue des recettes, variante de couleur, matériaux pris dans l'inventaire.
 * Partagé par le serveur (fabrication) et l'écran (aperçu, matériaux manquants).
 */
public final class WorkbenchCrafting {
    private WorkbenchCrafting() {}

    /** Toutes les recettes, triées par identifiant : même ordre (et mêmes indices) côté client et serveur. */
    public static List<RecipeHolder<WorkbenchRecipe>> recipes(RecipeManager manager) {
        List<RecipeHolder<WorkbenchRecipe>> list = new ArrayList<>(manager.getAllRecipesFor(ModRecipes.WORKBENCH.get()));
        list.sort(Comparator.comparing(holder -> holder.id().toString()));
        return list;
    }

    /** Objet fabriqué dans la couleur choisie (lampes et interrupteurs) ; les autres objets n'ont pas de couleur. */
    public static ItemStack variant(ItemStack result, DyeColor color) {
        Item item = result.getItem();
        if (item instanceof BlockItem block && block.getBlock() instanceof LampBlock lamp) {
            return result.transmuteCopy(ModBlocks.item(lamp.type(), color), result.getCount());
        }
        ModBlocks.SwitchKind kind = switchKind(item);
        if (kind != null) {
            return result.transmuteCopy(ModBlocks.switchItem(kind, color), result.getCount());
        }
        return result.copy();
    }

    public static boolean colorable(ItemStack result) {
        Item item = result.getItem();
        return item instanceof BlockItem block && block.getBlock() instanceof LampBlock || switchKind(item) != null;
    }

    private static @Nullable ModBlocks.SwitchKind switchKind(Item item) {
        if (!(item instanceof BlockItem block) || !(block.getBlock() instanceof LightSwitchBlock)) {
            return null;
        }
        for (ModBlocks.SwitchKind kind : ModBlocks.SwitchKind.values()) {
            for (DyeColor color : DyeColor.values()) {
                if (ModBlocks.switchItem(kind, color) == item) {
                    return kind;
                }
            }
        }
        return null;
    }

    /** Matériaux à fournir : ceux de la recette, plus un colorant pour une couleur autre que le blanc. */
    public static List<SizedIngredient> cost(WorkbenchRecipe recipe, DyeColor color) {
        List<SizedIngredient> cost = new ArrayList<>(recipe.materials());
        if (color != DyeColor.WHITE && colorable(recipe.result())) {
            TagKey<Item> dye = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "dyes/" + color.getSerializedName()));
            cost.add(SizedIngredient.of(dye, 1));
        }
        return cost;
    }

    /** Quantité de chaque matériau disponible dans l'inventaire (sans compter deux fois le même objet). */
    public static int[] available(Inventory inventory, List<SizedIngredient> cost) {
        int[] left = new int[inventory.items.size()];
        for (int i = 0; i < left.length; i++) {
            left[i] = inventory.items.get(i).getCount();
        }
        int[] found = new int[cost.size()];
        for (int c = 0; c < cost.size(); c++) {
            SizedIngredient material = cost.get(c);
            for (int i = 0; i < left.length && found[c] < material.count(); i++) {
                ItemStack stack = inventory.items.get(i);
                if (left[i] > 0 && material.ingredient().test(stack)) {
                    int take = Math.min(left[i], material.count() - found[c]);
                    left[i] -= take;
                    found[c] += take;
                }
            }
        }
        return found;
    }

    public static boolean canCraft(Inventory inventory, List<SizedIngredient> cost) {
        int[] found = available(inventory, cost);
        for (int c = 0; c < cost.size(); c++) {
            if (found[c] < cost.get(c).count()) {
                return false;
            }
        }
        return true;
    }

    private static void consume(Inventory inventory, List<SizedIngredient> cost) {
        for (SizedIngredient material : cost) {
            int needed = material.count();
            for (int i = 0; i < inventory.items.size() && needed > 0; i++) {
                ItemStack stack = inventory.items.get(i);
                if (!stack.isEmpty() && material.ingredient().test(stack)) {
                    int take = Math.min(stack.getCount(), needed);
                    ItemStack rest = stack.getCraftingRemainingItem();
                    stack.shrink(take);
                    needed -= take;
                    if (!rest.isEmpty()) {
                        rest.setCount(take);
                        inventory.placeItemBackInInventory(rest);
                    }
                }
            }
        }
        inventory.setChanged();
    }

    /** Fabrique (une fois, ou autant que possible jusqu'à une pile) et rend le nombre d'objets fabriqués. */
    public static int craft(ServerPlayer player, WorkbenchRecipe recipe, DyeColor color, boolean bulk) {
        List<SizedIngredient> cost = cost(recipe, color);
        ItemStack result = variant(recipe.result(), color);
        int made = 0;
        do {
            if (!canCraft(player.getInventory(), cost)) {
                break;
            }
            consume(player.getInventory(), cost);
            ItemStack out = result.copy();
            out.onCraftedBy(player.level(), player, out.getCount());
            made += out.getCount();
            if (!player.getInventory().add(out)) {
                player.drop(out, false);
            }
        } while (bulk && made + result.getCount() <= result.getMaxStackSize());
        if (made > 0) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.VILLAGER_WORK_TOOLSMITH, SoundSource.BLOCKS, 0.7F, 1.1F);
        }
        return made;
    }
}
