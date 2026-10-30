package de.smphalloween.pumpkin;

import de.smphalloween.HalloweenPlugin;
import de.smphalloween.data.PlayerStore;
import de.smphalloween.util.Text;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;

import java.io.File;
import java.io.IOException;
import java.util.*;

/** Spawnt, verwaltet und entfernt die Kürbis-Blöcke in der Welt. */
public final class SpawnManager {

    public static final class Active {
        public final PumpkinType type;
        public final World world;
        public final int x, y, z;
        public final BlockData original;
        public final long expiresAt;
        UUID label;
        boolean pending;

        Active(PumpkinType type, World world, int x, int y, int z, BlockData original, long expiresAt) {
            this.type = type;
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.original = original;
            this.expiresAt = expiresAt;
        }

        public Block block() { return world.getBlockAt(x, y, z); }

        public Location center() { return new Location(world, x + 0.5, y + 0.5, z + 0.5); }

        String key() { return SpawnManager.key(world, x, y, z); }

        boolean chunkLoaded() { return world.isChunkLoaded(x >> 4, z >> 4); }
    }

    private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};

    private final HalloweenPlugin plugin;
    private final Map<String, Active> active = new LinkedHashMap<>();
    private final Random rnd = new Random();
    private final Map<String, Particle> particleCache = new HashMap<>();
    private final File activeFile;
    private boolean dirty;
    private int ticksSinceSave;
    private int spawnCounter;

    public SpawnManager(HalloweenPlugin plugin) {
        this.plugin = plugin;
        this.activeFile = new File(plugin.getDataFolder(), "active.yml");
    }

    static String key(World w, int x, int y, int z) { return w.getName() + ":" + x + ":" + y + ":" + z; }

    public static String key(Block b) { return key(b.getWorld(), b.getX(), b.getY(), b.getZ()); }

    public Active at(Block b) { return active.get(key(b)); }

    public boolean isActive(Block b) { return active.containsKey(key(b)); }

    public int count() { return active.size(); }

    // ------------------------------------------------------------------ Start / Stop

    public void start() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::slowTick, 20L, 20L);
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::particleTick, 10L, 10L);
    }

    /** Beim Serverstart: übrig gebliebene Kürbisse (z.B. nach Crash) wieder entfernen. */
    public void cleanupLeftovers() {
        if (!activeFile.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(activeFile);
        for (String line : y.getStringList("entries")) {
            String[] p = line.split(";", 6);
            if (p.length < 6) continue;
            World w = Bukkit.getWorld(p[0]);
            Material m = Material.matchMaterial(p[4]);
            if (w == null || m == null) continue;
            try {
                Block b = w.getBlockAt(Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]));
                if (b.getType() == m) b.setBlockData(Bukkit.createBlockData(p[5]), false);
            } catch (IllegalArgumentException ignored) {
                // kaputte Zeile
            }
        }
        activeFile.delete();
    }

    /** Beim Ausschalten: alles entfernen, was sich sicher entfernen lässt. Rest wird für den nächsten Start gemerkt. */
    public void shutdown() {
        for (Active a : new ArrayList<>(active.values())) {
            if (a.chunkLoaded()) remove(a);
        }
        saveActive();
    }

    public void removeAll() {
        for (Active a : new ArrayList<>(active.values())) removeSoft(a);
    }

    // ------------------------------------------------------------------ Spawnen

    private boolean worldEnabled(World w) {
        List<String> worlds = plugin.getConfig().getStringList("spawning.worlds");
        return worlds.contains("*") || worlds.contains(w.getName());
    }

    private void spawnTick() {
        ConfigurationSection c = plugin.getConfig().getConfigurationSection("spawning");
        if (c == null) return;
        int maxTotal = c.getInt("max-total", 80);
        int perPlayer = c.getInt("max-per-player", 4);
        double near = c.getDouble("near-radius", 64);
        double chance = c.getDouble("chance-percent", 60);
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (active.size() >= maxTotal) return;
            if (p.getGameMode() == GameMode.SPECTATOR || !worldEnabled(p.getWorld())) continue;
            if (rnd.nextDouble() * 100.0 > chance) continue;
            if (countNear(p.getLocation(), near) >= perPlayer) continue;
            trySpawnNear(p, c.getDouble("min-radius", 12), c.getDouble("max-radius", 48));
        }
    }

    private int countNear(Location loc, double radius) {
        int n = 0;
        double r2 = radius * radius;
        for (Active a : active.values()) {
            if (a.world.equals(loc.getWorld()) && a.center().distanceSquared(loc) <= r2) n++;
        }
        return n;
    }

    private void trySpawnNear(Player p, double min, double max) {
        World w = p.getWorld();
        Location base = p.getLocation();
        if (max < min) max = min;
        for (int i = 0; i < 8; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2;
            double d = min + rnd.nextDouble() * (max - min);
            int x = (int) Math.floor(base.getX() + Math.cos(ang) * d);
            int z = (int) Math.floor(base.getZ() + Math.sin(ang) * d);
            if (!w.isChunkLoaded(x >> 4, z >> 4)) continue;
            if (!w.getWorldBorder().isInside(new Location(w, x + 0.5, base.getY(), z + 0.5))) continue;
            int y = w.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
            PumpkinType t = plugin.registry().pick(rnd);
            if (t == null) return;
            if (place(t, w.getBlockAt(x, y + 1, z))) return;
        }
    }

    /** Platziert einen Kürbis auf dem Block (nur auf festem Boden, nur in Luft / Gras / Farn). */
    public boolean place(PumpkinType t, Block target) {
        Material tt = target.getType();
        if (!(tt.isAir() || tt == Material.SHORT_GRASS || tt == Material.FERN)) return false;
        Block ground = target.getRelative(BlockFace.DOWN);
        Material gt = ground.getType();
        if (!gt.isSolid() || Tag.LEAVES.isTagged(gt)) return false;
        String k = key(target);
        if (active.containsKey(k)) return false;
        BlockData original = target.getBlockData();
        target.setType(t.material(), false);
        long life = plugin.getConfig().getLong("spawning.lifetime-seconds", 600) * 1000L;
        Active a = new Active(t, target.getWorld(), target.getX(), target.getY(), target.getZ(), original,
                System.currentTimeMillis() + life);
        active.put(k, a);
        spawnLabel(a);
        dirty = true;
        target.getWorld().spawnParticle(Particle.POOF, a.x + 0.5, a.y + 0.6, a.z + 0.5, 12, 0.3, 0.3, 0.3, 0.02);
        return true;
    }

    // ------------------------------------------------------------------ Entfernen

    /** Entfernt sofort (Chunk muss geladen sein). Gibt false zurück, wenn der Kürbis schon weg war. */
    public boolean remove(Active a) {
        if (!active.remove(a.key(), a)) return false;
        Block b = a.block();
        if (b.getType() == a.type.material()) b.setBlockData(a.original, false);
        removeLabel(a);
        dirty = true;
        return true;
    }

    /** Wie remove, lädt aber bei Bedarf den Chunk asynchron. */
    private void removeSoft(Active a) {
        if (a.chunkLoaded()) {
            remove(a);
            return;
        }
        if (a.pending) return;
        a.pending = true;
        a.world.getChunkAtAsync(a.x >> 4, a.z >> 4).thenAccept(chunk ->
                plugin.getServer().getScheduler().runTask(plugin, () -> remove(a)));
    }

    // ------------------------------------------------------------------ Labels

    private boolean labelsEnabled() { return plugin.getConfig().getBoolean("label.enabled", true); }

    private void spawnLabel(Active a) {
        if (!labelsEnabled() || !a.chunkLoaded()) return;
        Location l = new Location(a.world, a.x + 0.5, a.y + 1.35, a.z + 0.5);
        String text = "<bold>" + a.type.coloredName() + "</bold><newline><gray>" + a.type.rarity().starsText();
        TextDisplay td = a.world.spawn(l, TextDisplay.class, d -> {
            d.text(Text.mm(text));
            d.setBillboard(Display.Billboard.CENTER);
            d.setPersistent(false);
            d.setShadowed(true);
            d.setSeeThrough(false);
            d.setDefaultBackground(false);
            d.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            d.setViewRange(0.4f);
        });
        a.label = td.getUniqueId();
    }

    private void removeLabel(Active a) {
        if (a.label == null) return;
        Entity e = Bukkit.getEntity(a.label);
        if (e != null) e.remove();
        a.label = null;
    }

    private boolean playerNear(Active a, double radius) {
        double r2 = radius * radius;
        Location c = a.center();
        for (Player p : a.world.getPlayers()) {
            if (p.getLocation().distanceSquared(c) <= r2) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ Ticks

    private void slowTick() {
        boolean on = plugin.eventActive();
        long now = System.currentTimeMillis();
        List<Active> expired = new ArrayList<>();
        for (Active a : active.values()) if (!on || now >= a.expiresAt) expired.add(a);
        for (Active a : expired) removeSoft(a);

        if (on) {
            int interval = Math.max(1, plugin.getConfig().getInt("spawning.interval-seconds", 15));
            if (++spawnCounter >= interval) {
                spawnCounter = 0;
                spawnTick();
            }
            if (labelsEnabled()) {
                for (Active a : active.values()) {
                    if (!a.chunkLoaded()) continue;
                    Entity e = a.label == null ? null : Bukkit.getEntity(a.label);
                    if ((e == null || !e.isValid()) && playerNear(a, 48)) spawnLabel(a);
                }
            }
            hintTick();
        }

        if (++ticksSinceSave >= 10) {
            ticksSinceSave = 0;
            if (dirty) saveActive();
        }
    }

    private void hintTick() {
        if (!plugin.getConfig().getBoolean("hints.enabled", true) || active.isEmpty()) return;
        double radius = plugin.getConfig().getDouble("hints.radius", 24);
        double r2 = radius * radius;
        PlayerStore store = plugin.store();
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            PlayerStore.Entry e = store.peek(p.getUniqueId());
            if (e != null && e.hintsOff) continue;
            Location pl = p.getLocation();
            Active best = null;
            double bestD = Double.MAX_VALUE;
            for (Active a : active.values()) {
                if (!a.world.equals(pl.getWorld())) continue;
                double d = a.center().distanceSquared(pl);
                if (d <= r2 && d < bestD) {
                    bestD = d;
                    best = a;
                }
            }
            if (best == null) continue;
            double dx = best.x + 0.5 - pl.getX();
            double dz = best.z + 0.5 - pl.getZ();
            double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
            double rel = targetYaw - pl.getYaw();
            while (rel > 180) rel -= 360;
            while (rel < -180) rel += 360;
            int idx = ((int) Math.round(rel / 45.0) % 8 + 8) % 8;
            p.sendActionBar(Text.mm("<gold><bold>Kürbis in der Nähe</bold> <yellow>" + ARROWS[idx] + " "
                    + Math.round(Math.sqrt(bestD)) + "m"));
        }
    }

    private void particleTick() {
        for (Active a : active.values()) {
            if (!a.chunkLoaded()) continue;
            Particle p = particleFor(a.type);
            a.world.spawnParticle(p, a.x + 0.5, a.y + 1.15, a.z + 0.5, a.type.rarity().particleCount(),
                    0.3, 0.3, 0.3, 0.01);
        }
    }

    private Particle particleFor(PumpkinType t) {
        String name = t.particle() != null && !t.particle().isBlank() ? t.particle() : t.rarity().particle();
        return particleCache.computeIfAbsent(name.toUpperCase(Locale.ROOT), n -> {
            // per Feld-Lookup statt valueOf: funktioniert auch, wenn Paper Particle nicht mehr als Enum führt
            try {
                Object o = Particle.class.getField(n).get(null);
                if (o instanceof Particle found) return found;
            } catch (ReflectiveOperationException ignored) {
                // fällt unten auf FLAME zurück
            }
            plugin.getLogger().warning("Unbekannter Partikel '" + n + "' – nutze FLAME.");
            return Particle.FLAME;
        });
    }

    // ------------------------------------------------------------------ Persistenz der aktiven Kürbisse

    private void saveActive() {
        dirty = false;
        if (active.isEmpty()) {
            if (activeFile.exists()) activeFile.delete();
            return;
        }
        List<String> lines = new ArrayList<>();
        for (Active a : active.values()) {
            lines.add(a.world.getName() + ";" + a.x + ";" + a.y + ";" + a.z + ";" + a.type.material().name() + ";"
                    + a.original.getAsString());
        }
        YamlConfiguration y = new YamlConfiguration();
        y.set("entries", lines);
        try {
            y.save(activeFile);
        } catch (IOException e) {
            plugin.getLogger().warning("active.yml konnte nicht gespeichert werden: " + e.getMessage());
        }
    }
}
