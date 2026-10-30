package de.smphalloween.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.text.NumberFormat;
import java.util.Locale;

public final class Text {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final NumberFormat NF = NumberFormat.getIntegerInstance(Locale.GERMANY);

    private Text() {}

    public static Component mm(String s) { return MM.deserialize(s); }

    public static String esc(String s) { return MM.escapeTags(s); }

    public static String num(long v) { return NF.format(v); }

    public static String money(double v) { return "$" + num(Math.round(v)); }

    public static String stars(long v) { return num(v) + " Stars"; }

    public static String bar(int done, int total, int len) {
        int filled = total <= 0 ? 0 : (int) Math.round((double) done / total * len);
        filled = Math.max(0, Math.min(len, filled));
        return "<green>" + "█".repeat(filled) + "<dark_gray>" + "█".repeat(len - filled);
    }
}
