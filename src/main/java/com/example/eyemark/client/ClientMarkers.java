package com.example.eyemark.client;

import com.example.eyemark.net.Packets;
import com.example.eyemark.ModSounds;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Клиентское состояние меток + проекция мировых координат на экран. Только клиент. */
public final class ClientMarkers {
    /** Сколько после «увидел» игнорируем повторную рассылку этой же метки сервером. */
    private static final long IGNORE_SERVER_MS = 6000;

    public static final class CM {
        public final String id;
        public double x, y, z;
        public final long spawnedAt;
        /** 0 — ещё не задета; иначе момент, когда игрок навёл на неё прицел. */
        public long seenAt;
        // результат проекции (обновляется каждый кадр мира)
        public boolean projected;
        public boolean front;
        public float ndcX, ndcY;
        public float viewX, viewY;
        public float dist;
        /** Кэш подписи расстояния, чтобы не создавать строку каждый кадр. */
        public String label = "";
        public int labelDist = -1;

        CM(String id, double x, double y, double z, long now) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.z = z;
            this.spawnedAt = now;
        }
    }

    private static final Vector4f TMP_V = new Vector4f();
    private static final Vector4f TMP_C = new Vector4f();

    public static final List<CM> LIST = new ArrayList<>();

    private ClientMarkers() {}

    public static void onSync(List<Packets.Entry> entries) {
        long now = Util.getMillis();
        Map<String, Packets.Entry> incoming = new HashMap<>();
        for (Packets.Entry e : entries) incoming.put(e.id, e);

        List<CM> next = new ArrayList<>();
        for (CM m : LIST) {
            Packets.Entry e = incoming.get(m.id);
            if (m.seenAt > 0) {
                // уже исчезает — даём доиграть анимацию
                if (now - m.seenAt < IGNORE_SERVER_MS) {
                    incoming.remove(m.id);
                    next.add(m);
                }
            } else if (e != null) {
                m.x = e.x;
                m.y = e.y;
                m.z = e.z;
                incoming.remove(m.id);
                next.add(m);
            }
            // иначе метку удалили на сервере — выкидываем
        }
        for (Packets.Entry e : incoming.values()) {
            next.add(new CM(e.id, e.x, e.y, e.z, now));
        }
        boolean added = !incoming.isEmpty();
        LIST.clear();
        LIST.addAll(next);
        if (added) play(ModSounds.APPEAR.get(), 1.0f, 0.55f); // один тихий звук на пачку новых меток
    }

    public static void play(SoundEvent ev, float pitch, float volume) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        mc.getSoundManager().play(SimpleSoundInstance.forUI(ev, pitch, volume));
    }

    public static void clear() {
        LIST.clear();
    }

    /** Проецирует все метки. view — матрица только с поворотом камеры, proj — проекционная. */
    public static void project(Matrix4f view, Matrix4f proj, Vec3 cam) {
        Vector4f v = TMP_V;
        Vector4f c = TMP_C;
        for (int i = 0, n = LIST.size(); i < n; i++) {
            CM m = LIST.get(i);
            double rx = m.x - cam.x;
            double ry = m.y - cam.y;
            double rz = m.z - cam.z;
            v.set((float) rx, (float) ry, (float) rz, 1f);
            view.transform(v);
            m.viewX = v.x;
            m.viewY = v.y;
            c.set(v);
            proj.transform(c);
            m.front = c.w > 0.001f;
            if (m.front) {
                m.ndcX = c.x / c.w;
                m.ndcY = c.y / c.w;
            }
            m.dist = (float) Math.sqrt(rx * rx + ry * ry + rz * rz);
            int d = Math.round(m.dist);
            if (d != m.labelDist) {
                m.labelDist = d;
                m.label = d + " m";
            }
            m.projected = true;
        }
    }
}
