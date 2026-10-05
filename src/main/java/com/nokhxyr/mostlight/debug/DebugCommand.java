package com.nokhxyr.mostlight.debug;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.nokhxyr.mostlight.MostLight;
import com.nokhxyr.mostlight.block.LampBlock;
import com.nokhxyr.mostlight.block.LightStripBlock;
import com.nokhxyr.mostlight.block.entity.LampBlockEntity;
import com.nokhxyr.mostlight.link.LightSwitchBlock;
import com.nokhxyr.mostlight.link.SwitchBlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Operator commands to look into lamp bugs and broken chunk data:
 * <ul>
 * <li>{@code /mostlight inspect [pos]}: everything the mod knows about the lamp or switch looked at.</li>
 * <li>{@code /mostlight check [radius]}: lists the problems found in the loaded chunks around (radius in chunks).</li>
 * <li>{@code /mostlight repair [radius]}: fixes them. Only MostLight blocks and their data are changed.</li>
 * </ul>
 */
@EventBusSubscriber(modid = MostLight.MOD_ID)
public final class DebugCommand {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static final String KEY = "command." + MostLight.MOD_ID + ".";
    private static final int DEFAULT_RADIUS = 4;
    private static final int MAX_RADIUS = 32;
    /** Problems listed one by one in the chat; the others are only counted. */
    private static final int LISTED = 10;
    private static final SimpleCommandExceptionType NO_TARGET = new SimpleCommandExceptionType(Component.translatable(KEY + "no_target"));

    private DebugCommand() {}

    @SubscribeEvent
    static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(MostLight.MOD_ID)
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("inspect")
                        .executes(context -> inspect(context.getSource(), lookedAt(context.getSource())))
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(context -> inspect(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos")))))
                .then(Commands.literal("check")
                        .executes(context -> check(context.getSource(), DEFAULT_RADIUS, false))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(0, MAX_RADIUS))
                                .executes(context -> check(context.getSource(), radius(context), false))))
                .then(Commands.literal("repair")
                        .executes(context -> check(context.getSource(), DEFAULT_RADIUS, true))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(0, MAX_RADIUS))
                                .executes(context -> check(context.getSource(), radius(context), true)))));
    }

    private static int radius(CommandContext<CommandSourceStack> context) {
        return IntegerArgumentType.getInteger(context, "radius");
    }

    private static BlockPos lookedAt(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        HitResult hit = player.pick(player.blockInteractionRange() + 3, 0, false);
        if (hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK) {
            return block.getBlockPos();
        }
        throw NO_TARGET.create();
    }

    // ------------------------------------------------------------------ check / repair

    private static int check(CommandSourceStack source, int radius, boolean repair) {
        ServerLevel level = source.getLevel();
        LampDoctor.Report report = LampDoctor.scan(level, BlockPos.containing(source.getPosition()), radius);
        List<LampDoctor.Issue> issues = report.issues();
        source.sendSuccess(() -> Component.translatable(KEY + "check.summary", report.blocks(), report.chunks(), issues.size())
                .withStyle(issues.isEmpty() ? ChatFormatting.GREEN : ChatFormatting.GOLD), false);
        if (issues.isEmpty()) {
            return 0;
        }
        for (Map.Entry<LampDoctor.Kind, Integer> entry : report.counts().entrySet()) {
            source.sendSuccess(() -> Component.literal("  ").append(issueName(entry.getKey())).append(": " + entry.getValue()), false);
        }
        for (LampDoctor.Issue issue : issues.subList(0, Math.min(LISTED, issues.size()))) {
            source.sendSuccess(() -> Component.literal("  ").append(teleport(issue.pos())).append(" ").append(issueName(issue.kind())), false);
        }
        if (issues.size() > LISTED) {
            source.sendSuccess(() -> Component.translatable(KEY + "check.more", issues.size() - LISTED).withStyle(ChatFormatting.GRAY), false);
        }
        if (!repair) {
            source.sendSuccess(() -> Component.translatable(KEY + "check.hint", "/" + MostLight.MOD_ID + " repair " + radius)
                    .withStyle(ChatFormatting.GRAY), false);
            return issues.size();
        }
        int fixed = LampDoctor.repair(level, issues);
        source.sendSuccess(() -> Component.translatable(KEY + "repair.summary", fixed, issues.size())
                .withStyle(fixed == issues.size() ? ChatFormatting.GREEN : ChatFormatting.GOLD), true);
        LOGGER.info("[MostLight] repair at {} (radius {} chunks): {} problems, {} fixed {}", BlockPos.containing(source.getPosition()),
                radius, issues.size(), fixed, report.counts());
        return fixed;
    }

    private static Component issueName(LampDoctor.Kind kind) {
        return Component.translatable(KEY + "issue." + kind.key());
    }

    /** Coordinates that teleport the operator next to the block when clicked. */
    private static Component teleport(BlockPos pos) {
        String coords = pos.getX() + " " + pos.getY() + " " + pos.getZ();
        return Component.literal("[" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]").withStyle(style -> style
                .withColor(ChatFormatting.AQUA)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tp @s " + pos.getX() + ".5 " + (pos.getY() + 1) + " " + pos.getZ() + ".5"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable(KEY + "teleport", coords))));
    }

    // ------------------------------------------------------------------ inspect

    private static int inspect(CommandSourceStack source, BlockPos pos) {
        ServerLevel level = source.getLevel();
        BlockState state = level.getBlockState(pos);
        // as stored in the chunk: looking must not create a missing block entity
        BlockEntity stored = LampDoctor.stored(level, pos);
        if (!(state.getBlock() instanceof LampBlock) && !(stored instanceof SwitchBlockEntity)
                && !(state.getBlock() instanceof LightSwitchBlock)) {
            source.sendFailure(Component.translatable(KEY + "inspect.none", teleport(pos)));
            return 0;
        }
        // before anything below reads the lamp's neighbours
        List<LampDoctor.Issue> issues = LampDoctor.check(level, pos);
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(KEY + "inspect.header", state.getBlock().getName(), teleport(pos)).withStyle(ChatFormatting.YELLOW));
        lines.add(Component.translatable(KEY + "inspect.state", properties(state)));
        int measured = level.getBrightness(LightLayer.BLOCK, pos);
        if (state.getBlock() instanceof LampBlock) {
            LampBlockEntity memory = LampDoctor.stored(level, LampDoctor.memoryPos(pos, state)) instanceof LampBlockEntity m ? m : null;
            if (LampBlock.isLit(state)) {
                lines.add(Component.translatable(KEY + "inspect.light_on", LampBlock.brightness(state) + 1, LampBlock.STEPS,
                        state.getLightEmission(level, pos), measured));
            } else {
                lines.add(Component.translatable(KEY + "inspect.light_off", (memory == null ? 0 : memory.brightness()) + 1, LampBlock.STEPS, measured));
            }
            if (memory != null) {
                lines.add(Component.translatable(KEY + "inspect.redstone", yesNo(memory.powered()), yesNo(level.hasNeighborSignal(pos))));
            }
            if (stored instanceof LampBlockEntity entity) {
                lines.add(Component.translatable(KEY + "inspect.look", entity.finish().getSerializedName(), entity.tone().getSerializedName()));
                if (state.getBlock() instanceof LightStripBlock) {
                    lines.add(Component.translatable(KEY + "inspect.strips", strips(state, entity.stripLayout())));
                }
                if (entity.connections() != 0) {
                    String sides = java.util.Arrays.stream(Direction.values()).filter(entity::connected).map(Direction::getSerializedName)
                            .collect(Collectors.joining(", "));
                    lines.add(Component.translatable(KEY + "inspect.chain", LightStripBlock.chain(level, pos).size(), sides));
                }
            }
        }
        if (stored instanceof SwitchBlockEntity sw) {
            int present = 0;
            int gone = 0;
            for (BlockPos link : sw.links()) {
                if (!level.isLoaded(link)) {
                    continue;
                }
                if (level.getBlockState(link).getBlock() instanceof LampBlock) {
                    present++;
                } else {
                    gone++;
                }
            }
            lines.add(Component.translatable(KEY + "inspect.links", sw.links().size(), present, gone, sw.links().size() - present - gone));
        }
        if (issues.isEmpty()) {
            lines.add(Component.translatable(KEY + "inspect.ok").withStyle(ChatFormatting.GREEN));
        } else {
            MutableComponent names = Component.empty();
            for (int i = 0; i < issues.size(); i++) {
                names.append(i == 0 ? Component.empty() : Component.literal(", ")).append(issueName(issues.get(i).kind()));
            }
            lines.add(Component.translatable(KEY + "inspect.issues", names, "/" + MostLight.MOD_ID + " repair 0").withStyle(ChatFormatting.RED));
        }
        lines.forEach(line -> source.sendSuccess(() -> line, false));
        return issues.size();
    }

    private static Component yesNo(boolean value) {
        return Component.translatable(KEY + (value ? "yes" : "no"));
    }

    private static String properties(BlockState state) {
        return state.getValues().entrySet().stream().map(DebugCommand::property).collect(Collectors.joining(", "));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String property(Map.Entry<Property<?>, Comparable<?>> entry) {
        return entry.getKey().getName() + "=" + ((Property) entry.getKey()).getName(entry.getValue());
    }

    /** Each strip of the block: side, slot (low, middle, high) and whether it runs across. */
    private static String strips(BlockState state, int layout) {
        List<String> out = new ArrayList<>();
        for (Direction side : Direction.values()) {
            if (LightStripBlock.has(state, side)) {
                out.add(side.getSerializedName() + " " + LightStripBlock.Slot.values()[LampBlockEntity.slot(layout, side)].getSerializedName()
                        + (LampBlockEntity.rotated(layout, side) ? " (rotated)" : ""));
            }
        }
        return String.join(", ", out);
    }
}
