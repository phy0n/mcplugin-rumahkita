package id.rumahkita.restart;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class RumahKitaRestartPlugin extends JavaPlugin implements CommandExecutor {

    private List<LocalTime> restartTimes = new ArrayList<>();
    private List<Integer> warningSeconds = new ArrayList<>();
    private boolean restartPending = false;
    private int secondsUntilRestart = -1;
    private BukkitRunnable task;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigValues();

        getCommand("rkar").setExecutor(this);

        task = new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        };
        task.runTaskTimer(this, 20L, 20L);

        getLogger().info("RumahKitaRestart enabled.");
    }

    private void loadConfigValues() {
        restartTimes.clear();
        warningSeconds.clear();
        restartPending = false;
        secondsUntilRestart = -1;

        List<String> times = getConfig().getStringList("restart-times");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        for (String t : times) {
            try {
                restartTimes.add(LocalTime.parse(t, formatter));
            } catch (Exception e) {
                getLogger().warning("Invalid time format: " + t);
            }
        }

        List<String> warns = getConfig().getStringList("warnings");
        for (String w : warns) {
            w = w.toLowerCase().trim();
            int sec = 0;
            try {
                if (w.endsWith("m")) {
                    sec = Integer.parseInt(w.replace("m", "")) * 60;
                } else if (w.endsWith("s")) {
                    sec = Integer.parseInt(w.replace("s", ""));
                }
                if (sec > 0 && !warningSeconds.contains(sec)) {
                    warningSeconds.add(sec);
                }
            } catch (Exception e) {
                getLogger().warning("Invalid warning format: " + w);
            }
        }
    }

    private void tick() {
        if (!restartPending) {
            LocalTime now = LocalTime.now(java.time.ZoneId.of("Asia/Jakarta"));
            LocalTime nowTruncated = now.withSecond(0).withNano(0);

            for (LocalTime rt : restartTimes) {
                if (nowTruncated.equals(rt) && now.getSecond() == 0) {
                    startRestartCountdown(0);
                    break;
                }
            }

            for (LocalTime rt : restartTimes) {
                int diffSec = (rt.toSecondOfDay() - now.toSecondOfDay());
                if (diffSec < 0) {
                    diffSec += 24 * 3600;
                }
                
                if (warningSeconds.contains(diffSec)) {
                    startRestartCountdown(diffSec);
                    break;
                }
            }
        } else {
            secondsUntilRestart--;

            if (secondsUntilRestart <= 0) {
                executeRestart();
                return;
            }

            if (warningSeconds.contains(secondsUntilRestart)) {
                broadcastWarning(secondsUntilRestart);
            }
        }
    }

    private void startRestartCountdown(int seconds) {
        if (seconds == 0) {
            executeRestart();
            return;
        }
        
        restartPending = true;
        secondsUntilRestart = seconds;
        broadcastWarning(seconds);
    }

    private void executeRestart() {
        String kickMsg = ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.kick-message", "&cServer direstart."));
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.kickPlayer(kickMsg);
        }
        Bukkit.spigot().restart();
    }

    private void broadcastWarning(int seconds) {
        String timeStr = formatTimeLength(seconds);
        
        String prefix = ChatColor.translateAlternateColorCodes('&', "&8[&c&l!&8] &c&lRESTART &8\u00bb ");
        String msg = ChatColor.translateAlternateColorCodes('&', "&eServer akan restart dalam &c&l%time%&e!").replace("%time%", timeStr);
        
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(prefix + msg);
            
            if (seconds <= 10) {
                p.sendTitle(ChatColor.translateAlternateColorCodes('&', "&c&l\u26a0 &c" + seconds + " &c&l\u26a0"), ChatColor.YELLOW + "Bersiap untuk Server Restart", 5, 20, 5);
                p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
            } else if (seconds == 60) {
                p.sendTitle(ChatColor.translateAlternateColorCodes('&', "&c&l1 MENIT"), ChatColor.YELLOW + "Menuju Server Restart", 10, 40, 10);
                p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.0f);
            } else if (seconds == 300) {
                p.sendTitle(ChatColor.translateAlternateColorCodes('&', "&6&l5 MENIT"), ChatColor.YELLOW + "Menuju Server Restart", 10, 40, 10);
                p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            } else {
                p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("rkrestart.admin")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e=== &6AutoRestart Commands &e==="));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a/rkar time &7- Cek waktu restart selanjutnya"));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a/rkar now [waktu] &7- Paksa restart sesuai waktu"));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a/rkar cancel &7- Batalkan restart yang sedang berjalan"));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a/rkar reload &7- Muat ulang config"));
            return true;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("reload")) {
            reloadConfig();
            loadConfigValues();
            sender.sendMessage(ChatColor.GREEN + "AutoRestart config reloaded.");
        } else if (sub.equals("time")) {
            if (restartPending) {
                sender.sendMessage(ChatColor.GREEN + "Server akan direstart dalam " + formatTimeLength(secondsUntilRestart));
            } else {
                LocalTime now = LocalTime.now(java.time.ZoneId.of("Asia/Jakarta"));
                int minDiff = Integer.MAX_VALUE;
                LocalTime nextTime = null;
                for (LocalTime rt : restartTimes) {
                    int diffSec = rt.toSecondOfDay() - now.toSecondOfDay();
                    if (diffSec <= 0) {
                        diffSec += 24 * 3600;
                    }
                    if (diffSec < minDiff) {
                        minDiff = diffSec;
                        nextTime = rt;
                    }
                }
                if (nextTime != null) {
                    sender.sendMessage(ChatColor.GREEN + "Jadwal restart terdekat pukul " + nextTime.toString() + " (" + formatTimeLength(minDiff) + " lagi)");
                } else {
                    sender.sendMessage(ChatColor.RED + "Tidak ada jadwal restart di config.");
                }
            }
        } else if (sub.equals("now")) {
            if (args.length > 1) {
                int seconds = parseTime(args[1]);
                if (seconds > 0) {
                    startRestartCountdown(seconds);
                    sender.sendMessage(ChatColor.GREEN + "Server akan direstart dalam " + args[1]);
                } else {
                    sender.sendMessage(ChatColor.RED + "Format waktu salah. Gunakan m, h, atau s (contoh: 1m, 2h, 30s)");
                }
            } else {
                executeRestart();
            }
        } else if (sub.equals("cancel")) {
            if (restartPending) {
                restartPending = false;
                secondsUntilRestart = -1;
                Bukkit.broadcastMessage(ChatColor.GREEN + "Server restart dibatalkan!");
            } else {
                sender.sendMessage(ChatColor.RED + "Tidak ada restart yang sedang berjalan.");
            }
        }

        return true;
    }

    private int parseTime(String timeStr) {
        timeStr = timeStr.toLowerCase().trim();
        try {
            if (timeStr.endsWith("h")) {
                return Integer.parseInt(timeStr.replace("h", "")) * 3600;
            } else if (timeStr.endsWith("m")) {
                return Integer.parseInt(timeStr.replace("m", "")) * 60;
            } else if (timeStr.endsWith("s")) {
                return Integer.parseInt(timeStr.replace("s", ""));
            }
            return Integer.parseInt(timeStr);
        } catch (Exception e) {
            return -1;
        }
    }

    private String formatTimeLength(int totalSeconds) {
        if (totalSeconds < 60) return totalSeconds + " detik";
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        
        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append(" jam ");
        if (minutes > 0) sb.append(minutes).append(" menit ");
        if (seconds > 0) sb.append(seconds).append(" detik");
        return sb.toString().trim();
    }
}
