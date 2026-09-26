package com.eveportalnether.particle;

import com.eveportalnether.registry.ModParticles;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Coloured mote that orbits inward toward the portal centre.
 * <p>
 * Spawn it with {@code count = 0} so the velocity slots carry the orbit instead of motion:
 * {@code xd} = starting radius, {@code yd} = starting angle (radians), {@code zd} = 1 when the window plane
 * spans X/Y, 0 when it spans Z/Y.
 */
public record SpiralParticleOptions(int rgb) implements ParticleOptions {
    public static final MapCodec<SpiralParticleOptions> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(Codec.INT.fieldOf("color").forGetter(SpiralParticleOptions::rgb))
            .apply(instance, SpiralParticleOptions::new));
    public static final StreamCodec<ByteBuf, SpiralParticleOptions> STREAM_CODEC =
            ByteBufCodecs.INT.map(SpiralParticleOptions::new, SpiralParticleOptions::rgb);

    @Override
    public ParticleType<?> getType() {
        return ModParticles.WORMHOLE_SPIRAL.get();
    }
}
