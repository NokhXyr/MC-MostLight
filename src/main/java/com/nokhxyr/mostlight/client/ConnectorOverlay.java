package com.nokhxyr.mostlight.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.nokhxyr.mostlight.MostLight;
import com.nokhxyr.mostlight.MostLightConfig;
import com.nokhxyr.mostlight.block.LampBlock;
import com.nokhxyr.mostlight.block.LightStripBlock;
import com.nokhxyr.mostlight.block.entity.LampBlockEntity;
import com.nokhxyr.mostlight.item.LedConnectorItem;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Connecteur LED en main, en visant une bande ou un bloc lumineux : sa chaîne est entourée d'un seul contour vert
 * (les formes de ses blocs fusionnées, comme le cadre de sélection du jeu), et les voisins qu'on peut encore relier
 * ont un contour rouge pâle. Derrière un mur, les contours restent visibles en transparence.
 * Sous le viseur, sur un fond sombre : la taille de la chaîne et si elle est alimentée.
 */
@EventBusSubscriber(modid = MostLight.MOD_ID, value = Dist.CLIENT)
public final class ConnectorOverlay {
    private static final float[] LINKED = {0.35F, 1F, 0.5F};
    private static final float[] FREE = {1F, 0.35F, 0.3F};
    /** Contour fusionné de la dernière chaîne dessinée, recalculé seulement quand ses blocs changent. */
    private static List<BlockPos> cachedChain = List.of();
    private static List<BlockState> cachedStates = List.of();
    private static BlockPos cachedOrigin = BlockPos.ZERO;
    private static VoxelShape cachedShape = Shapes.empty();

    private ConnectorOverlay() {}

    /** Contours nets (testés en profondeur) et leur reflet pâle à travers les murs. */
    private static final class Lines extends RenderType {
        static final RenderType VISIBLE = lines("visible", true);
        static final RenderType HIDDEN = lines("hidden", false);

        private static RenderType lines(String name, boolean depth) {
            return create(MostLight.MOD_ID + "_chain_" + name, DefaultVertexFormat.POSITION_COLOR_NORMAL, VertexFormat.Mode.LINES, 1024,
                    false, false, CompositeState.builder()
                            .setShaderState(RENDERTYPE_LINES_SHADER)
                            .setLineState(new LineStateShard(OptionalDouble.of(depth ? 2.5 : 1.5)))
                            .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                            .setOutputState(ITEM_ENTITY_TARGET)
                            .setWriteMaskState(COLOR_WRITE)
                            .setCullState(NO_CULL)
                            .setDepthTestState(depth ? LEQUAL_DEPTH_TEST : NO_DEPTH_TEST)
                            .createCompositeState(false));
        }

        private Lines(String name, VertexFormat format, VertexFormat.Mode mode, int size, boolean crumbling, boolean sort,
                Runnable setup, Runnable clear) {
            super(name, format, mode, size, crumbling, sort, setup, clear);
        }
    }

    /** Ce que le connecteur vise : sa chaîne, son état et les voisins reliables pas encore reliés. */
    private record Target(BlockPos pos, List<BlockPos> chain, boolean powered, List<BlockPos> free) {}

    private static @Nullable Target target() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !(mc.player.getMainHandItem().getItem() instanceof LedConnectorItem
                || mc.player.getOffhandItem().getItem() instanceof LedConnectorItem)) {
            return null;
        }
        if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        Level level = mc.level;
        BlockPos pos = hit.getBlockPos();
        if (!LightStripBlock.chainable(level.getBlockState(pos)) || !(level.getBlockEntity(pos) instanceof LampBlockEntity lamp)) {
            return null;
        }
        List<BlockPos> chain = LightStripBlock.chain(level, pos);
        boolean powered = chain.stream().anyMatch(p -> level.getBlockEntity(p) instanceof com.nokhxyr.mostlight.block.entity.LampBlockEntity memory && memory.powered());
        List<BlockPos> free = new ArrayList<>();
        for (Direction d : Direction.values()) {
            BlockPos next = pos.relative(d);
            if (!lamp.connected(d) && LightStripBlock.chainable(level.getBlockState(next))) {
                free.add(next);
            }
        }
        return new Target(pos, chain, powered, free);
    }

    @SubscribeEvent
    static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Target target = target();
        if (target == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VoxelShape chain = chainShape(mc.level, target);
        // d'abord le reflet à travers les murs, puis les contours nets par-dessus
        for (RenderType type : new RenderType[] {Lines.HIDDEN, Lines.VISIBLE}) {
            boolean visible = type == Lines.VISIBLE;
            VertexConsumer lines = buffers.getBuffer(type);
            outline(pose, lines, chain, cachedOrigin, camera, LINKED, visible ? 0.95F : 0.3F);
            for (BlockPos pos : target.free()) {
                outline(pose, lines, mc.level.getBlockState(pos).getShape(mc.level, pos), pos, camera, FREE, visible ? 0.6F : 0.15F);
            }
            buffers.endBatch(type);
        }
    }

    /** Formes de tous les blocs de la chaîne réunies en une seule (les arêtes entre blocs voisins disparaissent). */
    private static VoxelShape chainShape(Level level, Target target) {
        List<BlockState> states = target.chain().stream().map(level::getBlockState).toList();
        if (target.chain().equals(cachedChain) && states.equals(cachedStates)) {
            return cachedShape;
        }
        BlockPos origin = target.pos();
        VoxelShape shape = Shapes.empty();
        for (int i = 0; i < target.chain().size(); i++) {
            BlockPos pos = target.chain().get(i);
            shape = Shapes.joinUnoptimized(shape, states.get(i).getShape(level, pos)
                    .move(pos.getX() - origin.getX(), pos.getY() - origin.getY(), pos.getZ() - origin.getZ()), BooleanOp.OR);
        }
        cachedChain = target.chain();
        cachedStates = states;
        cachedOrigin = origin;
        cachedShape = shape.optimize();
        return cachedShape;
    }

    private static void outline(PoseStack pose, VertexConsumer lines, VoxelShape shape, BlockPos pos, Vec3 camera, float[] c, float alpha) {
        if (shape.isEmpty()) {
            return;
        }
        double ox = pos.getX() - camera.x, oy = pos.getY() - camera.y, oz = pos.getZ() - camera.z;
        PoseStack.Pose last = pose.last();
        shape.forAllEdges((x1, y1, z1, x2, y2, z2) -> {
            float dx = (float) (x2 - x1), dy = (float) (y2 - y1), dz = (float) (z2 - z1);
            float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            dx /= len;
            dy /= len;
            dz /= len;
            lines.addVertex(last, (float) (x1 + ox), (float) (y1 + oy), (float) (z1 + oz)).setColor(c[0], c[1], c[2], alpha).setNormal(last, dx, dy, dz);
            lines.addVertex(last, (float) (x2 + ox), (float) (y2 + oy), (float) (z2 + oz)).setColor(c[0], c[1], c[2], alpha).setNormal(last, dx, dy, dz);
        });
    }

    /** Sous le viseur : « Chaîne : 27 / 256 · alimentée », et le nombre de voisins à relier. */
    @SubscribeEvent
    static void onGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.screen != null) {
            return;
        }
        Target target = target();
        if (target == null) {
            return;
        }
        GuiGraphics g = event.getGuiGraphics();
        String key = "gui." + MostLight.MOD_ID + ".connector.";
        Component line = Component.translatable(key + "chain", target.chain().size(), MostLightConfig.get(MostLightConfig.LED_CHAIN_LENGTH))
                .append(Component.literal("  ·  ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.translatable(key + (target.powered() ? "powered" : "unpowered"))
                        .withStyle(target.powered() ? ChatFormatting.GOLD : ChatFormatting.GREEN));
        Component free = target.free().isEmpty() ? null
                : Component.translatable(key + "free", target.free().size()).withStyle(ChatFormatting.RED);
        int x = g.guiWidth() / 2;
        int y = g.guiHeight() / 2 + 14;
        int width = Math.max(mc.font.width(line), free == null ? 0 : mc.font.width(free)) + 10;
        int height = free == null ? 13 : 24;
        g.fill(x - width / 2, y - 3, x + width / 2, y - 3 + height, 0xA0101014);
        g.drawCenteredString(mc.font, line, x, y, 0xFFFFFF);
        if (free != null) {
            g.drawCenteredString(mc.font, free, x, y + 11, 0xFFFFFF);
        }
    }
}
