package com.example.eyemark.client;

import com.example.eyemark.EyeMarkMod;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

public final class ClientSetup {
    private ClientSetup() {}

    @Mod.EventBusSubscriber(modid = EyeMarkMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {
        private ModBus() {}

        @SubscribeEvent
        public static void onOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAboveAll("eye_markers", EyeOverlay::render);
        }
    }

    @Mod.EventBusSubscriber(modid = EyeMarkMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class ForgeBus {
        private ForgeBus() {}

        @SubscribeEvent
        public static void onRenderLevel(RenderLevelStageEvent event) {
            if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
            if (ClientMarkers.LIST.isEmpty() || Minecraft.getInstance().player == null) return;
            // в этот момент PoseStack содержит только поворот камеры, а матрица проекции — актуальную
            Matrix4f view = new Matrix4f(event.getPoseStack().last().pose());
            Matrix4f proj = new Matrix4f(event.getProjectionMatrix());
            ClientMarkers.project(view, proj, event.getCamera().getPosition());
        }

        @SubscribeEvent
        public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            ClientMarkers.clear();
        }
    }
}
