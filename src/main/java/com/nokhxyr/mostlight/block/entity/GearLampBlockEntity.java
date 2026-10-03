package com.nokhxyr.mostlight.block.entity;

import com.nokhxyr.mostlight.block.LampBlock;
import com.nokhxyr.mostlight.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Lampe à engrenages : en plus de la finition, garde côté client l'angle des engrenages (ils tournent allumés). */
public class GearLampBlockEntity extends LampBlockEntity {
    /** Degrés par tick du grand engrenage ; les autres tournent selon leur rapport. */
    private static final double DEGREES_PER_TICK = 3.0;
    private double angle;
    private double lastTime = -1;

    public GearLampBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GEAR_LAMP.get(), pos, state);
    }

    /** Angle du grand engrenage au moment du rendu : avance quand la lampe est allumée, s'arrête sinon. */
    public double spin(float partialTick) {
        if (level == null) {
            return angle;
        }
        double now = level.getGameTime() + partialTick;
        if (lastTime >= 0 && now > lastTime && getBlockState().getValue(LampBlock.LIT)) {
            // plus la lampe est forte, plus les engrenages tournent vite (luminosité 0 = la plus forte)
            angle += (now - lastTime) * DEGREES_PER_TICK * (1.0 - 0.2 * getBlockState().getValue(LampBlock.BRIGHTNESS));
        }
        lastTime = now;
        return angle;
    }
}
