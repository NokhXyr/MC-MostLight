package com.nokhxyr.mostlight.client;

import com.mojang.logging.LogUtils;
import com.nokhxyr.mostlight.MostLight;
import com.nokhxyr.mostlight.showcase.ShowcaseBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import org.slf4j.Logger;

/**
 * Mode développement (./gradlew runShowcase) : crée un monde plat, construit la galerie,
 * vérifie l'éclairage et prend des captures de jour puis de nuit, puis quitte.
 * Inactif sans la propriété système mostlight.showcase=true.
 */
@EventBusSubscriber(modid = MostLight.MOD_ID, value = Dist.CLIENT)
public final class ShowcaseDirector {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean ENABLED = Boolean.getBoolean("mostlight.showcase");
    private static final String WORLD = "mostlight_showcase";
    private static final BlockPos ORIGIN = new BlockPos(0, -60, 0);

    private enum Stage { START, LOADING, BUILDING, SHOOTING, LEAVING, DONE }

    private static Stage stage = Stage.START;
    private static int wait;
    private static int viewIndex;
    private static boolean night;
    private static final AtomicReference<ShowcaseBuilder.Result> RESULT = new AtomicReference<>();
    private static final java.util.concurrent.atomic.AtomicInteger CHECKED = new java.util.concurrent.atomic.AtomicInteger();
    private static final java.util.concurrent.atomic.AtomicInteger FAILURES = new java.util.concurrent.atomic.AtomicInteger();

    private ShowcaseDirector() {}

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        if (!ENABLED || stage == Stage.DONE) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        muteMusic(mc);
        if (wait > 0) {
            wait--;
            return;
        }
        switch (stage) {
            case START -> {
                if (!(mc.screen instanceof TitleScreen)) {
                    mc.options.onboardAccessibility = false;
                    mc.setScreen(new TitleScreen());
                    wait = 20;
                    return;
                }
                deleteOldWorld(mc);
                mc.createWorldOpenFlows().createFreshLevel(WORLD,
                        new LevelSettings("MostLight Showcase", GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                                new GameRules(), WorldDataConfiguration.DEFAULT),
                        new WorldOptions(20261002L, false, false),
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
                server.execute(() -> {
                    ServerLevel level = server.overworld();
                    GameRules rules = level.getGameRules();
                    rules.getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                    rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
                    rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
                    level.setDayTime(6000);
                    RESULT.set(ShowcaseBuilder.build(level, ORIGIN));
                });
                stage = Stage.BUILDING;
            }
            case BUILDING -> {
                if (RESULT.get() == null) {
                    return;
                }
                viewIndex = 0;
                teleport(mc, RESULT.get().views().get(0));
                stage = Stage.SHOOTING;
                wait = 200;
            }
            case SHOOTING -> {
                if (mc.screen != null) {
                    // une fenêtre en arrière-plan ouvre le menu pause : on le referme et on attend un peu
                    mc.setScreen(null);
                    wait = 10;
                    return;
                }
                List<ShowcaseBuilder.View> views = RESULT.get().views();
                ShowcaseBuilder.View current = views.get(viewIndex);
                if (night && current.row() != null) {
                    MinecraftServer server = mc.getSingleplayerServer();
                    server.execute(() -> {
                        List<ShowcaseBuilder.Check> row = RESULT.get().checks().stream()
                                .filter(c -> ShowcaseBuilder.inRow(c, current.row())).toList();
                        FAILURES.addAndGet(ShowcaseBuilder.verify(server.overworld(), row));
                        CHECKED.addAndGet(row.size());
                    });
                }
                String name = String.format("showcase_%02d_%s.png", viewIndex, night ? "night" : "day");
                Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), message -> {});
                viewIndex++;
                if (viewIndex >= views.size()) {
                    if (night) {
                        LOGGER.info("[MostLight showcase] vérification : {}/{} lampes OK (tenue, émission, lumière autour)",
                                CHECKED.get() - FAILURES.get(), CHECKED.get());
                        stage = Stage.LEAVING;
                        return;
                    }
                    night = true;
                    viewIndex = 0;
                    MinecraftServer server = mc.getSingleplayerServer();
                    server.execute(() -> {
                        ServerLevel level = server.overworld();
                        level.setDayTime(18000);
                    });
                }
                teleport(mc, views.get(viewIndex));
                wait = viewIndex <= 1 ? 120 : 40;
            }
            case LEAVING -> {
                LOGGER.info("[MostLight showcase] captures terminées");
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

    /** Pas de musique pendant les tests ; réglage en mémoire seulement (options.txt n'est pas modifié). */
    private static void muteMusic(Minecraft mc) {
        OptionInstance<Double> music = mc.options.getSoundSourceOptionInstance(SoundSource.MUSIC);
        if (music.get() > 0) {
            music.set(0.0);
        }
        mc.getMusicManager().stopPlaying();
    }

    private static void teleport(Minecraft mc, ShowcaseBuilder.View view) {
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null || mc.player == null) {
            return;
        }
        var uuid = mc.player.getUUID();
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player != null) {
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
                player.teleportTo(server.overworld(), view.x(), view.y(), view.z(), view.yaw(), view.pitch());
            }
        });
    }

    private static void deleteOldWorld(Minecraft mc) {
        Path dir = mc.gameDirectory.toPath().resolve("saves").resolve(WORLD);
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> files = Files.walk(dir)) {
            files.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException e) {
            LOGGER.warn("[MostLight showcase] impossible de supprimer l'ancien monde", e);
        }
    }
}
