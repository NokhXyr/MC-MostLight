package com.nokhxyr.mostlight.registry;

import com.nokhxyr.mostlight.MostLight;
import com.nokhxyr.mostlight.block.CubeLampBlock;
import com.nokhxyr.mostlight.block.HorizontalLampBlock;
import com.nokhxyr.mostlight.block.LampBlock;
import com.nokhxyr.mostlight.block.LampType;
import com.nokhxyr.mostlight.block.LightStripBlock;
import com.nokhxyr.mostlight.block.OmniLampBlock;
import com.nokhxyr.mostlight.block.Placement;
import com.nokhxyr.mostlight.block.TallLampBlock;
import com.nokhxyr.mostlight.item.DesignerWrenchItem;
import com.nokhxyr.mostlight.item.LampItem;
import com.nokhxyr.mostlight.item.LedConnectorItem;
import com.nokhxyr.mostlight.item.LightStripItem;
import com.nokhxyr.mostlight.link.DimmerSwitchBlock;
import com.nokhxyr.mostlight.link.LampRemoteItem;
import com.nokhxyr.mostlight.link.LightSwitchBlock;
import com.nokhxyr.mostlight.link.SwitchItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
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
                items.put(color, ITEMS.register(name, () -> type == LampType.LIGHT_STRIP
                        ? new LightStripItem(block.get(), new Item.Properties())
                        : new LampItem(block.get(), new Item.Properties())));
                ALL.add(block);
            }
            LAMPS.put(type, blocks);
            LAMP_ITEMS.put(type, items);
        }
    }

    public static final DeferredBlock<LightSwitchBlock> LIGHT_SWITCH = BLOCKS.register("light_switch",
            () -> new LightSwitchBlock(switchProperties()));
    public static final DeferredBlock<DimmerSwitchBlock> DIMMER_SWITCH = BLOCKS.register("dimmer_switch",
            () -> new DimmerSwitchBlock(switchProperties()));
    public static final DeferredItem<SwitchItem> LIGHT_SWITCH_ITEM = ITEMS.register("light_switch",
            () -> new SwitchItem(LIGHT_SWITCH.get(), new Item.Properties(), "switch_usage"));
    public static final DeferredItem<SwitchItem> DIMMER_SWITCH_ITEM = ITEMS.register("dimmer_switch",
            () -> new SwitchItem(DIMMER_SWITCH.get(), new Item.Properties(), "dimmer_usage"));
    public static final DeferredItem<LampRemoteItem> LAMP_REMOTE = ITEMS.register("lamp_remote",
            () -> new LampRemoteItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<LedConnectorItem> LED_CONNECTOR = ITEMS.register("led_connector",
            () -> new LedConnectorItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<DesignerWrenchItem> DESIGNER_WRENCH = ITEMS.register("designer_wrench",
            () -> new DesignerWrenchItem(new Item.Properties().stacksTo(1)));

    private ModBlocks() {}

    private static BlockBehaviour.Properties switchProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.5F).sound(SoundType.STONE)
                .noCollission().pushReaction(PushReaction.DESTROY);
    }

    private static LampBlock create(LampType type, DyeColor color) {
        BlockBehaviour.Properties props = BlockBehaviour.Properties.of()
                .mapColor(color.getMapColor())
                .strength(0.5F)
                .sound(type.sound())
                .lightLevel(type.placement() == Placement.TALL ? TallLampBlock::lightLevel : LampBlock::lightLevel);
        if (type.placement() != Placement.CUBE) {
            props = props.noOcclusion().pushReaction(PushReaction.DESTROY);
        }
        if (type == LampType.LIGHT_STRIP) {
            // forme qui dépend de la block entity (position de chaque bande) : pas de cache par état
            props = props.dynamicShape();
        }
        return switch (type.placement()) {
            case TALL -> new TallLampBlock(type, color, props);
            case OMNI -> type == LampType.LIGHT_STRIP ? new LightStripBlock(type, color, props) : new OmniLampBlock(type, color, props);
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
