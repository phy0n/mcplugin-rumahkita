/*
 * Decompiled with CFR 0.152.
 */
package id.rumahkita.ramguard;

public final class CleanupReport {
    private int entitiesRemoved;
    private int mobsRemoved;
    private int chunksUnloaded;
    private boolean viewDistanceChanged;
    private boolean garbageCollectorRequested;

    public int getEntitiesRemoved() {
        return this.entitiesRemoved;
    }

    public int getMobsRemoved() {
        return this.mobsRemoved;
    }

    public int getChunksUnloaded() {
        return this.chunksUnloaded;
    }

    public boolean isViewDistanceChanged() {
        return this.viewDistanceChanged;
    }

    public boolean isGarbageCollectorRequested() {
        return this.garbageCollectorRequested;
    }

    public void addEntitiesRemoved(int amount) {
        this.entitiesRemoved += Math.max(0, amount);
    }

    public void addMobsRemoved(int amount) {
        this.mobsRemoved += Math.max(0, amount);
    }

    public void addChunksUnloaded(int amount) {
        this.chunksUnloaded += Math.max(0, amount);
    }

    public void setViewDistanceChanged(boolean viewDistanceChanged) {
        this.viewDistanceChanged = viewDistanceChanged;
    }

    public void setGarbageCollectorRequested(boolean garbageCollectorRequested) {
        this.garbageCollectorRequested = garbageCollectorRequested;
    }

    public String summary() {
        return "entities=" + this.entitiesRemoved + ", mobs=" + this.mobsRemoved + ", chunks=" + this.chunksUnloaded + ", viewDistanceChanged=" + this.viewDistanceChanged + ", gc=" + this.garbageCollectorRequested;
    }
}

