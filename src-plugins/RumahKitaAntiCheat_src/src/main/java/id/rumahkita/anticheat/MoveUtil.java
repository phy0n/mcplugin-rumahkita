/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.GameMode
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.block.Block
 *  org.bukkit.entity.Player
 *  org.bukkit.potion.PotionEffectType
 */
package id.rumahkita.anticheat;

import java.util.List;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public final class MoveUtil {
    private static final double[] FOOT_OFFSETS = new double[]{0.0, 0.28, -0.28};
    private static final double[] GROUND_Y_OFFSETS = new double[]{0.04, 0.12, 0.3, 0.55};

    private MoveUtil() {
    }

    public static boolean checkableGamemode(Player player) {
        return switch (player.getGameMode()) {
            case GameMode.SURVIVAL, GameMode.ADVENTURE -> true;
            default -> false;
        };
    }

    public static boolean isLiquidNear(Player player) {
        Location l = player.getLocation();
        return MoveUtil.blockAt(l).isLiquid() || MoveUtil.blockAt(l.clone().add(0.0, 1.0, 0.0)).isLiquid() || MoveUtil.blockAt(l.clone().subtract(0.0, 1.0, 0.0)).isLiquid() || MoveUtil.blockAt(l.clone().subtract(0.0, 0.15, 0.0)).isLiquid();
    }

    public static boolean isOnWaterSurface(Player player) {
        Location loc = player.getLocation();
        boolean feetAir = MoveUtil.isAirLike(MoveUtil.blockAt(loc).getType()) || MoveUtil.isAirLike(MoveUtil.blockAt(loc.clone().add(0.0, 0.35, 0.0)).getType());
        boolean waterBelow = false;
        for (double ox : FOOT_OFFSETS) {
            for (double oz : FOOT_OFFSETS) {
                Material below1 = MoveUtil.blockAt(loc.clone().add(ox, -0.15, oz)).getType();
                Material below2 = MoveUtil.blockAt(loc.clone().add(ox, -0.55, oz)).getType();
                if (below1 != Material.WATER && below2 != Material.WATER) continue;
                waterBelow = true;
            }
        }
        return feetAir && waterBelow && !MoveUtil.isStandingOnSolid(player);
    }

    public static boolean isStandingOnSolid(Player player) {
        Location loc = player.getLocation();
        if (player.isOnGround()) {
            return true;
        }
        for (double y : GROUND_Y_OFFSETS) {
            for (double ox : FOOT_OFFSETS) {
                for (double oz : FOOT_OFFSETS) {
                    Material m = MoveUtil.blockAt(loc.clone().add(ox, -y, oz)).getType();
                    if (!MoveUtil.isSupportBlock(m)) continue;
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isNearSolidForBuild(Player player) {
        double[] ys;
        Location loc = player.getLocation();
        double[] side = new double[]{0.0, 0.36, -0.36, 0.72, -0.72};
        for (double y : ys = new double[]{-0.25, 0.05, 0.55, 1.05, 1.65}) {
            for (double ox : side) {
                for (double oz : side) {
                    Material m;
                    if (ox == 0.0 && oz == 0.0 && y > 0.4 || !MoveUtil.isSupportBlock(m = MoveUtil.blockAt(loc.clone().add(ox, y, oz)).getType())) continue;
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isAirLike(Material m) {
        return m == Material.AIR || m == Material.CAVE_AIR || m == Material.VOID_AIR;
    }

    public static boolean isSupportBlock(Material m) {
        if (m.isSolid()) {
            return true;
        }
        String name = m.name();
        return m == Material.LILY_PAD || m == Material.FROSTED_ICE || m == Material.SCAFFOLDING || m == Material.SNOW || m == Material.CAKE || name.endsWith("_CARPET") || name.endsWith("_SLAB") || name.endsWith("_STAIRS") || name.endsWith("_TRAPDOOR") || name.endsWith("_FENCE") || name.endsWith("_WALL");
    }

    public static boolean isOnIce(Player player) {
        Material m = MoveUtil.blockAt(player.getLocation().clone().subtract(0.0, 0.1, 0.0)).getType();
        return m == Material.ICE || m == Material.PACKED_ICE || m == Material.BLUE_ICE || m == Material.FROSTED_ICE;
    }

    public static boolean isOnSlimeHoney(Player player) {
        Material m = MoveUtil.blockAt(player.getLocation().clone().subtract(0.0, 0.1, 0.0)).getType();
        return m == Material.SLIME_BLOCK || m == Material.HONEY_BLOCK;
    }

    public static boolean isOnSoul(Player player) {
        Material m = MoveUtil.blockAt(player.getLocation().clone().subtract(0.0, 0.1, 0.0)).getType();
        return m == Material.SOUL_SAND || m == Material.SOUL_SOIL;
    }

    public static boolean isWeb(Player player) {
        Material feet = MoveUtil.blockAt(player.getLocation()).getType();
        Material head = MoveUtil.blockAt(player.getLocation().clone().add(0.0, 1.0, 0.0)).getType();
        return feet == Material.COBWEB || head == Material.COBWEB;
    }

    public static boolean isClimbable(Player player) {
        Material feet = MoveUtil.blockAt(player.getLocation()).getType();
        Material head = MoveUtil.blockAt(player.getLocation().clone().add(0.0, 1.0, 0.0)).getType();
        return MoveUtil.climbable(feet) || MoveUtil.climbable(head);
    }

    private static boolean climbable(Material m) {
        return m == Material.LADDER || m == Material.VINE || m == Material.TWISTING_VINES || m == Material.TWISTING_VINES_PLANT || m == Material.WEEPING_VINES || m == Material.WEEPING_VINES_PLANT || m == Material.SCAFFOLDING;
    }

    public static boolean isBubbleColumn(Player player) {
        Material feet = MoveUtil.blockAt(player.getLocation()).getType();
        Material below = MoveUtil.blockAt(player.getLocation().clone().subtract(0.0, 1.0, 0.0)).getType();
        return feet == Material.BUBBLE_COLUMN || below == Material.BUBBLE_COLUMN;
    }

    public static boolean hasMovementExemptEffect(Player player) {
        return player.hasPotionEffect(PotionEffectType.LEVITATION) || player.hasPotionEffect(PotionEffectType.SLOW_FALLING) || player.hasPotionEffect(PotionEffectType.DOLPHINS_GRACE);
    }

    public static boolean hasAnyPermission(Player player, List<String> permissions) {
        for (String perm : permissions) {
            String base;
            if (!(perm.endsWith(".*") ? player.hasPermission((base = perm.substring(0, perm.length() - 2)) + ".*") || player.hasPermission(base) : player.hasPermission(perm))) continue;
            return true;
        }
        return false;
    }

    private static Block blockAt(Location location) {
        return location.getWorld().getBlockAt(location);
    }
}

