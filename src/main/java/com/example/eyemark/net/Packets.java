package com.example.eyemark.net;

import com.example.eyemark.client.ClientMarkers;
import com.example.eyemark.server.EyeMarkServer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class Packets {
    private Packets() {}

    /** Метка, как её видит клиент. */
    public static final class Entry {
        public final String id;
        public final double x;
        public final double y;
        public final double z;

        public Entry(String id, double x, double y, double z) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    /** Сервер → клиент: полный список меток игрока в его текущем измерении. */
    public static final class Sync {
        public final List<Entry> entries;

        public Sync(List<Entry> entries) {
            this.entries = entries;
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeVarInt(entries.size());
            for (Entry e : entries) {
                buf.writeUtf(e.id, 64);
                buf.writeDouble(e.x);
                buf.writeDouble(e.y);
                buf.writeDouble(e.z);
            }
        }

        public static Sync decode(FriendlyByteBuf buf) {
            int n = Math.min(buf.readVarInt(), 256);
            List<Entry> list = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                list.add(new Entry(buf.readUtf(64), buf.readDouble(), buf.readDouble(), buf.readDouble()));
            }
            return new Sync(list);
        }

        public void handle(Supplier<NetworkEvent.Context> sup) {
            NetworkEvent.Context ctx = sup.get();
            ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ClientMarkers.onSync(entries)));
            ctx.setPacketHandled(true);
        }
    }

    /** Клиент → сервер: игрок задел метку прицелом. */
    public static final class Seen {
        public final String id;

        public Seen(String id) {
            this.id = id;
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeUtf(id, 64);
        }

        public static Seen decode(FriendlyByteBuf buf) {
            return new Seen(buf.readUtf(64));
        }

        public void handle(Supplier<NetworkEvent.Context> sup) {
            NetworkEvent.Context ctx = sup.get();
            ctx.enqueueWork(() -> EyeMarkServer.onSeen(ctx.getSender(), id));
            ctx.setPacketHandled(true);
        }
    }
}
