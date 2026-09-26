package com.eveportalnether.events;

import com.eveportalnether.EvePortalNether;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = EvePortalNether.MODID)
public class PortalEventHandler {

    @SubscribeEvent
    public static void onPortalSpawn(BlockEvent.PortalSpawnEvent event) {
        // Cancel the vanilla Nether portal creation
        event.setCanceled(true);
    }
}
