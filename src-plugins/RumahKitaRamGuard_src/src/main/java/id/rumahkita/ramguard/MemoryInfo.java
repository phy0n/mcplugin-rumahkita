/*
 * Decompiled with CFR 0.152.
 */
package id.rumahkita.ramguard;

public final class MemoryInfo {
    private final long usedBytes;
    private final long maxBytes;
    private final double percent;

    public MemoryInfo(long usedBytes, long maxBytes, double percent) {
        this.usedBytes = usedBytes;
        this.maxBytes = maxBytes;
        this.percent = percent;
    }

    public long getUsedBytes() {
        return this.usedBytes;
    }

    public long getMaxBytes() {
        return this.maxBytes;
    }

    public double getPercent() {
        return this.percent;
    }

    public String getUsedFormatted() {
        return MemoryInfo.formatBytes(this.usedBytes);
    }

    public String getMaxFormatted() {
        return MemoryInfo.formatBytes(this.maxBytes);
    }

    public String getPercentFormatted() {
        return String.format("%.1f", this.percent);
    }

    public static MemoryInfo current() {
        Runtime runtime = Runtime.getRuntime();
        long max = runtime.maxMemory();
        long total = runtime.totalMemory();
        long free = runtime.freeMemory();
        long used = total - free;
        double percent = max <= 0L ? 0.0 : (double)used * 100.0 / (double)max;
        return new MemoryInfo(used, max, percent);
    }

    public static String formatBytes(long bytes) {
        double mib = (double)bytes / 1024.0 / 1024.0;
        if (mib >= 1024.0) {
            return String.format("%.2f GB", mib / 1024.0);
        }
        return String.format("%.0f MB", mib);
    }
}

