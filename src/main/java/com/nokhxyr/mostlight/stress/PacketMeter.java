package com.nokhxyr.mostlight.stress;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.zip.Deflater;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.BundlePacket;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.GameProtocols;
import net.neoforged.neoforge.network.connection.ConnectionType;

/**
 * Mesure le trafic envoyé aux joueurs simulés : chaque paquet est réellement encodé (codec du protocole de jeu)
 * puis compressé comme le ferait le serveur (seuil 256 octets), sur des threads à part comme Netty.
 */
public final class PacketMeter {
    private static final int COMPRESSION_THRESHOLD = 256;
    private static final int MAX_BACKLOG = 200_000;

    private final ExecutorService pool = Executors.newFixedThreadPool(4, runnable -> {
        Thread thread = new Thread(runnable, "MostLight-stress-encode");
        thread.setDaemon(true);
        return thread;
    });
    private final ThreadLocal<Deflater> deflater = ThreadLocal.withInitial(Deflater::new);
    private final ThreadLocal<byte[]> scratch = ThreadLocal.withInitial(() -> new byte[64 * 1024]);
    private final StreamCodec<ByteBuf, Packet<? super ClientGamePacketListener>> codec;
    private final AtomicInteger backlog = new AtomicInteger();

    final LongAdder packets = new LongAdder();
    final LongAdder rawBytes = new LongAdder();
    final LongAdder wireBytes = new LongAdder();
    final LongAdder failures = new LongAdder();
    final LongAdder dropped = new LongAdder();
    private final Map<String, LongAdder> bytesByType = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> countByType = new ConcurrentHashMap<>();
    private final Map<String, String> firstFailure = new ConcurrentHashMap<>();

    public PacketMeter(RegistryAccess access) {
        this.codec = GameProtocols.CLIENTBOUND_TEMPLATE.bind(RegistryFriendlyByteBuf.decorator(access, ConnectionType.OTHER)).codec();
    }

    /** Appelé sur le thread serveur pour chaque paquet envoyé à un joueur simulé. */
    public void record(Packet<?> packet) {
        if (packet instanceof BundlePacket<?> bundle) {
            for (Packet<?> sub : bundle.subPackets()) {
                record(sub);
            }
            return;
        }
        packets.increment();
        if (backlog.get() > MAX_BACKLOG) {
            dropped.increment();
            return;
        }
        backlog.incrementAndGet();
        pool.execute(() -> {
            try {
                encode(packet);
            } finally {
                backlog.decrementAndGet();
            }
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void encode(Packet<?> packet) {
        String type = packet.getClass().getSimpleName();
        ByteBuf buf = Unpooled.buffer(256);
        try {
            codec.encode(buf, (Packet) packet);
            int size = buf.readableBytes();
            int wire;
            if (size >= COMPRESSION_THRESHOLD) {
                byte[] input = new byte[size];
                buf.getBytes(buf.readerIndex(), input);
                Deflater d = deflater.get();
                d.reset();
                d.setInput(input);
                d.finish();
                byte[] out = scratch.get();
                int compressed = 0;
                while (!d.finished()) {
                    compressed += d.deflate(out);
                }
                wire = compressed + varIntSize(size);
            } else {
                wire = size + 1;
            }
            wire += varIntSize(wire);
            rawBytes.add(size);
            wireBytes.add(wire);
            bytesByType.computeIfAbsent(type, k -> new LongAdder()).add(wire);
            countByType.computeIfAbsent(type, k -> new LongAdder()).increment();
        } catch (Throwable e) {
            failures.increment();
            firstFailure.putIfAbsent(type, e.toString());
        } finally {
            buf.release();
        }
    }

    private static int varIntSize(int value) {
        int size = 1;
        while ((value & ~0x7F) != 0) {
            value >>>= 7;
            size++;
        }
        return size;
    }

    /** Attend que les paquets en file soient encodés (fin de phase). */
    public void drain() {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (backlog.get() > 0 && System.nanoTime() < deadline) {
            Thread.onSpinWait();
        }
    }

    public record Snapshot(long packets, long rawBytes, long wireBytes, long failures, long dropped) {
        public Snapshot minus(Snapshot o) {
            return new Snapshot(packets - o.packets, rawBytes - o.rawBytes, wireBytes - o.wireBytes, failures - o.failures, dropped - o.dropped);
        }
    }

    public Snapshot snapshot() {
        return new Snapshot(packets.sum(), rawBytes.sum(), wireBytes.sum(), failures.sum(), dropped.sum());
    }

    /** Types de paquets les plus lourds : [type, octets, nombre]. */
    public List<Object[]> topTypes(int limit) {
        return bytesByType.entrySet().stream()
                .sorted(Comparator.comparingLong((Map.Entry<String, LongAdder> e) -> e.getValue().sum()).reversed())
                .limit(limit)
                .map(e -> new Object[] {e.getKey(), e.getValue().sum(), countByType.get(e.getKey()).sum()})
                .toList();
    }

    public Map<String, String> failuresByType() {
        return firstFailure;
    }

    public void shutdown() {
        pool.shutdownNow();
    }
}
