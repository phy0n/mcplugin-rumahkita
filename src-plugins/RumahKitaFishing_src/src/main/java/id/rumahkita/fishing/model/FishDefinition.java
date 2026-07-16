/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 */
package id.rumahkita.fishing.model;

import id.rumahkita.fishing.model.Rarity;
import java.util.Collections;
import java.util.List;
import org.bukkit.Material;

public final class FishDefinition {
    private final String id;
    private final String displayName;
    private final Material material;
    private final Rarity rarity;
    private final double minWeight;
    private final double maxWeight;
    private final double basePricePerKg;
    private final double chance;
    private final List<String> allowedBiomes;
    private final List<String> allowedWorlds;
    private final String allowedTime;
    private final String allowedWeather;
    private final int customModelData;
    private final List<String> lore;

    public FishDefinition(String id, String displayName, Material material, Rarity rarity, double minWeight, double maxWeight, double basePricePerKg, double chance, List<String> allowedBiomes, List<String> allowedWorlds, String allowedTime, String allowedWeather, int customModelData, List<String> lore) {
        this.id = id;
        this.displayName = displayName;
        this.material = material;
        this.rarity = rarity;
        this.minWeight = Math.max(0.01, minWeight);
        this.maxWeight = Math.max(this.minWeight, maxWeight);
        this.basePricePerKg = Math.max(0.0, basePricePerKg);
        this.chance = Math.max(0.0, chance);
        this.allowedBiomes = allowedBiomes == null ? Collections.emptyList() : List.copyOf(allowedBiomes);
        this.allowedWorlds = allowedWorlds == null ? Collections.emptyList() : List.copyOf(allowedWorlds);
        this.allowedTime = allowedTime == null ? "ANY" : allowedTime.toUpperCase();
        this.allowedWeather = allowedWeather == null ? "ANY" : allowedWeather.toUpperCase();
        this.customModelData = customModelData;
        this.lore = lore == null ? Collections.emptyList() : List.copyOf(lore);
    }

    public String id() {
        return this.id;
    }

    public String displayName() {
        return this.displayName;
    }

    public Material material() {
        return this.material;
    }

    public Rarity rarity() {
        return this.rarity;
    }

    public double minWeight() {
        return this.minWeight;
    }

    public double maxWeight() {
        return this.maxWeight;
    }

    public double basePricePerKg() {
        return this.basePricePerKg;
    }

    public double chance() {
        return this.chance;
    }

    public List<String> allowedBiomes() {
        return this.allowedBiomes;
    }

    public List<String> allowedWorlds() {
        return this.allowedWorlds;
    }

    public String allowedTime() {
        return this.allowedTime;
    }

    public String allowedWeather() {
        return this.allowedWeather;
    }

    public int customModelData() {
        return this.customModelData;
    }

    public List<String> lore() {
        return this.lore;
    }
}

