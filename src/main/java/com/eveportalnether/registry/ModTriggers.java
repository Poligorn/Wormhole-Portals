package com.eveportalnether.registry;

import com.eveportalnether.EvePortalNether;
import com.eveportalnether.advancement.WormholeTrigger;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModTriggers {
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, EvePortalNether.MODID);

    public static final DeferredHolder<CriterionTrigger<?>, WormholeTrigger> WORMHOLE =
            TRIGGERS.register("wormhole", WormholeTrigger::new);

    private ModTriggers() {
    }
}
