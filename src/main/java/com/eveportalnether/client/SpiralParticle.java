package com.eveportalnether.client;

import com.eveportalnether.particle.SpiralParticleOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;

/**
 * Glowing mote that spirals from the rim of the portal window into its centre and fades as it falls in.
 */
public class SpiralParticle extends TextureSheetParticle {
    private final double centerX;
    private final double centerY;
    private final double centerZ;
    private final double startRadius;
    private final boolean alongX;
    private final float spin;
    private final float baseSize;
    private final SpriteSet sprites;
    private double angle;

    protected SpiralParticle(ClientLevel level, double x, double y, double z, double radius, double angle,
                             boolean alongX, int rgb, SpriteSet sprites) {
        super(level, x, y, z);
        this.centerX = x;
        this.centerY = y;
        this.centerZ = z;
        this.startRadius = Math.max(0.3, radius);
        this.angle = angle;
        this.alongX = alongX;
        this.sprites = sprites;
        this.spin = (0.14F + random.nextFloat() * 0.08F) * (random.nextBoolean() ? 1.0F : 0.85F);
        this.lifetime = 34 + random.nextInt(18);
        this.baseSize = 0.09F + random.nextFloat() * 0.07F;
        this.quadSize = baseSize;
        this.hasPhysics = false;
        this.gravity = 0.0F;
        float jitter = 0.85F + random.nextFloat() * 0.3F;
        this.rCol = Math.min(1.0F, (rgb >> 16 & 0xFF) / 255.0F * jitter + 0.1F);
        this.gCol = Math.min(1.0F, (rgb >> 8 & 0xFF) / 255.0F * jitter + 0.1F);
        this.bCol = Math.min(1.0F, (rgb & 0xFF) / 255.0F * jitter + 0.1F);
        this.alpha = 0.0F;
        setSpriteFromAge(sprites);
        place();
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
    }

    private void place() {
        float progress = age / (float) lifetime;
        double radius = startRadius * (1.0 - progress * progress * 0.35 - progress * 0.62);
        double across = Math.cos(angle) * radius;
        double up = Math.sin(angle) * radius * 1.35;
        if (alongX) {
            setPos(centerX + across, centerY + up, centerZ);
        } else {
            setPos(centerX, centerY + up, centerZ + across);
        }
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (age++ >= lifetime) {
            remove();
            return;
        }
        float progress = age / (float) lifetime;
        // Orbit speeds up as the mote is pulled inward, like water into a drain.
        angle += spin * (1.0F + progress * 2.2F);
        place();
        alpha = Mth.clamp(Math.min(progress * 6.0F, (1.0F - progress) * 2.5F), 0.0F, 0.95F);
        quadSize = baseSize * (1.0F - progress * 0.6F);
        setSpriteFromAge(sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    public static class Provider implements ParticleProvider<SpiralParticleOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SpiralParticleOptions options, ClientLevel level, double x, double y, double z,
                                       double radius, double angle, double axisFlag) {
            return new SpiralParticle(level, x, y, z, radius, angle, axisFlag > 0.5, options.rgb(), sprites);
        }
    }
}
