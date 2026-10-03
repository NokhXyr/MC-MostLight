package com.nokhxyr.mostlight.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.nokhxyr.mostlight.MostLight;
import com.nokhxyr.mostlight.block.GearLampBlock;
import com.nokhxyr.mostlight.block.LampBlock;
import com.nokhxyr.mostlight.block.entity.GearLampBlockEntity;
import com.nokhxyr.mostlight.registry.ModBlockEntities;
import com.nokhxyr.mostlight.registry.ModBlocks;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.registries.DeferredBlock;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/**
 * Lampes animées : le corps et (de loin) les engrenages sont un modèle précuit du chunk ; de près, les engrenages
 * (teintes 4 et 5 dans les quads) sont retirés du chunk et animés par GeckoLib. GeckoLib n'est utilisé que pour ça.
 */
@EventBusSubscriber(modid = MostLight.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GearLampClient {
    private GearLampClient() {}

    private static boolean animatedPart(BakedQuad quad) {
        return quad.getTintIndex() == 4 || quad.getTintIndex() == 5;
    }

    /** Modèle du chunk : sans les engrenages quand GeckoLib les anime. */
    static final class ChunkModel extends BakedModelWrapper<BakedModel> {
        ChunkModel(BakedModel original) {
            super(original);
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data,
                @Nullable RenderType renderType) {
            List<BakedQuad> quads = super.getQuads(state, side, rand, data, renderType);
            if (!Boolean.TRUE.equals(data.get(GearLampBlockEntity.ANIMATED))) {
                return quads;
            }
            return quads.stream().filter(q -> !animatedPart(q)).toList();
        }
    }

    @SubscribeEvent
    static void wrapModels(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        for (DeferredBlock<LampBlock> holder : ModBlocks.all()) {
            if (holder.get() instanceof GearLampBlock block) {
                for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                    ModelResourceLocation location = BlockModelShaper.stateToModelLocation(state);
                    BakedModel original = models.get(location);
                    if (original != null) {
                        models.put(location, new ChunkModel(original));
                    }
                }
            }
        }
    }

    @SubscribeEvent
    static void registerRenderer(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.GEAR_LAMP.get(), context -> new Renderer());
    }

    /** Engrenages GeckoLib : géométrie, planche de textures et animation « spin » générées par tools/generate.mjs. */
    static final class Model extends GeoModel<GearLampBlockEntity> {
        private static ResourceLocation rl(String path) {
            return ResourceLocation.fromNamespaceAndPath(MostLight.MOD_ID, path);
        }

        private static String id(GearLampBlockEntity lamp) {
            return lamp.getBlockState().getBlock() instanceof LampBlock block ? block.type().id() : "gear_lamp";
        }

        @Override
        public ResourceLocation getModelResource(GearLampBlockEntity lamp) {
            return rl("geo/block/" + id(lamp) + ".geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(GearLampBlockEntity lamp) {
            return rl("textures/geo/" + id(lamp) + ".png");
        }

        @Override
        public ResourceLocation getAnimationResource(GearLampBlockEntity lamp) {
            return rl("animations/block/" + id(lamp) + ".animation.json");
        }
    }

    static final class Renderer extends GeoBlockRenderer<GearLampBlockEntity> {
        Renderer() {
            super(new Model());
        }

        /** Rien à dessiner de loin : les engrenages sont alors dans le modèle du chunk. */
        @Override
        public boolean shouldRender(GearLampBlockEntity lamp, Vec3 camera) {
            return lamp.shouldAnimate(camera);
        }

        @Override
        public int getViewDistance() {
            return (int) GearLampBlockEntity.ANIMATED_DISTANCE + 8;
        }

        /** Même orientation que le blockstate (modèle dessiné face au nord). */
        @Override
        protected void rotateBlock(Direction ignored, PoseStack pose) {
            BlockState state = animatable.getBlockState();
            if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                pose.mulPose(Axis.YP.rotationDegrees(-(state.getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot() + 180)));
            }
        }

        /** Teinte de finition sur les pièces « t4 », comme le modèle précuit. */
        @Override
        public void renderRecursively(PoseStack pose, GearLampBlockEntity lamp, GeoBone bone, RenderType renderType, MultiBufferSource buffers,
                VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
            if (bone.getName().endsWith("#t4") && lamp.getBlockState().getBlock() instanceof LampBlock block) {
                colour = ClientSetup.tint(block.color(), lamp.finish(), lamp.tone(), 4);
            }
            super.renderRecursively(pose, lamp, bone, renderType, buffers, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
        }
    }
}
