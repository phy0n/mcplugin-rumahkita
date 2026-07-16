/*
 * Decompiled with CFR 0.152.
 */
package id.rumahkita.fishing.model;

import id.rumahkita.fishing.model.FishDefinition;
import java.util.UUID;

public final class CaughtFish {
    private final UUID uniqueId;
    private final FishDefinition definition;
    private final double weight;
    private final int price;
    private final UUID caughtByUuid;
    private final String caughtByName;
    private final long caughtAt;
    private final String biome;
    private final String world;

    public CaughtFish(UUID uniqueId, FishDefinition definition, double weight, int price, UUID caughtByUuid, String caughtByName, long caughtAt, String biome, String world) {
        this.uniqueId = uniqueId;
        this.definition = definition;
        this.weight = weight;
        this.price = price;
        this.caughtByUuid = caughtByUuid;
        this.caughtByName = caughtByName;
        this.caughtAt = caughtAt;
        this.biome = biome;
        this.world = world;
    }

    public UUID uniqueId() {
        return this.uniqueId;
    }

    public FishDefinition definition() {
        return this.definition;
    }

    public double weight() {
        return this.weight;
    }

    public int price() {
        return this.price;
    }

    public UUID caughtByUuid() {
        return this.caughtByUuid;
    }

    public String caughtByName() {
        return this.caughtByName;
    }

    public long caughtAt() {
        return this.caughtAt;
    }

    public String biome() {
        return this.biome;
    }

    public String world() {
        return this.world;
    }
}

