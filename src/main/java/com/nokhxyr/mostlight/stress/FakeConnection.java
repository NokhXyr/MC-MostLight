package com.nokhxyr.mostlight.stress;

import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.AttributeKey;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.PacketListener;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundChunkBatchFinishedPacket;
import net.neoforged.neoforge.network.registration.ChannelAttributes;
import org.jetbrains.annotations.Nullable;

/**
 * Connexion d'un joueur simulé : rien ne part sur le réseau, chaque paquet est mesuré par le {@link PacketMeter}.
 * Les lots de chunks sont acquittés comme le ferait un client rapide, sinon le serveur arrête d'en envoyer.
 */
final class FakeConnection extends Connection {
    private static final Field CHANNEL;

    static {
        try {
            CHANNEL = Connection.class.getDeclaredField("channel");
            CHANNEL.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Connection.channel introuvable", e);
        }
    }

    private final PacketMeter meter;
    private final AtomicInteger pendingAcks = new AtomicInteger();
    private volatile boolean open = true;

    /**
     * @param template channel of a real player whose negotiated mod channels are copied (singleplayer host), or null.
     *     Without them NeoForge refuses mod payloads sent on join (KubeJS in modpacks, for example).
     */
    FakeConnection(PacketMeter meter, @Nullable Channel template) {
        super(PacketFlow.SERVERBOUND);
        this.meter = meter;
        EmbeddedChannel channel = new EmbeddedChannel();
        if (template != null) {
            copy(template, channel, ChannelAttributes.PAYLOAD_SETUP);
            copy(template, channel, ChannelAttributes.ADHOC_CHANNELS);
            copy(template, channel, ChannelAttributes.COMMON_CHANNELS);
            copy(template, channel, ChannelAttributes.CONNECTION_TYPE);
        }
        try {
            // NeoForge lit les attributs du canal (canaux négociés) : il en faut un, même vide
            CHANNEL.set(this, channel);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static <T> void copy(Channel from, Channel to, AttributeKey<T> key) {
        T value = from.attr(key).get();
        if (value != null) {
            to.attr(key).set(value);
        }
    }

    @Override
    public void send(Packet<?> packet, @Nullable PacketSendListener listener, boolean flush) {
        if (!open) {
            return;
        }
        meter.record(packet);
        if (packet instanceof ClientboundChunkBatchFinishedPacket) {
            pendingAcks.incrementAndGet();
        }
        if (listener != null) {
            listener.onSuccess();
        }
    }

    /** Lots de chunks reçus depuis le dernier appel. */
    int takeAcks() {
        return pendingAcks.getAndSet(0);
    }

    @Override
    public boolean isConnected() {
        return open;
    }

    @Override
    public void setReadOnly() {}

    @Override
    public void handleDisconnection() {}

    @Override
    public <T extends PacketListener> void setupInboundProtocol(ProtocolInfo<T> protocol, T listener) {}

    @Override
    public void setupOutboundProtocol(ProtocolInfo<?> protocol) {}

    @Override
    public void disconnect(DisconnectionDetails details) {
        open = false;
    }
}
