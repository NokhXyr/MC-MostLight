// Fichier généré par tools/generate.mjs - ne pas modifier à la main.
package com.nokhxyr.mostlight.block;

import net.minecraft.world.level.block.SoundType;

public enum LampType {
    PENDANT_LAMP("pendant_lamp", LampCategory.CEILING, Placement.HANGING, SoundType.LANTERN),
    CHANDELIER("chandelier", LampCategory.CEILING, Placement.HANGING, SoundType.METAL),
    CEILING_LIGHT("ceiling_light", LampCategory.CEILING, Placement.HANGING, SoundType.GLASS),
    PAPER_LANTERN("paper_lantern", LampCategory.CEILING, Placement.HANGING, SoundType.WOOL),
    INDUSTRIAL_LAMP("industrial_lamp", LampCategory.CEILING, Placement.HANGING, SoundType.LANTERN),
    GLOBE_PENDANT("globe_pendant", LampCategory.CEILING, Placement.HANGING, SoundType.GLASS),
    HANGING_LANTERN("hanging_lantern", LampCategory.CEILING, Placement.HANGING, SoundType.LANTERN),
    WALL_SCONCE("wall_sconce", LampCategory.WALL, Placement.WALL, SoundType.LANTERN),
    WALL_LANTERN("wall_lantern", LampCategory.WALL, Placement.WALL, SoundType.LANTERN),
    NEON_TUBE("neon_tube", LampCategory.WALL, Placement.WALL, SoundType.GLASS),
    WALL_SPOT("wall_spot", LampCategory.WALL, Placement.WALL, SoundType.LANTERN),
    BULKHEAD_LIGHT("bulkhead_light", LampCategory.WALL, Placement.WALL, SoundType.METAL),
    WALL_TORCH_LAMP("wall_torch_lamp", LampCategory.WALL, Placement.WALL, SoundType.WOOD),
    TABLE_LAMP("table_lamp", LampCategory.TABLE, Placement.STANDING, SoundType.WOOL),
    DESK_LAMP("desk_lamp", LampCategory.TABLE, Placement.STANDING, SoundType.LANTERN),
    LAVA_LAMP("lava_lamp", LampCategory.TABLE, Placement.STANDING, SoundType.GLASS),
    MUSHROOM_LAMP("mushroom_lamp", LampCategory.TABLE, Placement.STANDING, SoundType.WOOL),
    BANKERS_LAMP("bankers_lamp", LampCategory.TABLE, Placement.STANDING, SoundType.GLASS),
    TABLE_LANTERN("table_lantern", LampCategory.TABLE, Placement.STANDING, SoundType.LANTERN),
    CANDLE_HOLDER("candle_holder", LampCategory.TABLE, Placement.STANDING, SoundType.CANDLE),
    FLOOR_LAMP("floor_lamp", LampCategory.FLOOR, Placement.TALL, SoundType.WOOL),
    ARC_LAMP("arc_lamp", LampCategory.FLOOR, Placement.TALL, SoundType.METAL),
    STREET_LAMP("street_lamp", LampCategory.FLOOR, Placement.TALL, SoundType.LANTERN),
    TRIPOD_LAMP("tripod_lamp", LampCategory.FLOOR, Placement.TALL, SoundType.WOOD),
    BOLLARD_LIGHT("bollard_light", LampCategory.FLOOR, Placement.STANDING, SoundType.METAL),
    GARDEN_LANTERN("garden_lantern", LampCategory.FLOOR, Placement.STANDING, SoundType.STONE),
    LAMP_BLOCK("lamp_block", LampCategory.BLOCK, Placement.CUBE, SoundType.GLASS),
    FRAMED_LAMP("framed_lamp", LampCategory.BLOCK, Placement.CUBE, SoundType.METAL),
    LAMP_PANEL("lamp_panel", LampCategory.BLOCK, Placement.OMNI, SoundType.GLASS),
    LIGHT_STRIP("light_strip", LampCategory.BLOCK, Placement.OMNI, SoundType.GLASS),
    SPOTLIGHT("spotlight", LampCategory.BLOCK, Placement.OMNI, SoundType.METAL),
    FLUSH_LIGHT("flush_light", LampCategory.BLOCK, Placement.OMNI, SoundType.GLASS);

    private final String id;
    private final LampCategory category;
    private final Placement placement;
    private final SoundType sound;

    LampType(String id, LampCategory category, Placement placement, SoundType sound) {
        this.id = id;
        this.category = category;
        this.placement = placement;
        this.sound = sound;
    }

    public String id() {
        return id;
    }

    public LampCategory category() {
        return category;
    }

    public Placement placement() {
        return placement;
    }

    public SoundType sound() {
        return sound;
    }
}
