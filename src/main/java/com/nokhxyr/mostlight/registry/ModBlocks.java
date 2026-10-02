package com.nokhxyr.mostlight.registry;

import com.nokhxyr.mostlight.MostLight;
import com.nokhxyr.mostlight.block.CubeLampBlock;
import com.nokhxyr.mostlight.block.HorizontalLampBlock;
import com.nokhxyr.mostlight.block.LampBlock;
import com.nokhxyr.mostlight.block.LampType;
import com.nokhxyr.mostlight.block.OmniLampBlock;
import com.nokhxyr.mostlight.block.Placement;
import com.nokhxyr.mostlight.block.TallLampBlock;
import com.nokhxyr.mostlight.item.LampItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Enregistre chaque type de lampe dans les 16 couleurs. */
public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MostLight.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MostLight.MOD_ID);

    private static final Map<LampType, Map<DyeColor, DeferredBlock<LampBlock>>> LAMPS = new EnumMap<>(LampType.class);
    private static final Map<LampType, Map<DyeColor, DeferredItem<LampItem>>> LAMP_ITEMS = new EnumMap<>(LampType.class);
    private static final List<DeferredBlock<LampBlock>> ALL = new ArrayList<>();

    static {
        for (LampType type : LampType.values()) {
            Map<DyeColor, DeferredBlock<LampBlock>> blocks = new EnumMap<>(DyeColor.class);
            Map<DyeColor, DeferredItem<LampItem>> items = new EnumMap<>(DyeColor.class);
            for (DyeColor color : DyeColor.values()) {
                String name = type.id() + "_" + color.getSerializedName();
                DeferredBlock<LampBlock> block = BLOCKS.register(name, () -> create(type, color));
                blocks.put(color, block);
                items.put(color, ITEMS.register(name, () -> new LampItem(block.get(), new Item.Properties())));
                ALL.add(block);
            }
            LAMPS.put(type, blocks);
            LAMP_ITEMS.put(type, items);
        }
    }

    private ModBlocks() {}

    private static LampBlock create(LampType type, DyeColor color) {
        BlockBehaviour.Properties props = BlockBehaviour.Properties.of()
                .mapColor(color.getMapColor())
                .strength(0.5F)
                .sound(type.sound())
                .lightLevel(type.placement() == Placement.TALL ? TallLampBlock::lightLevel : LampBlock::lightLevel);
        if (type.placement() != Placement.CUBE) {
            props = props.noOcclusion().pushReaction(PushReaction.DESTROY);
        }
        return switch (type.placement()) {
            case TALL -> new TallLampBlock(type, color, props);
            case OMNI -> new OmniLampBlock(type, color, props);
            case CUBE -> new CubeLampBlock(type, color, props);
            default -> new HorizontalLampBlock(type, color, props);
        };
    }

    public static LampBlock lamp(LampType type, DyeColor color) {
        return LAMPS.get(type).get(color).get();
    }

    public static LampItem item(LampType type, DyeColor color) {
        return LAMP_ITEMS.get(type).get(color).get();
    }

    public static List<DeferredBlock<LampBlock>> all() {
        return Collections.unmodifiableList(ALL);
    }
}
