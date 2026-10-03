package com.nokhxyr.mostlight.stress;

import com.mojang.logging.LogUtils;
import com.nokhxyr.mostlight.MostLight;
import com.nokhxyr.mostlight.block.LampBlock;
import com.nokhxyr.mostlight.block.LampType;
import com.nokhxyr.mostlight.block.TallLampBlock;
import com.nokhxyr.mostlight.block.entity.LampBlockEntity;
import com.nokhxyr.mostlight.component.ModComponents;
import com.nokhxyr.mostlight.registry.ModBlocks;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntConsumer;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

/**
 * Test de charge : construit le terrain, connecte des joueurs simulés qui jouent tous en même temps (allumer, poser,
 * casser, teindre, clé, interrupteurs, télécommande), lance des horloges redstone et des tempêtes d'allumage,
 * fait voyager les joueurs (déchargement / rechargement des chunks), vérifie la lumière et l'intégrité, puis écrit
 * un rapport (stress/rapport-*.md). Chaque phase mesure le temps de tick, le réseau, la mémoire et le GC.
 */
@EventBusSubscriber(modid = MostLight.MOD_ID)
public final class StressDriver {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static volatile StressDriver active;
    private static volatile String currentPhase = "";
    private static volatile boolean finished;
    private static volatile Path reportFile;

    private final MinecraftServer server;
    private final ServerLevel level;
    private final StressConfig config;
    private final StressField field;
    private final PacketMeter meter;
    private final Path dir;
    private final RandomSource random = RandomSource.create(42L);
    private final List<StressBot> bots = new ArrayList<>();
    private final List<Phase> phases = new ArrayList<>();
    private final Map<String, Integer> errors = new LinkedHashMap<>();
    private final Map<String, int[]> actionStats = new LinkedHashMap<>();
    private final List<String> notes = new ArrayList<>();
    private final List<StressField.Lamp> clockLamps = new ArrayList<>();
    private final LongArrayList stormLoops = new LongArrayList();
    /** Lampes cassées par les joueurs, reposées ensuite (le terrain garde sa taille). */
    private final Map<StressField.Zone, java.util.ArrayDeque<StressField.Lamp>> broken = new java.util.HashMap<>();
    private int phaseIndex = -1;
    private int phaseTick;
    private long tickStart;
    private long nextTick;
    private boolean clockState;
    private Deque<Runnable> buildSteps;
    private int lightWait;
    private int lightStep;

    /** Une phase : nom, durée (ou condition de fin), réglages, et ses mesures. */
    private final class Phase {
        final String id;
        final String title;
        final int ticks;
        final Runnable setup;
        final IntConsumer perTick;
        final java.util.function.BooleanSupplier until;
        final LongArrayList tickNanos = new LongArrayList();
        long wallStart;
        long wallEnd;
        PacketMeter.Snapshot netStart;
        PacketMeter.Snapshot netEnd;
        long gcTimeStart;
        long gcCountStart;
        long gcTime;
        long gcCount;
        long heapMax;
        int chunksMax;
        int botsActive;
        int actionsOk;
        int actionsFail;
        int lampsAtEnd = -1;

        Phase(String id, String title, int ticks, Runnable setup, IntConsumer perTick, java.util.function.BooleanSupplier until) {
            this.id = id;
            this.title = title;
            this.ticks = ticks;
            this.setup = setup;
            this.perTick = perTick;
            this.until = until;
        }
    }

    private StressDriver(MinecraftServer server, ServerLevel level, BlockPos center, StressConfig config, Path dir) {
        this.server = server;
        this.level = level;
        this.config = config;
        this.dir = dir;
        this.meter = new PacketMeter(server.registryAccess());
        this.field = new StressField(level, center, config);
        definePhases();
    }

    public static void start(MinecraftServer server, ServerLevel level, BlockPos center, StressConfig config, Path dir) {
        if (active != null) {
            return;
        }
        finished = false;
        StressDriver driver = new StressDriver(server, level, center, config, dir);
        LOGGER.info("[MostLight stress] démarrage ({}) : {} joueurs simulés, phases de {} s", config.mode(), config.bots(), config.phaseTicks() / 20);
        active = driver;
    }

    public static boolean isFinished() {
        return finished;
    }

    public static String phase() {
        return currentPhase;
    }

    public static Path report() {
        return reportFile;
    }

    @SubscribeEvent
    static void onTickStart(ServerTickEvent.Pre event) {
        StressDriver driver = active;
        if (driver != null && driver.server == event.getServer()) {
            driver.tickStart = System.nanoTime();
            try {
                driver.tick();
            } catch (Throwable e) {
                driver.error("pilote", e);
            }
        }
    }

    @SubscribeEvent
    static void onTickEnd(ServerTickEvent.Post event) {
        StressDriver driver = active;
        if (driver != null && driver.server == event.getServer() && driver.phaseIndex >= 0 && driver.phaseIndex < driver.phases.size()) {
            driver.phases.get(driver.phaseIndex).tickNanos.add(System.nanoTime() - driver.tickStart);
        }
        if (driver != null && driver.server == event.getServer() && !driver.config.client()) {
            driver.pace();
        }
    }

    /**
     * Le serveur de GameTest enchaîne les ticks sans attendre : on rétablit le rythme d'un vrai serveur
     * (un tick toutes les 50 ms, tâches traitées pendant l'attente, rattrapage après un tick long, abandon
     * du retard au-delà de 2 s comme « Can't keep up »).
     */
    private void pace() {
        long now = System.nanoTime();
        nextTick = nextTick == 0 ? now + 50_000_000L : nextTick + 50_000_000L;
        if (now - nextTick > 2_000_000_000L) {
            nextTick = now;
            return;
        }
        while (System.nanoTime() < nextTick) {
            if (!server.pollTask()) {
                java.util.concurrent.locks.LockSupport.parkNanos(Math.min(nextTick - System.nanoTime(), 500_000L));
            }
        }
    }

    // ------------------------------------------------------------------ phases

    private void definePhases() {
        int t = config.phaseTicks();
        phases.add(new Phase("baseline", "Monde vide, aucun joueur", 600, () -> {
            GameRules rules = level.getGameRules();
            rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
            rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
            if (!config.client()) {
                server.getPlayerList().setViewDistance(12);
                server.getPlayerList().setSimulationDistance(10);
            }
        }, tick -> {}, null));
        phases.add(new Phase("build", "Construction du terrain (une zone par tick)", 6000, () -> {
            clearLeftovers();
            field.chunks.forEach(c -> level.setChunkForced(c.x, c.z, true));
            buildSteps = field.buildSteps(level);
            lightWait = 0;
        }, tick -> {
            if (!buildSteps.isEmpty()) {
                buildSteps.poll().run();
            } else {
                lightWait++;
            }
        }, () -> buildSteps.isEmpty() && lightWait >= 20 && !lightBusy()));
        phases.add(new Phase("idle", "Terrain chargé, aucun joueur", t, this::prepareClocks, tick -> {}, null));
        phases.add(new Phase("bots_1", "1 joueur actif", t, () -> spawnBots(1), this::playTick, null));
        phases.add(new Phase("bots_10", Math.min(10, config.bots()) + " joueurs actifs", t, () -> spawnBots(Math.min(10, config.bots())), this::playTick, null));
        phases.add(new Phase("bots_all", config.bots() + " joueurs actifs", t, () -> spawnBots(config.bots()), this::playTick, null));
        phases.add(new Phase("redstone", config.bots() + " joueurs + horloges redstone (10 % des lampes, 1 Hz)", t, () -> {}, tick -> {
            playTick(tick);
            if (tick % 20 == 0) {
                toggleClocks();
            }
        }, null));
        phases.add(new Phase("storm", config.bots() + " joueurs + tempête : toutes les lampes basculent toutes les 5 s", t, () -> {}, tick -> {
            playTick(tick);
            if (tick % 100 == 0) {
                long start = System.nanoTime();
                int changed = 0;
                for (StressField.Zone zone : field.zones) {
                    for (StressField.Lamp lamp : zone.lamps) {
                        changed += StressField.setLit(level, lamp.pos(), null) ? 1 : 0;
                    }
                }
                for (StressField.Lamp lamp : field.lattice) {
                    changed += StressField.setLit(level, lamp.pos(), null) ? 1 : 0;
                }
                stormLoops.add(System.nanoTime() - start);
                notes.add(String.format(Locale.ROOT, "Tempête : %d lampes basculées en %.1f ms", changed, (System.nanoTime() - start) / 1e6));
            }
        }, null));
        phases.add(new Phase("light", "Contrôle de la lumière du cube : tout éteint puis tout allumé", 2400, () -> lightStep = 0,
                this::lightCheckTick, () -> lightStep >= 4));
        phases.add(new Phase("travel_out", "Les joueurs partent explorer loin (chunks déchargés et sauvegardés)", t, () -> {
            field.chunks.forEach(c -> level.setChunkForced(c.x, c.z, false));
            for (int i = 0; i < bots.size(); i++) {
                StressBot bot = bots.get(i);
                bot.teleportTo(level, 6000 + i * 400, field.ground + 20, 6000 + i * 400, -90, 0);
            }
        }, tick -> {
            for (StressBot bot : bots) {
                // vol rapide vers l'est : génération de nouveaux chunks en continu
                bot.moveTo(bot.getX() + 1.0, bot.getY(), bot.getZ(), -90);
            }
        }, null));
        phases.add(new Phase("travel_back", "Retour des joueurs (chunks rechargés depuis le disque)", t, () -> {
            for (int i = 0; i < bots.size(); i++) {
                BlockPos c = field.zoneCenter(field.zones.get(i));
                bots.get(i).teleportTo(level, c.getX(), field.ground + 8, c.getZ(), 0, 20);
            }
        }, this::playTick, null));
        phases.add(new Phase("integrity", "Rechargement complet et contrôle d'intégrité", 2400, () -> {
            field.chunks.forEach(c -> level.setChunkForced(c.x, c.z, true));
            lightWait = 0;
        }, tick -> lightWait++, () -> lightWait >= 60 && !lightBusy()));
        phases.add(new Phase("save", "Sauvegarde complète du monde", 20, this::timedSave, tick -> {}, null));
    }

    private void tick() {
        if (phaseIndex < 0) {
            nextPhase();
        }
        Phase phase = phases.get(phaseIndex);
        phase.perTick.accept(phaseTick);
        phaseTick++;
        if (phaseTick % 20 == 0) {
            sample(phase);
        }
        boolean done = phase.until != null ? phase.until.getAsBoolean() || phaseTick >= phase.ticks : phaseTick >= phase.ticks;
        if (done) {
            endPhase(phase);
            if (phaseIndex + 1 >= phases.size()) {
                finish();
            } else {
                nextPhase();
            }
        }
    }

    private void nextPhase() {
        phaseIndex++;
        phaseTick = 0;
        Phase phase = phases.get(phaseIndex);
        currentPhase = phase.id;
        LOGGER.info("[MostLight stress] phase {} : {}", phase.id, phase.title);
        phase.setup.run();
        phase.netStart = meter.snapshot();
        long[] gc = gc();
        phase.gcTimeStart = gc[0];
        phase.gcCountStart = gc[1];
        phase.wallStart = System.nanoTime();
    }

    private void endPhase(Phase phase) {
        phase.wallEnd = System.nanoTime();
        meter.drain();
        phase.netEnd = meter.snapshot();
        long[] gc = gc();
        phase.gcTime = gc[0] - phase.gcTimeStart;
        phase.gcCount = gc[1] - phase.gcCountStart;
        phase.botsActive = bots.size();
        sample(phase);
        if (!phase.id.equals("baseline") && !phase.id.equals("travel_out")) {
            phase.lampsAtEnd = countLamps();
        }
        double[] s = stats(phase.tickNanos);
        LOGGER.info("[MostLight stress] fin {} : MSPT moy {} / p95 {} / p99 {} / max {} ms, TPS {}, {} joueurs",
                phase.id, f1(s[0]), f1(s[2]), f1(s[3]), f1(s[4]), f1(tps(phase)), bots.size());
    }

    private void sample(Phase phase) {
        Runtime rt = Runtime.getRuntime();
        phase.heapMax = Math.max(phase.heapMax, rt.totalMemory() - rt.freeMemory());
        phase.chunksMax = Math.max(phase.chunksMax, level.getChunkSource().getLoadedChunksCount());
    }

    private static long[] gc() {
        long time = 0;
        long count = 0;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            time += Math.max(0, bean.getCollectionTime());
            count += Math.max(0, bean.getCollectionCount());
        }
        return new long[] {time, count};
    }

    private boolean lightBusy() {
        return level.getChunkSource().getLightEngine().hasLightWork();
    }

    // ------------------------------------------------------------------ joueurs

    private void spawnBots(int target) {
        while (bots.size() < target && bots.size() < field.zones.size()) {
            int i = bots.size();
            BlockPos c = field.zoneCenter(field.zones.get(i));
            String name = String.format(Locale.ROOT, "MLBot%02d", i + 1);
            try {
                bots.add(StressBot.join(server, level, name, meter, c.getX() + 0.5, field.ground + 8, c.getZ() + 0.5));
            } catch (Throwable e) {
                error("connexion d'un joueur", e);
                return;
            }
        }
    }

    /** Un tick de jeu : chaque joueur se déplace et fait une action. */
    private void playTick(int tick) {
        for (int i = 0; i < bots.size(); i++) {
            StressBot bot = bots.get(i);
            StressField.Zone zone = field.zones.get(i);
            BlockPos c = field.zoneCenter(zone);
            double radius = config.zoneSize() / 2.0 - 2;
            double angle = (tick + i * 37) * 0.5 / radius;
            double x = c.getX() + 0.5 + radius * Math.cos(angle);
            double z = c.getZ() + 0.5 + radius * Math.sin(angle);
            double y = field.ground + 6 + 2 * Math.sin(angle * 3);
            bot.moveTo(x, y, z, (float) Math.toDegrees(angle) + 180);
            try {
                act(bot, zone);
            } catch (Throwable e) {
                error("action d'un joueur", e);
                count("erreur", false);
            }
        }
    }

    private void act(StressBot bot, StressField.Zone zone) {
        if (zone.lamps.isEmpty()) {
            return;
        }
        StressField.Lamp target = zone.lamps.get(random.nextInt(zone.lamps.size()));
        BlockPos pos = target.pos();
        int roll = random.nextInt(100);
        if (config.vanilla()) {
            // même charge de travail, actions possibles avec des ampoules en cuivre
            if (roll < 52) {
                count("basculer", StressField.setLit(level, pos, null));
            } else if (roll < 64) {
                StressField.Lamp hole = brokenOf(zone).poll();
                count("poser", hole != null && replace(bot, hole, new ItemStack(Items.WAXED_COPPER_BULB)));
            } else if (roll < 76) {
                breakLamp(bot, zone, target);
            } else {
                // équivalent d'un interrupteur / télécommande : 64 lampes d'un coup
                int n = 0;
                for (int k = 0; k < 64; k++) {
                    n += StressField.setLit(level, zone.lamps.get(random.nextInt(zone.lamps.size())).pos(), null) ? 1 : 0;
                }
                count("groupe de 64", n > 0);
            }
            return;
        }
        if (roll < 30) {
            count("allumer/éteindre", use(bot, ItemStack.EMPTY, pos, false));
        } else if (roll < 38) {
            count("luminosité (accroupi)", use(bot, ItemStack.EMPTY, pos, true));
        } else if (roll < 52) {
            StressField.Lamp hole = brokenOf(zone).poll();
            if (hole == null) {
                count("poser", false);
            } else {
                // un modèle au hasard parmi ceux qui se fixent pareil (plafond, mur, sol...)
                List<LampType> types = Arrays.stream(LampType.values()).filter(t -> t.placement() == hole.placement()).toList();
                LampType type = types.get(random.nextInt(types.size()));
                count("poser", replace(bot, hole, new ItemStack(ModBlocks.item(type, DyeColor.values()[random.nextInt(16)]))));
            }
        } else if (roll < 64) {
            breakLamp(bot, zone, target);
        } else if (roll < 74) {
            count("teindre", use(bot, new ItemStack(DyeItem.byColor(DyeColor.values()[random.nextInt(16)])), pos, false));
        } else if (roll < 82) {
            count("clé de décorateur", use(bot, new ItemStack(ModBlocks.DESIGNER_WRENCH.get()), pos, random.nextBoolean()));
        } else if (roll < 90) {
            BlockPos button = zone.switches.get(random.nextInt(zone.switches.size()));
            count("interrupteur (64 lampes)", use(bot, ItemStack.EMPTY, button, random.nextInt(4) == 0));
        } else if (roll < 95) {
            ItemStack remote = new ItemStack(ModBlocks.LAMP_REMOTE.get());
            remote.set(ModComponents.LINKS.get(), List.copyOf(zone.remoteLinks));
            bot.setItemInHand(InteractionHand.MAIN_HAND, remote);
            bot.setShiftKeyDown(random.nextInt(4) == 0);
            InteractionResult r = bot.gameMode.useItem(bot, level, remote, InteractionHand.MAIN_HAND);
            bot.setShiftKeyDown(false);
            count("télécommande (64 lampes)", r.consumesAction());
        } else {
            count("lier (interrupteur en main)", use(bot, new ItemStack(ModBlocks.LIGHT_SWITCH_ITEM.get()), pos, true));
        }
    }

    /** Clic droit sur un bloc, par le même chemin qu'un paquet d'utilisation d'objet. */
    private boolean use(StressBot bot, ItemStack stack, BlockPos pos, boolean sneak) {
        if (level.getBlockState(pos).isAir()) {
            return false;
        }
        bot.setItemInHand(InteractionHand.MAIN_HAND, stack);
        bot.setShiftKeyDown(sneak);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.5, 0), Direction.UP, pos, false);
        InteractionResult result = bot.gameMode.useItemOn(bot, level, stack, InteractionHand.MAIN_HAND, hit);
        bot.setShiftKeyDown(false);
        return result.consumesAction();
    }

    private java.util.ArrayDeque<StressField.Lamp> brokenOf(StressField.Zone zone) {
        return broken.computeIfAbsent(zone, z -> new java.util.ArrayDeque<>());
    }

    private void breakLamp(StressBot bot, StressField.Zone zone, StressField.Lamp target) {
        boolean ok = !level.getBlockState(target.pos()).isAir() && bot.gameMode.destroyBlock(target.pos());
        if (ok) {
            brokenOf(zone).add(target);
        }
        count("casser", ok);
    }

    /** Repose une lampe cassée contre son support d'origine, comme un clic droit du joueur sur ce support. */
    private boolean replace(StressBot bot, StressField.Lamp hole, ItemStack item) {
        BlockPos pos = hole.pos();
        if (!level.getBlockState(pos).isAir()) {
            return false;
        }
        BlockPos support = hole.support() != null ? hole.support() : pos.below();
        Direction face = Direction.getNearest(pos.getX() - support.getX(), pos.getY() - support.getY(), pos.getZ() - support.getZ());
        bot.setItemInHand(InteractionHand.MAIN_HAND, item);
        Vec3 hitVec = Vec3.atCenterOf(support).relative(face, 0.5);
        InteractionResult result = bot.gameMode.useItemOn(bot, level, item, InteractionHand.MAIN_HAND, new BlockHitResult(hitVec, face, support, false));
        return result.consumesAction();
    }

    private void count(String action, boolean ok) {
        int[] c = actionStats.computeIfAbsent(action, k -> new int[2]);
        c[ok ? 0 : 1]++;
        Phase phase = phases.get(phaseIndex);
        if (ok) {
            phase.actionsOk++;
        } else {
            phase.actionsFail++;
        }
    }

    // ------------------------------------------------------------------ redstone, lumière, intégrité

    /**
     * Les autres GameTests effacent leur zone sans mises à jour : des moitiés de lampes hautes tombent en objets
     * (comme une porte vanilla). Ces objets tourneraient pendant toute la mesure : on les retire avant.
     */
    private void clearLeftovers() {
        int removed = 0;
        for (net.minecraft.world.entity.Entity entity : level.getAllEntities()) {
            if (entity instanceof net.minecraft.world.entity.item.ItemEntity) {
                entity.discard();
                removed++;
            }
        }
        notes.add(removed + " objets laissés au sol par les autres tests automatiques retirés avant la mesure");
    }

    private void prepareClocks() {
        clockLamps.clear();
        for (StressField.Zone zone : field.zones) {
            for (StressField.Lamp lamp : zone.lamps) {
                if (lamp.support() != null && random.nextInt(10) == 0) {
                    clockLamps.add(lamp);
                }
            }
        }
        notes.add(field.lampCount() + " lampes posées (" + field.lattice.size() + " dans le cube), "
                + clockLamps.size() + " reliées aux horloges redstone");
    }

    /** Horloge : le support de chaque lampe reliée devient un bloc de redstone, puis redevient normal. */
    private void toggleClocks() {
        clockState = !clockState;
        for (StressField.Lamp lamp : clockLamps) {
            BlockState current = level.getBlockState(lamp.support());
            if (clockState && !current.is(Blocks.REDSTONE_BLOCK)) {
                level.setBlock(lamp.support(), Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
            } else if (!clockState && current.is(Blocks.REDSTONE_BLOCK)) {
                level.setBlock(lamp.support(), lamp.supportState(), Block.UPDATE_ALL);
            }
        }
    }

    /** Éteint tout le cube, attend la fin du calcul de lumière, vérifie 0 partout ; puis l'inverse avec 15. */
    private void lightCheckTick(int tick) {
        if (clockState) {
            toggleClocks();
        }
        switch (lightStep) {
            case 0, 2 -> {
                boolean on = lightStep == 2;
                long start = System.nanoTime();
                for (StressField.Lamp lamp : field.lattice) {
                    StressField.setLit(level, lamp.pos(), on);
                }
                notes.add(String.format(Locale.ROOT, "Cube : %d lampes %s en %.1f ms", field.lattice.size(),
                        on ? "allumées" : "éteintes", (System.nanoTime() - start) / 1e6));
                lightWait = 0;
                lightStep++;
            }
            case 1, 3 -> {
                lightWait++;
                if (lightWait >= 20 && !lightBusy()) {
                    int expected = lightStep == 3 ? 15 : 0;
                    int wrong = 0;
                    for (StressField.Lamp lamp : field.lattice) {
                        if (level.getBrightness(LightLayer.BLOCK, lamp.pos()) != expected) {
                            wrong++;
                        }
                    }
                    notes.add(String.format(Locale.ROOT, "Lumière du cube %s : stabilisée en %d ticks, %d/%d positions incorrectes (attendu %d)",
                            expected == 0 ? "éteint" : "allumé", lightWait, wrong, field.lattice.size(), expected));
                    if (wrong > 0) {
                        errors.merge("lumière incorrecte dans le cube (" + (expected == 0 ? "éteint" : "allumé") + ")", wrong, Integer::sum);
                    }
                    lightStep++;
                }
            }
            default -> {}
        }
    }

    private int countLamps() {
        int n = 0;
        for (StressField.Zone zone : field.zones) {
            for (StressField.Lamp lamp : zone.lamps) {
                n += isLamp(level.getBlockState(lamp.pos())) ? 1 : 0;
            }
        }
        for (StressField.Lamp lamp : field.lattice) {
            n += isLamp(level.getBlockState(lamp.pos())) ? 1 : 0;
        }
        return n;
    }

    private static boolean isLamp(BlockState state) {
        return state.getBlock() instanceof LampBlock || state.is(Blocks.WAXED_COPPER_BULB);
    }

    /** Après rechargement : block entities présentes, émission conforme à l'état, lumière au moins égale à l'émission. */
    private void integrity() {
        int checked = 0;
        int missingEntity = 0;
        int badEmission = 0;
        int darkSource = 0;
        List<StressField.Lamp> all = new ArrayList<>(field.lattice);
        field.zones.forEach(z -> all.addAll(z.lamps));
        for (StressField.Lamp lamp : all) {
            BlockState state = level.getBlockState(lamp.pos());
            if (!(state.getBlock() instanceof LampBlock)) {
                continue;
            }
            checked++;
            if (!(level.getBlockEntity(lamp.pos()) instanceof LampBlockEntity)) {
                missingEntity++;
            }
            boolean lowerHalf = state.hasProperty(TallLampBlock.HALF) && state.getValue(TallLampBlock.HALF) == DoubleBlockHalf.LOWER;
            int emission = state.getLightEmission();
            if (!lowerHalf && emission != LampBlock.lightLevel(state)) {
                badEmission++;
            }
            if (level.getBrightness(LightLayer.BLOCK, lamp.pos()) < emission) {
                darkSource++;
            }
        }
        notes.add(String.format(Locale.ROOT, "Intégrité après rechargement : %d lampes contrôlées, %d sans block entity, "
                + "%d émissions incorrectes, %d sources plus sombres que leur émission", checked, missingEntity, badEmission, darkSource));
        if (missingEntity + badEmission + darkSource > 0) {
            errors.merge("intégrité", missingEntity + badEmission + darkSource, Integer::sum);
        }
    }

    private void timedSave() {
        integrity();
        long start = System.nanoTime();
        server.saveEverything(true, true, true);
        long ms = (System.nanoTime() - start) / 1_000_000;
        long bytes = folderSize(server.getWorldPath(LevelResource.ROOT));
        notes.add(String.format(Locale.ROOT, "Sauvegarde complète : %d ms, monde sur disque %.1f Mo", ms, bytes / 1048576.0));
    }

    private static long folderSize(Path root) {
        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(Files::isRegularFile).mapToLong(p -> p.toFile().length()).sum();
        } catch (IOException e) {
            return -1;
        }
    }

    private void error(String where, Throwable e) {
        String key = where + " : " + e;
        int n = errors.merge(key, 1, Integer::sum);
        if (n <= 3) {
            LOGGER.error("[MostLight stress] erreur ({})", where, e);
        }
    }

    // ------------------------------------------------------------------ rapport

    private void finish() {
        for (StressBot bot : bots) {
            try {
                bot.leave();
            } catch (Throwable e) {
                error("déconnexion", e);
            }
        }
        field.chunks.forEach(c -> level.setChunkForced(c.x, c.z, false));
        meter.drain();
        try {
            writeReport();
        } catch (IOException e) {
            LOGGER.error("[MostLight stress] rapport impossible à écrire", e);
        }
        meter.shutdown();
        active = null;
        currentPhase = "done";
        finished = true;
    }

    private static double[] stats(LongArrayList nanos) {
        if (nanos.isEmpty()) {
            return new double[5];
        }
        long[] sorted = nanos.toLongArray();
        Arrays.sort(sorted);
        double sum = 0;
        for (long n : sorted) {
            sum += n;
        }
        return new double[] {sum / sorted.length / 1e6, sorted[sorted.length / 2] / 1e6, pct(sorted, 0.95), pct(sorted, 0.99),
                sorted[sorted.length - 1] / 1e6};
    }

    private static double pct(long[] sorted, double p) {
        return sorted[Math.min(sorted.length - 1, (int) Math.ceil(p * sorted.length) - 1)] / 1e6;
    }

    private static double tps(Phase phase) {
        double seconds = (phase.wallEnd - phase.wallStart) / 1e9;
        return seconds <= 0 ? 0 : Math.min(20.0, phase.tickNanos.size() / seconds);
    }

    private static String f1(double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }

    private void writeReport() throws IOException {
        Files.createDirectories(dir);
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path file = dir.resolve("rapport-" + (config.client() ? "client" : "serveur") + (config.vanilla() ? "-vanilla" : "") + "-" + stamp + ".md");
        StringBuilder md = new StringBuilder();
        Runtime rt = Runtime.getRuntime();
        md.append("# Test de charge MostLight : ").append(config.mode()).append("\n\n");
        md.append("- Date : ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append('\n');
        md.append("- Joueurs simulés : ").append(config.bots()).append(" (1 action par tick chacun, ~").append(config.bots() * 20).append(" actions/s au total)\n");
        md.append("- Processeurs : ").append(rt.availableProcessors()).append(", mémoire max JVM : ").append(rt.maxMemory() / 1048576).append(" Mo\n");
        md.append("- Java ").append(System.getProperty("java.version")).append('\n');
        for (String note : notes) {
            md.append("- ").append(note).append('\n');
        }
        if (!stormLoops.isEmpty()) {
            double[] s = stats(stormLoops);
            md.append(String.format(Locale.ROOT, "- Tempêtes : %d, bascule de toutes les lampes en %.0f ms en moyenne (max %.0f ms)%n",
                    stormLoops.size(), s[0], s[4]));
        }
        md.append("\n## Temps de tick par phase\n\n");
        md.append("Un tick doit durer moins de 50 ms pour tenir 20 TPS.\n\n");
        md.append("| Phase | Joueurs | MSPT moyen | médian | p95 | p99 | max | Ticks > 50 ms | TPS | Lampes | Chunks | Mémoire max | GC |\n");
        md.append("|---|---|---|---|---|---|---|---|---|---|---|---|---|\n");
        for (Phase p : phases) {
            if (p.wallEnd == 0) {
                continue;
            }
            double[] s = stats(p.tickNanos);
            long slow = p.tickNanos.longStream().filter(n -> n > 50_000_000L).count();
            md.append(String.format(Locale.ROOT, "| %s | %d | %.2f ms | %.2f | %.2f | %.2f | %.1f | %d | %.1f | %s | %d | %d Mo | %d (%d ms) |%n",
                    p.title, p.botsActive, s[0], s[1], s[2], s[3], s[4], slow, tps(p),
                    p.lampsAtEnd < 0 ? "-" : String.valueOf(p.lampsAtEnd), p.chunksMax, p.heapMax / 1048576, p.gcCount, p.gcTime));
        }
        md.append("\n## Réseau (par joueur)\n\n");
        md.append("Octets réellement encodés par le codec du jeu, compressés comme sur un serveur (seuil 256).\n\n");
        md.append("| Phase | Paquets/s | Ko/s compressés | Ko/s bruts | Actions/s (ok / refusées) |\n|---|---|---|---|---|\n");
        for (Phase p : phases) {
            if (p.wallEnd == 0 || p.botsActive == 0 || p.netEnd == null) {
                continue;
            }
            PacketMeter.Snapshot d = p.netEnd.minus(p.netStart);
            double seconds = (p.wallEnd - p.wallStart) / 1e9;
            double per = seconds * p.botsActive;
            md.append(String.format(Locale.ROOT, "| %s | %.0f | %.1f | %.1f | %.0f / %.0f |%n", p.title, d.packets() / per,
                    d.wireBytes() / per / 1024, d.rawBytes() / per / 1024, p.actionsOk / seconds, p.actionsFail / seconds));
        }
        PacketMeter.Snapshot total = meter.snapshot();
        md.append(String.format(Locale.ROOT, "%nTotal : %d paquets, %.1f Mo compressés, %d non encodables, %d non mesurés (file pleine).%n",
                total.packets(), total.wireBytes() / 1048576.0, total.failures(), total.dropped()));
        md.append("\nPaquets les plus lourds :\n\n| Type | Mo | Nombre |\n|---|---|---|\n");
        for (Object[] row : meter.topTypes(12)) {
            md.append(String.format(Locale.ROOT, "| %s | %.1f | %d |%n", row[0], (Long) row[1] / 1048576.0, (Long) row[2]));
        }
        if (!meter.failuresByType().isEmpty()) {
            md.append("\nPaquets non encodables (protocole vanilla, attendu pour les paquets propres à NeoForge) :\n\n");
            meter.failuresByType().forEach((type, err) -> md.append("- ").append(type).append(" : ").append(err).append('\n'));
        }
        md.append("\n## Actions des joueurs\n\n| Action | Réussies | Refusées |\n|---|---|---|\n");
        actionStats.forEach((action, c) -> md.append("| ").append(action).append(" | ").append(c[0]).append(" | ").append(c[1]).append(" |\n"));
        md.append("\n## Erreurs\n\n");
        if (errors.isEmpty()) {
            md.append("Aucune.\n");
        } else {
            errors.forEach((k, v) -> md.append("- ").append(k).append(" (x").append(v).append(")\n"));
        }
        Files.writeString(file, md.toString());
        // détail tick par tick pour les graphiques
        StringBuilder csv = new StringBuilder("phase,tick,ms\n");
        for (Phase p : phases) {
            for (int i = 0; i < p.tickNanos.size(); i++) {
                csv.append(p.id).append(',').append(i).append(',').append(String.format(Locale.ROOT, "%.3f", p.tickNanos.getLong(i) / 1e6)).append('\n');
            }
        }
        Files.writeString(Path.of(file.toString().replace(".md", "-ticks.csv")), csv.toString());
        reportFile = file;
        LOGGER.info("[MostLight stress] rapport écrit : {}", file.toAbsolutePath());
        LOGGER.info("[MostLight stress] erreurs : {}", errors.isEmpty() ? "aucune" : errors);
    }
}
