package com.eveportalnether.registry;

import com.eveportalnether.EvePortalNether;
import com.eveportalnether.particle.SpiralParticleOptions;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, EvePortalNether.MODID);

    public static final DeferredHolder<ParticleType<?>, ParticleType<SpiralParticleOptions>> WORMHOLE_SPIRAL =
            PARTICLE_TYPES.register("wormhole_spiral", () -> new ParticleType<SpiralParticleOptions>(false) {
                @Override
                public MapCodec<SpiralParticleOptions> codec() {
                    return SpiralParticleOptions.CODEC;
                }

                @Override
                public StreamCodec<? super RegistryFriendlyByteBuf, SpiralParticleOptions> streamCodec() {
                    return SpiralParticleOptions.STREAM_CODEC;
                }
            });

    private ModParticles() {
    }
}
