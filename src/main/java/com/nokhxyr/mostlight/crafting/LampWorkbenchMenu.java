package com.nokhxyr.mostlight.crafting;

import com.nokhxyr.mostlight.registry.ModBlocks;
import java.util.Optional;
import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.CommonHooks;

/**
 * Menu de l'établi de luminaire : grille 3 x 3, case de résultat et inventaire, disposés comme l'établi vanilla,
 * mais qui ne cherche que les recettes de l'établi de luminaire.
 */
public class LampWorkbenchMenu extends AbstractContainerMenu {
    public static final int RESULT_SLOT = 0;
    public static final int GRID_START = 1;
    public static final int INVENTORY_START = 10;
    public static final int INVENTORY_END = 46;
    private final CraftingContainer craftSlots = new TransientCraftingContainer(this, 3, 3);
    private final ResultContainer resultSlots = new ResultContainer();
    private final ContainerLevelAccess access;
    private final Player player;

    public LampWorkbenchMenu(int id, Inventory inventory) {
        this(id, inventory, ContainerLevelAccess.NULL);
    }

    public LampWorkbenchMenu(int id, Inventory inventory, ContainerLevelAccess access) {
        super(ModRecipes.WORKBENCH_MENU.get(), id);
        this.access = access;
        this.player = inventory.player;
        addSlot(new WorkbenchResultSlot(player, craftSlots, resultSlots, 0, 124, 35));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new Slot(craftSlots, col + row * 3, 30 + col * 18, 17 + row * 18));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public void slotsChanged(Container container) {
        access.execute((level, pos) -> updateResult(level));
    }

    private void updateResult(Level level) {
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        CraftingInput input = craftSlots.asCraftInput();
        ItemStack result = ItemStack.EMPTY;
        Optional<RecipeHolder<WorkbenchRecipe>> recipe = level.getRecipeManager().getRecipeFor(ModRecipes.WORKBENCH.get(), input, level);
        if (recipe.isPresent() && resultSlots.setRecipeUsed(level, serverPlayer, recipe.get())) {
            ItemStack crafted = recipe.get().value().assemble(input, level.registryAccess());
            if (crafted.isItemEnabled(level.enabledFeatures())) {
                result = crafted;
            }
        }
        resultSlots.setItem(0, result);
        setRemoteSlot(0, result);
        serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(containerId, incrementStateId(), 0, result));
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        access.execute((level, pos) -> clearContainer(player, craftSlots));
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.LAMP_WORKBENCH.get());
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != resultSlots && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            moved = stack.copy();
            if (index == RESULT_SLOT) {
                access.execute((level, pos) -> stack.getItem().onCraftedBy(stack, level, player));
                if (!moveItemStackTo(stack, INVENTORY_START, INVENTORY_END, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stack, moved);
            } else if (index >= INVENTORY_START) {
                if (!moveItemStackTo(stack, GRID_START, INVENTORY_START, false)) {
                    if (index < 37) {
                        if (!moveItemStackTo(stack, 37, INVENTORY_END, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (!moveItemStackTo(stack, INVENTORY_START, 37, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            } else if (!moveItemStackTo(stack, INVENTORY_START, INVENTORY_END, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (stack.getCount() == moved.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
            if (index == RESULT_SLOT) {
                player.drop(stack, false);
            }
        }
        return moved;
    }

    /** Case de résultat : consomme les ingrédients selon la recette de l'établi de luminaire. */
    private static final class WorkbenchResultSlot extends ResultSlot {
        private final CraftingContainer grid;

        WorkbenchResultSlot(Player player, CraftingContainer grid, Container result, int index, int x, int y) {
            super(player, grid, result, index, x, y);
            this.grid = grid;
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            checkTakeAchievements(stack);
            CraftingInput.Positioned positioned = grid.asPositionedCraftInput();
            CraftingInput input = positioned.input();
            int left = positioned.left();
            int top = positioned.top();
            CommonHooks.setCraftingPlayer(player);
            NonNullList<ItemStack> remaining = player.level().getRecipeManager()
                    .getRecipeFor(ModRecipes.WORKBENCH.get(), input, player.level())
                    .map(recipe -> recipe.value().getRemainingItems(input))
                    .orElseGet(() -> NonNullList.withSize(input.size(), ItemStack.EMPTY));
            CommonHooks.setCraftingPlayer(null);
            for (int row = 0; row < input.height(); row++) {
                for (int col = 0; col < input.width(); col++) {
                    int slot = col + left + (row + top) * grid.getWidth();
                    ItemStack current = grid.getItem(slot);
                    ItemStack rest = remaining.get(col + row * input.width());
                    if (!current.isEmpty()) {
                        grid.removeItem(slot, 1);
                        current = grid.getItem(slot);
                    }
                    if (!rest.isEmpty()) {
                        if (current.isEmpty()) {
                            grid.setItem(slot, rest);
                        } else if (ItemStack.isSameItemSameComponents(current, rest)) {
                            rest.grow(current.getCount());
                            grid.setItem(slot, rest);
                        } else if (!player.getInventory().add(rest)) {
                            player.drop(rest, false);
                        }
                    }
                }
            }
        }
    }
}
