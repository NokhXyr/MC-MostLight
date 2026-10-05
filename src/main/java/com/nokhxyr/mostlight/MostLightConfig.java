package com.nokhxyr.mostlight;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Réglages du mod : config/mostlight-common.toml (créé au premier lancement). */
public final class MostLightConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue LED_CHAIN_LENGTH;
    public static final ModConfigSpec.IntValue SWITCH_MAX_LINKS;
    public static final ModConfigSpec.IntValue SWITCH_RANGE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("leds");
        LED_CHAIN_LENGTH = builder
                .comment("Nombre maximal de bandes LED reliées dans une même chaîne redstone / Max LED strips in one redstone chain")
                .defineInRange("maxChainLength", 256, 1, 4096);
        builder.pop();
        builder.push("switches");
        SWITCH_MAX_LINKS = builder
                .comment("Nombre maximal de lampes liées à un interrupteur ou une télécommande / Max lamps linked to a switch or remote")
                .defineInRange("maxLinks", 64, 1, com.nokhxyr.mostlight.component.ModComponents.MAX_LINKS);
        SWITCH_RANGE = builder
                .comment("Portée maximale (en blocs) entre un interrupteur et ses lampes / Max distance in blocks between a switch and its lamps")
                .defineInRange("range", 64, 8, 1024);
        builder.pop();
        SPEC = builder.build();
    }

    private MostLightConfig() {}

    /** Valeur lue sans planter si la config n'est pas encore chargée (tests, tout début du chargement). */
    public static int get(ModConfigSpec.IntValue value) {
        try {
            return value.get();
        } catch (IllegalStateException e) {
            return value.getDefault();
        }
    }
}
