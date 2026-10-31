package com.example.eyemark.net;

import com.example.eyemark.EyeMarkMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class Net {
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(EyeMarkMod.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private Net() {}

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, Packets.Sync.class, Packets.Sync::encode, Packets.Sync::decode, Packets.Sync::handle);
        CHANNEL.registerMessage(id++, Packets.Seen.class, Packets.Seen::encode, Packets.Seen::decode, Packets.Seen::handle);
    }
}
