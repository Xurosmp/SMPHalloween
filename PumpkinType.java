package de.smphalloween.pumpkin;

import org.bukkit.Material;

public record PumpkinType(String id, String name, Rarity rarity, double weight, Material material,
                          double money, long stars, String particle, String description) {

    /** Name mit Seltenheits-Farbe (MiniMessage). */
    public String coloredName() { return rarity.color() + name; }
}
