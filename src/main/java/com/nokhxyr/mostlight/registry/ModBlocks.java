package com.nokhxyr.mostlight.registry;

import com.nokhxyr.mostlight.MostLight;
import com.nokhxyr.mostlight.block.CubeLampBlock;
import com.nokhxyr.mostlight.block.AnimatedLampBlock;
import com.nokhxyr.mostlight.block.FanLampBlock;
import com.nokhxyr.mostlight.block.LampShapes;
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
import com.nokhxyr.mostlight.link.DoubleSwitchBlock;
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
                items.put(color, ITEMS.register(name, () -> type.isStrip()
                        ? new LightStripItem(block.get(), new Item.Properties())
                        : new LampItem(block.get(), new Item.Properties())));
                ALL.add(block);
            }
            LAMPS.put(type, blocks);
            LAMP_ITEMS.put(type, items);
        }
    }

    /** Interrupteurs : simple, variateur, double (lumière + ventilateur), chacun en 16 couleurs (blanc = id sans couleur). */
    public enum SwitchKind {
        LIGHT("light_switch", "switch_usage"),
        DIMMER("dimmer_switch", "dimmer_usage"),
        DOUBLE("double_switch", "double_usage");

        public final String id;
        public final String usageKey;

        SwitchKind(String id, String usageKey) {
            this.id = id;
            this.usageKey = usageKey;
        }

        public String name(DyeColor color) {
            return color == DyeColor.WHITE ? id : id + "_" + color.getSerializedName();
        }

        LightSwitchBlock create() {
            return switch (this) {
                case LIGHT -> new LightSwitchBlock(switchProperties());
                case DIMMER -> new DimmerSwitchBlock(switchProperties());
                case DOUBLE -> new DoubleSwitchBlock(switchProperties());
            };
        }
    }

    private static final Map<SwitchKind, Map<DyeColor, DeferredBlock<LightSwitchBlock>>> SWITCHES = new EnumMap<>(SwitchKind.class);
    private static final Map<SwitchKind, Map<DyeColor, DeferredItem<SwitchItem>>> SWITCH_ITEMS = new EnumMap<>(SwitchKind.class);

    static {
        for (SwitchKind kind : SwitchKind.values()) {
            Map<DyeColor, DeferredBlock<LightSwitchBlock>> blocks = new EnumMap<>(DyeColor.class);
            Map<DyeColor, DeferredItem<SwitchItem>> items = new EnumMap<>(DyeColor.class);
            for (DyeColor color : DyeColor.values()) {
                String name = kind.name(color);
                DeferredBlock<LightSwitchBlock> block = BLOCKS.register(name, kind::create);
                blocks.put(color, block);
                items.put(color, ITEMS.register(name, () -> new SwitchItem(block.get(), new Item.Properties(), kind.usageKey)));
            }
            SWITCHES.put(kind, blocks);
            SWITCH_ITEMS.put(kind, items);
        }
    }

    public static final DeferredBlock<LightSwitchBlock> LIGHT_SWITCH = SWITCHES.get(SwitchKind.LIGHT).get(DyeColor.WHITE);
    public static final DeferredBlock<LightSwitchBlock> DIMMER_SWITCH = SWITCHES.get(SwitchKind.DIMMER).get(DyeColor.WHITE);
    public static final DeferredItem<SwitchItem> LIGHT_SWITCH_ITEM = SWITCH_ITEMS.get(SwitchKind.LIGHT).get(DyeColor.WHITE);
    public static final DeferredItem<SwitchItem> DIMMER_SWITCH_ITEM = SWITCH_ITEMS.get(SwitchKind.DIMMER).get(DyeColor.WHITE);
    public static final DeferredItem<SwitchItem> DOUBLE_SWITCH_ITEM = SWITCH_ITEMS.get(SwitchKind.DOUBLE).get(DyeColor.WHITE);

    public static LightSwitchBlock switchBlock(SwitchKind kind, DyeColor color) {
        return SWITCHES.get(kind).get(color).get();
    }

    public static SwitchItem switchItem(SwitchKind kind, DyeColor color) {
        return SWITCH_ITEMS.get(kind).get(color).get();
    }

    /** Tous les interrupteurs (pour la block entity et les teintes). */
    public static List<LightSwitchBlock> allSwitches() {
        List<LightSwitchBlock> out = new ArrayList<>();
        SWITCHES.values().forEach(m -> m.values().forEach(b -> out.add(b.get())));
        return out;
    }
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
        if (type.isStrip()) {
            // forme qui dépend de la block entity (position de chaque bande) : pas de cache par état
            props = props.dynamicShape();
        }
        return switch (type.placement()) {
            case TALL -> new TallLampBlock(type, color, props);
            case OMNI -> type.isStrip() ? new LightStripBlock(type, color, props) : new OmniLampBlock(type, color, props);
            case CUBE -> new CubeLampBlock(type, color, props);
            default -> {
                LampShapes.Animation animation = LampShapes.animation(type.id());
                if (animation == null) {
                    yield new HorizontalLampBlock(type, color, props);
                }
                yield "fan".equals(animation.trigger()) ? new FanLampBlock(type, color, props) : new AnimatedLampBlock(type, color, props);
            }
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
