/*
 * Decompiled with CFR 0.152.
 */
package id.rumahkita.anticheat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ViolationTracker {
    private final Map<UUID, Map<String, Integer>> violations = new HashMap<UUID, Map<String, Integer>>();

    public int add(UUID uuid, String type) {
        Map playerMap = this.violations.computeIfAbsent(uuid, k -> new HashMap());
        int value = playerMap.getOrDefault(type, 0) + 1;
        playerMap.put(type, value);
        return value;
    }

    public int get(UUID uuid, String type) {
        return this.violations.getOrDefault(uuid, Map.of()).getOrDefault(type, 0);
    }

    public void clear(UUID uuid) {
        this.violations.remove(uuid);
    }

    public String summary(UUID uuid) {
        Map<String, Integer> map = this.violations.get(uuid);
        if (map == null || map.isEmpty()) {
            return "none";
        }
        return map.toString();
    }
}

