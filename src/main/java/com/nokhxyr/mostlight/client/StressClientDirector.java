package com.nokhxyr.mostlight.client;

import com.mojang.logging.LogUtils;
import com.nokhxyr.mostlight.MostLight;
import com.nokhxyr.mostlight.stress.StressConfig;
import com.nokhxyr.mostlight.stress.StressDriver;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import org.slf4j.Logger;

/**
 * Test de charge côté client (./gradlew runStressClient) : monde plat, même scénario que le serveur sur le serveur
 * intégré (joueurs simulés visibles autour de la caméra), et mesure des images par seconde de chaque phase.
 * La caméra tourne lentement au-dessus du terrain pour tout avoir à l'écran. Inactif sans -Dmostlight.stress.client=true.
 */
@EventBusSubscriber(modid = MostLight.MOD_ID, value = Dist.CLIENT)
public final class StressClientDirector {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String WORLD = "mostlight_stress";

    private enum Stage { START, LOADING, RUNNING, LEAVING, DONE }

    private static Stage stage = Stage.START;
    private static int wait;
    private static long lastFrame;
    private static float yaw;
    private static final Map<String, LongArrayList> FRAMES = new LinkedHashMap<>();

    private StressClientDirector() {}

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        if (!StressConfig.CLIENT || stage == Stage.DONE) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        mc.options.getSoundSourceOptionInstance(SoundSource.MUSIC).set(0.0);
        mc.getMusicManager().stopPlaying();
        if (wait > 0) {
            wait--;
            return;
        }
        switch (stage) {
            case START -> {
                if (mc.getOverlay() != null) {
                    // resources still loading: big modpacks set up their screens at the end (Twilight Forest crashes otherwise)
                    wait = 20;
                    return;
                }
                if (!(mc.screen instanceof TitleScreen)) {
                    mc.options.onboardAccessibility = false;
                    mc.setScreen(new TitleScreen());
                    wait = 20;
                    return;
                }
                ShowcaseDirector.deleteWorld(mc, WORLD);
                mc.createWorldOpenFlows().createFreshLevel(WORLD,
                        new LevelSettings("MostLight Stress", GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                                new GameRules(), WorldDataConfiguration.DEFAULT),
                        new WorldOptions(20261003L, false, false),
                        access -> access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                        new TitleScreen());
                stage = Stage.LOADING;
            }
            case LOADING -> {
                MinecraftServer server = mc.getSingleplayerServer();
                if (mc.player == null || server == null || mc.player.tickCount < 40) {
                    return;
                }
                mc.options.hideGui = true;
                mc.options.pauseOnLostFocus = false;
                // réglages en mémoire seulement : distance de rendu 12, images non limitées, pas de synchro verticale
                mc.options.renderDistance().set(12);
                // -Dmostlight.stress.fps caps the frame rate so the client does not starve the integrated server
                mc.options.framerateLimit().set(Integer.getInteger("mostlight.stress.fps", 260));
                mc.options.enableVsync().set(false);
                var uuid = mc.player.getUUID();
                server.execute(() -> {
                    var level = server.overworld();
                    level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                    level.setDayTime(18000);
                    BlockPos center = new BlockPos(0, 0, 0);
                    ServerPlayer player = server.getPlayerList().getPlayer(uuid);
                    if (player != null) {
                        player.getAbilities().flying = true;
                        player.onUpdateAbilities();
                        player.teleportTo(level, 0.5, level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, 0, 0) + 30, 0.5, 0, 35);
                    }
                    StressDriver.start(server, level, center, StressConfig.fromProperties(true), mc.gameDirectory.toPath().resolve("stress"));
                });
                stage = Stage.RUNNING;
            }
            case RUNNING -> {
                if (mc.screen != null) {
                    mc.setScreen(null);
                }
                if (StressDriver.isFinished()) {
                    writeClientReport();
                    stage = Stage.LEAVING;
                }
            }
            case LEAVING -> {
                mc.options.hideGui = false;
                if (mc.level != null) {
                    mc.level.disconnect();
                }
                mc.disconnect(new TitleScreen());
                stage = Stage.DONE;
                mc.stop();
            }
            default -> {}
        }
    }

    /** Durée de chaque image, rangée par phase ; la caméra tourne d'un tour toutes les 30 s. */
    @SubscribeEvent
    static void onFrame(RenderFrameEvent.Post event) {
        if (!StressConfig.CLIENT || stage != Stage.RUNNING) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        long now = System.nanoTime();
        if (mc.player != null && mc.screen == null) {
            if (lastFrame != 0) {
                long dt = now - lastFrame;
                yaw = (yaw + dt / 1e9f * 12f) % 360f;
                mc.player.setYRot(yaw);
                mc.player.setYHeadRot(yaw);
                mc.player.setXRot(35);
                String phase = StressDriver.phase();
                if (!phase.isEmpty()) {
                    LongArrayList frames = FRAMES.computeIfAbsent(phase, k -> new LongArrayList());
                    frames.add(dt);
                    // une image du jeu par phase (contrôle de ce que voit la caméra)
                    if (frames.size() == 600) {
                        net.minecraft.client.Screenshot.grab(mc.gameDirectory, "stress_" + phase + ".png", mc.getMainRenderTarget(), message -> {});
                    }
                }
            }
        }
        lastFrame = now;
    }

    private static void writeClientReport() {
        StringBuilder md = new StringBuilder("\n## Images par seconde (client)\n\n");
        Minecraft mc = Minecraft.getInstance();
        md.append("Fenêtre ").append(mc.getWindow().getWidth()).append('x').append(mc.getWindow().getHeight())
                .append(", distance de rendu 12, sans limite d'images ni synchro verticale. GPU : ")
                .append(com.mojang.blaze3d.platform.GlUtil.getRenderer()).append("\n\n");
        md.append("| Phase | Images | FPS moyen | FPS médian | 1 % bas | Image la plus lente |\n|---|---|---|---|---|---|\n");
        FRAMES.forEach((phase, frames) -> {
            long[] sorted = frames.toLongArray();
            Arrays.sort(sorted);
            double sum = 0;
            for (long n : sorted) {
                sum += n;
            }
            double avg = sum / sorted.length;
            double median = sorted[sorted.length / 2];
            double p99 = sorted[Math.min(sorted.length - 1, (int) Math.ceil(0.99 * sorted.length) - 1)];
            md.append(String.format(Locale.ROOT, "| %s | %d | %.0f | %.0f | %.0f | %.1f ms |%n", phase, sorted.length,
                    1e9 / avg, 1e9 / median, 1e9 / p99, sorted[sorted.length - 1] / 1e6));
            LOGGER.info("[MostLight stress] client {} : {} FPS moyen, {} FPS 1 % bas", phase,
                    Math.round(1e9 / avg), Math.round(1e9 / p99));
        });
        Path report = StressDriver.report();
        if (report == null) {
            return;
        }
        try {
            Files.writeString(report, md.toString(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            LOGGER.error("[MostLight stress] impossible d'ajouter les FPS au rapport", e);
        }
    }
}
