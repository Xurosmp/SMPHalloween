package de.smphalloween;

import de.smphalloween.data.PlayerStore;
import de.smphalloween.gui.MenuListener;
import de.smphalloween.pumpkin.Collector;
import de.smphalloween.pumpkin.PumpkinListener;
import de.smphalloween.pumpkin.PumpkinRegistry;
import de.smphalloween.pumpkin.SpawnManager;
import de.smphalloween.util.CoreBridge;
import de.smphalloween.util.Text;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

public final class HalloweenPlugin extends JavaPlugin {
    private CoreBridge core;
    private PumpkinRegistry registry;
    private PlayerStore store;
    private SpawnManager spawns;
    private Collector collector;

    private String prefix = "<gold><bold>HALLOWEEN</bold> <dark_gray>▸ <gray>";
    private LocalDate start;
    private LocalDate end;
    private ZoneId zone = ZoneId.of("Europe/Berlin");

    public CoreBridge core() { return core; }
    public PumpkinRegistry registry() { return registry; }
    public PlayerStore store() { return store; }
    public SpawnManager spawns() { return spawns; }
    public Collector collector() { return collector; }
    public String prefix() { return prefix; }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!new File(getDataFolder(), "pumpkins.yml").exists()) saveResource("pumpkins.yml", false);

        core = new CoreBridge(getLogger());
        if (!core.init()) {
            getLogger().severe("SMPCore wurde nicht gefunden oder die API passt nicht – SMPHalloween wird deaktiviert.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        readSettings();
        registry = new PumpkinRegistry(this);
        registry.load();
        store = new PlayerStore(this);
        store.load();
        collector = new Collector(this);
        spawns = new SpawnManager(this);
        spawns.cleanupLeftovers();
        spawns.start();

        getServer().getPluginManager().registerEvents(new PumpkinListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(), this);
        PluginCommand cmd = getCommand("halloween");
        if (cmd != null) {
            HalloweenCommand hc = new HalloweenCommand(this);
            cmd.setExecutor(hc);
            cmd.setTabCompleter(hc);
        }
        getServer().getScheduler().runTaskTimer(this, () -> {
            if (store.isDirty()) store.save(true);
        }, 20L * 60, 20L * 60);

        getLogger().info("SMPHalloween aktiviert – " + registry.size() + " Kürbisse im Index, Anbindung an SMPCore steht.");
    }

    @Override
    public void onDisable() {
        if (spawns != null) spawns.shutdown();
        if (store != null) store.save(false);
    }

    private void readSettings() {
        prefix = getConfig().getString("prefix", prefix);
        try {
            zone = ZoneId.of(getConfig().getString("event.timezone", "Europe/Berlin"));
        } catch (DateTimeException e) {
            zone = ZoneId.of("Europe/Berlin");
        }
        start = parseDate(getConfig().getString("event.start", ""));
        end = parseDate(getConfig().getString("event.end", ""));
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDate.parse(s.trim());
        } catch (DateTimeException e) {
            getLogger().warning("Ungültiges Datum in config.yml: '" + s + "' (erwartet yyyy-MM-dd)");
            return null;
        }
    }

    public void reloadAll() {
        reloadConfig();
        readSettings();
        registry.load();
    }

    /** Event läuft, wenn nicht per /halloween toggle ausgeschaltet und im (optionalen) Zeitraum. */
    public boolean eventActive() {
        if (store == null || !store.eventEnabled()) return false;
        LocalDate today = LocalDate.now(zone);
        if (start != null && today.isBefore(start)) return false;
        return end == null || !today.isAfter(end);
    }

    public void send(CommandSender to, String msg) { to.sendMessage(Text.mm(prefix + msg)); }

    public void sendTop(CommandSender to) {
        var list = store.top(10, registry);
        send(to, "<gold><bold>Top 10 Kürbis-Sammler</bold>");
        if (list.isEmpty()) {
            to.sendMessage(Text.mm("<gray>Noch niemand hat einen Kürbis entdeckt."));
            return;
        }
        int i = 1;
        for (Map.Entry<UUID, PlayerStore.Entry> e : list) {
            to.sendMessage(Text.mm("<yellow>" + i++ + ". <white>" + Text.esc(e.getValue().name) + " <dark_gray>» <gold>"
                    + e.getValue().foundCount(registry) + "<gray>/" + registry.size() + " <dark_gray>(" + e.getValue().total + " gesammelt)"));
        }
    }
}
