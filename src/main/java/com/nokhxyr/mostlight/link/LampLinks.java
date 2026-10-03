package com.nokhxyr.mostlight.link;

import com.nokhxyr.mostlight.MostLight;
import com.nokhxyr.mostlight.MostLightConfig;
import com.nokhxyr.mostlight.block.LampBlock;
import com.nokhxyr.mostlight.component.ModComponents;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Liaison entre interrupteurs/télécommande et lampes. */
public final class LampLinks {
    /** Limites réglables dans config/mostlight-common.toml. */
    public static int maxLinks() {
        return MostLightConfig.get(MostLightConfig.SWITCH_MAX_LINKS);
    }

    public static int range() {
        return MostLightConfig.get(MostLightConfig.SWITCH_RANGE);
    }

    private LampLinks() {}

    public static List<BlockPos> get(ItemStack stack) {
        return stack.getOrDefault(ModComponents.LINKS.get(), List.of());
    }

    /**
     * Lie ou délie la lampe visée à l'objet tenu. Renvoie le message à afficher.
     */
    public static Component toggleLink(ItemStack stack, BlockPos lamp) {
        List<BlockPos> links = new ArrayList<>(get(stack));
        BlockPos key = lamp.immutable();
        String result;
        if (links.remove(key)) {
            result = "unlinked";
        } else if (links.size() >= maxLinks()) {
            return Component.translatable("message." + MostLight.MOD_ID + ".link_full", maxLinks()).withStyle(ChatFormatting.RED);
        } else {
            links.add(key);
            result = "linked";
        }
        if (links.isEmpty()) {
            stack.remove(ModComponents.LINKS.get());
        } else {
            stack.set(ModComponents.LINKS.get(), List.copyOf(links));
        }
        return Component.translatable("message." + MostLight.MOD_ID + "." + result, links.size());
    }

    /** Lampes liées encore présentes, chargées et à portée. */
    private static List<BlockPos> live(Level level, BlockPos origin, List<BlockPos> links) {
        List<BlockPos> out = new ArrayList<>();
        int range = range();
        for (BlockPos pos : links) {
            if (pos.closerThan(origin, range) && level.isLoaded(pos) && level.getBlockState(pos).getBlock() instanceof LampBlock) {
                out.add(pos);
            }
        }
        return out;
    }

    /** Inverse l'ensemble : si une lampe est allumée, tout s'éteint, sinon tout s'allume. Renvoie le nouvel état. */
    public static boolean toggleAll(Level level, BlockPos origin, List<BlockPos> links) {
        List<BlockPos> lamps = live(level, origin, links);
        boolean anyLit = lamps.stream().anyMatch(p -> level.getBlockState(p).getValue(LampBlock.LIT));
        for (BlockPos pos : lamps) {
            BlockState state = level.getBlockState(pos);
            ((LampBlock) state.getBlock()).setLit(level, pos, state, !anyLit);
        }
        return !anyLit;
    }

    /** Ventilateurs liés : si l'un tourne, tous s'arrêtent, sinon tous démarrent. Renvoie le nouvel état. */
    public static boolean toggleFans(Level level, BlockPos origin, List<BlockPos> links) {
        List<BlockPos> fans = live(level, origin, links).stream()
                .filter(p -> level.getBlockState(p).getBlock() instanceof com.nokhxyr.mostlight.block.FanLampBlock).toList();
        boolean anyOn = fans.stream().anyMatch(p -> level.getBlockState(p).getValue(com.nokhxyr.mostlight.block.FanLampBlock.FAN));
        for (BlockPos pos : fans) {
            BlockState state = level.getBlockState(pos);
            ((com.nokhxyr.mostlight.block.FanLampBlock) state.getBlock()).setFan(level, pos, state, !anyOn);
        }
        return !anyOn;
    }

    /** Passe toutes les lampes liées au niveau de luminosité suivant. Renvoie le niveau de lumière obtenu. */
    public static int cycleBrightness(Level level, BlockPos origin, List<BlockPos> links) {
        List<BlockPos> lamps = live(level, origin, links);
        if (lamps.isEmpty()) {
            return 0;
        }
        BlockState first = level.getBlockState(lamps.get(0));
        int next = first.getValue(LampBlock.LIT) ? first.getValue(LampBlock.BRIGHTNESS) + 1 : 0;
        for (BlockPos pos : lamps) {
            BlockState state = level.getBlockState(pos);
            ((LampBlock) state.getBlock()).setBrightness(level, pos, state, next);
        }
        return LampBlock.lightLevel(level.getBlockState(lamps.get(0)));
    }

    /** Réponse commune quand rien n'est lié. */
    public static void warnNoLinks(Player player) {
        player.displayClientMessage(Component.translatable("message." + MostLight.MOD_ID + ".no_links").withStyle(ChatFormatting.GOLD), true);
    }

    public static void click(Level level, BlockPos pos, boolean on) {
        level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.4F, on ? 0.9F : 0.7F);
    }

    /** Lignes d'info-bulle communes aux objets de liaison. */
    public static void appendTooltip(ItemStack stack, List<Component> tooltip, String usageKey) {
        tooltip.add(Component.translatable("tooltip." + MostLight.MOD_ID + ".links", get(stack).size()).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip." + MostLight.MOD_ID + ".link_hint").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip." + MostLight.MOD_ID + "." + usageKey).withStyle(ChatFormatting.GRAY));
    }
}
