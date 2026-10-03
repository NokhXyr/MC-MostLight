// Fichier généré par tools/generate.mjs - ne pas modifier à la main.
package com.nokhxyr.mostlight.block;

import net.minecraft.world.level.block.SoundType;

public enum LampType {
    PENDANT_LAMP("pendant_lamp", LampCategory.CEILING, Placement.HANGING, SoundType.LANTERN, LampFinish.BLACK),
    CHANDELIER("chandelier", LampCategory.CEILING, Placement.HANGING, SoundType.METAL, LampFinish.BRASS),
    CRYSTAL_CHANDELIER("crystal_chandelier", LampCategory.CEILING, Placement.HANGING, SoundType.GLASS, LampFinish.GOLD),
    CEILING_LIGHT("ceiling_light", LampCategory.CEILING, Placement.HANGING, SoundType.GLASS, LampFinish.WHITE),
    PAPER_LANTERN("paper_lantern", LampCategory.CEILING, Placement.HANGING, SoundType.WOOL, LampFinish.BLACK),
    INDUSTRIAL_LAMP("industrial_lamp", LampCategory.CEILING, Placement.HANGING, SoundType.LANTERN, LampFinish.BLACK),
    GLOBE_PENDANT("globe_pendant", LampCategory.CEILING, Placement.HANGING, SoundType.GLASS, LampFinish.BRASS),
    HANGING_LANTERN("hanging_lantern", LampCategory.CEILING, Placement.HANGING, SoundType.LANTERN, LampFinish.BLACK),
    CLUSTER_PENDANT("cluster_pendant", LampCategory.CEILING, Placement.HANGING, SoundType.GLASS, LampFinish.BLACK),
    DOME_PENDANT("dome_pendant", LampCategory.CEILING, Placement.HANGING, SoundType.LANTERN, LampFinish.BLACK),
    RING_LIGHT("ring_light", LampCategory.CEILING, Placement.HANGING, SoundType.METAL, LampFinish.STEEL),
    MOROCCAN_LANTERN("moroccan_lantern", LampCategory.CEILING, Placement.HANGING, SoundType.LANTERN, LampFinish.BRASS),
    TRACK_LIGHT("track_light", LampCategory.CEILING, Placement.HANGING, SoundType.METAL, LampFinish.BLACK),
    FAN_LIGHT("fan_light", LampCategory.CEILING, Placement.HANGING, SoundType.WOOD, LampFinish.OAK),
    BULB_PENDANT("bulb_pendant", LampCategory.CEILING, Placement.HANGING, SoundType.GLASS, LampFinish.BRASS),
    WALL_SCONCE("wall_sconce", LampCategory.WALL, Placement.WALL, SoundType.LANTERN, LampFinish.BRASS),
    DOUBLE_SCONCE("double_sconce", LampCategory.WALL, Placement.WALL, SoundType.LANTERN, LampFinish.BRASS),
    WALL_LANTERN("wall_lantern", LampCategory.WALL, Placement.WALL, SoundType.LANTERN, LampFinish.BLACK),
    NEON_TUBE("neon_tube", LampCategory.WALL, Placement.WALL, SoundType.GLASS, LampFinish.STEEL),
    NEON_RING("neon_ring", LampCategory.WALL, Placement.WALL, SoundType.GLASS, LampFinish.STEEL),
    WALL_SPOT("wall_spot", LampCategory.WALL, Placement.WALL, SoundType.LANTERN, LampFinish.BLACK),
    BULKHEAD_LIGHT("bulkhead_light", LampCategory.WALL, Placement.WALL, SoundType.METAL, LampFinish.BLACK),
    WALL_TORCH_LAMP("wall_torch_lamp", LampCategory.WALL, Placement.WALL, SoundType.WOOD, LampFinish.DARK_OAK),
    WALL_CANDLE("wall_candle", LampCategory.WALL, Placement.WALL, SoundType.CANDLE, LampFinish.BRASS),
    PICTURE_LIGHT("picture_light", LampCategory.WALL, Placement.WALL, SoundType.METAL, LampFinish.BRASS),
    INDUSTRIAL_WALL_LAMP("industrial_wall_lamp", LampCategory.WALL, Placement.WALL, SoundType.LANTERN, LampFinish.BLACK),
    WALL_GLOBE("wall_globe", LampCategory.WALL, Placement.WALL, SoundType.GLASS, LampFinish.BRASS),
    WALL_UPLIGHT("wall_uplight", LampCategory.WALL, Placement.WALL, SoundType.METAL, LampFinish.STEEL),
    STRING_LIGHTS("string_lights", LampCategory.WALL, Placement.WALL, SoundType.GLASS, LampFinish.BLACK),
    GEAR_LAMP("gear_lamp", LampCategory.TABLE, Placement.STANDING, SoundType.METAL, LampFinish.BRASS),
    TABLE_LAMP("table_lamp", LampCategory.TABLE, Placement.STANDING, SoundType.WOOL, LampFinish.BRASS),
    DESK_LAMP("desk_lamp", LampCategory.TABLE, Placement.STANDING, SoundType.LANTERN, LampFinish.BLACK),
    LAVA_LAMP("lava_lamp", LampCategory.TABLE, Placement.STANDING, SoundType.GLASS, LampFinish.STEEL),
    MUSHROOM_LAMP("mushroom_lamp", LampCategory.TABLE, Placement.STANDING, SoundType.WOOL, LampFinish.OAK),
    BANKERS_LAMP("bankers_lamp", LampCategory.TABLE, Placement.STANDING, SoundType.GLASS, LampFinish.BRASS),
    TABLE_LANTERN("table_lantern", LampCategory.TABLE, Placement.STANDING, SoundType.LANTERN, LampFinish.BLACK),
    CANDLE_HOLDER("candle_holder", LampCategory.TABLE, Placement.STANDING, SoundType.CANDLE, LampFinish.BRASS),
    CANDLE_JAR("candle_jar", LampCategory.TABLE, Placement.STANDING, SoundType.GLASS, LampFinish.GOLD),
    ORB_LAMP("orb_lamp", LampCategory.TABLE, Placement.STANDING, SoundType.GLASS, LampFinish.WHITE),
    MOON_LAMP("moon_lamp", LampCategory.TABLE, Placement.STANDING, SoundType.STONE, LampFinish.OAK),
    TIFFANY_LAMP("tiffany_lamp", LampCategory.TABLE, Placement.STANDING, SoundType.GLASS, LampFinish.BRASS),
    SALT_LAMP("salt_lamp", LampCategory.TABLE, Placement.STANDING, SoundType.STONE, LampFinish.OAK),
    OIL_LAMP("oil_lamp", LampCategory.TABLE, Placement.STANDING, SoundType.GLASS, LampFinish.BRASS),
    SHOJI_LAMP("shoji_lamp", LampCategory.TABLE, Placement.STANDING, SoundType.WOOD, LampFinish.OAK),
    FLOOR_LAMP("floor_lamp", LampCategory.FLOOR, Placement.TALL, SoundType.WOOL, LampFinish.BLACK),
    ARC_LAMP("arc_lamp", LampCategory.FLOOR, Placement.TALL, SoundType.METAL, LampFinish.STEEL),
    STREET_LAMP("street_lamp", LampCategory.FLOOR, Placement.TALL, SoundType.LANTERN, LampFinish.BLACK),
    DOUBLE_STREET_LAMP("double_street_lamp", LampCategory.FLOOR, Placement.TALL, SoundType.LANTERN, LampFinish.BLACK),
    TRIPOD_LAMP("tripod_lamp", LampCategory.FLOOR, Placement.TALL, SoundType.WOOD, LampFinish.OAK),
    TORCHIERE("torchiere", LampCategory.FLOOR, Placement.TALL, SoundType.METAL, LampFinish.BRASS),
    PAPER_FLOOR_LAMP("paper_floor_lamp", LampCategory.FLOOR, Placement.TALL, SoundType.WOOL, LampFinish.OAK),
    STUDIO_LIGHT("studio_light", LampCategory.FLOOR, Placement.TALL, SoundType.METAL, LampFinish.BLACK),
    STONE_LANTERN("stone_lantern", LampCategory.FLOOR, Placement.TALL, SoundType.STONE, LampFinish.STEEL),
    BOLLARD_LIGHT("bollard_light", LampCategory.FLOOR, Placement.STANDING, SoundType.METAL, LampFinish.BLACK),
    GARDEN_LANTERN("garden_lantern", LampCategory.FLOOR, Placement.STANDING, SoundType.STONE, LampFinish.STEEL),
    PATH_LIGHT("path_light", LampCategory.FLOOR, Placement.STANDING, SoundType.METAL, LampFinish.BLACK),
    BRAZIER("brazier", LampCategory.FLOOR, Placement.STANDING, SoundType.METAL, LampFinish.BLACK),
    LAMP_BLOCK("lamp_block", LampCategory.BLOCK, Placement.CUBE, SoundType.GLASS, LampFinish.STEEL),
    FRAMED_LAMP("framed_lamp", LampCategory.BLOCK, Placement.CUBE, SoundType.METAL, LampFinish.BLACK),
    HONEYCOMB_LAMP("honeycomb_lamp", LampCategory.BLOCK, Placement.CUBE, SoundType.GLASS, LampFinish.GOLD),
    NEON_FRAME("neon_frame", LampCategory.BLOCK, Placement.CUBE, SoundType.GLASS, LampFinish.BLACK),
    SHOJI_BLOCK("shoji_block", LampCategory.BLOCK, Placement.CUBE, SoundType.WOOD, LampFinish.OAK),
    LAMP_PANEL("lamp_panel", LampCategory.BLOCK, Placement.OMNI, SoundType.GLASS, LampFinish.STEEL),
    LIGHT_STRIP("light_strip", LampCategory.BLOCK, Placement.OMNI, SoundType.GLASS, LampFinish.BLACK),
    SPOTLIGHT("spotlight", LampCategory.BLOCK, Placement.OMNI, SoundType.METAL, LampFinish.BLACK),
    FLUSH_LIGHT("flush_light", LampCategory.BLOCK, Placement.OMNI, SoundType.GLASS, LampFinish.WHITE),
    CRYSTAL_CLUSTER("crystal_cluster", LampCategory.BLOCK, Placement.OMNI, SoundType.AMETHYST, LampFinish.STEEL);

    private final String id;
    private final LampCategory category;
    private final Placement placement;
    private final SoundType sound;
    private final LampFinish defaultFinish;

    LampType(String id, LampCategory category, Placement placement, SoundType sound, LampFinish defaultFinish) {
        this.id = id;
        this.category = category;
        this.placement = placement;
        this.sound = sound;
        this.defaultFinish = defaultFinish;
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

    public LampFinish defaultFinish() {
        return defaultFinish;
    }
}
