package com.eveportalnether.registry;

import com.eveportalnether.EvePortalNether;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, EvePortalNether.MODID);

    /**
     * Marker effect: the portal the player just came through refuses them while it lasts.
     */
    public static final DeferredHolder<MobEffect, MobEffect> SPATIAL_TRACE = EFFECTS.register("spatial_trace",
            () -> new MobEffect(MobEffectCategory.NEUTRAL, 0x8A4BFF) {
            });

    public static void register(IEventBus eventBus) {
        EFFECTS.register(eventBus);
    }
}
