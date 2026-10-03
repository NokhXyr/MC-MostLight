package com.nokhxyr.mostlight.client;

import com.nokhxyr.mostlight.MostLight;
import com.nokhxyr.mostlight.block.LampType;
import com.nokhxyr.mostlight.crafting.LampWorkbenchMenu;
import com.nokhxyr.mostlight.crafting.ModRecipes;
import com.nokhxyr.mostlight.crafting.WorkbenchCrafting;
import com.nokhxyr.mostlight.crafting.WorkbenchRecipe;
import com.nokhxyr.mostlight.registry.ModBlocks;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.lwjgl.glfw.GLFW;

/**
 * Écran de l'établi de luminaire, façon catalogue (comme l'établi de Refurbished ou de Decocraft) : onglets par
 * catégorie, recherche, grille de tous les objets ; à droite l'aperçu de l'objet choisi, ses matériaux (vert = dans
 * l'inventaire, rouge = manquant) et la palette des 16 couleurs ; bouton Fabriquer (Maj : autant que possible).
 */
public class LampWorkbenchScreen extends AbstractContainerScreen<LampWorkbenchMenu> {
    private static final String[] TABS = {"all", "ceiling", "wall", "table", "floor", "block", "switches", "tools"};
    private static final int COLS = 8;
    private static final int ROWS = 4;
    private static final int GRID_X = 8;
    private static final int GRID_Y = 50;
    private static final int TABS_Y = 31;
    private static final int PANEL_X = 160;
    private static final int PREVIEW_Y = 17;
    private static final int MATERIALS_Y = 78;
    private static final int PALETTE_Y = 134;

    private final List<RecipeHolder<WorkbenchRecipe>> all = new ArrayList<>();
    private final List<RecipeHolder<WorkbenchRecipe>> shown = new ArrayList<>();
    private ItemStack[] tabIcons;
    private EditBox search;
    private Button craft;
    private int tab;
    private int scroll;
    private RecipeHolder<WorkbenchRecipe> selected;
    private DyeColor color = DyeColor.WHITE;

    public LampWorkbenchScreen(LampWorkbenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = LampWorkbenchMenu.WIDTH;
        imageHeight = LampWorkbenchMenu.HEIGHT;
        inventoryLabelX = LampWorkbenchMenu.INVENTORY_X;
        inventoryLabelY = LampWorkbenchMenu.INVENTORY_Y - 11;
    }

    private static Component text(String key) {
        return Component.translatable("gui." + MostLight.MOD_ID + ".workbench." + key);
    }

    @Override
    protected void init() {
        super.init();
        tabIcons = new ItemStack[] {
                new ItemStack(ModBlocks.LAMP_WORKBENCH_ITEM.get()),
                new ItemStack(ModBlocks.item(LampType.CHANDELIER, DyeColor.WHITE)),
                new ItemStack(ModBlocks.item(LampType.WALL_SCONCE, DyeColor.WHITE)),
                new ItemStack(ModBlocks.item(LampType.TABLE_LAMP, DyeColor.WHITE)),
                new ItemStack(ModBlocks.item(LampType.FLOOR_LAMP, DyeColor.WHITE)),
                new ItemStack(ModBlocks.item(LampType.LAMP_BLOCK, DyeColor.WHITE)),
                new ItemStack(ModBlocks.LIGHT_SWITCH_ITEM.get()),
                new ItemStack(ModBlocks.DESIGNER_WRENCH.get()),
        };
        all.clear();
        if (minecraft != null && minecraft.level != null) {
            all.addAll(WorkbenchCrafting.recipes(minecraft.level.getRecipeManager()));
        }
        search = new EditBox(font, leftPos + GRID_X + 1, topPos + 17, COLS * 18 - 2, 12, text("search"));
        search.setHint(text("search").copy().withStyle(ChatFormatting.DARK_GRAY));
        search.setResponder(s -> refilter());
        addRenderableWidget(search);
        craft = Button.builder(text("craft"), b -> craft()).bounds(leftPos + GRID_X, topPos + GRID_Y + ROWS * 18 + 4, COLS * 18, 18).build();
        craft.setTooltip(Tooltip.create(text("bulk")));
        addRenderableWidget(craft);
        refilter();
    }

    private void refilter() {
        String query = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        shown.clear();
        for (RecipeHolder<WorkbenchRecipe> holder : all) {
            WorkbenchRecipe recipe = holder.value();
            boolean inTab = tab == 0 || TABS[tab].equals(recipe.category());
            boolean matches = query.isEmpty() || recipe.result().getHoverName().getString().toLowerCase(Locale.ROOT).contains(query);
            if (inTab && matches) {
                shown.add(holder);
            }
        }
        scroll = Mth.clamp(scroll, 0, maxScroll());
        if (selected == null && !shown.isEmpty()) {
            selected = shown.getFirst();
        }
    }

    private int maxScroll() {
        return Math.max(0, (shown.size() + COLS - 1) / COLS - ROWS);
    }

    private List<SizedIngredient> cost() {
        return selected == null ? List.of() : WorkbenchCrafting.cost(selected.value(), color);
    }

    private void craft() {
        if (selected == null || minecraft == null || minecraft.gameMode == null) {
            return;
        }
        int index = all.indexOf(selected);
        if (index >= 0) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, LampWorkbenchMenu.button(index, color, hasShiftDown()));
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        craft.active = selected != null && minecraft != null && minecraft.player != null
                && WorkbenchCrafting.canCraft(minecraft.player.getInventory(), cost());
    }

    // ------------------------------------------------------------------ dessin

    /** Panneau en relief, comme les fonds des interfaces vanilla. */
    private static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFF000000);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFFC6C6C6);
        g.fill(x + 1, y + 1, x + w - 2, y + 3, 0xFFFFFFFF);
        g.fill(x + 1, y + 1, x + 3, y + h - 2, 0xFFFFFFFF);
        g.fill(x + 2, y + h - 3, x + w - 1, y + h - 1, 0xFF555555);
        g.fill(x + w - 3, y + 2, x + w - 1, y + h - 1, 0xFF555555);
    }

    /** Creux (case, cadre d'aperçu) : ombre en haut à gauche, lumière en bas à droite. */
    private static void inset(GuiGraphics g, int x, int y, int w, int h, int fill) {
        g.fill(x, y, x + w, y + h, 0xFF373737);
        g.fill(x + 1, y + 1, x + w, y + h, 0xFFFFFFFF);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        panel(g, x, y, imageWidth, imageHeight);
        // onglets
        for (int i = 0; i < TABS.length; i++) {
            int tx = x + GRID_X + i * 18, ty = y + TABS_Y;
            inset(g, tx, ty, 18, 18, i == tab ? 0xFFE8D9A8 : 0xFF8B8B8B);
        }
        // grille du catalogue
        inset(g, x + GRID_X - 1, y + GRID_Y - 1, COLS * 18 + 2, ROWS * 18 + 2, 0xFF8B8B8B);
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int index = (scroll + r) * COLS + c;
                int sx = x + GRID_X + c * 18, sy = y + GRID_Y + r * 18;
                boolean chosen = index < shown.size() && shown.get(index) == selected;
                inset(g, sx, sy, 18, 18, chosen ? 0xFFE8D9A8 : 0xFF8B8B8B);
            }
        }
        // barre de défilement
        int barX = x + GRID_X + COLS * 18 + 2;
        g.fill(barX, y + GRID_Y, barX + 4, y + GRID_Y + ROWS * 18, 0xFF555555);
        int max = maxScroll();
        int knob = max == 0 ? 0 : (ROWS * 18 - 12) * scroll / max;
        g.fill(barX, y + GRID_Y + knob, barX + 4, y + GRID_Y + knob + 12, 0xFFE0E0E0);
        // aperçu, matériaux, palette
        inset(g, x + PANEL_X, y + PREVIEW_Y, imageWidth - PANEL_X - 8, 46, 0xFF8B8B8B);
        List<SizedIngredient> cost = cost();
        for (int i = 0; i < Math.min(6, cost.size()); i++) {
            inset(g, x + PANEL_X + (i % 2) * 46, y + MATERIALS_Y + (i / 2) * 18, 18, 18, 0xFF8B8B8B);
        }
        for (DyeColor dye : DyeColor.values()) {
            int px = x + PANEL_X + (dye.getId() % 8) * 11, py = y + PALETTE_Y + (dye.getId() / 8) * 11;
            g.fill(px, py, px + 10, py + 10, dye == color ? 0xFFFFFFFF : 0xFF373737);
            g.fill(px + 1, py + 1, px + 9, py + 9, 0xFF000000 | dye.getTextureDiffuseColor());
        }
        // inventaire
        for (Slot slot : menu.slots) {
            inset(g, x + slot.x - 1, y + slot.y - 1, 18, 18, 0xFF8B8B8B);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, 6, 0x404040, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        for (int i = 0; i < TABS.length; i++) {
            g.renderItem(tabIcons[i], x + GRID_X + i * 18 + 1, y + TABS_Y + 1);
        }
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int index = (scroll + r) * COLS + c;
                if (index < shown.size()) {
                    ItemStack stack = WorkbenchCrafting.variant(shown.get(index).value().result(), color);
                    g.renderItem(stack, x + GRID_X + c * 18 + 1, y + GRID_Y + r * 18 + 1);
                    g.renderItemDecorations(font, stack, x + GRID_X + c * 18 + 1, y + GRID_Y + r * 18 + 1);
                }
            }
        }
        if (selected != null) {
            ItemStack result = WorkbenchCrafting.variant(selected.value().result(), color);
            int px = x + PANEL_X + (imageWidth - PANEL_X - 8) / 2;
            g.pose().pushPose();
            g.pose().translate(px - 20, y + PREVIEW_Y + 3, 0);
            g.pose().scale(2.5F, 2.5F, 1);
            g.renderItem(result, 0, 0);
            g.pose().popPose();
            if (result.getCount() > 1) {
                g.drawString(font, "x" + result.getCount(), x + imageWidth - 24, y + PREVIEW_Y + 36, 0xFFFFFF, true);
            }
            Component name = result.getHoverName();
            String clipped = font.plainSubstrByWidth(name.getString(), imageWidth - PANEL_X - 8);
            g.drawString(font, clipped, x + PANEL_X, y + PREVIEW_Y + 49, 0x404040, false);
            // matériaux : nombre dans l'inventaire / nombre demandé, vert ou rouge
            List<SizedIngredient> cost = cost();
            int[] found = minecraft != null && minecraft.player != null
                    ? WorkbenchCrafting.available(minecraft.player.getInventory(), cost) : new int[cost.size()];
            for (int i = 0; i < Math.min(6, cost.size()); i++) {
                SizedIngredient material = cost.get(i);
                ItemStack[] options = material.ingredient().getItems();
                if (options.length == 0) {
                    continue;
                }
                ItemStack shownItem = options[(int) (System.currentTimeMillis() / 1000 % options.length)];
                int mx = x + PANEL_X + (i % 2) * 46, my = y + MATERIALS_Y + (i / 2) * 18;
                g.renderItem(shownItem, mx + 1, my + 1);
                boolean enough = found[i] >= material.count();
                g.drawString(font, found[i] + "/" + material.count(), mx + 20, my + 5, enough ? 0x2E8B2E : 0xC0392B, false);
            }
        }
        renderTooltip(g, mouseX, mouseY);
        renderHover(g, mouseX, mouseY);
    }

    /** Infobulles des onglets, du catalogue, des matériaux et de la palette. */
    private void renderHover(GuiGraphics g, int mouseX, int mouseY) {
        int rx = mouseX - leftPos, ry = mouseY - topPos;
        if (ry >= TABS_Y && ry < TABS_Y + 18 && rx >= GRID_X && rx < GRID_X + TABS.length * 18) {
            g.renderTooltip(font, text("tab." + TABS[(rx - GRID_X) / 18]), mouseX, mouseY);
            return;
        }
        int index = gridIndex(rx, ry);
        if (index >= 0) {
            g.renderTooltip(font, WorkbenchCrafting.variant(shown.get(index).value().result(), color), mouseX, mouseY);
            return;
        }
        List<SizedIngredient> cost = cost();
        for (int i = 0; i < Math.min(6, cost.size()); i++) {
            int mx = PANEL_X + (i % 2) * 46, my = MATERIALS_Y + (i / 2) * 18;
            ItemStack[] options = cost.get(i).ingredient().getItems();
            if (rx >= mx && rx < mx + 18 && ry >= my && ry < my + 18 && options.length > 0) {
                g.renderTooltip(font, options[(int) (System.currentTimeMillis() / 1000 % options.length)], mouseX, mouseY);
                return;
            }
        }
        DyeColor dye = paletteColor(rx, ry);
        if (dye != null) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("color.minecraft." + dye.getSerializedName()));
            if (dye != DyeColor.WHITE && selected != null && WorkbenchCrafting.colorable(selected.value().result())) {
                lines.add(text("dye").copy().withStyle(ChatFormatting.GRAY));
            }
            g.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }

    private int gridIndex(int rx, int ry) {
        if (rx < GRID_X || ry < GRID_Y || rx >= GRID_X + COLS * 18 || ry >= GRID_Y + ROWS * 18) {
            return -1;
        }
        int index = (scroll + (ry - GRID_Y) / 18) * COLS + (rx - GRID_X) / 18;
        return index < shown.size() ? index : -1;
    }

    private DyeColor paletteColor(int rx, int ry) {
        if (rx < PANEL_X || ry < PALETTE_Y || rx >= PANEL_X + 88 || ry >= PALETTE_Y + 22) {
            return null;
        }
        int id = (ry - PALETTE_Y) / 11 * 8 + (rx - PANEL_X) / 11;
        return id < 16 ? DyeColor.byId(id) : null;
    }

    // ------------------------------------------------------------------ souris et clavier

    private void click() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int rx = (int) mouseX - leftPos, ry = (int) mouseY - topPos;
        if (ry >= TABS_Y && ry < TABS_Y + 18 && rx >= GRID_X && rx < GRID_X + TABS.length * 18) {
            tab = (rx - GRID_X) / 18;
            scroll = 0;
            refilter();
            click();
            return true;
        }
        int index = gridIndex(rx, ry);
        if (index >= 0) {
            selected = shown.get(index);
            click();
            return true;
        }
        DyeColor dye = paletteColor(rx, ry);
        if (dye != null) {
            color = dye;
            click();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int rx = (int) mouseX - leftPos, ry = (int) mouseY - topPos;
        if (rx >= GRID_X && rx < GRID_X + COLS * 18 + 8 && ry >= GRID_Y && ry < GRID_Y + ROWS * 18) {
            scroll = Mth.clamp(scroll - (int) Math.signum(scrollY), 0, maxScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key != GLFW.GLFW_KEY_ESCAPE && search.isFocused()) {
            search.keyPressed(key, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @EventBusSubscriber(modid = MostLight.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    static final class Registration {
        private Registration() {}

        @SubscribeEvent
        static void register(RegisterMenuScreensEvent event) {
            event.register(ModRecipes.WORKBENCH_MENU.get(), LampWorkbenchScreen::new);
        }
    }
}
