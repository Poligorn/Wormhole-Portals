package com.eveportalnether.world;

import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;

public enum PortalColor implements StringRepresentable {
    RED("red", 'R', 0xFF3B3B),
    ORANGE("orange", 'O', 0xFF8A2B),
    YELLOW("yellow", 'Y', 0xFFD23B),
    GREEN("green", 'G', 0x4CFF5A),
    CYAN("cyan", 'C', 0x3BFFE6),
    BLUE("blue", 'B', 0x3B7BFF),
    VIOLET("violet", 'V', 0x9B4BFF),
    PINK("pink", 'P', 0xFF4BD8),
    WHITE("white", 'W', 0xF2F2FF);

    private final String name;
    private final char letter;
    private final int rgb;

    PortalColor(String name, char letter, int rgb) {
        this.name = name;
        this.letter = letter;
        this.rgb = rgb;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public char letter() {
        return letter;
    }

    public int rgb() {
        return rgb;
    }

    public float red() {
        return ((rgb >> 16) & 0xFF) / 255.0F;
    }

    public float green() {
        return ((rgb >> 8) & 0xFF) / 255.0F;
    }

    public float blue() {
        return (rgb & 0xFF) / 255.0F;
    }

    /**
     * Window tint: the colour washes towards grey as the portal decays.
     */
    public int tint(int decay) {
        float grey = switch (decay) {
            case 0 -> 0.0F;
            case 1 -> 0.35F;
            case 2 -> 0.65F;
            default -> 0.8F;
        };
        float brightness = decay >= 2 ? 0.75F : 1.0F;
        int r = mix((rgb >> 16) & 0xFF, grey, brightness);
        int g = mix((rgb >> 8) & 0xFF, grey, brightness);
        int b = mix(rgb & 0xFF, grey, brightness);
        return (r << 16) | (g << 8) | b;
    }

    private static int mix(int channel, float amount, float brightness) {
        return Math.min(255, Math.round((channel + (0x80 - channel) * amount) * brightness));
    }

    public static PortalColor random(RandomSource random) {
        PortalColor[] values = values();
        return values[random.nextInt(values.length)];
    }

    public static PortalColor byName(String name, PortalColor fallback) {
        for (PortalColor color : values()) {
            if (color.name.equals(name)) {
                return color;
            }
        }
        return fallback;
    }
}
