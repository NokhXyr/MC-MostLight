package com.nokhxyr.mostlight.showcase;

import com.nokhxyr.mostlight.MostLight;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** /mostlight showcase : construit la galerie de lampes autour du joueur (opérateurs). */
@EventBusSubscriber(modid = MostLight.MOD_ID)
public final class ShowcaseCommand {
    private ShowcaseCommand() {}

    @SubscribeEvent
    static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(MostLight.MOD_ID)
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("showcase").executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    ShowcaseBuilder.Result result = ShowcaseBuilder.build(player.serverLevel(), player.blockPosition());
                    context.getSource().sendSuccess(() -> Component.literal(
                            "MostLight : " + result.checks().size() + " lampes posées autour de toi"), false);
                    return result.checks().size();
                })));
    }
}
