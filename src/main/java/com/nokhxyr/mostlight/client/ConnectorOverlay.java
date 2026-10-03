package com.nokhxyr.mostlight.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.nokhxyr.mostlight.MostLight;
import com.nokhxyr.mostlight.block.LampBlock;
import com.nokhxyr.mostlight.block.LightStripBlock;
import com.nokhxyr.mostlight.block.entity.LampBlockEntity;
import com.nokhxyr.mostlight.item.LedConnectorItem;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalDouble;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Connecteur LED en main, en visant une bande ou un bloc lumineux : sa chaîne s'affiche, et seulement elle. Un trait
 * relie chaque bloc à ceux auxquels il est relié (vert, jaune quand la chaîne est alimentée), visible à travers les
 * blocs. Sur le bloc visé, une petite croix rouge marque chaque voisin reliable mais pas encore relié.
 */
@EventBusSubscriber(modid = MostLight.MOD_ID, value = Dist.CLIENT)
public final class ConnectorOverlay {
    private ConnectorOverlay() {}

    /** Traits épais, sans test de profondeur : la chaîne reste visible derrière les murs. */
    private static final class Lines extends RenderType {
        static final RenderType THROUGH_WALLS = create(MostLight.MOD_ID + "_chain_lines", DefaultVertexFormat.POSITION_COLOR_NORMAL,
                VertexFormat.Mode.LINES, 1024, false, false, CompositeState.builder()
                        .setShaderState(RENDERTYPE_LINES_SHADER)
                        .setLineState(new LineStateShard(OptionalDouble.of(3)))
                        .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setOutputState(ITEM_ENTITY_TARGET)
                        .setWriteMaskState(COLOR_WRITE)
                        .setCullState(NO_CULL)
                        .setDepthTestState(NO_DEPTH_TEST)
                        .createCompositeState(false));

        private Lines(String name, VertexFormat format, VertexFormat.Mode mode, int size, boolean crumbling, boolean sort,
                Runnable setup, Runnable clear) {
            super(name, format, mode, size, crumbling, sort, setup, clear);
        }
    }

    @SubscribeEvent
    static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !(mc.player.getMainHandItem().getItem() instanceof LedConnectorItem
                || mc.player.getOffhandItem().getItem() instanceof LedConnectorItem)) {
            return;
        }
        if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        Level level = mc.level;
        BlockPos target = hit.getBlockPos();
        if (!LightStripBlock.chainable(level.getBlockState(target))) {
            return;
        }
        List<BlockPos> chain = LightStripBlock.chain(level, target);
        boolean powered = false;
        for (BlockPos pos : chain) {
            if (level.getBlockState(pos).getValue(LampBlock.POWERED)) {
                powered = true;
                break;
            }
        }
        float r = powered ? 1F : 0.25F, g = powered ? 0.85F : 1F, b = powered ? 0.2F : 0.4F;

        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(Lines.THROUGH_WALLS);
        Set<BlockPos> members = new HashSet<>(chain);
        for (BlockPos pos : chain) {
            if (!(level.getBlockEntity(pos) instanceof LampBlockEntity lamp)) {
                continue;
            }
            Vec3 from = LightStripBlock.anchor(level.getBlockState(pos), pos);
            for (Direction d : Direction.values()) {
                BlockPos next = pos.relative(d);
                // chaque liaison une seule fois
                if (lamp.connected(d) && members.contains(next) && d.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
                    line(pose, lines, from, LightStripBlock.anchor(level.getBlockState(next), next), r, g, b);
                }
            }
            // petit losange sur chaque bloc de la chaîne
            diamond(pose, lines, from, 0.06, r, g, b);
        }
        // bloc visé : croix rouges vers les voisins reliables non reliés
        if (level.getBlockEntity(target) instanceof LampBlockEntity lamp) {
            Vec3 c = Vec3.atCenterOf(target);
            for (Direction d : Direction.values()) {
                BlockState other = level.getBlockState(target.relative(d));
                if (!lamp.connected(d) && LightStripBlock.chainable(other)) {
                    Vec3 n = Vec3.atLowerCornerOf(d.getNormal());
                    Vec3 m = c.add(n.scale(0.5));
                    Vec3 p = perpendicular(d).scale(0.08);
                    Vec3 q = n.cross(perpendicular(d)).scale(0.08);
                    line(pose, lines, m.add(p).add(q), m.subtract(p).subtract(q), 1F, 0.25F, 0.2F);
                    line(pose, lines, m.add(p).subtract(q), m.subtract(p).add(q), 1F, 0.25F, 0.2F);
                }
            }
        }
        buffers.endBatch(Lines.THROUGH_WALLS);
        pose.popPose();
    }

    private static Vec3 perpendicular(Direction d) {
        return d.getAxis() == Direction.Axis.Y ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
    }

    private static void diamond(PoseStack pose, VertexConsumer lines, Vec3 c, double s, float r, float g, float b) {
        Vec3[] tips = {c.add(s, 0, 0), c.add(0, s, 0), c.add(-s, 0, 0), c.add(0, -s, 0)};
        for (int i = 0; i < 4; i++) {
            line(pose, lines, tips[i], tips[(i + 1) % 4], r, g, b);
        }
    }

    private static void line(PoseStack pose, VertexConsumer lines, Vec3 a, Vec3 b, float r, float g, float bl) {
        Vec3 n = b.subtract(a).normalize();
        PoseStack.Pose last = pose.last();
        lines.addVertex(last, (float) a.x, (float) a.y, (float) a.z).setColor(r, g, bl, 1F).setNormal(last, (float) n.x, (float) n.y, (float) n.z);
        lines.addVertex(last, (float) b.x, (float) b.y, (float) b.z).setColor(r, g, bl, 1F).setNormal(last, (float) n.x, (float) n.y, (float) n.z);
    }
}
