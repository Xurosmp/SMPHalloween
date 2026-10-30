package de.smphalloween.pumpkin;

import de.smphalloween.HalloweenPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public final class PumpkinRegistry {
    private final HalloweenPlugin plugin;
    private final Map<String, PumpkinType> byId = new LinkedHashMap<>();
    private List<PumpkinType> ordered = List.of();
    private double totalWeight;

    public PumpkinRegistry(HalloweenPlugin plugin) { this.plugin = plugin; }

    public void load() {
        byId.clear();
        File f = new File(plugin.getDataFolder(), "pumpkins.yml");
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        ConfigurationSection root = y.getConfigurationSection("pumpkins");
        List<PumpkinType> list = new ArrayList<>();
        if (root != null) {
            for (String id : root.getKeys(false)) {
                ConfigurationSection s = root.getConfigurationSection(id);
                if (s == null) continue;
                Material m = Material.matchMaterial(s.getString("block", "PUMPKIN"));
                if (m == null || !m.isBlock() || m.isAir()) {
                    plugin.getLogger().warning("pumpkins.yml: Ungültiger Block bei '" + id + "' – übersprungen.");
                    continue;
                }
                Rarity r = Rarity.parse(s.getString("rarity"), Rarity.COMMON);
                double w = Math.max(0.0001, s.getDouble("weight", 10));
                list.add(new PumpkinType(id.toLowerCase(Locale.ROOT), s.getString("name", id), r, w, m,
                        Math.max(0, s.getDouble("money", 0)), Math.max(0, s.getLong("stars", 0)),
                        s.getString("particle", null), s.getString("description", "")));
            }
        }
        list.sort(Comparator.comparingInt(t -> t.rarity().ordinal()));
        double sum = 0;
        for (PumpkinType t : list) {
            byId.put(t.id(), t);
            sum += t.weight();
        }
        ordered = List.copyOf(list);
        totalWeight = sum;
    }

    public List<PumpkinType> all() { return ordered; }

    public int size() { return ordered.size(); }

    public PumpkinType get(String id) { return id == null ? null : byId.get(id.toLowerCase(Locale.ROOT)); }

    public List<PumpkinType> ofRarity(Rarity r) {
        List<PumpkinType> out = new ArrayList<>();
        for (PumpkinType t : ordered) if (t.rarity() == r) out.add(t);
        return out;
    }

    public PumpkinType pick(Random rnd) {
        if (ordered.isEmpty()) return null;
        double roll = rnd.nextDouble() * totalWeight;
        for (PumpkinType t : ordered) {
            roll -= t.weight();
            if (roll < 0) return t;
        }
        return ordered.get(ordered.size() - 1);
    }

    /** Spawn-Wahrscheinlichkeit in Prozent. */
    public double chancePercent(PumpkinType t) { return totalWeight <= 0 ? 0 : t.weight() / totalWeight * 100.0; }
}
