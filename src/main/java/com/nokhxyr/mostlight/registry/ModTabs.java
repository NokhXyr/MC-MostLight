package com.nokhxyr.mostlight.registry;

import com.nokhxyr.mostlight.MostLight;
import com.nokhxyr.mostlight.block.LampCategory;
import com.nokhxyr.mostlight.block.LampType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Un onglet créatif par catégorie de lampe. */
public final class ModTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MostLight.MOD_ID);

    static {
        for (LampCategory category : LampCategory.values()) {
            LampType icon = firstOf(category);
            TABS.register(category.id(), () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + MostLight.MOD_ID + "." + category.id()))
                    .icon(() -> new ItemStack(ModBlocks.item(icon, DyeColor.YELLOW)))
                    .displayItems((params, output) -> {
                        for (LampType type : LampType.values()) {
                            if (type.category() == category) {
                                for (DyeColor color : DyeColor.values()) {
                                    output.accept(ModBlocks.item(type, color));
                                }
                            }
                        }
                    })
                    .build());
        }
    }

    private ModTabs() {}

    private static LampType firstOf(LampCategory category) {
        for (LampType type : LampType.values()) {
            if (type.category() == category) {
                return type;
            }
        }
        throw new IllegalStateException("Aucune lampe dans la catégorie " + category);
    }
}
