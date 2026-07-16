/*
 * Decompiled with CFR 0.152.
 */
package id.rumahkita.fishing.model;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class PlayerFishingData {
    private final UUID uuid;
    private String name;
    private int level;
    private int exp;
    private long totalCatches;
    private long totalSold;
    private long totalEarned;
    private double biggestFishWeight;
    private String biggestFishName;
    private long legendaryCatches;
    private long mythicCatches;
    private final Set<String> discoveredFishes;

    public PlayerFishingData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.level = 1;
        this.exp = 0;
        this.biggestFishName = "-";
        this.discoveredFishes = new HashSet<String>();
    }

    public UUID uuid() {
        return this.uuid;
    }

    public String name() {
        return this.name;
    }

    public void name(String name) {
        this.name = name;
    }

    public int level() {
        return this.level;
    }

    public void level(int level) {
        this.level = Math.max(1, level);
    }

    public int exp() {
        return this.exp;
    }

    public void exp(int exp) {
        this.exp = Math.max(0, exp);
    }

    public long totalCatches() {
        return this.totalCatches;
    }

    public void totalCatches(long totalCatches) {
        this.totalCatches = Math.max(0L, totalCatches);
    }

    public long totalSold() {
        return this.totalSold;
    }

    public void totalSold(long totalSold) {
        this.totalSold = Math.max(0L, totalSold);
    }

    public long totalEarned() {
        return this.totalEarned;
    }

    public void totalEarned(long totalEarned) {
        this.totalEarned = Math.max(0L, totalEarned);
    }

    public double biggestFishWeight() {
        return this.biggestFishWeight;
    }

    public void biggestFishWeight(double biggestFishWeight) {
        this.biggestFishWeight = Math.max(0.0, biggestFishWeight);
    }

    public String biggestFishName() {
        return this.biggestFishName;
    }

    public void biggestFishName(String biggestFishName) {
        this.biggestFishName = biggestFishName == null ? "-" : biggestFishName;
    }

    public long legendaryCatches() {
        return this.legendaryCatches;
    }

    public void legendaryCatches(long legendaryCatches) {
        this.legendaryCatches = Math.max(0L, legendaryCatches);
    }

    public long mythicCatches() {
        return this.mythicCatches;
    }

    public void mythicCatches(long mythicCatches) {
        this.mythicCatches = Math.max(0L, mythicCatches);
    }

    public Set<String> discoveredFishes() {
        return this.discoveredFishes;
    }

    public void reset() {
        this.level = 1;
        this.exp = 0;
        this.totalCatches = 0L;
        this.totalSold = 0L;
        this.totalEarned = 0L;
        this.biggestFishWeight = 0.0;
        this.biggestFishName = "-";
        this.legendaryCatches = 0L;
        this.mythicCatches = 0L;
        this.discoveredFishes.clear();
    }
}

