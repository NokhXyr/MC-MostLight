package com.nokhxyr.mostlight.stress;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundChunkBatchReceivedPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.network.connection.ConnectionType;

/**
 * Joueur simulé : un vrai ServerPlayer (chunks, suivi des entités, paquets, interactions) relié à une
 * {@link FakeConnection}. Il est tické comme un joueur connecté (doTick, acquittement des chunks).
 */
public final class StressBot extends ServerPlayer {
    /** Distance de rendu demandée par le « client » : 12 chunks, comme un joueur typique. */
    private static final ClientInformation INFO = new ClientInformation("fr_fr", 12, ChatVisiblity.FULL, true, 0x7F,
            HumanoidArm.RIGHT, false, true);

    private final FakeConnection link;

    private StressBot(MinecraftServer server, ServerLevel level, GameProfile profile, FakeConnection link) {
        super(server, level, profile, INFO);
        this.link = link;
    }

    /** Connecte un joueur simulé par le même chemin qu'un vrai joueur (PlayerList.placeNewPlayer). */
    public static StressBot join(MinecraftServer server, ServerLevel level, String name, PacketMeter meter, double x, double y, double z) {
        GameProfile profile = new GameProfile(UUIDUtil.createOfflinePlayerUUID(name), name);
        // a real player (singleplayer host) gives the mod channels negotiated with an actual client
        io.netty.channel.Channel template = server.getPlayerList().getPlayers().stream()
                .filter(p -> !(p instanceof StressBot))
                .map(p -> p.connection.getConnection().channel())
                .findFirst().orElse(null);
        FakeConnection connection = new FakeConnection(meter, template);
        StressBot bot = new StressBot(server, level, profile, connection);
        server.getPlayerList().placeNewPlayer(connection, bot,
                new CommonListenerCookie(profile, 0, INFO, false, ConnectionType.OTHER));
        bot.setGameMode(GameType.CREATIVE);
        bot.getAbilities().flying = true;
        bot.onUpdateAbilities();
        bot.teleportTo(level, x, y, z, 0, 20);
        return bot;
    }

    @Override
    public void tick() {
        for (int i = link.takeAcks(); i > 0; i--) {
            connection.handleChunkBatchReceived(new ServerboundChunkBatchReceivedPacket(64.0F));
        }
        super.tick();
        // ce que ServerGamePacketListenerImpl.tick fait pour un vrai joueur (sans keep-alive)
        doTick();
    }

    /** Déplacement comme un paquet de mouvement : position, puis mise à jour des chunks suivis. */
    public void moveTo(double x, double y, double z, float yaw) {
        absMoveTo(x, y, z, yaw, 20);
        connection.resetPosition();
        serverLevel().getChunkSource().move(this);
    }

    public void leave() {
        connection.onDisconnect(new DisconnectionDetails(Component.literal("fin du test de charge")));
    }
}
