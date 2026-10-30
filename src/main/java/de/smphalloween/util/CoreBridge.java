package de.smphalloween.util;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Verbindung zu SMPCore per Reflection. So braucht dieses Plugin beim Bauen keine SMPCore-JAR
 * und die build.yml funktioniert ohne weitere Abhängigkeiten.
 * Genutzt wird: SMPCore.get().economy().add(UUID, double) und .addCrystals(UUID, long)
 */
public final class CoreBridge {
    private final Logger log;
    private Object economy;
    private Method add;
    private Method addCrystals;

    public CoreBridge(Logger log) { this.log = log; }

    public boolean init() {
        try {
            Class<?> core = Class.forName("de.smpcore.SMPCore");
            Object instance = core.getMethod("get").invoke(null);
            if (instance == null) return false;
            economy = instance.getClass().getMethod("economy").invoke(instance);
            if (economy == null) return false;
            add = economy.getClass().getMethod("add", UUID.class, double.class);
            addCrystals = economy.getClass().getMethod("addCrystals", UUID.class, long.class);
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            log.severe("SMPCore-API nicht nutzbar: " + e);
            return false;
        }
    }

    public void giveMoney(UUID id, double amount) {
        if (amount <= 0) return;
        try {
            add.invoke(economy, id, amount);
        } catch (ReflectiveOperationException e) {
            log.warning("Geld konnte nicht vergeben werden: " + e);
        }
    }

    public void giveStars(UUID id, long amount) {
        if (amount <= 0) return;
        try {
            addCrystals.invoke(economy, id, amount);
        } catch (ReflectiveOperationException e) {
            log.warning("Stars konnten nicht vergeben werden: " + e);
        }
    }
}
