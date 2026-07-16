/*
 * Decompiled with CFR 0.152.
 */
package id.rumahkita.fishing.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public final class NumberUtil {
    private static final DecimalFormat WEIGHT_FORMAT = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,###", DecimalFormatSymbols.getInstance(Locale.US));

    private NumberUtil() {
    }

    public static double randomDouble(double min, double max) {
        if (max <= min) {
            return min;
        }
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    public static double round2(double value) {
        return (double)Math.round(value * 100.0) / 100.0;
    }

    public static String weight(double value) {
        return WEIGHT_FORMAT.format(value);
    }

    public static String money(long value) {
        return MONEY_FORMAT.format(value);
    }

    public static String money(double value) {
        return MONEY_FORMAT.format(Math.round(value));
    }

    public static int safeIntPrice(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value <= 0.0) {
            return 0;
        }
        if (value > 2.147483647E9) {
            return Integer.MAX_VALUE;
        }
        return (int)Math.round(value);
    }
}

