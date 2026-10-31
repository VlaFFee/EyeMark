package com.example.eyemark.server;

import com.example.eyemark.EyeMarkData;
import com.example.eyemark.EyeMarkMod;
import com.example.eyemark.Marker;
import com.example.eyemark.net.Net;
import com.example.eyemark.net.Packets;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** Серверная логика меток: рассылка списков и обработка «игрок посмотрел на метку». */
public final class EyeMarkServer {
    private EyeMarkServer() {}

    private static String dimOf(ServerPlayer player) {
        return player.level().dimension().location().toString();
    }

    /** Отправляет игроку актуальные метки для его текущего измерения. */
    public static void sync(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        String dim = dimOf(player);
        List<Packets.Entry> list = new ArrayList<>();
        for (Marker m : EyeMarkData.get(server).of(player.getUUID()).values()) {
            if (m.dim.equals(dim)) list.add(new Packets.Entry(m.id, m.x, m.y, m.z));
        }
        Net.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Packets.Sync(list));
    }

    /** Клиент сообщил, что игрок задел метку прицелом: убираем её навсегда и выполняем привязанную команду. */
    public static void onSeen(ServerPlayer player, String id) {
        if (player == null || id == null || id.isEmpty() || id.length() > 64) return;
        MinecraftServer server = player.getServer();
        if (server == null) return;
        EyeMarkData data = EyeMarkData.get(server);
        Marker m = data.of(player.getUUID()).get(id);
        if (m == null || !m.dim.equals(dimOf(player))) return; // метка из другого измерения: игнорируем
        data.remove(player.getUUID(), id);

        if (!m.command.isEmpty()) {
            try {
                CommandSourceStack source = player.createCommandSourceStack().withPermission(2).withSuppressedOutput();
                server.getCommands().performPrefixedCommand(source, m.command);
            } catch (Exception e) {
                EyeMarkMod.LOGGER.warn("[EyeMark] Command of marker '{}' failed: {}", id, m.command, e);
            }
        }
    }
}
