package com.nokhxyr.mostlight.stress;

/**
 * Réglages du test de charge (propriétés système, voir build.gradle) :
 * mostlight.stress (serveur), mostlight.stress.client (client), mostlight.stress.vanilla (référence vanilla),
 * mostlight.stress.bots (joueurs simulés), mostlight.stress.phase (secondes par phase).
 */
public record StressConfig(boolean client, boolean vanilla, int bots, int zoneSize, int spacing, int lattice, int phaseTicks) {
    public static final boolean SERVER = Boolean.getBoolean("mostlight.stress");
    public static final boolean CLIENT = Boolean.getBoolean("mostlight.stress.client");

    public static StressConfig fromProperties(boolean client) {
        int bots = setting("mostlight.stress.bots", "MOSTLIGHT_STRESS_BOTS", 20);
        int seconds = setting("mostlight.stress.phase", "MOSTLIGHT_STRESS_PHASE", client ? 20 : 45);
        boolean vanilla = Boolean.getBoolean("mostlight.stress.vanilla");
        // côté client tout doit rester dans la distance de rendu : zones plus petites et plus serrées
        return client
                ? new StressConfig(true, vanilla, bots, 40, 48, 16, seconds * 20)
                : new StressConfig(false, vanilla, bots, 64, 96, 32, seconds * 20);
    }

    /** Propriété système, sinon variable d'environnement (pratique avec gradlew), sinon valeur par défaut. */
    private static int setting(String property, String env, int fallback) {
        Integer value = Integer.getInteger(property);
        if (value != null) {
            return value;
        }
        try {
            String raw = System.getenv(env);
            return raw == null ? fallback : Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public String mode() {
        return (client ? "client" : "serveur") + (vanilla ? " / référence vanilla" : " / MostLight");
    }
}
