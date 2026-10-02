package com.nokhxyr.mostlight;

import com.nokhxyr.mostlight.registry.ModBlocks;
import com.nokhxyr.mostlight.registry.ModTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(MostLight.MOD_ID)
public class MostLight {
    public static final String MOD_ID = "mostlight";

    public MostLight(IEventBus modBus) {
        ModBlocks.BLOCKS.register(modBus);
        ModBlocks.ITEMS.register(modBus);
        ModTabs.TABS.register(modBus);
    }
}
