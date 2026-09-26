package com.eveportalnether.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * One criterion for every wormhole moment. Advancement JSON selects the moment with
 * {@code "conditions": {"event": "travel"}}; see {@link WormholeEvents} for the names.
 */
public class WormholeTrigger extends SimpleCriterionTrigger<WormholeTrigger.Instance> {
    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    public void trigger(ServerPlayer player, String event) {
        trigger(player, instance -> instance.event().map(event::equals).orElse(true));
    }

    public record Instance(Optional<ContextAwarePredicate> player, Optional<String> event)
            implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
                Codec.STRING.optionalFieldOf("event").forGetter(Instance::event)
        ).apply(instance, Instance::new));
    }
}
