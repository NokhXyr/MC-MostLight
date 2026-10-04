package com.nokhxyr.mostlight.block.entity;

import com.nokhxyr.mostlight.block.FanLampBlock;
import com.nokhxyr.mostlight.block.LampBlock;
import com.nokhxyr.mostlight.block.LampShapes;
import com.nokhxyr.mostlight.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Lampe à pièces animées (engrenages, bulles de lave, pales). Niveau de détail (comme les lanternes animées d'Amendments) : de loin, les pièces animées font
 * partie du modèle précuit du chunk (coût nul) ; à moins de {@link #ANIMATED_DISTANCE} blocs de la caméra, ils sont
 * retirés du chunk et dessinés animés par GeckoLib (rotation tant que la lampe est allumée).
 */
public class AnimatedLampBlockEntity extends LampBlockEntity implements GeoBlockEntity {
    public static final ModelProperty<Boolean> ANIMATED = new ModelProperty<>();
    public static final double ANIMATED_DISTANCE = 32;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    /** Côté client : engrenages actuellement animés par GeckoLib (et absents du modèle du chunk). */
    private boolean animated;
    /** Quelques images de recouvrement pendant la reconstruction du chunk, pour éviter un clignotement. */
    private int overlapFrames;

    public AnimatedLampBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ANIMATED_LAMP.get(), pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        LampShapes.Animation animation = animation();
        if (animation == null) {
            return;
        }
        RawAnimation raw = RawAnimation.begin().thenLoop(animation.name());
        controllers.add(new AnimationController<>(this, "parts", 0, state -> running() ? state.setAndContinue(raw) : PlayState.STOP));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    /**
     * Appelé à chaque image par le rendu GeckoLib : bascule entre engrenages animés (près) et précuits (loin).
     * Renvoie vrai si GeckoLib doit dessiner les engrenages.
     */
    private LampShapes.@org.jetbrains.annotations.Nullable Animation animation() {
        return getBlockState().getBlock() instanceof LampBlock lamp ? LampShapes.animation(lamp.type().id()) : null;
    }

    /** Déclencheur de l'animation : lampe allumée, ou ventilateur en marche. */
    public boolean running() {
        LampShapes.Animation animation = animation();
        BlockState state = getBlockState();
        if (animation == null) {
            return false;
        }
        return "fan".equals(animation.trigger())
                ? state.hasProperty(FanLampBlock.FAN) && state.getValue(FanLampBlock.FAN)
                : LampBlock.isLit(state);
    }

    public boolean shouldAnimate(Vec3 camera) {
        // à l'arrêt, les pièces restent dans le modèle précuit : rien à animer
        boolean near = running() && Vec3.atCenterOf(worldPosition).closerThan(camera, ANIMATED_DISTANCE);
        if (near != animated) {
            animated = near;
            if (level != null && level.isClientSide) {
                requestModelDataUpdate();
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_IMMEDIATE);
            }
            if (!near) {
                overlapFrames = 4;
            }
        }
        if (overlapFrames > 0) {
            overlapFrames--;
            return true;
        }
        return animated;
    }

    @Override
    public ModelData getModelData() {
        return ModelData.builder().with(ANIMATED, animated).build();
    }
}
