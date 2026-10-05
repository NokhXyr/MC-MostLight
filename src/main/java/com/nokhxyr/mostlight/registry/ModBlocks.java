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
import com.nokhxyr.mostlight.block.RodLampBlock;
import com.nokhxyr.mostlight.block.TallLampBlock;
import com.nokhxyr.mostlight.crafting.LampWorkbenchBlock;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import com.nokhxyr.mostlight.item.ItemColor;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registers one block and one item per lamp model and per switch kind; the 16 colours are a block state property. */
public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MostLight.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MostLight.MOD_ID);

    private static final Map<LampType, DeferredBlock<LampBlock>> LAMPS = new EnumMap<>(LampType.class);
    private static final Map<LampType, DeferredItem<LampItem>> LAMP_ITEMS = new EnumMap<>(LampType.class);
    private static final List<DeferredBlock<LampBlock>> ALL = new ArrayList<>();

    static {
        for (LampType type : LampType.values()) {
            DeferredBlock<LampBlock> block = BLOCKS.register(type.id(), () -> create(type));
            LAMPS.put(type, block);
            LAMP_ITEMS.put(type, ITEMS.register(type.id(), () -> type.isStrip()
                    ? new LightStripItem(block.get(), new Item.Properties())
                    : new LampItem(block.get(), new Item.Properties())));
            ALL.add(block);
        }
    }

    /** Interrupteurs : simple, variateur, double (lumière + ventilateur), chacun en 16 couleurs (propriété color). */
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

        LightSwitchBlock create() {
            return switch (this) {
                case LIGHT -> new LightSwitchBlock(switchProperties());
                case DIMMER -> new DimmerSwitchBlock(switchProperties());
                case DOUBLE -> new DoubleSwitchBlock(switchProperties());
            };
        }
    }

    private static final Map<SwitchKind, DeferredBlock<LightSwitchBlock>> SWITCHES = new EnumMap<>(SwitchKind.class);
    private static final Map<SwitchKind, DeferredItem<SwitchItem>> SWITCH_ITEMS = new EnumMap<>(SwitchKind.class);

    static {
        for (SwitchKind kind : SwitchKind.values()) {
            DeferredBlock<LightSwitchBlock> block = BLOCKS.register(kind.id, kind::create);
            SWITCHES.put(kind, block);
            SWITCH_ITEMS.put(kind, ITEMS.register(kind.id, () -> new SwitchItem(block.get(), new Item.Properties(), kind.usageKey)));
        }
    }

    public static final DeferredBlock<LightSwitchBlock> LIGHT_SWITCH = SWITCHES.get(SwitchKind.LIGHT);
    public static final DeferredBlock<LightSwitchBlock> DIMMER_SWITCH = SWITCHES.get(SwitchKind.DIMMER);
    public static final DeferredItem<SwitchItem> LIGHT_SWITCH_ITEM = SWITCH_ITEMS.get(SwitchKind.LIGHT);
    public static final DeferredItem<SwitchItem> DIMMER_SWITCH_ITEM = SWITCH_ITEMS.get(SwitchKind.DIMMER);
    public static final DeferredItem<SwitchItem> DOUBLE_SWITCH_ITEM = SWITCH_ITEMS.get(SwitchKind.DOUBLE);

    public static LightSwitchBlock switchBlock(SwitchKind kind) {
        return SWITCHES.get(kind).get();
    }

    public static SwitchItem switchItem(SwitchKind kind) {
        return SWITCH_ITEMS.get(kind).get();
    }

    /** A switch item in one colour. */
    public static ItemStack switchStack(SwitchKind kind, DyeColor color) {
        return ItemColor.with(new ItemStack(switchItem(kind)), color);
    }

    /** Tous les interrupteurs (pour la block entity et les teintes). */
    public static List<LightSwitchBlock> allSwitches() {
        List<LightSwitchBlock> out = new ArrayList<>();
        SWITCHES.values().forEach(b -> out.add(b.get()));
        return out;
    }
    /** Établi de luminaire : toutes les recettes du mod s'y font. */
    public static final DeferredBlock<LampWorkbenchBlock> LAMP_WORKBENCH = BLOCKS.register("lamp_workbench",
            () -> new LampWorkbenchBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredItem<net.minecraft.world.item.BlockItem> LAMP_WORKBENCH_ITEM = ITEMS.registerSimpleBlockItem(LAMP_WORKBENCH);
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

    private static LampBlock create(LampType type) {
        BlockBehaviour.Properties props = BlockBehaviour.Properties.of()
                .mapColor(state -> state.getValue(LampBlock.COLOR).getMapColor())
                .strength(0.5F)
                .sound(type.sound())
                .lightLevel(type.placement() == Placement.TALL ? TallLampBlock::lightLevel : LampBlock::lightLevel);
        // aucune lampe ne cache ses voisins : les blocs lumineux laissent voir à travers (cadre, verre, papier)
        props = props.noOcclusion();
        if (type.placement() == Placement.TALL) {
            // deux moitiés : un piston n'en pousserait qu'une, la lampe casse (les contraptions de Create la déplacent entière)
            props = props.pushReaction(PushReaction.DESTROY);
        }
        if (type.isStrip()) {
            // forme qui dépend de la block entity (position de chaque bande) : pas de cache par état
            props = props.dynamicShape();
        }
        return switch (type.placement()) {
            case TALL -> new TallLampBlock(type, props);
            case OMNI -> type.isStrip() ? new LightStripBlock(type, props)
                    : LampShapes.hasLying(type.id()) ? new RodLampBlock(type, props) : new OmniLampBlock(type, props);
            case CUBE -> new CubeLampBlock(type, props);
            default -> {
                LampShapes.Animation animation = LampShapes.animation(type.id());
                if (animation == null) {
                    yield new HorizontalLampBlock(type, props);
                }
                yield "fan".equals(animation.trigger()) ? new FanLampBlock(type, props) : new AnimatedLampBlock(type, props);
            }
        };
    }

    public static LampBlock lamp(LampType type) {
        return LAMPS.get(type).get();
    }

    /** The lamp's default state in one colour. */
    public static BlockState state(LampType type, DyeColor color) {
        return lamp(type).defaultBlockState().setValue(LampBlock.COLOR, color);
    }

    public static LampItem item(LampType type) {
        return LAMP_ITEMS.get(type).get();
    }

    /** A lamp item in one colour. */
    public static ItemStack stack(LampType type, DyeColor color) {
        return ItemColor.with(new ItemStack(item(type)), color);
    }

    public static List<DeferredBlock<LampBlock>> all() {
        return Collections.unmodifiableList(ALL);
    }
}
