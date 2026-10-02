package com.nokhxyr.mostlight.block;

public enum LampCategory {
    CEILING("ceiling"),
    WALL("wall"),
    TABLE("table"),
    FLOOR("floor"),
    BLOCK("block");

    private final String id;

    LampCategory(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
