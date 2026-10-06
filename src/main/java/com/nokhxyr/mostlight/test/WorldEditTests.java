package com.nokhxyr.mostlight.test;

import com.nokhxyr.mostlight.block.HorizontalLampBlock;
import com.nokhxyr.mostlight.block.LampBlock;
import com.nokhxyr.mostlight.block.LampFinish;
import com.nokhxyr.mostlight.block.LampType;
import com.nokhxyr.mostlight.block.LightStripBlock;
import com.nokhxyr.mostlight.block.LightTone;
import com.nokhxyr.mostlight.block.OmniLampBlock;
import com.nokhxyr.mostlight.block.RodLampBlock;
import com.nokhxyr.mostlight.block.TallLampBlock;
import com.nokhxyr.mostlight.block.entity.LampBlockEntity;
import com.nokhxyr.mostlight.link.LightSwitchBlock;
import com.nokhxyr.mostlight.link.SwitchBlockEntity;
import com.nokhxyr.mostlight.registry.ModBlocks;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.Nullable;

/**
 * WorldEdit on lamps, driven the way a builder does it: an op in creative mode types {@code //pos1}, {@code //copy},
 * {@code //rotate}, {@code //paste}, {@code //move}, {@code //undo}... then clicks the switches. A test passes when the
 * world comes out whole: every block keeps its data, no lamp is left in half or without its block entity, nothing is
 * duplicated, and switches drive the lamps they should.
 * <p>
 * Run with ./gradlew runGameTestServerWorldEdit, with the WorldEdit jar in run-gametest-worldedit/mods. Without
 * WorldEdit the tests pass at once: WorldEdit is optional.
 */
// own namespace and run, like the Create tests: WorldEdit talks to every player that joins
@GameTestHolder("mostlight_worldedit")
@PrefixGameTestTemplate(false)
public class WorldEditTests {
    /** The build: a 4 x 4 corner of the test area, stone floor at y = 1, lamps at y = 2 and 3. */
    private static final BlockPos FROM = new BlockPos(0, 1, 0);
    private static final BlockPos TO = new BlockPos(3, 3, 3);
    /** Where the builder stands to copy (inside the build) and to paste (the opposite corner, free). */
    private static final BlockPos ORIGIN = new BlockPos(2, 2, 2);
    private static final BlockPos TARGET = new BlockPos(6, 2, 6);

    private static final BlockPos WALL = new BlockPos(0, 2, 0);
    private static final BlockPos SCONCE = new BlockPos(1, 2, 0);
    private static final BlockPos STREET = new BlockPos(3, 2, 0);
    private static final BlockPos GEAR = new BlockPos(2, 2, 1);
    private static final BlockPos STRIP_WALL = new BlockPos(0, 2, 2);
    private static final BlockPos STRIP = new BlockPos(1, 2, 2);
    private static final BlockPos ROD = new BlockPos(3, 2, 2);
    private static final BlockPos SWITCH = new BlockPos(1, 2, 3);
    private static final BlockPos CUBE_A = new BlockPos(2, 2, 3);
    private static final BlockPos CUBE_B = new BlockPos(3, 2, 3);

    private static boolean noWorldEdit(GameTestHelper helper) {
        if (ModList.get().isLoaded("worldedit")) {
            return false;
        }
        helper.succeed();
        return true;
    }

    // ------------------------------------------------------------------ the builder

    /** A mock player made op and put in creative mode, who types WorldEdit commands and keeps what WorldEdit answers. */
    private static final class Builder {
        private final GameTestHelper helper;
        private final ServerPlayer player;
        private final List<String> log = new ArrayList<>();

        Builder(GameTestHelper helper) {
            this.helper = helper;
            player = helper.makeMockServerPlayerInLevel();
            player.setGameMode(GameType.CREATIVE);
            player.getInventory().clearContent();
            helper.getLevel().getServer().getPlayerList().op(player.getGameProfile());
            heard();
        }

        Builder standAt(BlockPos rel) {
            Vec3 feet = Vec3.atBottomCenterOf(helper.absolutePos(rel));
            player.moveTo(feet.x, feet.y, feet.z, 0, 0);
            return this;
        }

        Builder run(String command) {
            log.add("> " + command);
            helper.getLevel().getServer().getCommands().performPrefixedCommand(player.createCommandSourceStack(), command);
            log.addAll(heard());
            return this;
        }

        Builder select(BlockPos from, BlockPos to) {
            return run("//pos1 " + coords(from)).run("//pos2 " + coords(to));
        }

        private String coords(BlockPos rel) {
            BlockPos abs = helper.absolutePos(rel);
            return abs.getX() + "," + abs.getY() + "," + abs.getZ();
        }

        /** Right click with an empty hand, as the server handles a real click. */
        void click(BlockPos rel) {
            BlockPos abs = helper.absolutePos(rel);
            player.gameMode.useItemOn(player, helper.getLevel(), ItemStack.EMPTY, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
        }

        /** Chat lines sent to the player since the last call (WorldEdit's answers). */
        private List<String> heard() {
            List<String> out = new ArrayList<>();
            if (player.connection.getConnection().channel() instanceof EmbeddedChannel channel) {
                Object packet;
                while ((packet = channel.readOutbound()) != null) {
                    if (packet instanceof ClientboundSystemChatPacket chat && !chat.overlay()) {
                        out.add(chat.content().getString());
                    }
                }
            }
            return out;
        }

        String transcript() {
            return String.join(" / ", log);
        }
    }

    // ------------------------------------------------------------------ the build

    /**
     * A small street corner using every kind of block data: a gold street lamp (two halves, lit, dimmed), a blue sconce
     * on a wall (off), an animated gear lamp, a floor + wall LED strip with custom positions, a lying light rod, two cube
     * lamps joined by the LED connector, and a floor switch linked to the sconce and the street lamp.
     */
    private static void build(GameTestHelper helper) {
        for (int x = FROM.getX(); x <= TO.getX(); x++) {
            for (int z = FROM.getZ(); z <= TO.getZ(); z++) {
                helper.setBlock(new BlockPos(x, FROM.getY(), z), Blocks.STONE);
            }
        }
        helper.setBlock(WALL, Blocks.STONE);
        helper.setBlock(STRIP_WALL, Blocks.STONE);

        helper.setBlock(SCONCE, ModBlocks.state(LampType.WALL_SCONCE, DyeColor.BLUE).setValue(HorizontalLampBlock.FACING, Direction.EAST)
                .setValue(LampBlock.LIGHT, 0));
        lamp(helper, SCONCE).setBrightness(1);
        look(helper, SCONCE, LampFinish.COPPER, LightTone.COOL);

        BlockState street = LampBlock.lit(ModBlocks.state(LampType.STREET_LAMP, DyeColor.RED).setValue(TallLampBlock.FACING, Direction.SOUTH), 2);
        helper.setBlock(STREET, street);
        helper.setBlock(STREET.above(), street.setValue(TallLampBlock.HALF, DoubleBlockHalf.UPPER));
        lamp(helper, STREET).setBrightness(2);
        look(helper, STREET, LampFinish.GOLD, LightTone.WARM);

        helper.setBlock(GEAR, ModBlocks.state(LampType.GEAR_LAMP, DyeColor.ORANGE).setValue(HorizontalLampBlock.FACING, Direction.WEST));

        BlockState strip = ModBlocks.state(LampType.LIGHT_STRIP, DyeColor.CYAN);
        for (Direction d : Direction.values()) {
            strip = strip.setValue(LightStripBlock.SIDES.get(d), d == Direction.DOWN || d == Direction.WEST);
        }
        helper.setBlock(STRIP, strip);
        lamp(helper, STRIP).setStrip(Direction.DOWN, 2, true);
        lamp(helper, STRIP).setStrip(Direction.WEST, 0, true);

        BlockState rod = ModBlocks.state(LampType.LIGHT_ROD, DyeColor.LIME).setValue(OmniLampBlock.FACING, Direction.UP);
        if (rod.getBlock() instanceof RodLampBlock) {
            rod = rod.setValue(RodLampBlock.LYING, true).setValue(RodLampBlock.AXIS, Direction.Axis.X);
        }
        helper.setBlock(ROD, rod);

        helper.setBlock(CUBE_A, ModBlocks.state(LampType.LAMP_BLOCK, DyeColor.PURPLE));
        helper.setBlock(CUBE_B, ModBlocks.state(LampType.LAMP_BLOCK, DyeColor.PURPLE));
        lamp(helper, CUBE_A).setConnected(Direction.EAST, true);
        lamp(helper, CUBE_B).setConnected(Direction.WEST, true);

        helper.setBlock(SWITCH, ModBlocks.LIGHT_SWITCH.get().defaultBlockState()
                .setValue(LightSwitchBlock.FACE, AttachFace.FLOOR).setValue(LightSwitchBlock.FACING, Direction.NORTH));
        ((SwitchBlockEntity) helper.getBlockEntity(SWITCH)).setLinks(List.of(helper.absolutePos(SCONCE), helper.absolutePos(STREET)));
    }

    private static LampBlockEntity lamp(GameTestHelper helper, BlockPos rel) {
        return (LampBlockEntity) helper.getBlockEntity(rel);
    }

    private static void look(GameTestHelper helper, BlockPos rel, LampFinish finish, LightTone tone) {
        BlockState state = helper.getBlockState(rel);
        ((LampBlock) state.getBlock()).setLook(helper.getLevel(), helper.absolutePos(rel), state, finish, tone);
    }

    // ------------------------------------------------------------------ what a block is, and how it should come out

    private static HolderLookup.Provider registries(GameTestHelper helper) {
        return helper.getLevel().registryAccess();
    }

    /** One block of the build: its state, its saved data, and for lamps their look and hitbox. */
    private record Snap(BlockPos rel, BlockState state, @Nullable CompoundTag tag, @Nullable String look, VoxelShape shape) {}

    private static @Nullable String look(@Nullable BlockEntity entity) {
        return entity instanceof LampBlockEntity lamp
                ? lamp.finish() + "/" + lamp.tone() + "/brightness " + lamp.brightness() + "/powered " + lamp.powered()
                : null;
    }

    private static List<Snap> snapshot(GameTestHelper helper) {
        List<Snap> out = new ArrayList<>();
        BlockPos.betweenClosed(FROM, TO).forEach(p -> {
            BlockPos rel = p.immutable();
            BlockState state = helper.getBlockState(rel);
            if (state.isAir()) {
                return;
            }
            BlockEntity entity = helper.getLevel().getBlockEntity(helper.absolutePos(rel));
            CompoundTag tag = entity == null ? null : entity.saveWithoutMetadata(registries(helper));
            out.add(new Snap(rel, state, tag, look(entity), state.getShape(helper.getLevel(), helper.absolutePos(rel))));
        });
        return out;
    }

    /** What //rotate or //flip does to the clipboard, in vanilla terms (BlockPos.rotate convention). */
    private record Change(Rotation rotation, Mirror mirror) {
        static final Change NONE = new Change(Rotation.NONE, Mirror.NONE);

        BlockPos offset(BlockPos o) {
            BlockPos m = switch (mirror) {
                case FRONT_BACK -> new BlockPos(-o.getX(), o.getY(), o.getZ());
                case LEFT_RIGHT -> new BlockPos(o.getX(), o.getY(), -o.getZ());
                default -> o;
            };
            return m.rotate(rotation);
        }

        BlockState state(BlockState state) {
            return state.mirror(mirror).rotate(rotation);
        }

        /** The hitbox as it must look once moved: the same boxes, turned or mirrored around the block's center. */
        VoxelShape shape(VoxelShape shape) {
            VoxelShape out = Shapes.empty();
            for (AABB box : shape.toAabbs()) {
                out = Shapes.or(out, Shapes.create(new AABB(point(box.minX, box.minY, box.minZ), point(box.maxX, box.maxY, box.maxZ))));
            }
            return out;
        }

        private Vec3 point(double x, double y, double z) {
            double cx = x - 0.5;
            double cz = z - 0.5;
            if (mirror == Mirror.FRONT_BACK) {
                cx = -cx;
            } else if (mirror == Mirror.LEFT_RIGHT) {
                cz = -cz;
            }
            double rx = switch (rotation) {
                case CLOCKWISE_90 -> -cz;
                case CLOCKWISE_180 -> -cx;
                case COUNTERCLOCKWISE_90 -> cz;
                default -> cx;
            };
            double rz = switch (rotation) {
                case CLOCKWISE_90 -> cx;
                case CLOCKWISE_180 -> -cz;
                case COUNTERCLOCKWISE_90 -> -cx;
                default -> cz;
            };
            return new Vec3(rx + 0.5, y, rz + 0.5);
        }
    }

    private static BlockPos moved(BlockPos rel, BlockPos from, BlockPos to, Change change) {
        return to.offset(change.offset(rel.subtract(from)));
    }

    /**
     * Every block of {@code scene}, copied from {@code from} and pasted at {@code to} through {@code change}: same state
     * (turned the vanilla way), same look, standing on its support. With {@code exact}, the saved data must also be
     * identical byte for byte. With {@code sided}, the block entity data kept per side must have followed the turn too:
     * LED strip positions (seen through the strip's hitbox) and LED connector links.
     */
    private static void compare(GameTestHelper helper, List<Snap> scene, BlockPos from, BlockPos to, Change change, boolean exact,
            boolean sided, List<String> wrong) {
        for (Snap s : scene) {
            BlockPos rel = moved(s.rel(), from, to, change);
            BlockPos abs = helper.absolutePos(rel);
            BlockState expected = change.state(s.state());
            BlockState got = helper.getBlockState(rel);
            String where = s.state().getBlock().getName().getString() + " " + s.rel().toShortString() + " -> " + rel.toShortString();
            if (!got.equals(expected)) {
                wrong.add(where + ": state " + got + ", expected " + expected);
                continue;
            }
            BlockEntity entity = helper.getLevel().getBlockEntity(abs);
            if (s.tag() != null && entity == null) {
                wrong.add(where + ": block entity lost");
                continue;
            }
            if (exact && s.tag() != null && !s.tag().equals(entity.saveWithoutMetadata(registries(helper)))) {
                wrong.add(where + ": data " + entity.saveWithoutMetadata(registries(helper)) + ", expected " + s.tag());
            }
            if (s.look() != null && !s.look().equals(look(entity))) {
                wrong.add(where + ": look " + look(entity) + ", expected " + s.look());
            }
            if (got.getBlock() instanceof LampBlock && !got.canSurvive(helper.getLevel(), abs)) {
                wrong.add(where + ": no support");
            }
            if (!sided) {
                continue;
            }
            // other hitboxes follow the state alone (checked above); a strip's follows its block entity
            VoxelShape shape = got.getShape(helper.getLevel(), abs);
            if (got.getBlock() instanceof LightStripBlock && Shapes.joinIsNotEmpty(shape, change.shape(s.shape()), BooleanOp.NOT_SAME)) {
                wrong.add(where + ": strip hitbox " + shape.toAabbs() + ", expected " + change.shape(s.shape()).toAabbs());
            }
            if (got.is(ModBlocks.lamp(LampType.LAMP_BLOCK)) && LightStripBlock.chain(helper.getLevel(), abs).size() != 2) {
                wrong.add(where + ": LED connector chain of " + LightStripBlock.chain(helper.getLevel(), abs).size() + ", expected 2");
            }
        }
    }

    // ------------------------------------------------------------------ world health

    /**
     * Anything a broken edit leaves behind, over the whole test area: a lamp or a switch without its block entity (or with
     * one of the wrong type), half a tall lamp, an LED strip block holding no strip, data that does not survive a save
     * and reload.
     */
    private static void health(GameTestHelper helper, List<String> wrong) {
        helper.forEveryBlockInStructure(rel -> {
            BlockPos abs = helper.absolutePos(rel);
            BlockState state = helper.getBlockState(rel);
            BlockEntity entity = helper.getLevel().getBlockEntity(abs);
            String where = state.getBlock().getName().getString() + " " + rel.toShortString();
            boolean ours = state.getBlock() instanceof LampBlock || state.getBlock() instanceof LightSwitchBlock;
            if (!ours) {
                return;
            }
            if (entity == null || !entity.getType().isValid(state)) {
                wrong.add(where + ": " + (entity == null ? "no block entity" : "block entity of the wrong type " + entity.getType()));
                return;
            }
            CompoundTag saved = entity.saveWithFullMetadata(registries(helper));
            BlockEntity reloaded = BlockEntity.loadStatic(abs, state, saved, registries(helper));
            if (reloaded == null || !saved.equals(reloaded.saveWithFullMetadata(registries(helper)))) {
                wrong.add(where + ": data changes on reload " + saved);
            }
            if (state.getBlock() instanceof TallLampBlock tall) {
                boolean lower = state.getValue(TallLampBlock.HALF) == DoubleBlockHalf.LOWER;
                BlockState other = helper.getLevel().getBlockState(lower ? abs.above() : abs.below());
                if (!(other.getBlock() == tall && other.getValue(TallLampBlock.HALF) != state.getValue(TallLampBlock.HALF))) {
                    wrong.add(where + ": " + (lower ? "lower" : "upper") + " half alone");
                }
            }
            if (state.getBlock() instanceof LightStripBlock && Direction.stream().noneMatch(d -> LightStripBlock.has(state, d))) {
                wrong.add(where + ": LED strip block with no strip");
            }
        });
    }

    private static int drops(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds().inflate(3)).stream()
                .mapToInt(e -> e.getItem().getCount()).sum();
    }

    private static void check(GameTestHelper helper, Builder builder, Consumer<List<String>> checks) {
        List<String> wrong = new ArrayList<>();
        checks.accept(wrong);
        helper.assertTrue(wrong.isEmpty(), wrong.size() + " problem(s): " + String.join(" | ", wrong)
                + " || WorldEdit: " + builder.transcript());
        helper.succeed();
    }

    // ------------------------------------------------------------------ copy, rotate, flip

    /**
     * //copy then //paste through {@code change} ({@code command} null: a plain copy), then look at both builds. With
     * {@code sided}, only the per-side block entity data of the pasted build is checked.
     */
    private static void copyPaste(GameTestHelper helper, @Nullable String command, Change change, boolean sided) {
        build(helper);
        List<Snap> scene = snapshot(helper);
        Builder builder = new Builder(helper);
        builder.standAt(ORIGIN).select(FROM, TO).run("//copy");
        if (command != null) {
            builder.run(command);
        }
        builder.standAt(TARGET).run("//paste");
        helper.runAfterDelay(5, () -> check(helper, builder, wrong -> {
            if (sided) {
                List<String> all = new ArrayList<>();
                compare(helper, scene, ORIGIN, TARGET, change, false, true, all);
                all.stream().filter(w -> w.contains("strip hitbox") || w.contains("LED connector")).forEach(wrong::add);
                return;
            }
            boolean plain = change == Change.NONE;
            compare(helper, scene, ORIGIN, ORIGIN, Change.NONE, true, true, wrong);
            compare(helper, scene, ORIGIN, TARGET, change, plain, plain, wrong);
            health(helper, wrong);
            if (drops(helper) != 0) {
                wrong.add(drops(helper) + " item(s) dropped");
            }
        }));
    }

    /** A plain copy is a perfect copy: same blocks, same data byte for byte, the original untouched. */
    @GameTest(template = "empty", timeoutTicks = 100, batch = "worldedit_copy")
    public static void copyPasteIsExact(GameTestHelper helper) {
        if (noWorldEdit(helper)) {
            return;
        }
        copyPaste(helper, null, Change.NONE, false);
    }

    /** //rotate 90: every lamp turns with the build, keeps its look and its support, the original stays untouched. */
    @GameTest(template = "empty", timeoutTicks = 100, batch = "worldedit_rotate90")
    public static void rotate90KeepsLamps(GameTestHelper helper) {
        if (noWorldEdit(helper)) {
            return;
        }
        copyPaste(helper, "//rotate 90", new Change(Rotation.CLOCKWISE_90, Mirror.NONE), false);
    }

    @GameTest(template = "empty", timeoutTicks = 100, batch = "worldedit_rotate180")
    public static void rotate180KeepsLamps(GameTestHelper helper) {
        if (noWorldEdit(helper)) {
            return;
        }
        copyPaste(helper, "//rotate 180", new Change(Rotation.CLOCKWISE_180, Mirror.NONE), false);
    }

    /** //flip east: a mirror image, east and west swapped. */
    @GameTest(template = "empty", timeoutTicks = 100, batch = "worldedit_flip")
    public static void flipKeepsLamps(GameTestHelper helper) {
        if (noWorldEdit(helper)) {
            return;
        }
        copyPaste(helper, "//flip east", new Change(Rotation.NONE, Mirror.FRONT_BACK), false);
    }

    /*
     * Known limit, reported but not blocking: LED strip positions and LED connector links are kept per side in the block
     * entity. WorldEdit turns block states only (vanilla structures do the same), so after //rotate or //flip a strip
     * keeps its faces but may show its old position on them, and connector links point the old way.
     */

    @GameTest(template = "empty", timeoutTicks = 100, batch = "worldedit_rotate90_sided", required = false)
    public static void rotate90TurnsStripsAndLinks(GameTestHelper helper) {
        if (noWorldEdit(helper)) {
            return;
        }
        copyPaste(helper, "//rotate 90", new Change(Rotation.CLOCKWISE_90, Mirror.NONE), true);
    }

    @GameTest(template = "empty", timeoutTicks = 100, batch = "worldedit_flip_sided", required = false)
    public static void flipMirrorsStripsAndLinks(GameTestHelper helper) {
        if (noWorldEdit(helper)) {
            return;
        }
        copyPaste(helper, "//flip east", new Change(Rotation.NONE, Mirror.FRONT_BACK), true);
    }

    /** //flip up turns the build upside down: no vanilla equivalent, but no lamp may come out broken or doubled. */
    @GameTest(template = "empty", timeoutTicks = 100, batch = "worldedit_flip_up")
    public static void flipUpsideDownBreaksNothing(GameTestHelper helper) {
        if (noWorldEdit(helper)) {
            return;
        }
        build(helper);
        Builder builder = new Builder(helper);
        builder.standAt(ORIGIN).select(FROM, TO).run("//copy").run("//flip up").standAt(TARGET).run("//paste");
        helper.runAfterDelay(5, () -> check(helper, builder, wrong -> health(helper, wrong)));
    }

    // ------------------------------------------------------------------ move

    /** //move: every block arrives with its data byte for byte, nothing is left behind, nothing drops on the way. */
    @GameTest(template = "empty", timeoutTicks = 100, batch = "worldedit_move")
    public static void moveKeepsLamps(GameTestHelper helper) {
        if (noWorldEdit(helper)) {
            return;
        }
        build(helper);
        List<Snap> scene = snapshot(helper);
        Builder builder = new Builder(helper);
        builder.standAt(ORIGIN).select(FROM, TO).run("//move 5 east");
        BlockPos shift = new BlockPos(5, 0, 0);
        helper.runAfterDelay(5, () -> check(helper, builder, wrong -> {
            compare(helper, scene, BlockPos.ZERO, shift, Change.NONE, true, true, wrong);
            for (Snap s : scene) {
                if (helper.getBlockState(s.rel()).getBlock() instanceof LampBlock) {
                    wrong.add("lamp left behind at " + s.rel().toShortString());
                }
            }
            health(helper, wrong);
            if (drops(helper) != 0) {
                wrong.add(drops(helper) + " item(s) dropped");
            }
        }));
    }

    // ------------------------------------------------------------------ switches

    /*
     * Known limit, reported but not blocking: a switch keeps its links as absolute positions (by design). A pasted switch
     * drives the lamps of the build it was copied from, and after //move it drives the old spots, until re-linked.
     */

    /** A pasted switch drives the pasted lamps, never the lamps of the build it was copied from. */
    @GameTest(template = "empty", timeoutTicks = 100, batch = "worldedit_switch_paste", required = false)
    public static void pastedSwitchDrivesItsOwnLamps(GameTestHelper helper) {
        if (noWorldEdit(helper)) {
            return;
        }
        build(helper);
        Builder builder = new Builder(helper);
        builder.standAt(ORIGIN).select(FROM, TO).run("//copy").standAt(TARGET).run("//paste");
        BlockPos pastedSwitch = moved(SWITCH, ORIGIN, TARGET, Change.NONE);
        BlockPos pastedStreet = moved(STREET, ORIGIN, TARGET, Change.NONE);
        helper.runAfterDelay(2, () -> builder.click(pastedSwitch));
        helper.runAfterDelay(5, () -> check(helper, builder, wrong -> {
            // the switch turns its group off (the street lamp is lit)
            if (LampBlock.isLit(helper.getBlockState(pastedStreet))) {
                wrong.add("pasted switch did not reach the pasted street lamp");
            }
            if (!LampBlock.isLit(helper.getBlockState(STREET))) {
                wrong.add("pasted switch turned off the ORIGINAL street lamp");
            }
        }));
    }

    /** //move: the switch still drives the lamps that moved with it. */
    @GameTest(template = "empty", timeoutTicks = 100, batch = "worldedit_move_switch", required = false)
    public static void movedSwitchDrivesMovedLamps(GameTestHelper helper) {
        if (noWorldEdit(helper)) {
            return;
        }
        build(helper);
        Builder builder = new Builder(helper);
        builder.standAt(ORIGIN).select(FROM, TO).run("//move 5 east");
        BlockPos shift = new BlockPos(5, 0, 0);
        helper.runAfterDelay(2, () -> builder.click(SWITCH.offset(shift)));
        helper.runAfterDelay(5, () -> check(helper, builder, wrong -> {
            if (LampBlock.isLit(helper.getBlockState(STREET.offset(shift)))) {
                wrong.add("moved switch no longer reaches the moved street lamp");
            }
        }));
    }

    // ------------------------------------------------------------------ erase and undo

    /** //set air over the whole build, then //undo: back exactly as it was, and no item was dropped on the way. */
    @GameTest(template = "empty", timeoutTicks = 100, batch = "worldedit_undo")
    public static void setAirThenUndoRestoresEverything(GameTestHelper helper) {
        if (noWorldEdit(helper)) {
            return;
        }
        build(helper);
        List<Snap> scene = snapshot(helper);
        Builder builder = new Builder(helper);
        builder.standAt(ORIGIN).select(FROM, TO).run("//set air");
        int[] dropsAfterSet = {0};
        helper.runAfterDelay(3, () -> {
            dropsAfterSet[0] = drops(helper);
            builder.run("//undo");
        });
        helper.runAfterDelay(8, () -> check(helper, builder, wrong -> {
            compare(helper, scene, ORIGIN, ORIGIN, Change.NONE, true, true, wrong);
            health(helper, wrong);
            if (dropsAfterSet[0] != 0 || drops(helper) != 0) {
                wrong.add(dropsAfterSet[0] + " item(s) dropped by //set air, " + drops(helper) + " in total after //undo");
            }
        }));
    }

    // ------------------------------------------------------------------ half a lamp

    /**
     * Selections that cut a street lamp in two: only its lower half copied and pasted, only its upper half copied and
     * pasted, then its upper half erased in place. No half may stay in the world on its own.
     */
    @GameTest(template = "empty", timeoutTicks = 100, batch = "worldedit_halves")
    public static void halfSelectionsLeaveNoHalfLamp(GameTestHelper helper) {
        if (noWorldEdit(helper)) {
            return;
        }
        build(helper);
        Builder builder = new Builder(helper);
        builder.standAt(ORIGIN)
                .select(STREET.below(), STREET).run("//copy").standAt(TARGET).run("//paste")
                .standAt(ORIGIN).select(STREET.above(), STREET.above()).run("//copy").standAt(TARGET.south(2)).run("//paste")
                .select(STREET.above(), STREET.above()).run("//set air");
        helper.runAfterDelay(5, () -> check(helper, builder, wrong -> {
            health(helper, wrong);
            // the erased street lamp may come back as its one item, never as more
            if (drops(helper) > 1) {
                wrong.add(drops(helper) + " item(s) dropped, at most 1 expected");
            }
        }));
    }

    // ------------------------------------------------------------------ random states

    /**
     * //set with random states of every lamp and switch over the whole area, then //undo. Nonsense on purpose (lone
     * halves, strips without a face, lamps in the air), the way a typo or a bad schematic would place them: the world
     * must stay sound, and //undo must leave nothing behind.
     */
    @GameTest(template = "empty", timeoutTicks = 200, batch = "worldedit_random")
    public static void randomStatesNeverCorrupt(GameTestHelper helper) {
        if (noWorldEdit(helper)) {
            return;
        }
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }
        List<String> pattern = new ArrayList<>();
        for (LampType type : LampType.values()) {
            pattern.add("*mostlight:" + type.id());
        }
        for (ModBlocks.SwitchKind kind : ModBlocks.SwitchKind.values()) {
            pattern.add("*mostlight:" + kind.id);
        }
        Builder builder = new Builder(helper);
        builder.standAt(ORIGIN).select(new BlockPos(0, 2, 0), new BlockPos(8, 4, 8)).run("//set " + String.join(",", pattern));
        List<String> afterSet = new ArrayList<>();
        int[] placed = {0};
        helper.runAfterDelay(5, () -> {
            helper.forEveryBlockInStructure(rel -> placed[0] += helper.getBlockState(rel).getBlock() instanceof LampBlock ? 1 : 0);
            health(helper, afterSet);
            builder.run("//undo");
        });
        helper.runAfterDelay(10, () -> check(helper, builder, wrong -> {
            if (placed[0] == 0) {
                wrong.add("//set placed no lamp");
            }
            afterSet.forEach(w -> wrong.add("after //set: " + w));
            helper.forEveryBlockInStructure(rel -> {
                if (rel.getY() >= 2 && (!helper.getBlockState(rel).isAir() || helper.getLevel().getBlockEntity(helper.absolutePos(rel)) != null)) {
                    wrong.add("after //undo: " + helper.getBlockState(rel) + " left at " + rel.toShortString());
                }
            });
        }));
    }
}
