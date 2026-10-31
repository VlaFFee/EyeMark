package com.example.eyemark.client;

import com.example.eyemark.EyeMarkMod;
import com.example.eyemark.ModSounds;
import com.example.eyemark.net.Net;
import com.example.eyemark.net.Packets;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/** Рисует метки-глазки на экране или на краю экрана со стрелкой в их сторону. */
public final class EyeOverlay {
    private static final ResourceLocation EYE = new ResourceLocation(EyeMarkMod.MODID, "textures/gui/eye.png");
    private static final ResourceLocation ARROW = new ResourceLocation(EyeMarkMod.MODID, "textures/gui/arrow.png");

    /** Сколько метка живёт после того, как на неё посмотрели (2–3 секунды). */
    private static final long DISAPPEAR_MS = 2500;
    private static final long FADE_OUT_MS = 800;
    private static final long FADE_IN_MS = 300;

    private static final float ICON_SCALE = 1.5f;     // 16px * 1.5 = 24px
    private static final float ARROW_SCALE = 1.25f;
    private static final float MARGIN = 40f;          // отступ иконки от края экрана
    private static final float ARROW_DIST = 26f;      // насколько стрелка вынесена наружу от иконки
    private static final float ON_SCREEN_MARGIN = 20f;
    private static final float GAZE_RADIUS = 14f;     // радиус «прицела» в GUI-пикселях

    private EyeOverlay() {}

    public static void render(ForgeGui gui, GuiGraphics g, float partialTick, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen != null || ClientMarkers.LIST.isEmpty()) return;

        long now = Util.getMillis();
        float cx = w / 2f;
        float cy = h / 2f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        java.util.List<ClientMarkers.CM> list = ClientMarkers.LIST;
        for (int i = list.size() - 1; i >= 0; i--) {
            ClientMarkers.CM m = list.get(i);
            if (m.seenAt > 0 && now - m.seenAt > DISAPPEAR_MS) {
                list.remove(i);
                continue;
            }
            if (!m.projected) continue;

            float px = 0, py = 0;
            boolean onScreen = false;
            if (m.front) {
                px = (m.ndcX * 0.5f + 0.5f) * w;
                py = (1f - (m.ndcY * 0.5f + 0.5f)) * h;
                onScreen = Math.abs(px - cx) <= cx - ON_SCREEN_MARGIN && Math.abs(py - cy) <= cy - ON_SCREEN_MARGIN;

                // задел прицелом хоть на мгновение — запускаем исчезновение
                if (m.seenAt == 0) {
                    float ddx = px - cx;
                    float ddy = py - cy;
                    if (ddx * ddx + ddy * ddy <= GAZE_RADIUS * GAZE_RADIUS) {
                        m.seenAt = now;
                        Net.CHANNEL.sendToServer(new Packets.Seen(m.id));
                        ClientMarkers.play(ModSounds.SEEN.get(), 1.0f, 0.5f);
                    }
                }
            }

            float ix, iy;
            float arrowAngle = 0;
            if (onScreen) {
                ix = px;
                iy = py;
            } else {
                // направление от центра экрана к метке
                float dx, dy;
                if (m.front) {
                    dx = px - cx;
                    dy = py - cy;
                } else {
                    dx = m.viewX;
                    dy = -m.viewY;
                }
                if (Math.abs(dx) < 1e-4f && Math.abs(dy) < 1e-4f) dy = 1f;
                float len = (float) Math.sqrt(dx * dx + dy * dy);
                float nx = dx / len;
                float ny = dy / len;
                float halfW = cx - MARGIN;
                float halfH = cy - MARGIN;
                float tx = Math.abs(nx) < 1e-5f ? Float.MAX_VALUE : halfW / Math.abs(nx);
                float ty = Math.abs(ny) < 1e-5f ? Float.MAX_VALUE : halfH / Math.abs(ny);
                float t = Math.min(tx, ty);
                ix = cx + nx * t;
                iy = cy + ny * t;
                arrowAngle = (float) Math.atan2(ny, nx);
                drawArrow(g, ix + nx * ARROW_DIST, iy + ny * ARROW_DIST, arrowAngle, alphaOf(m, now));
            }

            drawEye(g, mc, m, ix, iy, now);
        }

        g.setColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    private static float alphaOf(ClientMarkers.CM m, long now) {
        float in = clamp01((now - m.spawnedAt) / (float) FADE_IN_MS);
        float out = 1f;
        if (m.seenAt > 0) {
            long remain = DISAPPEAR_MS - (now - m.seenAt);
            out = clamp01(remain / (float) FADE_OUT_MS);
        }
        return in * out;
    }

    private static void drawEye(GuiGraphics g, Minecraft mc, ClientMarkers.CM m, float x, float y, long now) {
        float alpha = alphaOf(m, now);
        if (alpha <= 0.01f) return;
        float scale = ICON_SCALE;
        if (m.seenAt > 0) {
            // лёгкая пульсация, пока метка исчезает
            scale *= 1f + 0.10f * (float) Math.sin((now - m.seenAt) * 0.02);
        } else {
            scale *= 1f + 0.04f * (float) Math.sin(now * 0.004);
        }

        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(x, y, 0);
        ps.scale(scale, scale, 1f);
        g.setColor(1f, 1f, 1f, alpha);
        g.blit(EYE, -8, -8, 0, 0, 16, 16, 16, 16);
        ps.popPose();

        int a = (int) (alpha * 255f);
        if (a >= 16) {
            ps.pushPose();
            ps.translate(x, y + 16f, 0);
            ps.scale(0.8f, 0.8f, 1f);
            g.setColor(1f, 1f, 1f, 1f);
            g.drawCenteredString(mc.font, m.label, 0, 0, (a << 24) | 0xFFFFFF);
            ps.popPose();
        }
        g.setColor(1f, 1f, 1f, 1f);
    }

    private static void drawArrow(GuiGraphics g, float x, float y, float angle, float alpha) {
        if (alpha <= 0.01f) return;
        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(x, y, 0);
        ps.mulPose(Axis.ZP.rotation(angle));
        ps.scale(ARROW_SCALE, ARROW_SCALE, 1f);
        g.setColor(1f, 1f, 1f, alpha);
        g.blit(ARROW, -4, -4, 0, 0, 8, 8, 8, 8);
        ps.popPose();
        g.setColor(1f, 1f, 1f, 1f);
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : Math.min(v, 1f);
    }
}
