package com.nokhxyr.mostlight.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.nokhxyr.mostlight.MostLight;
import com.nokhxyr.mostlight.block.LampBlock;
import com.nokhxyr.mostlight.block.LightStripBlock;
import com.nokhxyr.mostlight.block.entity.LampBlockEntity;
import com.nokhxyr.mostlight.item.LedConnectorItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Connecteur LED en main : chaque bande proche affiche ses liaisons redstone. Flèche verte = reliée à la voisine de
 * ce côté, croix rouge = bande voisine non reliée, marque jaune au centre = bande alimentée.
 */
@EventBusSubscriber(modid = MostLight.MOD_ID, value = Dist.CLIENT)
public final class ConnectorOverlay {
    private static final int RADIUS = 12;
    private static final int MAX_STRIPS = 512;

    private ConnectorOverlay() {}

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
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        BlockPos center = mc.player.blockPosition();
        int drawn = 0;
        for (int cx = (center.getX() - RADIUS) >> 4; cx <= (center.getX() + RADIUS) >> 4; cx++) {
            for (int cz = (center.getZ() - RADIUS) >> 4; cz <= (center.getZ() + RADIUS) >> 4; cz++) {
                if (!mc.level.hasChunk(cx, cz)) {
                    continue;
                }
                LevelChunk chunk = mc.level.getChunk(cx, cz);
                for (BlockEntity entity : chunk.getBlockEntities().values()) {
                    if (drawn >= MAX_STRIPS || !(entity instanceof LampBlockEntity lamp)
                            || !(entity.getBlockState().getBlock() instanceof LightStripBlock)
                            || !entity.getBlockPos().closerThan(center, RADIUS)) {
                        continue;
                    }
                    draw(pose, lines, lamp);
                    drawn++;
                }
            }
        }
        buffers.endBatch(RenderType.lines());
        pose.popPose();
    }

    private static void draw(PoseStack pose, VertexConsumer lines, LampBlockEntity lamp) {
        BlockPos pos = lamp.getBlockPos();
        Vec3 c = Vec3.atCenterOf(pos);
        BlockState state = lamp.getBlockState();
        for (Direction d : Direction.values()) {
            Vec3 n = Vec3.atLowerCornerOf(d.getNormal());
            BlockPos next = pos.relative(d);
            boolean neighbor = lamp.getLevel() != null && lamp.getLevel().getBlockState(next).getBlock() instanceof LightStripBlock;
            if (lamp.connected(d)) {
                // flèche verte du centre vers la voisine reliée
                Vec3 tip = c.add(n.scale(0.48));
                line(pose, lines, c.add(n.scale(0.12)), tip, 0.25F, 1F, 0.4F);
                Vec3 side = perpendicular(d).scale(0.09);
                Vec3 back = tip.subtract(n.scale(0.12));
                line(pose, lines, tip, back.add(side), 0.25F, 1F, 0.4F);
                line(pose, lines, tip, back.subtract(side), 0.25F, 1F, 0.4F);
            } else if (neighbor) {
                // croix rouge : bande voisine non reliée
                Vec3 m = c.add(n.scale(0.45));
                Vec3 a = perpendicular(d).scale(0.08);
                Vec3 b = n.cross(perpendicular(d)).scale(0.08);
                line(pose, lines, m.add(a).add(b), m.subtract(a).subtract(b), 1F, 0.25F, 0.2F);
                line(pose, lines, m.add(a).subtract(b), m.subtract(a).add(b), 1F, 0.25F, 0.2F);
            }
        }
        if (state.getValue(LampBlock.POWERED)) {
            line(pose, lines, c.add(-0.07, 0, 0), c.add(0.07, 0, 0), 1F, 0.85F, 0.2F);
            line(pose, lines, c.add(0, -0.07, 0), c.add(0, 0.07, 0), 1F, 0.85F, 0.2F);
            line(pose, lines, c.add(0, 0, -0.07), c.add(0, 0, 0.07), 1F, 0.85F, 0.2F);
        }
    }

    private static Vec3 perpendicular(Direction d) {
        return d.getAxis() == Direction.Axis.Y ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
    }

    private static void line(PoseStack pose, VertexConsumer lines, Vec3 a, Vec3 b, float r, float g, float bl) {
        Vec3 n = b.subtract(a).normalize();
        PoseStack.Pose last = pose.last();
        lines.addVertex(last, (float) a.x, (float) a.y, (float) a.z).setColor(r, g, bl, 1F).setNormal(last, (float) n.x, (float) n.y, (float) n.z);
        lines.addVertex(last, (float) b.x, (float) b.y, (float) b.z).setColor(r, g, bl, 1F).setNormal(last, (float) n.x, (float) n.y, (float) n.z);
    }
}
