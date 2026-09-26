package com.eveportalnether.events;

import com.eveportalnether.Config;
import com.eveportalnether.EvePortalNether;
import com.eveportalnether.advancement.WormholeEvents;
import com.eveportalnether.network.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@EventBusSubscriber(modid = EvePortalNether.MODID)
public final class AnonymityHandler {
    private static final Map<UUID, String> ANONYMOUS_IDS = new HashMap<>();

    private AnonymityHandler() {
    }

    public static boolean isAnonymous(Player player) {
        return Config.ANONYMITY_ENABLED.get() && player.level().dimension() == Level.NETHER;
    }

    private static Component anonymousName(Player player) {
        String id = ANONYMOUS_IDS.computeIfAbsent(player.getUUID(), uuid -> newId());
        return Component.literal(id).withStyle(ChatFormatting.DARK_GRAY);
    }

    private static String newId() {
        Set<String> taken = new HashSet<>(ANONYMOUS_IDS.values());
        String id;
        do {
            id = String.format("??_%03d", ThreadLocalRandom.current().nextInt(1000));
        } while (taken.contains(id) && taken.size() < 1000);
        return id;
    }

    @SubscribeEvent
    public static void onNameFormat(PlayerEvent.NameFormat event) {
        if (event.getEntity() instanceof ServerPlayer player && isAnonymous(player)) {
            event.setDisplayname(anonymousName(player));
        }
    }

    @SubscribeEvent
    public static void onTabListNameFormat(PlayerEvent.TabListNameFormat event) {
        if (event.getEntity() instanceof ServerPlayer player && isAnonymous(player)) {
            event.setDisplayName(anonymousName(player));
        }
    }

    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            refresh(player, true);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            refresh(player, event.getTo() == Level.NETHER);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            refresh(player, false);
        }
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        ANONYMOUS_IDS.remove(event.getEntity().getUUID());
    }

    /**
     * @param newIdentity roll a fresh anonymous ID, so a player is not recognisable between Nether visits
     */
    private static void refresh(ServerPlayer player, boolean newIdentity) {
        if (newIdentity || !isAnonymous(player)) {
            ANONYMOUS_IDS.remove(player.getUUID());
        }
        player.refreshDisplayName();
        player.refreshTabListName();
        ModNetwork.sendAnonymity(player, Config.ANONYMITY_ENABLED.get());
        if (isAnonymous(player)) {
            WormholeEvents.fire(player, WormholeEvents.ANONYMOUS);
        }
    }
}
