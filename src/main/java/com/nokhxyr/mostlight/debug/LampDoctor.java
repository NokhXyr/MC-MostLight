package com.nokhxyr.mostlight.debug;

import com.nokhxyr.mostlight.block.LampBlock;
import com.nokhxyr.mostlight.block.LightStripBlock;
import com.nokhxyr.mostlight.block.TallLampBlock;
import com.nokhxyr.mostlight.block.entity.LampBlockEntity;
import com.nokhxyr.mostlight.link.LightSwitchBlock;
import com.nokhxyr.mostlight.link.SwitchBlockEntity;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.jetbrains.annotations.Nullable;

/**
 * Finds and fixes broken MostLight data in loaded chunks: lamps or switches without their block entity (or with one of
 * the wrong kind), block entities left behind without their block, two-block lamps missing a half, light that does not
 * match a lamp, LED connections to a block that no longer links back, and switch links to a lamp that is gone. Only
 * MostLight blocks are touched, and chunks that are not loaded are skipped (never loaded by the scan).
 */
public final class LampDoctor {
    public enum Kind {
        MISSING_ENTITY, WRONG_ENTITY, ORPHAN_ENTITY, BROKEN_TALL, DARK_LIGHT, DEAD_CONNECTION, DEAD_LINK;

        public String key() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public record Issue(Kind kind, BlockPos pos) {}

    /** What a scan found: every issue, the number of MostLight blocks looked at and the chunks scanned. */
    public record Report(List<Issue> issues, int blocks, int chunks) {
        public Map<Kind, Integer> counts() {
            Map<Kind, Integer> out = new EnumMap<>(Kind.class);
            issues.forEach(issue -> out.merge(issue.kind(), 1, Integer::sum));
            return out;
        }
    }

    private LampDoctor() {}

    /** The block entity stored in the chunk, without creating a missing one (Level.getBlockEntity would). */
    public static @Nullable BlockEntity stored(ServerLevel level, BlockPos pos) {
        return level.getChunkAt(pos).getBlockEntities().get(pos);
    }

    /** Where a lamp keeps its brightness and redstone memory: the lower half of a two-block lamp. */
    public static BlockPos memoryPos(BlockPos pos, BlockState state) {
        return state.getBlock() instanceof TallLampBlock && state.getValue(TallLampBlock.HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
    }

    private static boolean ours(BlockState state) {
        return state.getBlock() instanceof LampBlock || state.getBlock() instanceof LightSwitchBlock;
    }

    /** Scans the loaded chunks within {@code radius} chunks of {@code center}. */
    public static Report scan(ServerLevel level, BlockPos center, int radius) {
        List<Issue> issues = new ArrayList<>();
        int blocks = 0;
        int chunks = 0;
        int cx = SectionPos.blockToSectionCoord(center.getX());
        int cz = SectionPos.blockToSectionCoord(center.getZ());
        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int z = cz - radius; z <= cz + radius; z++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(x, z);
                if (chunk != null) {
                    chunks++;
                    blocks += scanChunk(level, chunk, issues);
                }
            }
        }
        return new Report(issues, blocks, chunks);
    }

    private static int scanChunk(ServerLevel level, LevelChunk chunk, List<Issue> issues) {
        int blocks = 0;
        LevelChunkSection[] sections = chunk.getSections();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection section = sections[i];
            if (section.hasOnlyAir() || !section.maybeHas(LampDoctor::ours)) {
                continue;
            }
            int baseY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(i));
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockState state = section.getBlockState(x, y, z);
                        if (ours(state)) {
                            blocks++;
                            pos.set(chunk.getPos().getMinBlockX() + x, baseY + y, chunk.getPos().getMinBlockZ() + z);
                            checkBlock(level, chunk, pos.immutable(), state, issues);
                        }
                    }
                }
            }
        }
        // block entities of ours whose block is gone or replaced
        for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
            BlockEntity entity = entry.getValue();
            if ((entity instanceof LampBlockEntity || entity instanceof SwitchBlockEntity) && !ours(chunk.getBlockState(entry.getKey()))) {
                issues.add(new Issue(Kind.ORPHAN_ENTITY, entry.getKey()));
            }
        }
        return blocks;
    }

    /** The issues of the MostLight block at {@code pos} (none for any other block). */
    public static List<Issue> check(ServerLevel level, BlockPos pos) {
        List<Issue> issues = new ArrayList<>();
        BlockState state = level.getBlockState(pos);
        LevelChunk chunk = level.getChunkAt(pos);
        if (ours(state)) {
            checkBlock(level, chunk, pos.immutable(), state, issues);
        } else if (chunk.getBlockEntities().get(pos) instanceof LampBlockEntity || chunk.getBlockEntities().get(pos) instanceof SwitchBlockEntity) {
            issues.add(new Issue(Kind.ORPHAN_ENTITY, pos.immutable()));
        }
        return issues;
    }

    private static void checkBlock(ServerLevel level, LevelChunk chunk, BlockPos pos, BlockState state, List<Issue> issues) {
        // the chunk's own map: Level.getBlockEntity would quietly create a missing one
        BlockEntity entity = chunk.getBlockEntities().get(pos);
        if (entity == null) {
            issues.add(new Issue(Kind.MISSING_ENTITY, pos));
        } else if (state.getBlock() instanceof EntityBlock block) {
            BlockEntity expected = block.newBlockEntity(pos, state);
            if (expected != null && expected.getType() != entity.getType()) {
                issues.add(new Issue(Kind.WRONG_ENTITY, pos));
            }
        }
        if (entity instanceof SwitchBlockEntity sw && sw.links().stream().anyMatch(link -> deadLink(level, link))) {
            issues.add(new Issue(Kind.DEAD_LINK, pos));
        }
        if (!(state.getBlock() instanceof LampBlock)) {
            return;
        }
        if (state.getBlock() instanceof TallLampBlock && partner(level, pos, state) == null) {
            issues.add(new Issue(Kind.BROKEN_TALL, pos));
        }
        if (level.getBrightness(LightLayer.BLOCK, pos) < state.getLightEmission(level, pos)) {
            issues.add(new Issue(Kind.DARK_LIGHT, pos));
        }
        if (entity instanceof LampBlockEntity lamp && lamp.connections() != 0) {
            for (Direction side : Direction.values()) {
                if (lamp.connected(side) && deadConnection(level, pos, state, side)) {
                    issues.add(new Issue(Kind.DEAD_CONNECTION, pos));
                    break;
                }
            }
        }
    }

    /** The other half of a two-block lamp, or null when it is missing. */
    private static @Nullable BlockPos partner(ServerLevel level, BlockPos pos, BlockState state) {
        boolean lower = state.getValue(TallLampBlock.HALF) == DoubleBlockHalf.LOWER;
        BlockPos other = lower ? pos.above() : pos.below();
        BlockState found = level.getBlockState(other);
        boolean matches = found.getBlock() instanceof TallLampBlock tall && tall.type() == ((LampBlock) state.getBlock()).type()
                && found.getValue(TallLampBlock.HALF) == (lower ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER);
        return matches ? other : null;
    }

    /** A link to a loaded position that no longer holds a lamp (unloaded ones are left alone). */
    private static boolean deadLink(ServerLevel level, BlockPos link) {
        return level.isLoaded(link) && !(level.getBlockState(link).getBlock() instanceof LampBlock);
    }

    /** A connection the neighbour does not return: neighbour gone, not chainable, or not connected back. */
    private static boolean deadConnection(ServerLevel level, BlockPos pos, BlockState state, Direction side) {
        BlockPos next = pos.relative(side);
        if (!level.isLoaded(next)) {
            return false;
        }
        return !LightStripBlock.chainable(state) || !LightStripBlock.chainable(level.getBlockState(next))
                || !(stored(level, next) instanceof LampBlockEntity other) || !other.connected(side.getOpposite());
    }

    /** Fixes the issues found by a scan; returns how many were fixed. */
    public static int repair(ServerLevel level, List<Issue> issues) {
        int fixed = 0;
        for (Issue issue : issues) {
            if (fix(level, issue)) {
                fixed++;
            }
        }
        return fixed;
    }

    private static boolean fix(ServerLevel level, Issue issue) {
        BlockPos pos = issue.pos();
        BlockState state = level.getBlockState(pos);
        LevelChunk chunk = level.getChunkAt(pos);
        switch (issue.kind()) {
            case MISSING_ENTITY -> {
                // Level.getBlockEntity creates the block entity the block asks for
                return chunk.getBlockEntities().get(pos) == null && level.getBlockEntity(pos) != null;
            }
            case WRONG_ENTITY -> {
                chunk.removeBlockEntity(pos);
                return level.getBlockEntity(pos) != null;
            }
            case ORPHAN_ENTITY -> {
                if (ours(state)) {
                    return false;
                }
                chunk.removeBlockEntity(pos);
                return true;
            }
            case BROKEN_TALL -> {
                return state.getBlock() instanceof TallLampBlock tall && tall.repairHalf(level, pos, state);
            }
            case DARK_LIGHT -> {
                level.getChunkSource().getLightEngine().checkBlock(pos);
                return true;
            }
            case DEAD_CONNECTION -> {
                if (!(level.getBlockEntity(pos) instanceof LampBlockEntity lamp)) {
                    return false;
                }
                for (Direction side : Direction.values()) {
                    if (lamp.connected(side) && deadConnection(level, pos, state, side)) {
                        lamp.setConnected(side, false);
                    }
                }
                return true;
            }
            case DEAD_LINK -> {
                if (!(level.getBlockEntity(pos) instanceof SwitchBlockEntity sw)) {
                    return false;
                }
                sw.setLinks(sw.links().stream().filter(link -> !deadLink(level, link)).toList());
                return true;
            }
        }
        return false;
    }
}
