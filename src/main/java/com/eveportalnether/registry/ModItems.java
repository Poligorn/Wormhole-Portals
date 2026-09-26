package com.eveportalnether.registry;

import com.eveportalnether.EvePortalNether;
import com.eveportalnether.item.PortalDetectorItem;
import com.eveportalnether.item.PortalRadarItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final int RADAR_DURABILITY = 16;
    public static final int DETECTOR_DURABILITY = 16;

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(EvePortalNether.MODID);

    public static final DeferredItem<Item> WORMHOLE_ITEM = ITEMS.register("wormhole",
        () -> new BlockItem(ModBlocks.WORMHOLE.get(), new Item.Properties())
    );

    public static final DeferredItem<Item> PORTAL_DETECTOR = ITEMS.register("portal_detector",
        () -> new PortalDetectorItem(new Item.Properties().durability(DETECTOR_DURABILITY))
    );

    public static final DeferredItem<Item> PORTAL_RADAR = ITEMS.register("portal_radar",
        () -> new PortalRadarItem(new Item.Properties().durability(RADAR_DURABILITY))
    );

    public static final DeferredItem<Item> PORTAL_SHARD = ITEMS.register("portal_shard",
        () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON))
    );

    public static final DeferredItem<Item> INCOMPLETE_PORTAL_RADAR = ITEMS.register("incomplete_portal_radar",
        () -> new Item(new Item.Properties().stacksTo(1))
    );

    public static final DeferredItem<Item> INCOMPLETE_PORTAL_DETECTOR = ITEMS.register("incomplete_portal_detector",
        () -> new Item(new Item.Properties().stacksTo(1))
    );

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
