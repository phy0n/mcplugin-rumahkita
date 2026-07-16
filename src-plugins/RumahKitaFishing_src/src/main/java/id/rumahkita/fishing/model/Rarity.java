/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Sound
 */
package id.rumahkita.fishing.model;

import org.bukkit.Sound;

public enum Rarity {
    COMMON("&f", Sound.ENTITY_EXPERIENCE_ORB_PICKUP, "WATER_SPLASH", "SPLASH"),
    UNCOMMON("&a", Sound.ENTITY_PLAYER_LEVELUP, "HAPPY_VILLAGER", "VILLAGER_HAPPY"),
    RARE("&b", Sound.BLOCK_NOTE_BLOCK_PLING, "CRIT"),
    EPIC("&5", Sound.ENTITY_FIREWORK_ROCKET_BLAST, "ENCHANT", "ENCHANTMENT_TABLE"),
    LEGENDARY("&6", Sound.UI_TOAST_CHALLENGE_COMPLETE, "TOTEM_OF_UNDYING", "TOTEM"),
    MYTHIC("&d", Sound.ENTITY_ENDER_DRAGON_GROWL, "DRAGON_BREATH");

    private final String color;
    private final Sound sound;
    private final String[] particleNames;

    private Rarity(String color, Sound sound, String ... particleNames) {
        this.color = color;
        this.sound = sound;
        this.particleNames = particleNames;
    }

    public String color() {
        return this.color;
    }

    public Sound sound() {
        return this.sound;
    }

    public String[] particleNames() {
        return this.particleNames;
    }

    public static Rarity fromString(String value) {
        if (value == null) {
            return COMMON;
        }
        try {
            return Rarity.valueOf(value.trim().toUpperCase());
        }
        catch (IllegalArgumentException exception) {
            return COMMON;
        }
    }

    public boolean isRareOrBetter() {
        return this.ordinal() >= RARE.ordinal();
    }
}

