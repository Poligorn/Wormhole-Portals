package com.eveportalnether.world;

import com.eveportalnether.Config;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;

public enum PortalTier implements StringRepresentable {
    UNSTABLE("unstable", 'U', 0xFF6B6B, PortalColor.RED),
    STANDARD("standard", 'S', 0x6BB8FF, PortalColor.BLUE),
    MASSIVE("massive", 'M', 0xFFD06B, PortalColor.YELLOW);

    private final String name;
    private final char letter;
    private final int textColor;
    private final PortalColor legacyColor;

    PortalTier(String name, char letter, int textColor, PortalColor legacyColor) {
        this.name = name;
        this.letter = letter;
        this.textColor = textColor;
        this.legacyColor = legacyColor;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public String translationKey() {
        return "eveportalnether.tier." + name;
    }

    public char letter() {
        return letter;
    }

    /**
     * Colour of the tier name in analyzer readouts only; the portal itself is coloured independently.
     */
    public int textColor() {
        return textColor;
    }

    /**
     * Colour for portals saved before colours were independent from the tier.
     */
    public PortalColor legacyColor() {
        return legacyColor;
    }

    public Config.TierSpec spec() {
        return switch (this) {
            case UNSTABLE -> Config.UNSTABLE;
            case STANDARD -> Config.STANDARD;
            case MASSIVE -> Config.MASSIVE;
        };
    }

    public int rollTicks(RandomSource random) {
        return rollRange(random, spec().minMinutes.get(), spec().maxMinutes.get()) * 1200;
    }

    public int rollUses(RandomSource random) {
        return rollRange(random, spec().minUses.get(), spec().maxUses.get());
    }

    public static PortalTier roll(RandomSource random) {
        int total = 0;
        for (PortalTier tier : values()) {
            total += tier.spec().weight.get();
        }
        if (total <= 0) {
            return STANDARD;
        }
        int pick = random.nextInt(total);
        for (PortalTier tier : values()) {
            pick -= tier.spec().weight.get();
            if (pick < 0) {
                return tier;
            }
        }
        return STANDARD;
    }

    public static PortalTier byName(String name) {
        for (PortalTier tier : values()) {
            if (tier.name.equals(name)) {
                return tier;
            }
        }
        return STANDARD;
    }

    private static int rollRange(RandomSource random, int min, int max) {
        return max <= min ? min : min + random.nextInt(max - min + 1);
    }
}
