package com.eveportalnether.advancement;

import com.eveportalnether.registry.ModTriggers;
import com.eveportalnether.world.PortalColor;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

/**
 * Names of the moments fired into {@link WormholeTrigger}. They must match the {@code event} values in
 * {@code data/eveportalnether/advancement/*.json}.
 */
public final class WormholeEvents {
    public static final String RUMBLE = "rumble";
    public static final String WITNESS_OPENING = "witness_opening";
    public static final String TRAVEL = "travel";
    public static final String LAST_ENTRY = "last_entry";
    public static final String RETURN_LOCKED = "return_locked";
    public static final String DECRYPT = "decrypt";
    public static final String DECRYPT_MASSIVE = "decrypt_massive";
    public static final String SURVIVE_IMPLOSION = "survive_implosion";
    public static final String ANONYMOUS = "anonymous";

    private WormholeEvents() {
    }

    public static void fire(ServerPlayer player, String event) {
        ModTriggers.WORMHOLE.get().trigger(player, event);
    }

    /** {@code travel_red}, {@code travel_blue}, … — one per colour for the "all colours" challenge. */
    public static String travelColor(PortalColor color) {
        return "travel_" + color.name().toLowerCase(Locale.ROOT);
    }
}
