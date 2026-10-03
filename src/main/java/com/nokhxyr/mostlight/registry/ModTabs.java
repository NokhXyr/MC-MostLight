package com.nokhxyr.mostlight.registry;

import com.nokhxyr.mostlight.MostLight;
import com.nokhxyr.mostlight.block.LampCategory;
import com.nokhxyr.mostlight.block.LampFinish;
import com.nokhxyr.mostlight.block.LampType;
import com.nokhxyr.mostlight.block.LightTone;
import com.nokhxyr.mostlight.component.ModComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Un onglet créatif par catégorie, plus un onglet outils + finitions. */
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
        TABS.register("finishes", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup." + MostLight.MOD_ID + ".finishes"))
                .icon(() -> new ItemStack(ModBlocks.DESIGNER_WRENCH.get()))
                .displayItems((params, output) -> {
                    output.accept(ModBlocks.DESIGNER_WRENCH.get());
                    for (ModBlocks.SwitchKind kind : ModBlocks.SwitchKind.values()) {
                        for (DyeColor color : DyeColor.values()) {
                            output.accept(ModBlocks.switchItem(kind, color));
                        }
                    }
                    output.accept(ModBlocks.LAMP_REMOTE.get());
                    output.accept(ModBlocks.LED_CONNECTOR.get());
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
                })
                .build());
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
