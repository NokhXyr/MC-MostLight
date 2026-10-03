package com.nokhxyr.mostlight.crafting;

import com.nokhxyr.mostlight.registry.ModBlocks;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * Menu de l'établi de luminaire : pas de grille, seulement l'inventaire du joueur. L'écran choisit une recette du
 * catalogue et une couleur, puis envoie un « clic de bouton » (comme la table d'enchantement) que le serveur vérifie
 * et exécute : bouton = indice de la recette x 64 + couleur x 2 + (1 si Maj : autant que possible).
 */
public class LampWorkbenchMenu extends AbstractContainerMenu {
    public static final int WIDTH = 260;
    public static final int HEIGHT = 240;
    public static final int INVENTORY_X = 49;
    public static final int INVENTORY_Y = 158;
    private final ContainerLevelAccess access;

    public LampWorkbenchMenu(int id, Inventory inventory) {
        this(id, inventory, ContainerLevelAccess.NULL);
    }

    public LampWorkbenchMenu(int id, Inventory inventory, ContainerLevelAccess access) {
        super(ModRecipes.WORKBENCH_MENU.get(), id);
        this.access = access;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, INVENTORY_X + col * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, INVENTORY_X + col * 18, INVENTORY_Y + 58));
        }
    }

    public static int button(int recipe, DyeColor color, boolean bulk) {
        return recipe << 6 | color.getId() << 1 | (bulk ? 1 : 0);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer server)) {
            return false;
        }
        List<RecipeHolder<WorkbenchRecipe>> recipes = WorkbenchCrafting.recipes(server.level().getRecipeManager());
        int index = id >> 6;
        if (index < 0 || index >= recipes.size()) {
            return false;
        }
        DyeColor color = DyeColor.byId((id >> 1) & 31);
        WorkbenchCrafting.craft(server, recipes.get(index).value(), color, (id & 1) != 0);
        broadcastChanges();
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.LAMP_WORKBENCH.get());
    }

    /** Maj + clic : entre l'inventaire et la barre d'action. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack moved = stack.copy();
        boolean fromInventory = index < 27;
        if (!moveItemStackTo(stack, fromInventory ? 27 : 0, fromInventory ? 36 : 27, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return moved;
    }
}
