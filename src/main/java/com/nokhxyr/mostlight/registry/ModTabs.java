package com.nokhxyr.mostlight.registry;

import com.nokhxyr.mostlight.MostLight;
import com.nokhxyr.mostlight.block.LampCategory;
import com.nokhxyr.mostlight.block.LampFinish;
import com.nokhxyr.mostlight.block.LampType;
import com.nokhxyr.mostlight.block.LightTone;
import com.nokhxyr.mostlight.component.ModComponents;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Onglets créatifs, rangés les uns à la suite des autres et chacun avec sa barre de recherche :
 * <ol>
 *   <li>Outils et interrupteurs : établi de luminaire, clé, connecteur, télécommande, puis les interrupteurs
 *       (simple, variateur, double) dans les 16 couleurs ;</li>
 *   <li>une catégorie de lampes par onglet (plafond, mur, table, sol, blocs), chaque modèle dans ses 16 couleurs ;</li>
 *   <li>Finitions et teintes : chaque modèle dans ses 10 finitions puis ses 4 teintes de lumière.</li>
 * </ol>
 */
public final class ModTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MostLight.MOD_ID);

    private static ResourceKey<CreativeModeTab> previous;

    static {
        tab("tools", () -> new ItemStack(ModBlocks.LAMP_WORKBENCH_ITEM.get()), (params, output) -> {
            output.accept(ModBlocks.LAMP_WORKBENCH_ITEM.get());
            output.accept(ModBlocks.DESIGNER_WRENCH.get());
            output.accept(ModBlocks.LED_CONNECTOR.get());
            output.accept(ModBlocks.LAMP_REMOTE.get());
            for (ModBlocks.SwitchKind kind : ModBlocks.SwitchKind.values()) {
                for (DyeColor color : DyeColor.values()) {
                    output.accept(ModBlocks.switchItem(kind, color));
                }
            }
        });
        for (LampCategory category : LampCategory.values()) {
            LampType icon = firstOf(category);
            tab(category.id(), () -> new ItemStack(ModBlocks.item(icon, DyeColor.YELLOW)), (params, output) -> {
                for (LampType type : LampType.values()) {
                    if (type.category() == category) {
                        for (DyeColor color : DyeColor.values()) {
                            output.accept(ModBlocks.item(type, color));
                        }
                    }
                }
            });
        }
        tab("finishes", () -> new ItemStack(ModBlocks.DESIGNER_WRENCH.get()), (params, output) -> {
            for (LampType type : LampType.values()) {
                for (LampFinish finish : LampFinish.values()) {
                    ItemStack stack = new ItemStack(ModBlocks.item(type, DyeColor.WHITE));
                    stack.set(ModComponents.FINISH.get(), finish);
                    output.accept(stack);
                }
                for (LightTone tone : LightTone.values()) {
                    if (tone != LightTone.AUTO) {
                        ItemStack stack = new ItemStack(ModBlocks.item(type, DyeColor.LIGHT_BLUE));
                        stack.set(ModComponents.LIGHT_TONE.get(), tone);
                        output.accept(stack);
                    }
                }
            }
        });
    }

    private ModTabs() {}

    /** Un onglet placé juste après le précédent du mod, avec une barre de recherche. */
    private static void tab(String id, Supplier<ItemStack> icon, CreativeModeTab.DisplayItemsGenerator items) {
        ResourceKey<CreativeModeTab> after = previous;
        TABS.register(id, () -> {
            CreativeModeTab.Builder builder = CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + MostLight.MOD_ID + "." + id))
                    .icon(icon)
                    .displayItems(items)
                    .withSearchBar();
            if (after != null) {
                builder.withTabsBefore(after);
            }
            return builder.build();
        });
        previous = ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.fromNamespaceAndPath(MostLight.MOD_ID, id));
    }

    private static LampType firstOf(LampCategory category) {
        for (LampType type : LampType.values()) {
            if (type.category() == category) {
                return type;
            }
        }
        throw new IllegalStateException("Aucune lampe dans la catégorie " + category);
    }
}
