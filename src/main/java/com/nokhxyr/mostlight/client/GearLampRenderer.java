package com.nokhxyr.mostlight.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.nokhxyr.mostlight.MostLight;
import com.nokhxyr.mostlight.block.HorizontalLampBlock;
import com.nokhxyr.mostlight.block.LampBlock;
import com.nokhxyr.mostlight.block.LampShapes;
import com.nokhxyr.mostlight.block.entity.GearLampBlockEntity;
import com.nokhxyr.mostlight.registry.ModBlockEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Fait tourner les engrenages des lampes à engrenages (modèles block/<lampe>_gear_*), teintés selon la finition. */
@EventBusSubscriber(modid = MostLight.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GearLampRenderer implements BlockEntityRenderer<GearLampBlockEntity> {
    public GearLampRenderer(BlockEntityRendererProvider.Context context) {}

    private static ModelResourceLocation location(String model) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(MostLight.MOD_ID, "block/" + model));
    }

    @SubscribeEvent
    static void registerModels(ModelEvent.RegisterAdditional event) {
        for (var gears : LampShapes.allGears()) {
            for (LampShapes.Gear gear : gears) {
                event.register(location(gear.model()));
            }
        }
    }

    @SubscribeEvent
    static void registerRenderer(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.GEAR_LAMP.get(), GearLampRenderer::new);
    }

    @Override
    public void render(GearLampBlockEntity entity, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = entity.getBlockState();
        if (!(state.getBlock() instanceof LampBlock lamp) || !state.hasProperty(HorizontalLampBlock.FACING)) {
            return;
        }
        double angle = entity.spin(partialTick);
        Direction facing = state.getValue(HorizontalLampBlock.FACING);
        int color = entity.finish().color();
        float r = (color >> 16 & 0xFF) / 255F;
        float g = (color >> 8 & 0xFF) / 255F;
        float b = (color & 0xFF) / 255F;
        Minecraft mc = Minecraft.getInstance();
        VertexConsumer consumer = buffers.getBuffer(RenderType.cutout());
        for (LampShapes.Gear gear : LampShapes.gears(lamp.type().id())) {
            BakedModel model = mc.getModelManager().getModel(location(gear.model()));
            pose.pushPose();
            // orientation du blockstate (modèle dessiné face au nord)
            pose.translate(0.5, 0.5, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot() + 180));
            pose.translate(-0.5, -0.5, -0.5);
            // l'engrenage est modélisé autour de (8, 8) : on l'amène à son axe puis on le fait tourner
            pose.translate(gear.cx() / 16.0, gear.cy() / 16.0, gear.cz() / 16.0);
            pose.mulPose(Axis.ZP.rotationDegrees((float) ((angle * gear.speed()) % 360.0)));
            pose.translate(-0.5, -0.5, -gear.cz() / 16.0);
            mc.getBlockRenderer().getModelRenderer().renderModel(pose.last(), consumer, state, model, r, g, b, light, overlay,
                    ModelData.EMPTY, RenderType.cutout());
            pose.popPose();
        }
    }
}
