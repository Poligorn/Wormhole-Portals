package com.eveportalnether.registry;

import com.eveportalnether.EvePortalNether;
import com.eveportalnether.block.entity.WormholeBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, EvePortalNether.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WormholeBlockEntity>> WORMHOLE_BLOCK_ENTITY = BLOCK_ENTITIES.register("wormhole",
        () -> BlockEntityType.Builder.of(WormholeBlockEntity::new, ModBlocks.WORMHOLE.get()).build(null)
    );

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
