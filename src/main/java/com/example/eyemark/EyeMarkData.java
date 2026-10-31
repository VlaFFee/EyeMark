package com.example.eyemark;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Метки всех игроков, хранятся в мире (переживают перезапуск). */
public final class EyeMarkData extends SavedData {
    public static final String NAME = "eyemark_markers";
    public static final int MAX_PER_PLAYER = 64;

    private final Map<UUID, Map<String, Marker>> byPlayer = new HashMap<>();
    private int nextId = 1;

    public static EyeMarkData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(EyeMarkData::load, EyeMarkData::new, NAME);
    }

    public static EyeMarkData load(CompoundTag tag) {
        EyeMarkData d = new EyeMarkData();
        d.nextId = Math.max(1, tag.getInt("Next"));
        ListTag players = tag.getList("Players", Tag.TAG_COMPOUND);
        for (int i = 0; i < players.size(); i++) {
            CompoundTag pt = players.getCompound(i);
            UUID id;
            try {
                id = UUID.fromString(pt.getString("Player"));
            } catch (IllegalArgumentException e) {
                continue;
            }
            Map<String, Marker> map = new LinkedHashMap<>();
            ListTag list = pt.getList("Markers", Tag.TAG_COMPOUND);
            for (int j = 0; j < list.size(); j++) {
                Marker m = Marker.load(list.getCompound(j));
                if (!m.id.isEmpty()) map.put(m.id, m);
            }
            if (!map.isEmpty()) d.byPlayer.put(id, map);
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("Next", nextId);
        ListTag players = new ListTag();
        for (Map.Entry<UUID, Map<String, Marker>> e : byPlayer.entrySet()) {
            if (e.getValue().isEmpty()) continue;
            CompoundTag pt = new CompoundTag();
            pt.putString("Player", e.getKey().toString());
            ListTag list = new ListTag();
            for (Marker m : e.getValue().values()) list.add(m.save());
            pt.put("Markers", list);
            players.add(pt);
        }
        tag.put("Players", players);
        return tag;
    }

    public int nextId() {
        setDirty();
        return nextId++;
    }

    public Map<String, Marker> of(UUID player) {
        Map<String, Marker> m = byPlayer.get(player);
        return m == null ? Collections.emptyMap() : m;
    }

    /** @return false, если у игрока уже максимум меток (замена метки с тем же id всегда разрешена). */
    public boolean put(UUID player, Marker marker) {
        Map<String, Marker> map = byPlayer.computeIfAbsent(player, k -> new LinkedHashMap<>());
        if (!map.containsKey(marker.id) && map.size() >= MAX_PER_PLAYER) return false;
        map.put(marker.id, marker);
        setDirty();
        return true;
    }

    public Marker remove(UUID player, String id) {
        Map<String, Marker> map = byPlayer.get(player);
        if (map == null) return null;
        Marker m = map.remove(id);
        if (m != null) {
            if (map.isEmpty()) byPlayer.remove(player);
            setDirty();
        }
        return m;
    }

    public int clear(UUID player) {
        Map<String, Marker> map = byPlayer.remove(player);
        if (map == null) return 0;
        setDirty();
        return map.size();
    }
}
