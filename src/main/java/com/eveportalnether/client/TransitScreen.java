package com.eveportalnether.client;

import com.eveportalnether.EvePortalNether;
import com.eveportalnether.network.TransitPayload;
import com.eveportalnether.world.PortalColor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Replaces the vanilla "loading terrain" background during a wormhole jump with the portal's own swirl
 * and colour, then fades the screen out of that colour once the player has arrived.
 */
public final class TransitScreen {
    public static final ResourceLocation LAYER_ID = ResourceLocation.fromNamespaceAndPath(EvePortalNether.MODID, "transit");
    private static final ResourceLocation WINDOW_SPRITE = ResourceLocation.fromNamespaceAndPath(EvePortalNether.MODID, "block/wormhole_center");
    private static final long ACTIVE_MS = 30_000L;
    private static final long ARRIVAL_FADE_MS = 1_400L;

    private static PortalColor color;
    private static String signature = "";
    private static long startedAt = -1;
    private static long arrivedAt = -1;

    private TransitScreen() {
    }

    public static void begin(TransitPayload payload) {
        color = payload.color();
        signature = payload.signature();
        startedAt = System.currentTimeMillis();
        arrivedAt = startedAt;
    }

    public static void clear() {
        color = null;
        startedAt = -1;
        arrivedAt = -1;
    }

    private static boolean inTransit() {
        return color != null && startedAt >= 0 && System.currentTimeMillis() - startedAt < ACTIVE_MS;
    }

    public static void onScreenRender(ScreenEvent.Render.Pre event) {
        if (!(event.getScreen() instanceof ReceivingLevelScreen) || !inTransit()) {
            return;
        }
        event.setCanceled(true);
        GuiGraphics g = event.getGuiGraphics();
        int w = g.guiWidth();
        int h = g.guiHeight();
        long now = System.currentTimeMillis();
        float t = (now - startedAt) / 1000.0F;

        g.fill(0, 0, w, h, 0xFF000000);
        // fill() is batched while sprite blits draw immediately; flush so the black backdrop stays underneath.
        g.flush();
        drawSwirl(g, w, h, 1.0F);
        drawVignette(g, w, h, 0.55F);

        Minecraft minecraft = Minecraft.getInstance();
        int cx = w / 2;
        int cy = h / 2;
        float pulse = 0.75F + 0.25F * Mth.sin(t * 5.0F);
        int textColor = argb(pulse, lighten(color.rgb()));
        g.drawCenteredString(minecraft.font, Component.translatable("transit.eveportalnether.title"), cx, cy - 22, textColor);
        g.drawCenteredString(minecraft.font, Component.literal(signature), cx, cy - 6, 0xFFFFFFFF);
        String dots = ".".repeat(1 + (int) (t * 2.5F) % 3);
        g.drawCenteredString(minecraft.font, Component.translatable("transit.eveportalnether.loading").append(dots), cx, cy + 14, 0xFFA9BCCF);
        // Arrival fade starts only after the loading screen actually goes away.
        arrivedAt = now;
    }

    public static void renderArrival(GuiGraphics g, DeltaTracker delta) {
        if (color == null || arrivedAt < 0 || Minecraft.getInstance().screen instanceof ReceivingLevelScreen) {
            return;
        }
        long elapsed = System.currentTimeMillis() - arrivedAt;
        if (elapsed >= ARRIVAL_FADE_MS) {
            clear();
            return;
        }
        float fade = 1.0F - elapsed / (float) ARRIVAL_FADE_MS;
        fade *= fade;
        int w = g.guiWidth();
        int h = g.guiHeight();
        g.flush();
        drawSwirl(g, w, h, fade);
        drawVignette(g, w, h, 0.4F * fade);
    }

    /**
     * Same trick as the vanilla Nether portal loading screen: the animated block sprite from the atlas,
     * stretched over the whole screen. The atlas keeps animating while the level loads.
     */
    private static void drawSwirl(GuiGraphics g, int w, int h, float alpha) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(WINDOW_SPRITE);
        g.blit(0, 0, 0, w, h, sprite, brighten(color.red()), brighten(color.green()), brighten(color.blue()), alpha);
    }

    private static float brighten(float channel) {
        return Math.min(1.0F, channel * 1.15F + 0.08F);
    }

    private static void drawVignette(GuiGraphics g, int w, int h, float strength) {
        int band = Math.max(24, h / 3);
        int dark = argb(strength, 0x000000);
        g.fillGradient(0, 0, w, band, dark, 0x00000000);
        g.fillGradient(0, h - band, w, h, 0x00000000, dark);
        int core = argb(strength * 0.55F, 0x000000);
        g.fill(0, h / 2 - 30, w, h / 2 + 28, core);
    }

    private static int lighten(int rgb) {
        int r = (rgb >> 16 & 0xFF) + 255 >> 1;
        int gr = (rgb >> 8 & 0xFF) + 255 >> 1;
        int b = (rgb & 0xFF) + 255 >> 1;
        return r << 16 | gr << 8 | b;
    }

    private static int argb(float alpha, int rgb) {
        return Mth.clamp((int) (alpha * 255.0F), 5, 255) << 24 | rgb;
    }
}
