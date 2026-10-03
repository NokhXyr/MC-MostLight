package com.nokhxyr.mostlight.item;

import com.nokhxyr.mostlight.MostLight;
import com.nokhxyr.mostlight.MostLightConfig;
import com.nokhxyr.mostlight.block.LightStripBlock;
import com.nokhxyr.mostlight.block.entity.LampBlockEntity;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Connecteur LED : relie une bande (ou un bloc lumineux) à sa voisine du côté visé (le bord de la bande le plus proche du clic ;
 * accroupi, la bande située devant la face cliquée). Une chaîne de bandes reliées s'allume en entier dès
 * qu'une seule est alimentée par la redstone. Les liaisons de la chaîne s'affichent en particules.
 */
public class LedConnectorItem extends Item {
    private static final DustParticleOptions LINK = new DustParticleOptions(new Vector3f(0.2F, 1.0F, 0.45F), 0.8F);
    private static final DustParticleOptions CUT = new DustParticleOptions(new Vector3f(1.0F, 0.25F, 0.2F), 0.8F);

    public LedConnectorItem(Properties properties) {
        super(properties);
    }

    /** Côté visé : vers le bord de la face cliquée le plus proche du point de clic (accroupi : la face elle-même). */
    public static Direction targetSide(UseOnContext context) {
        Direction face = context.getClickedFace();
        Player player = context.getPlayer();
        if (player != null && player.isSecondaryUseActive()) {
            return face;
        }
        Vec3 hit = context.getClickLocation().subtract(Vec3.atCenterOf(context.getClickedPos()));
        double[] v = {hit.x, hit.y, hit.z};
        v[face.getAxis().ordinal()] = 0;
        int axis = Math.abs(v[0]) >= Math.abs(v[1]) ? 0 : 1;
        if (Math.abs(v[2]) > Math.abs(v[axis])) {
            axis = 2;
        }
        Direction.Axis a = Direction.Axis.values()[axis];
        return Direction.fromAxisAndDirection(a, v[axis] >= 0 ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!LightStripBlock.chainable(level.getBlockState(pos))) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Player player = context.getPlayer();
        Direction side = targetSide(context);
        BlockPos other = pos.relative(side);
        String dir = "direction." + MostLight.MOD_ID + "." + side.getName();
        if (!(level.getBlockEntity(pos) instanceof LampBlockEntity lamp)) {
            return InteractionResult.FAIL;
        }
        boolean connect = !lamp.connected(side);
        if (connect && !(LightStripBlock.chainable(level.getBlockState(other)) && level.getBlockEntity(other) instanceof LampBlockEntity)) {
            message(player, Component.translatable("message." + MostLight.MOD_ID + ".no_strip", Component.translatable(dir)).withStyle(ChatFormatting.GOLD));
            return InteractionResult.FAIL;
        }
        lamp.setConnected(side, connect);
        if (level.getBlockEntity(other) instanceof LampBlockEntity neighbor && LightStripBlock.chainable(level.getBlockState(other))) {
            neighbor.setConnected(side.getOpposite(), connect);
        }
        LightStripBlock.updateChain(level, pos);
        if (!connect) {
            LightStripBlock.updateChain(level, other);
        }
        List<BlockPos> chain = LightStripBlock.chain(level, pos);
        int limit = MostLightConfig.get(MostLightConfig.LED_CHAIN_LENGTH);
        Component state = Component.translatable("message." + MostLight.MOD_ID + (connect ? ".strip_linked" : ".strip_unlinked"),
                Component.translatable(dir), chain.size());
        message(player, chain.size() >= limit
                ? state.copy().append(Component.translatable("message." + MostLight.MOD_ID + ".chain_limit", limit).withStyle(ChatFormatting.RED))
                : state);
        level.playSound(null, pos, SoundEvents.COPPER_BULB_TURN_ON, SoundSource.BLOCKS, 0.6F, connect ? 1.4F : 0.8F);
        if (level instanceof ServerLevel server) {
            showChain(server, chain);
            Vec3 edge = Vec3.atCenterOf(pos).add(Vec3.atLowerCornerOf(side.getNormal()).scale(0.5));
            server.sendParticles(connect ? LINK : CUT, edge.x, edge.y, edge.z, 8, 0.08, 0.08, 0.08, 0);
        }
        return InteractionResult.CONSUME;
    }

    /** Une petite étincelle verte sur chaque liaison de la chaîne. */
    public static void showChain(ServerLevel level, List<BlockPos> chain) {
        for (BlockPos pos : chain) {
            if (level.getBlockEntity(pos) instanceof LampBlockEntity lamp) {
                for (Direction d : Direction.values()) {
                    if (lamp.connected(d)) {
                        Vec3 edge = Vec3.atCenterOf(pos).add(Vec3.atLowerCornerOf(d.getNormal()).scale(0.5));
                        level.sendParticles(LINK, edge.x, edge.y, edge.z, 1, 0, 0, 0, 0);
                    }
                }
            }
        }
    }

    private static void message(Player player, Component text) {
        if (player != null) {
            player.displayClientMessage(text, true);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip." + MostLight.MOD_ID + ".connector").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip." + MostLight.MOD_ID + ".connector_sneak").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip." + MostLight.MOD_ID + ".connector_limit",
                MostLightConfig.get(MostLightConfig.LED_CHAIN_LENGTH)).withStyle(ChatFormatting.DARK_GRAY));
    }
}
