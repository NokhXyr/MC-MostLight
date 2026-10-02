package com.nokhxyr.mostlight.block;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Données dérivées des modèles par tools/generate.mjs (en pixels, orientation de référence),
 * lues depuis mostlight_shapes.json.
 */
final class GeneratedShapes {
    /** Boîtes de collision : {x1, y1, z1, x2, y2, z2}. */
    static final Map<String, double[][]> BOXES = new HashMap<>();
    /** Points de flamme : {x, y, z, grande (1) ou petite (0)}. */
    static final Map<String, double[][]> FLAMES = new HashMap<>();

    static {
        try (InputStream in = GeneratedShapes.class.getResourceAsStream("/mostlight_shapes.json")) {
            if (in == null) {
                throw new IllegalStateException("mostlight_shapes.json introuvable : lancer node tools/generate.mjs");
            }
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            read(root.getAsJsonObject("boxes"), BOXES);
            read(root.getAsJsonObject("flames"), FLAMES);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private GeneratedShapes() {}

    private static void read(JsonObject source, Map<String, double[][]> target) {
        for (Map.Entry<String, JsonElement> entry : source.entrySet()) {
            JsonArray list = entry.getValue().getAsJsonArray();
            double[][] values = new double[list.size()][];
            for (int i = 0; i < list.size(); i++) {
                JsonArray numbers = list.get(i).getAsJsonArray();
                values[i] = new double[numbers.size()];
                for (int j = 0; j < numbers.size(); j++) {
                    values[i][j] = numbers.get(j).getAsDouble();
                }
            }
            target.put(entry.getKey(), values);
        }
    }
}
