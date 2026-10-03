package com.nokhxyr.mostlight;

import com.nokhxyr.mostlight.component.ModComponents;
import com.nokhxyr.mostlight.crafting.ModRecipes;
import com.nokhxyr.mostlight.registry.ModBlockEntities;
import com.nokhxyr.mostlight.registry.ModBlocks;
import com.nokhxyr.mostlight.registry.ModTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.common.Mod;

@Mod(MostLight.MOD_ID)
public class MostLight {
    public static final String MOD_ID = "mostlight";

    public MostLight(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, MostLightConfig.SPEC);
        ModBlocks.BLOCKS.register(modBus);
        ModBlocks.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModComponents.COMPONENTS.register(modBus);
        ModTabs.TABS.register(modBus);
        ModRecipes.TYPES.register(modBus);
        ModRecipes.SERIALIZERS.register(modBus);
        ModRecipes.MENUS.register(modBus);
    }
}
