package com.example.eyemark.server;

import com.example.eyemark.EyeMarkMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EyeMarkMod.MODID)
public final class ServerEvents {
    private ServerEvents() {}

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        EyeMarkCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) EyeMarkServer.sync(p);
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) EyeMarkServer.sync(p);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) EyeMarkServer.sync(p);
    }
}
