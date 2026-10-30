package de.smphalloween.pumpkin;

import de.smphalloween.HalloweenPlugin;
import de.smphalloween.data.PlayerStore;
import de.smphalloween.util.Text;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;

import java.time.Duration;

/** Vergibt Belohnungen, wenn ein Kürbis eingesammelt wird. */
public final class Collector {
    private final HalloweenPlugin plugin;

    public Collector(HalloweenPlugin plugin) { this.plugin = plugin; }

    public void collect(Player p, PumpkinType t) {
        PlayerStore store = plugin.store();
        PlayerStore.Entry e = store.entry(p.getUniqueId(), p.getName());
        boolean first = e.found.getOrDefault(t.id(), 0) <= 0;
        e.found.merge(t.id(), 1, Integer::sum);
        e.total++;
        store.markDirty();

        if (!first) {
            long pct = plugin.getConfig().getLong("duplicate-money-percent", 15);
            double money = Math.round(t.money() * pct / 100.0);
            plugin.core().giveMoney(p.getUniqueId(), money);
            String extra = money > 0 ? " <dark_gray>» <green>+" + Text.money(money) : "";
            p.sendActionBar(Text.mm("<gray>Schon im Index: " + t.coloredName() + extra));
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            return;
        }

        plugin.core().giveMoney(p.getUniqueId(), t.money());
        plugin.core().giveStars(p.getUniqueId(), t.stars());

        int found = e.foundCount(plugin.registry());
        int total = plugin.registry().size();
        plugin.send(p, "<gold>Neu im Index: " + t.coloredName() + " <dark_gray>(" + t.rarity().tag()
                + "<dark_gray>) <gray>» <green>+" + Text.money(t.money()) + " <gray>und <light_purple>+"
                + Text.stars(t.stars()) + " <dark_gray>[<yellow>" + found + "<gray>/<yellow>" + total + "<dark_gray>]");
        p.showTitle(Title.title(Text.mm("<gold><bold>NEUER KÜRBIS!"), Text.mm(t.coloredName()),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(1800), Duration.ofMillis(400))));
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);

        Rarity bc = Rarity.parseOrNull(plugin.getConfig().getString("broadcast-from", "EPIC"));
        if (bc != null && t.rarity().ordinal() >= bc.ordinal()) {
            Bukkit.broadcast(Text.mm(plugin.prefix() + "<yellow>" + Text.esc(p.getName()) + " <gray>hat den Kürbis "
                    + t.coloredName() + " <dark_gray>(" + t.rarity().tag() + "<dark_gray>) <gray>entdeckt!"));
        }
        Rarity fw = Rarity.parseOrNull(plugin.getConfig().getString("firework-from", "LEGENDARY"));
        if (fw != null && t.rarity().ordinal() >= fw.ordinal()) firework(p.getLocation(), t.rarity());

        checkBonuses(p, e, t.rarity());
    }

    private void checkBonuses(Player p, PlayerStore.Entry e, Rarity r) {
        PumpkinRegistry reg = plugin.registry();

        // Set-Bonus der Seltenheit
        String claimKey = "rarity:" + r.name();
        if (!e.claimed.contains(claimKey)) {
            boolean all = true;
            for (PumpkinType x : reg.ofRarity(r)) if (e.found.getOrDefault(x.id(), 0) <= 0) all = false;
            if (all) {
                e.claimed.add(claimKey);
                ConfigurationSection s = plugin.getConfig().getConfigurationSection("bonuses.rarity." + r.name());
                if (s != null) {
                    double money = s.getDouble("money", 0);
                    long stars = s.getLong("stars", 0);
                    plugin.core().giveMoney(p.getUniqueId(), money);
                    plugin.core().giveStars(p.getUniqueId(), stars);
                    plugin.send(p, "<gold><bold>SET KOMPLETT!</bold> <gray>Alle " + r.tag() + "<gray>en Kürbisse entdeckt » <green>+"
                            + Text.money(money) + " <gray>und <light_purple>+" + Text.stars(stars));
                    p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                }
            }
        }

        // Kompletter Index
        if (!e.claimed.contains("complete") && e.foundCount(reg) >= reg.size() && reg.size() > 0) {
            e.claimed.add("complete");
            ConfigurationSection s = plugin.getConfig().getConfigurationSection("bonuses.complete");
            double money = s == null ? 0 : s.getDouble("money", 0);
            long stars = s == null ? 0 : s.getLong("stars", 0);
            plugin.core().giveMoney(p.getUniqueId(), money);
            plugin.core().giveStars(p.getUniqueId(), stars);
            Bukkit.broadcast(Text.mm(plugin.prefix() + "<gold><bold>" + Text.esc(p.getName())
                    + "</bold> <gray>hat den <gold>kompletten Kürbis-Index <gray>gefüllt!"));
            plugin.send(p, "<gold><bold>INDEX KOMPLETT!</bold> <gray>» <green>+" + Text.money(money)
                    + " <gray>und <light_purple>+" + Text.stars(stars));
            firework(p.getLocation(), Rarity.MYTHIC);
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        }
        plugin.store().markDirty();
    }

    private void firework(Location loc, Rarity r) {
        Firework f = loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta m = f.getFireworkMeta();
        m.addEffect(FireworkEffect.builder().with(FireworkEffect.Type.BALL_LARGE)
                .withColor(Color.fromRGB(r.rgb()), Color.ORANGE).withFade(Color.BLACK).build());
        m.setPower(0);
        f.setFireworkMeta(m);
        f.detonate();
    }
}
