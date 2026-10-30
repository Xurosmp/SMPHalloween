package de.smphalloween.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class MenuListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof IndexMenu menu)) return;
        e.setCancelled(true);
        if (e.getClickedInventory() != null && e.getClickedInventory().equals(e.getInventory())) {
            menu.click(e.getSlot());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof IndexMenu) e.setCancelled(true);
    }
}
