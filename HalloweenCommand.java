package de.smphalloween;

import de.smphalloween.data.PlayerStore;
import de.smphalloween.gui.IndexMenu;
import de.smphalloween.pumpkin.PumpkinType;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class HalloweenCommand implements TabExecutor {
    private final HalloweenPlugin plugin;

    public HalloweenCommand(HalloweenPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        String sub = a.length == 0 ? "index" : a[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "index", "open", "menu" -> {
                if (!(s instanceof Player p)) {
                    plugin.send(s, "<red>Nur für Spieler.");
                    return true;
                }
                if (!p.hasPermission("halloween.use")) {
                    plugin.send(s, "<red>Dafür fehlt dir die Berechtigung.");
                    return true;
                }
                IndexMenu.open(plugin, p, 0);
            }
            case "top" -> plugin.sendTop(s);
            case "hints", "hinweise" -> {
                if (!(s instanceof Player p)) return true;
                PlayerStore.Entry e = plugin.store().entry(p.getUniqueId(), p.getName());
                e.hintsOff = !e.hintsOff;
                plugin.store().markDirty();
                plugin.send(p, "Kürbis-Hinweise: " + (e.hintsOff ? "<red>aus" : "<green>an"));
            }
            case "help" -> help(s);
            default -> {
                if (!s.hasPermission("halloween.admin")) {
                    help(s);
                    return true;
                }
                admin(s, sub, a);
            }
        }
        return true;
    }

    private void help(CommandSender s) {
        plugin.send(s, "<gold>/halloween <gray>– Kürbis-Index öffnen");
        plugin.send(s, "<gold>/halloween top <gray>– Bestenliste");
        plugin.send(s, "<gold>/halloween hints <gray>– Hinweise in der Action-Bar an/aus");
        if (s.hasPermission("halloween.admin")) {
            plugin.send(s, "<red>Admin: <gray>spawn <id> | clear | toggle | status | reload | grant <Spieler> <id> | reset <Spieler>");
        }
    }

    private void admin(CommandSender s, String sub, String[] a) {
        switch (sub) {
            case "spawn" -> {
                if (!(s instanceof Player p)) {
                    plugin.send(s, "<red>Nur für Spieler.");
                    return;
                }
                PumpkinType t = a.length > 1 ? plugin.registry().get(a[1]) : null;
                if (t == null) {
                    plugin.send(s, "<red>Nutzung: /halloween spawn <id>");
                    return;
                }
                Block target = p.getTargetBlockExact(6);
                Block at = target == null ? p.getLocation().getBlock() : target.getRelative(BlockFace.UP);
                plugin.send(s, plugin.spawns().place(t, at)
                        ? "<green>" + t.coloredName() + " <green>gespawnt."
                        : "<red>Dort kann kein Kürbis stehen (fester Boden + freier Platz nötig).");
            }
            case "clear" -> {
                int n = plugin.spawns().count();
                plugin.spawns().removeAll();
                plugin.send(s, "<green>" + n + " Kürbisse entfernt.");
            }
            case "toggle" -> {
                boolean now = !plugin.store().eventEnabled();
                plugin.store().setEventEnabled(now);
                plugin.send(s, "Event ist jetzt " + (now ? "<green>AN" : "<red>AUS"));
            }
            case "status" -> {
                plugin.send(s, "Event: " + (plugin.eventActive() ? "<green>aktiv" : "<red>inaktiv")
                        + " <dark_gray>| <gray>Kürbis-Typen: <yellow>" + plugin.registry().size()
                        + " <dark_gray>| <gray>aktive Kürbisse: <yellow>" + plugin.spawns().count()
                        + " <dark_gray>| <gray>Spieler mit Index: <yellow>" + plugin.store().playerCount());
            }
            case "reload" -> {
                plugin.reloadAll();
                plugin.send(s, "<green>Config & Kürbisse neu geladen.");
            }
            case "grant" -> {
                if (a.length < 3) {
                    plugin.send(s, "<red>Nutzung: /halloween grant <Spieler> <id>");
                    return;
                }
                Player target = Bukkit.getPlayerExact(a[1]);
                PumpkinType t = plugin.registry().get(a[2]);
                if (target == null || t == null) {
                    plugin.send(s, "<red>Spieler muss online sein und die Kürbis-ID muss existieren.");
                    return;
                }
                plugin.collector().collect(target, t);
                plugin.send(s, "<green>Kürbis an " + target.getName() + " vergeben.");
            }
            case "reset" -> {
                if (a.length < 2) {
                    plugin.send(s, "<red>Nutzung: /halloween reset <Spieler>");
                    return;
                }
                Player online = Bukkit.getPlayerExact(a[1]);
                UUID id = online != null ? online.getUniqueId() : plugin.store().findByName(a[1]);
                if (id == null) {
                    plugin.send(s, "<red>Spieler nicht gefunden.");
                    return;
                }
                plugin.store().reset(id);
                plugin.send(s, "<green>Index von " + a[1] + " zurückgesetzt (bereits erhaltene Belohnungen bleiben).");
            }
            default -> help(s);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] a) {
        List<String> out = new ArrayList<>();
        boolean admin = s.hasPermission("halloween.admin");
        if (a.length == 1) {
            out.addAll(List.of("index", "top", "hints", "help"));
            if (admin) out.addAll(List.of("spawn", "clear", "toggle", "status", "reload", "grant", "reset"));
        } else if (admin && a.length == 2) {
            switch (a[0].toLowerCase(Locale.ROOT)) {
                case "spawn" -> plugin.registry().all().forEach(t -> out.add(t.id()));
                case "grant", "reset" -> Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
                default -> { }
            }
        } else if (admin && a.length == 3 && a[0].equalsIgnoreCase("grant")) {
            plugin.registry().all().forEach(t -> out.add(t.id()));
        }
        String last = a.length == 0 ? "" : a[a.length - 1].toLowerCase(Locale.ROOT);
        out.removeIf(x -> !x.toLowerCase(Locale.ROOT).startsWith(last));
        return out;
    }
}
