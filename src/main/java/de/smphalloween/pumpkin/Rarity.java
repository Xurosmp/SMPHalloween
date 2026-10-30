package de.smphalloween.pumpkin;

import java.util.Locale;

public enum Rarity {
    COMMON("Gewöhnlich", "<white>", 0xFFFFFF, "SMALL_FLAME", 1),
    RARE("Selten", "<aqua>", 0x55FFFF, "HAPPY_VILLAGER", 3),
    EPIC("Episch", "<light_purple>", 0xFF55FF, "WITCH", 5),
    LEGENDARY("Legendär", "<gold>", 0xFFAA00, "TOTEM_OF_UNDYING", 6),
    MYTHIC("Mythisch", "<red>", 0xFF5555, "SOUL_FIRE_FLAME", 8);

    private final String display;
    private final String color;
    private final int rgb;
    private final String particle;
    private final int particleCount;

    Rarity(String display, String color, int rgb, String particle, int particleCount) {
        this.display = display;
        this.color = color;
        this.rgb = rgb;
        this.particle = particle;
        this.particleCount = particleCount;
    }

    public String display() { return display; }
    public String color() { return color; }
    public int rgb() { return rgb; }
    public String particle() { return particle; }
    public int particleCount() { return particleCount; }

    /** z.B. "<aqua>Selten" */
    public String tag() { return color + display; }

    /** ★ je nach Seltenheit */
    public String starsText() { return "★".repeat(ordinal() + 1); }

    public static Rarity parse(String s, Rarity def) {
        if (s == null) return def;
        try {
            return valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return def;
        }
    }

    /** Für broadcast-from / firework-from. Gibt null zurück bei NONE/ungültig. */
    public static Rarity parseOrNull(String s) {
        if (s == null) return null;
        try {
            return valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
