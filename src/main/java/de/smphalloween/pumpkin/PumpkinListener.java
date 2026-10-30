package de.smphalloween.pumpkin;

import de.smphalloween.HalloweenPlugin;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/** Sammeln per Abbauen ODER Rechtsklick (Rechtsklick funktioniert auch im Abenteuermodus / für Bedrock). */
public final class PumpkinListener implements Listener {
    private final HalloweenPlugin plugin;

    public PumpkinListener(HalloweenPlugin plugin) { this.plugin = plugin; }

    private boolean tryCollect(Player p, Block b) {
        SpawnManager.Active a = plugin.spawns().at(b);
        if (a == null) return false;
        if (p.getGameMode() == GameMode.SPECTATOR || !p.hasPermission("halloween.use")) return true;
        if (plugin.spawns().remove(a)) plugin.collector().collect(p, a.type);
        return true;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBreak(BlockBreakEvent e) {
        if (!plugin.spawns().isActive(e.getBlock())) return;
        e.setCancelled(true);
        e.setDropItems(false);
        tryCollect(e.getPlayer(), e.getBlock());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getHand() != EquipmentSlot.HAND) return;
        Block b = e.getClickedBlock();
        if (b == null || !plugin.spawns().isActive(b)) return;
        e.setUseInteractedBlock(Event.Result.DENY);
        e.setUseItemInHand(Event.Result.DENY);
        e.setCancelled(true);
        tryCollect(e.getPlayer(), b);
    }

    // ---- Schutz der Event-Blöcke ----

    @EventHandler(priority = EventPriority.HIGH)
    public void onExplode(EntityExplodeEvent e) {
        e.blockList().removeIf(b -> plugin.spawns().isActive(b));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockExplode(BlockExplodeEvent e) {
        e.blockList().removeIf(b -> plugin.spawns().isActive(b));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBurn(BlockBurnEvent e) {
        if (plugin.spawns().isActive(e.getBlock())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityChange(EntityChangeBlockEvent e) {
        if (plugin.spawns().isActive(e.getBlock())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        for (Block b : e.getBlocks()) {
            if (plugin.spawns().isActive(b)) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPistonRetract(BlockPistonRetractEvent e) {
        for (Block b : e.getBlocks()) {
            if (plugin.spawns().isActive(b)) {
                e.setCancelled(true);
                return;
            }
        }
    }
}
