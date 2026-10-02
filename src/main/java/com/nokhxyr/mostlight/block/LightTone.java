package com.nokhxyr.mostlight.block;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.DyeColor;

/**
 * Teinte de la lumière. Teinte 1 = diffuseurs (verre, papier...), teinte 2 = ampoules et filaments.
 */
public enum LightTone implements StringRepresentable {
    /** Diffuseurs de la couleur de la lampe, ampoules chaudes. */
    AUTO("auto"),
    /** Tout de la couleur de la lampe. */
    COLORED("colored"),
    WARM("warm"),
    NEUTRAL("neutral"),
    COOL("cool");

    private static final int WARM_COLOR = 0xFFD49A;
    private static final int NEUTRAL_COLOR = 0xFFFFFF;
    private static final int COOL_COLOR = 0xC8DEFF;

    private final String id;

    LightTone(String id) {
        this.id = id;
    }

    public LightTone next() {
        return values()[(ordinal() + 1) % values().length];
    }

    /** Couleur RGB à appliquer à l'index de teinte donné (1 ou 2). */
    public int tint(DyeColor dye, int index) {
        return switch (this) {
            case AUTO -> index == 2 ? WARM_COLOR : lightened(dye);
            case COLORED -> lightened(dye);
            case WARM -> WARM_COLOR;
            case NEUTRAL -> NEUTRAL_COLOR;
            case COOL -> COOL_COLOR;
        };
    }

    public static int lightened(DyeColor dye) {
        int rgb = dye.getTextureDiffuseColor() & 0xFFFFFF;
        return (lighten(rgb >> 16 & 0xFF) << 16) | (lighten(rgb >> 8 & 0xFF) << 8) | lighten(rgb & 0xFF);
    }

    private static int lighten(int channel) {
        return channel + (255 - channel) * 35 / 100;
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
