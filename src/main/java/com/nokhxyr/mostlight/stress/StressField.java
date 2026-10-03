package com.nokhxyr.mostlight.stress;

import com.nokhxyr.mostlight.block.HorizontalLampBlock;
import com.nokhxyr.mostlight.block.LampBlock;
import com.nokhxyr.mostlight.block.LampFinish;
import com.nokhxyr.mostlight.block.LampType;
import com.nokhxyr.mostlight.block.LightTone;
import com.nokhxyr.mostlight.block.OmniLampBlock;
import com.nokhxyr.mostlight.block.Placement;
import com.nokhxyr.mostlight.block.TallLampBlock;
import com.nokhxyr.mostlight.link.LightSwitchBlock;
import com.nokhxyr.mostlight.link.SwitchBlockEntity;
import com.nokhxyr.mostlight.registry.ModBlocks;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.jetbrains.annotations.Nullable;

/**
 * Terrain du test de charge : une zone de lampes de tous les modèles par joueur (une lampe tous les 2 blocs,
 * supports, interrupteurs liés), plus un cube dense de lampes en damier pour pousser le moteur de lumière.
 * En mode vanilla, chaque lampe devient une ampoule en cuivre cirée (même lumière, même logique on/off).
 */
public final class StressField {
    private static final Direction[] HORIZONTAL = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    private static final LampType[] CUBE_TYPES = Arrays.stream(LampType.values())
            .filter(t -> t.placement() == Placement.CUBE).toArray(LampType[]::new);

    /** Lampe posée : position du bloc lampe et support éventuel (bloc permuté avec un bloc de redstone par les horloges). */
    public record Lamp(BlockPos pos, @Nullable BlockPos support, @Nullable BlockState supportState, Placement placement) {}

    public static final class Zone {
        public final int index;
        public final BlockPos origin;
        public final List<Lamp> lamps = new ArrayList<>();
        public final List<BlockPos> switches = new ArrayList<>();
        /** 64 lampes liées à la télécommande du joueur de la zone. */
        public final List<BlockPos> remoteLinks = new ArrayList<>();

        Zone(int index, BlockPos origin) {
            this.index = index;
            this.origin = origin;
        }
    }

    public final StressConfig config;
    public final int ground;
    public final List<Zone> zones = new ArrayList<>();
    public final List<Lamp> lattice = new ArrayList<>();
    public final BlockPos latticeOrigin;
    public final Set<ChunkPos> chunks = new LinkedHashSet<>();
    private final RandomSource random;

    public StressField(ServerLevel level, BlockPos center, StressConfig config) {
        this.config = config;
        this.random = RandomSource.create(20261003L);
        // grille de cellules : une par zone + une pour le cube, centrée sur le centre
        int cells = config.bots() + 1;
        int cols = (int) Math.ceil(Math.sqrt(cells));
        int rows = (int) Math.ceil(cells / (double) cols);
        int x0 = center.getX() - cols * config.spacing() / 2;
        int z0 = center.getZ() - rows * config.spacing() / 2;
        // le monde du serveur de GameTest est vide : on pose alors notre propre sol à la hauteur d'un monde plat
        int surface = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, center.getX(), center.getZ());
        this.ground = surface <= level.getMinBuildHeight() ? -60 : surface;
        // le cube au milieu de la grille (sous la caméra du test client)
        int latticeCell = Math.min(cells - 1, (rows / 2) * cols + cols / 2);
        BlockPos lat = null;
        int zone = 0;
        for (int i = 0; i < cells; i++) {
            BlockPos cell = new BlockPos(x0 + (i % cols) * config.spacing(), ground, z0 + (i / cols) * config.spacing());
            if (i == latticeCell) {
                int margin = (config.spacing() - config.lattice()) / 2;
                lat = cell.offset(margin, 1, margin);
                addChunks(lat, config.lattice());
            } else {
                zones.add(new Zone(zone++, cell));
                addChunks(cell.offset(-4, 0, -4), config.zoneSize() + 8);
            }
        }
        this.latticeOrigin = lat;
    }

    private void addChunks(BlockPos from, int size) {
        for (int x = from.getX() >> 4; x <= (from.getX() + size) >> 4; x++) {
            for (int z = from.getZ() >> 4; z <= (from.getZ() + size) >> 4; z++) {
                chunks.add(new ChunkPos(x, z));
            }
        }
    }

    public BlockPos zoneCenter(Zone zone) {
        return zone.origin.offset(config.zoneSize() / 2, 0, config.zoneSize() / 2);
    }

    public BlockPos latticeCenter() {
        return latticeOrigin.offset(config.lattice() / 2, config.lattice() / 2, config.lattice() / 2);
    }

    public int lampCount() {
        return zones.stream().mapToInt(z -> z.lamps.size()).sum() + lattice.size();
    }

    /** Étapes de construction, une par tick : une zone entière, puis le cube couche par couche. */
    public Deque<Runnable> buildSteps(ServerLevel level) {
        Deque<Runnable> steps = new ArrayDeque<>();
        for (Zone zone : zones) {
            steps.add(() -> buildZone(level, zone));
        }
        for (int y = 0; y < config.lattice(); y++) {
            int layer = y;
            steps.add(() -> buildLatticeLayer(level, layer));
        }
        return steps;
    }

    private void buildZone(ServerLevel level, Zone zone) {
        int size = config.zoneSize();
        // sol plein sous la zone (supports des lampes posées au sol)
        for (int dx = -4; dx < size + 4; dx++) {
            for (int dz = -4; dz < size + 4; dz++) {
                level.setBlock(zone.origin.offset(dx, -1, dz), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        for (int dx = 0; dx < size; dx += 2) {
            for (int dz = 0; dz < size; dz += 2) {
                Lamp lamp = placeLamp(level, zone.origin.offset(dx, 0, dz));
                if (lamp != null) {
                    zone.lamps.add(lamp);
                }
            }
        }
        // blocs qui laissent passer la lumière au-dessus des lampes : canopée de feuillages (~1/3) et verre
        for (int dx = -2; dx < size + 2; dx++) {
            for (int dz = -2; dz < size + 2; dz++) {
                BlockPos top = zone.origin.offset(dx, 4, dz);
                if ((dx + dz) % 3 == 0) {
                    level.setBlock(top, Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true),
                            Block.UPDATE_CLIENTS);
                } else if ((dx * 7 + dz * 3) % 11 == 0) {
                    level.setBlock(top, (dx % 2 == 0 ? Blocks.GLASS : Blocks.LIGHT_BLUE_STAINED_GLASS).defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
        if (!config.vanilla()) {
            // 4 interrupteurs et 4 variateurs, chacun lié à 64 lampes de la zone (le maximum)
            for (int i = 0; i < 8; i++) {
                BlockPos pos = zone.origin.offset(-3, 1, i * 4);
                level.setBlock(pos.below(), Blocks.SMOOTH_QUARTZ.defaultBlockState(), Block.UPDATE_ALL);
                level.setBlock(pos.south(), Blocks.SMOOTH_QUARTZ.defaultBlockState(), Block.UPDATE_ALL);
                LightSwitchBlock block = i < 4 ? ModBlocks.LIGHT_SWITCH.get() : ModBlocks.DIMMER_SWITCH.get();
                level.setBlock(pos, block.defaultBlockState().setValue(LightSwitchBlock.FACE, AttachFace.WALL)
                        .setValue(LightSwitchBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);
                if (level.getBlockEntity(pos) instanceof SwitchBlockEntity entity) {
                    entity.setLinks(sample(zone, 64));
                }
                zone.switches.add(pos);
            }
            zone.remoteLinks.addAll(sample(zone, 64));
        }
    }

    private List<BlockPos> sample(Zone zone, int count) {
        List<BlockPos> out = new ArrayList<>();
        for (int i = 0; i < count && !zone.lamps.isEmpty(); i++) {
            out.add(zone.lamps.get(random.nextInt(zone.lamps.size())).pos());
        }
        return out;
    }

    /** Pose une lampe d'un modèle et d'une couleur au hasard avec son support. */
    private @Nullable Lamp placeLamp(ServerLevel level, BlockPos pos) {
        LampType type = LampType.values()[random.nextInt(LampType.values().length)];
        DyeColor color = DyeColor.values()[random.nextInt(16)];
        Direction facing = HORIZONTAL[random.nextInt(4)];
        LampBlock block = ModBlocks.lamp(type, color);
        BlockPos lampPos = pos;
        BlockPos support = pos.below();
        BlockState state;
        switch (type.placement()) {
            case HANGING -> {
                lampPos = pos.above(2);
                support = pos.above(3);
                level.setBlock(support, Blocks.DARK_OAK_PLANKS.defaultBlockState(), Block.UPDATE_ALL);
                state = block.defaultBlockState().setValue(HorizontalLampBlock.FACING, facing);
            }
            case WALL -> {
                lampPos = pos.above();
                support = lampPos.relative(facing.getOpposite());
                level.setBlock(support, Blocks.SMOOTH_QUARTZ.defaultBlockState(), Block.UPDATE_ALL);
                state = block.defaultBlockState().setValue(HorizontalLampBlock.FACING, facing);
            }
            // bande LED : posée au sol par défaut
            case OMNI -> state = block instanceof com.nokhxyr.mostlight.block.LightStripBlock
                    ? block.defaultBlockState()
                    : block.defaultBlockState().setValue(OmniLampBlock.FACING, Direction.UP);
            case STANDING -> state = block.defaultBlockState().setValue(HorizontalLampBlock.FACING, facing);
            case TALL -> state = block.defaultBlockState().setValue(TallLampBlock.FACING, facing);
            default -> {
                support = null;
                state = block.defaultBlockState();
            }
        }
        BlockState supportState = support == null ? null : level.getBlockState(support);
        if (config.vanilla()) {
            level.setBlock(lampPos, Blocks.WAXED_COPPER_BULB.defaultBlockState().setValue(BlockStateProperties.LIT, true), Block.UPDATE_ALL);
            return new Lamp(lampPos, support, supportState, type.placement());
        }
        level.setBlock(lampPos, state, Block.UPDATE_ALL);
        if (type.placement() == Placement.TALL) {
            level.setBlock(lampPos.above(), state.setValue(TallLampBlock.HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
        }
        BlockState placed = level.getBlockState(lampPos);
        if (!(placed.getBlock() instanceof LampBlock lamp)) {
            return null;
        }
        // 30 % des lampes avec une finition et une teinte : données de block entity à synchroniser
        if (random.nextInt(10) < 3) {
            lamp.setLook(level, lampPos, placed, LampFinish.values()[random.nextInt(LampFinish.values().length)],
                    LightTone.values()[random.nextInt(LightTone.values().length)]);
        }
        return new Lamp(lampPos, support, supportState, type.placement());
    }

    /** Une couche du cube : une lampe sur deux en damier 3D (de la lumière partout, aucune case pleine). */
    private void buildLatticeLayer(ServerLevel level, int y) {
        int size = config.lattice();
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                if (((x + y + z) & 1) != 0) {
                    continue;
                }
                BlockPos pos = latticeOrigin.offset(x, y, z);
                BlockState state = config.vanilla()
                        ? Blocks.WAXED_COPPER_BULB.defaultBlockState().setValue(BlockStateProperties.LIT, true)
                        : ModBlocks.lamp(CUBE_TYPES[random.nextInt(CUBE_TYPES.length)], DyeColor.values()[random.nextInt(16)]).defaultBlockState();
                level.setBlock(pos, state, Block.UPDATE_ALL);
                lattice.add(new Lamp(pos, null, null, Placement.CUBE));
            }
        }
    }

    /** Allume ou éteint par le même chemin que les interrupteurs (ou bascule si on == null). Renvoie vrai si changé. */
    public static boolean setLit(ServerLevel level, BlockPos pos, @Nullable Boolean on) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof LampBlock lamp) {
            boolean target = on == null ? !state.getValue(LampBlock.LIT) : on;
            if (state.getValue(LampBlock.LIT) == target) {
                return false;
            }
            lamp.setLit(level, pos, state, target);
            return true;
        }
        if (state.is(Blocks.WAXED_COPPER_BULB)) {
            boolean target = on == null ? !state.getValue(BlockStateProperties.LIT) : on;
            if (state.getValue(BlockStateProperties.LIT) == target) {
                return false;
            }
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, target), Block.UPDATE_ALL);
            return true;
        }
        return false;
    }
}
