package de.smphalloween.gui;

import de.smphalloween.HalloweenPlugin;
import de.smphalloween.data.PlayerStore;
import de.smphalloween.pumpkin.PumpkinRegistry;
import de.smphalloween.pumpkin.PumpkinType;
import de.smphalloween.pumpkin.Rarity;
import de.smphalloween.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/** Der Kürbis-Index als Kisten-GUI (Einzel-Tap, damit es auch mit Bedrock funktioniert). */
public final class IndexMenu implements InventoryHolder {
    private static final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43};
    private static final int PREV = 45, TOP = 48, CLOSE = 49, HINTS = 50, NEXT = 53, INFO = 4;

    private final HalloweenPlugin plugin;
    private final Player player;
    private final int page;
    private final int pages;
    private final Inventory inv;

    private IndexMenu(HalloweenPlugin plugin, Player player, int page) {
        this.plugin = plugin;
        this.player = player;
        PumpkinRegistry reg = plugin.registry();
        this.pages = Math.max(1, (int) Math.ceil(reg.size() / (double) SLOTS.length));
        this.page = Math.max(0, Math.min(page, pages - 1));
        PlayerStore.Entry e = plugin.store().entry(player.getUniqueId(), player.getName());
        String title = "<gold>Kürbis-Index <dark_gray>(" + e.foundCount(reg) + "/" + reg.size() + ")"
                + (pages > 1 ? " <gray>S." + (this.page + 1) : "");
        this.inv = Bukkit.createInventory(this, 54, Text.mm(title));
        render(e);
    }

    public static void open(HalloweenPlugin plugin, Player player, int page) {
        player.openInventory(new IndexMenu(plugin, player, page).inv);
    }

    @Override
    public Inventory getInventory() { return inv; }

    private void render(PlayerStore.Entry e) {
        PumpkinRegistry reg = plugin.registry();
        ItemStack border = make(Material.BLACK_STAINED_GLASS_PANE, " ", List.of(), false);
        for (int i = 0; i < 54; i++) inv.setItem(i, border);
        ItemStack orange = make(Material.ORANGE_STAINED_GLASS_PANE, " ", List.of(), false);
        for (int i = 0; i < 9; i++) inv.setItem(i, orange);
        for (int i = 45; i < 54; i++) inv.setItem(i, orange);

        for (int i = 0; i < SLOTS.length; i++) inv.setItem(SLOTS[i], null);

        List<PumpkinType> all = reg.all();
        int start = page * SLOTS.length;
        for (int i = 0; i < SLOTS.length && start + i < all.size(); i++) {
            inv.setItem(SLOTS[i], pumpkinItem(all.get(start + i), e));
        }

        inv.setItem(INFO, infoItem(e));
        if (page > 0) inv.setItem(PREV, make(Material.ARROW, "<yellow>◀ Vorherige Seite", List.of(), false));
        if (page < pages - 1) inv.setItem(NEXT, make(Material.ARROW, "<yellow>Nächste Seite ▶", List.of(), false));
        inv.setItem(TOP, make(Material.GOLD_INGOT, "<gold>Bestenliste",
                List.of("<gray>Wer hat die meisten Kürbisse entdeckt?", "", "<yellow>Klicken zum Anzeigen"), false));
        inv.setItem(CLOSE, make(Material.BARRIER, "<red>Schließen", List.of(), false));
        inv.setItem(HINTS, make(Material.SPYGLASS, "<aqua>Kürbis-Hinweise: " + (e.hintsOff ? "<red>AUS" : "<green>AN"),
                List.of("<gray>Zeigt in der Action-Bar, wenn ein", "<gray>Kürbis in der Nähe ist.", "", "<yellow>Klicken zum Umschalten"), false));
    }

    private ItemStack pumpkinItem(PumpkinType t, PlayerStore.Entry e) {
        int count = e.found.getOrDefault(t.id(), 0);
        List<String> lore = new ArrayList<>();
        lore.add("<gray>Seltenheit: " + t.rarity().tag() + " <dark_gray>" + t.rarity().starsText());
        lore.add("<gray>Fundchance: <white>" + formatChance(plugin.registry().chancePercent(t)) + "%");
        lore.add("");
        if (count > 0) {
            if (!t.description().isBlank()) {
                lore.add("<gray><italic>" + Text.esc(t.description()));
                lore.add("");
            }
            lore.add("<green>✔ Entdeckt <dark_gray>(" + count + "x gesammelt)");
            lore.add("<gray>Belohnung erhalten: <green>" + Text.money(t.money()) + " <gray>+ <light_purple>" + Text.stars(t.stars()));
            return make(t.material(), t.coloredName(), lore, true);
        }
        lore.add("<red>✘ Noch nicht entdeckt");
        lore.add("<gray>Erste Entdeckung bringt:");
        lore.add("<green>" + Text.money(t.money()) + " <gray>+ <light_purple>" + Text.stars(t.stars()));
        return make(Material.GRAY_DYE, "<dark_gray>??? <gray>(" + t.rarity().tag() + "<gray>)", lore, false);
    }

    private ItemStack infoItem(PlayerStore.Entry e) {
        PumpkinRegistry reg = plugin.registry();
        int found = e.foundCount(reg);
        int total = reg.size();
        int pct = total == 0 ? 0 : (int) Math.round(found * 100.0 / total);
        List<String> lore = new ArrayList<>();
        lore.add(Text.bar(found, total, 20) + " <white>" + pct + "%");
        lore.add("<gray>Entdeckt: <yellow>" + found + "<gray>/<yellow>" + total + " <dark_gray>| <gray>Gesammelt gesamt: <yellow>" + e.total);
        lore.add("");
        for (Rarity r : Rarity.values()) {
            List<PumpkinType> list = reg.ofRarity(r);
            if (list.isEmpty()) continue;
            int f = 0;
            for (PumpkinType t : list) if (e.found.getOrDefault(t.id(), 0) > 0) f++;
            boolean claimed = e.claimed.contains("rarity:" + r.name());
            ConfigurationSection b = plugin.getConfig().getConfigurationSection("bonuses.rarity." + r.name());
            String bonus = b == null ? "" : " <dark_gray>(Bonus: " + Text.money(b.getDouble("money")) + " + " + Text.stars(b.getLong("stars")) + ")";
            lore.add(r.tag() + "<gray>: <white>" + f + "/" + list.size() + (claimed ? " <green>✔" : bonus));
        }
        ConfigurationSection c = plugin.getConfig().getConfigurationSection("bonuses.complete");
        if (c != null) {
            lore.add("");
            lore.add("<gold>Kompletter Index: <green>" + Text.money(c.getDouble("money")) + " <gray>+ <light_purple>" + Text.stars(c.getLong("stars"))
                    + (e.claimed.contains("complete") ? " <green>✔" : ""));
        }
        return make(Material.JACK_O_LANTERN, "<gold><bold>Dein Kürbis-Index", lore, true);
    }

    private static String formatChance(double p) {
        if (p >= 10) return String.valueOf(Math.round(p));
        if (p >= 1) return String.format(java.util.Locale.GERMANY, "%.1f", p);
        return String.format(java.util.Locale.GERMANY, "%.2f", p);
    }

    static ItemStack make(Material mat, String name, List<String> lore, boolean glow) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        m.displayName(Text.mm("<!italic>" + name));
        List<Component> l = new ArrayList<>();
        for (String s : lore) l.add(Text.mm("<!italic>" + s));
        m.lore(l);
        if (glow) m.setEnchantmentGlintOverride(true);
        m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        it.setItemMeta(m);
        return it;
    }

    void click(int slot) {
        if (slot == CLOSE) {
            player.closeInventory();
        } else if (slot == PREV && page > 0) {
            open(plugin, player, page - 1);
        } else if (slot == NEXT && page < pages - 1) {
            open(plugin, player, page + 1);
        } else if (slot == TOP) {
            player.closeInventory();
            plugin.sendTop(player);
        } else if (slot == HINTS) {
            PlayerStore.Entry e = plugin.store().entry(player.getUniqueId(), player.getName());
            e.hintsOff = !e.hintsOff;
            plugin.store().markDirty();
            open(plugin, player, page);
        } else {
            return;
        }
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1f);
    }
}
