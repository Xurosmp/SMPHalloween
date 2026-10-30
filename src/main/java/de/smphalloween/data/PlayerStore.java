package de.smphalloween.data;

import de.smphalloween.HalloweenPlugin;
import de.smphalloween.pumpkin.PumpkinRegistry;
import de.smphalloween.pumpkin.PumpkinType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

/** Speichert den Index jedes Spielers in plugins/SMPHalloween/data.yml */
public final class PlayerStore {
    public static final class Entry {
        public String name;
        public long total;
        public boolean hintsOff;
        public final Map<String, Integer> found = new LinkedHashMap<>();
        public final Set<String> claimed = new HashSet<>();

        public int foundCount(PumpkinRegistry reg) {
            int n = 0;
            for (PumpkinType t : reg.all()) if (found.getOrDefault(t.id(), 0) > 0) n++;
            return n;
        }
    }

    private static final Object FILE_LOCK = new Object();

    private final HalloweenPlugin plugin;
    private final File file;
    private final Map<UUID, Entry> players = new HashMap<>();
    private boolean eventEnabled = true;
    private boolean dirty;

    public PlayerStore(HalloweenPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
    }

    public void load() {
        players.clear();
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        eventEnabled = y.getBoolean("event-enabled", true);
        ConfigurationSection root = y.getConfigurationSection("players");
        if (root == null) return;
        for (String key : root.getKeys(false)) {
            UUID id;
            try {
                id = UUID.fromString(key);
            } catch (IllegalArgumentException e) {
                continue;
            }
            ConfigurationSection s = root.getConfigurationSection(key);
            if (s == null) continue;
            Entry e = new Entry();
            e.name = s.getString("name", key);
            e.total = s.getLong("total", 0);
            e.hintsOff = s.getBoolean("hints-off", false);
            ConfigurationSection f = s.getConfigurationSection("found");
            if (f != null) for (String id2 : f.getKeys(false)) e.found.put(id2, f.getInt(id2));
            e.claimed.addAll(s.getStringList("claimed"));
            players.put(id, e);
        }
    }

    public void save(boolean async) {
        YamlConfiguration y = new YamlConfiguration();
        y.set("event-enabled", eventEnabled);
        for (Map.Entry<UUID, Entry> me : players.entrySet()) {
            String p = "players." + me.getKey();
            Entry e = me.getValue();
            y.set(p + ".name", e.name);
            y.set(p + ".total", e.total);
            y.set(p + ".hints-off", e.hintsOff);
            for (Map.Entry<String, Integer> f : e.found.entrySet()) y.set(p + ".found." + f.getKey(), f.getValue());
            y.set(p + ".claimed", new ArrayList<>(e.claimed));
        }
        String content = y.saveToString();
        dirty = false;
        if (async && plugin.isEnabled()) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> write(content));
        } else {
            write(content);
        }
    }

    private void write(String content) {
        synchronized (FILE_LOCK) {
            try {
                File parent = file.getParentFile();
                if (parent != null) parent.mkdirs();
                File tmp = new File(file.getPath() + ".tmp");
                Files.writeString(tmp.toPath(), content, StandardCharsets.UTF_8);
                Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                plugin.getLogger().warning("data.yml konnte nicht gespeichert werden: " + ex.getMessage());
            }
        }
    }

    public void markDirty() { dirty = true; }

    public boolean isDirty() { return dirty; }

    public boolean eventEnabled() { return eventEnabled; }

    public void setEventEnabled(boolean v) {
        eventEnabled = v;
        dirty = true;
    }

    public Entry entry(UUID id, String name) {
        Entry e = players.computeIfAbsent(id, k -> new Entry());
        if (name != null) e.name = name;
        if (e.name == null) e.name = id.toString();
        return e;
    }

    public Entry peek(UUID id) { return players.get(id); }

    public UUID findByName(String name) {
        for (Map.Entry<UUID, Entry> me : players.entrySet()) {
            if (me.getValue().name != null && me.getValue().name.equalsIgnoreCase(name)) return me.getKey();
        }
        return null;
    }

    public void reset(UUID id) {
        if (players.remove(id) != null) dirty = true;
    }

    public int playerCount() { return players.size(); }

    public List<Map.Entry<UUID, Entry>> top(int limit, PumpkinRegistry reg) {
        List<Map.Entry<UUID, Entry>> list = new ArrayList<>(players.entrySet());
        list.removeIf(e -> e.getValue().foundCount(reg) == 0);
        list.sort((a, b) -> {
            int c = Integer.compare(b.getValue().foundCount(reg), a.getValue().foundCount(reg));
            return c != 0 ? c : Long.compare(b.getValue().total, a.getValue().total);
        });
        return list.size() > limit ? list.subList(0, limit) : list;
    }
}
