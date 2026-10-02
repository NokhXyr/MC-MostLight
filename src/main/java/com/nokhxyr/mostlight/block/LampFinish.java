// Fichier généré par tools/generate.mjs - ne pas modifier à la main.
package com.nokhxyr.mostlight.block;

import net.minecraft.util.StringRepresentable;

/** Finition du cadre, appliquée par teinte (tintindex 3). */
public enum LampFinish implements StringRepresentable {
    STEEL("steel", 0xC4C9D1),
    BLACK("black", 0x45454D),
    BRASS("brass", 0xE0B862),
    COPPER("copper", 0xD9824A),
    GOLD("gold", 0xFFD84A),
    ROSE_GOLD("rose_gold", 0xEBA894),
    VERDIGRIS("verdigris", 0x6FB59B),
    WHITE("white", 0xF4F2EC),
    OAK("oak", 0xC79A5E),
    DARK_OAK("dark_oak", 0x6B4A2C);

    private final String id;
    private final int color;

    LampFinish(String id, int color) {
        this.id = id;
        this.color = color;
    }

    public int color() {
        return color;
    }

    public LampFinish next() {
        return values()[(ordinal() + 1) % values().length];
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
