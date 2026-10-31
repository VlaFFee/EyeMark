package com.example.eyemark;

import net.minecraft.nbt.CompoundTag;

/** Одна метка игрока: id, измерение, координаты и (необязательно) команда, которая выполнится, когда игрок на неё посмотрит. */
public final class Marker {
    public final String id;
    public final String dim;
    public final double x;
    public final double y;
    public final double z;
    public final String command;

    public Marker(String id, String dim, double x, double y, double z, String command) {
        this.id = id;
        this.dim = dim;
        this.x = x;
        this.y = y;
        this.z = z;
        this.command = command == null ? "" : command;
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putString("Id", id);
        t.putString("Dim", dim);
        t.putDouble("X", x);
        t.putDouble("Y", y);
        t.putDouble("Z", z);
        if (!command.isEmpty()) t.putString("Cmd", command);
        return t;
    }

    public static Marker load(CompoundTag t) {
        return new Marker(t.getString("Id"), t.getString("Dim"),
                t.getDouble("X"), t.getDouble("Y"), t.getDouble("Z"), t.getString("Cmd"));
    }
}
