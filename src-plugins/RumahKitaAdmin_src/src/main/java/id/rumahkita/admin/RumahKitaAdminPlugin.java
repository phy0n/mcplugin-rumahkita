package id.rumahkita.admin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.bukkit.Statistic;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class RumahKitaAdminPlugin extends JavaPlugin implements CommandExecutor, TabCompleter, Listener {

    private final Set<UUID> frozenPlayers = new HashSet<>();
    private final Set<UUID> vanishedPlayers = new HashSet<>();
    private final Set<UUID> staffChatToggled = new HashSet<>();
    private final Set<UUID> mutedPlayers = new HashSet<>();
    private final Set<UUID> spyPlayers = new HashSet<>();
    private final Set<UUID> godPlayers = new HashSet<>();
    private final Set<UUID> jailedPlayers = new HashSet<>();
    private final Map<UUID, org.bukkit.permissions.PermissionAttachment> vanishPerms = new HashMap<>();
    private final Map<UUID, Location> spectateLocations = new HashMap<>();
    private final Map<UUID, GameMode> spectateGameModes = new HashMap<>();
    
    private boolean maintenanceMode = false;
    private boolean chatLocked = false;
    private Location jailLocation = null;
    
    private File dataFile;
    private FileConfiguration dataConfig;

    private List<java.time.LocalTime> restartTimes = new java.util.ArrayList<>();
    private java.util.Map<java.util.UUID, Long> lastChatTimes = new java.util.HashMap<>();
    private java.util.Map<java.util.UUID, String> lastMessages = new java.util.HashMap<>();
    private List<Integer> warningSeconds = new ArrayList<>();
    private boolean restartPending = false;
    private int secondsUntilRestart = -1;
    private org.bukkit.scheduler.BukkitRunnable restartTask;

    @Override
    public void onEnable() {
        if (getCommand("rka") != null) {
            getCommand("rka").setExecutor(this);
            getCommand("rka").setTabCompleter(this);
        }
        if (getCommand("sc") != null) {
            getCommand("sc").setExecutor(this);
        }
        getServer().getPluginManager().registerEvents(this, this);
        createDataConfig();
        loadJailData();
        
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
        loadRestartConfig();
        restartTask = new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                tickRestart();
            }
        };
        restartTask.runTaskTimer(this, 20L, 20L);

        getLogger().info("RumahKitaAdmin successfully enabled!");
    }

    @Override
    public void onDisable() {
        saveJailData();
        frozenPlayers.clear();
        vanishedPlayers.clear();
        staffChatToggled.clear();
        mutedPlayers.clear();
        spyPlayers.clear();
        godPlayers.clear();
        getLogger().info("RumahKitaAdmin successfully disabled!");
    }

    private String getMsg(String path, String def) {
        return ChatColor.translateAlternateColorCodes('&', getConfig().getString(path, def));
    }

    private void createDataConfig() {
        dataFile = new File(getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                getLogger().severe("Could not create data.yml!");
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    private void saveDataConfig() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            getLogger().severe("Could not save data.yml!");
        }
    }

    private void loadJailData() {
        if (dataConfig.contains("jail.world")) {
            jailLocation = new Location(
                Bukkit.getWorld(dataConfig.getString("jail.world")),
                dataConfig.getDouble("jail.x"),
                dataConfig.getDouble("jail.y"),
                dataConfig.getDouble("jail.z"),
                (float) dataConfig.getDouble("jail.yaw"),
                (float) dataConfig.getDouble("jail.pitch")
            );
        }
        if (dataConfig.contains("jailed_players")) {
            for (String uuidStr : dataConfig.getStringList("jailed_players")) {
                jailedPlayers.add(UUID.fromString(uuidStr));
            }
        }
    }

    private void saveJailData() {
        List<String> jailedList = new ArrayList<>();
        for (UUID uuid : jailedPlayers) {
            jailedList.add(uuid.toString());
        }
        dataConfig.set("jailed_players", jailedList);
        saveDataConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("rumahkita.admin")) {
            sender.sendMessage(getMsg("messages.no-permission", "&cUnknown command. Type \"/help\" for help."));
            return true;
        }

        if (command.getName().equalsIgnoreCase("sc")) {
            return handleStaffChat(sender, args);
        }

        if (args.length == 0) {
            sendHelpMenu(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "checkip": return handleCheckIp(sender, args);
            case "checkalts": return handleCheckAlts(sender, args);
            case "allowalt": return handleAllowAlt(sender, args);
            case "blockalt": return handleBlockAlt(sender, args);
            case "unblockalt": return handleUnblockAlt(sender, args);
            case "setmainaccount": return handleSetMain(sender, args);
            case "blockip": return handleBlockIp(sender, args);
            case "unblockip": return handleUnblockIp(sender, args);
            case "vpn": return handleVpn(sender, args);
            case "clearchat": return handleClearChat(sender);
            case "freeze": return handleFreeze(sender, args);
            case "invsee": return handleInvsee(sender, args);
            case "ec": return handleEc(sender, args);
            case "kick": return handleKick(sender, args);
            case "broadcast": return handleBroadcast(sender, args);
            case "vanish": return handleVanish(sender, args);
            case "smite": return handleSmite(sender, args);
            case "troll": return handleTroll(sender, args);
            case "heal": return handleHeal(sender, args);
            case "fly": return handleFly(sender, args);
            case "speed": return handleSpeed(sender, args);
            case "mute": return handleMute(sender, args);
            case "spy": return handleSpy(sender);
            case "god": return handleGod(sender, args);
            case "maintenance": return handleMaintenance(sender);
            case "chatlock": return handleChatLock(sender);
            case "setjail": return handleSetJail(sender);
            case "jail": return handleJail(sender, args);
            case "unjail": return handleUnjail(sender, args);
            case "warn": return handleWarn(sender, args);
            case "inspect": return handleInspect(sender, args);
            case "sudo": return handleSudo(sender, args);
            case "spectate": return handleSpectate(sender, args);
            case "restart": return handleRestart(sender, args);
            case "reload": return handleReload(sender);
            default:
                sendHelpMenu(sender);
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("rumahkita.admin")) {
            return new ArrayList<>();
        }

        if (command.getName().equalsIgnoreCase("rka")) {
            if (args.length == 1) {
                List<String> subs = Arrays.asList(
                    "checkip", "checkalts", "clearchat", "freeze", "invsee", "ec", "kick", "broadcast", 
                    "vanish", "smite", "troll", "heal", "fly", "speed", "mute", "spy", "god", "maintenance",
                    "chatlock", "setjail", "jail", "unjail", "warn", "inspect", "sudo", "spectate", "restart", "reload", "allowalt", "blockalt", "unblockalt", "setmainaccount", "blockip", "unblockip", "vpn"
                );
                return subs.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
            } else if (args.length == 2) {
                String sub = args[0].toLowerCase();
                if (sub.equals("restart")) {
                    return Arrays.asList("now", "time", "cancel", "reload").stream().filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
                } else if (sub.equals("vpn")) {
                    return Arrays.asList("check", "allow", "remove", "on", "off").stream().filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
                }
                List<String> targetCommands = Arrays.asList("checkip", "checkalts", "allowalt", "blockalt", "unblockalt", "setmainaccount", "freeze", "invsee", "ec", "kick", "smite", "troll", "heal", "fly", "mute", "jail", "unjail", "warn", "inspect", "sudo", "spectate");
                if (targetCommands.contains(sub)) {
                    return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
                } else if (sub.equals("speed")) {
                    return Arrays.asList("1", "2", "3", "4", "5", "10");
                }
            } else if (args.length == 3 && args[0].equalsIgnoreCase("troll")) {
                List<String> trolls = Arrays.asList("launch", "fakeop", "spin", "blind", "drop", "scare", "fakeban", "cobweb", "shuffle", "potato");
                return trolls.stream().filter(s -> s.startsWith(args[2].toLowerCase())).collect(Collectors.toList());
            }
        }
        return new ArrayList<>();
    }

    private void sendHelpMenu(CommandSender sender) {
        sender.sendMessage(ChatColor.DARK_GRAY + "==== " + ChatColor.AQUA + ChatColor.BOLD + "RumahKita Admin" + ChatColor.DARK_GRAY + " ====");
        
        sender.sendMessage(ChatColor.YELLOW + "Moderation & Discipline");
        sender.sendMessage(ChatColor.GREEN + " /rka kick <p> " + ChatColor.GRAY + "- Kick a player.");
        sender.sendMessage(ChatColor.GREEN + " /rka warn <p> <msg> " + ChatColor.GRAY + "- Warn a player.");
        sender.sendMessage(ChatColor.GREEN + " /rka mute <p> " + ChatColor.GRAY + "- Mute player chat.");
        sender.sendMessage(ChatColor.GREEN + " /rka freeze <p> " + ChatColor.GRAY + "- Freeze player.");
        sender.sendMessage(ChatColor.GREEN + " /rka jail/unjail <p> " + ChatColor.GRAY + "- Jail a player.");
        sender.sendMessage(ChatColor.GREEN + " /rka setjail " + ChatColor.GRAY + "- Set jail location.");
        sender.sendMessage(ChatColor.GREEN + " /rka clearchat " + ChatColor.GRAY + "- Clear chat.");
        sender.sendMessage(ChatColor.GREEN + " /rka chatlock " + ChatColor.GRAY + "- Lock global chat.");
        
        sender.sendMessage(ChatColor.YELLOW + "\nIntelligence & Surveillance");
        sender.sendMessage(ChatColor.GREEN + " /rka vanish " + ChatColor.GRAY + "- Hide from normal players.");
        sender.sendMessage(ChatColor.GREEN + " /rka spy " + ChatColor.GRAY + "- Spy on commands.");
        sender.sendMessage(ChatColor.GREEN + " /rka spectate <p> " + ChatColor.GRAY + "- Spectate a player.");
        sender.sendMessage(ChatColor.GREEN + " /rka inspect <p> " + ChatColor.GRAY + "- Inspect player stats.");
        sender.sendMessage(ChatColor.GREEN + " /rka checkip <p> " + ChatColor.GRAY + "- Check real IP.");
        sender.sendMessage(ChatColor.GREEN + " /rka checkalts <p> " + ChatColor.GRAY + "- Find alt accounts.");
        sender.sendMessage(ChatColor.GREEN + " /rka setmainaccount <p> " + ChatColor.GRAY + "- Set player as Main Account.");
        sender.sendMessage(ChatColor.GREEN + " /rka allowalt <p> <alt> " + ChatColor.GRAY + "- Bypass alt limit (Whitelist IP).");
        sender.sendMessage(ChatColor.GREEN + " /rka blockalt <p> <alt> " + ChatColor.GRAY + "- Block alt forever.");
        sender.sendMessage(ChatColor.GREEN + " /rka unblockalt <alt> " + ChatColor.GRAY + "- Unblock an alt account.");
        sender.sendMessage(ChatColor.GREEN + " /rka blockip <ip> " + ChatColor.GRAY + "- Permanently block IP.");
        sender.sendMessage(ChatColor.GREEN + " /rka unblockip <ip> " + ChatColor.GRAY + "- Unblock an IP.");
        sender.sendMessage(ChatColor.GREEN + " /rka vpn <on/off/allow/remove> " + ChatColor.GRAY + "- Anti-VPN system.");
        sender.sendMessage(ChatColor.DARK_GRAY + "--------------------------------------------------");
        sender.sendMessage(ChatColor.GREEN + " /rka invsee/ec <p> " + ChatColor.GRAY + "- View inv/enderchest.");

        sender.sendMessage(ChatColor.YELLOW + "\nAdmin Utility");
        sender.sendMessage(ChatColor.GREEN + " /rka god " + ChatColor.GRAY + "- God mode.");
        sender.sendMessage(ChatColor.GREEN + " /rka heal <p> " + ChatColor.GRAY + "- Fill health/food.");
        sender.sendMessage(ChatColor.GREEN + " /rka speed <1-10> " + ChatColor.GRAY + "- Set speed.");
        sender.sendMessage(ChatColor.GREEN + " /rka sudo <p> <cmd> " + ChatColor.GRAY + "- Force player action.");
        sender.sendMessage(ChatColor.GREEN + " /rka broadcast <msg> " + ChatColor.GRAY + "- Server broadcast.");
        sender.sendMessage(ChatColor.GREEN + " /rka maintenance " + ChatColor.GRAY + "- Lock server.");
        sender.sendMessage(ChatColor.GREEN + " /rka restart <now|time|cancel|reload> " + ChatColor.GRAY + "- AutoRestart system.");
        sender.sendMessage(ChatColor.GREEN + " /rka reload " + ChatColor.GRAY + "- Reload config.");
        
        sender.sendMessage(ChatColor.YELLOW + "\nTroll Features");
        sender.sendMessage(ChatColor.GREEN + " /rka troll <p> <type> " + ChatColor.GRAY + "- launch|fakeop|spin|blind|drop|scare|fakeban|cobweb|shuffle|potato");
        
        sender.sendMessage(ChatColor.DARK_GRAY + "=======================================");
    }

    // --- NEW MODERATION COMMANDS ---

    private boolean handleReload(CommandSender sender) {
        reloadConfig();
        loadJailData();
        loadRestartConfig();
        sender.sendMessage(ChatColor.GREEN + "RumahKitaAdmin configuration reloaded successfully!");
        return true;
    }

    private boolean handleChatLock(CommandSender sender) {
        chatLocked = !chatLocked;
        if (chatLocked) {
            Bukkit.broadcastMessage(ChatColor.RED + "Global chat has been locked by an Admin.");
        } else {
            Bukkit.broadcastMessage(ChatColor.GREEN + "Global chat has been unlocked.");
        }
        return true;
    }

    private boolean handleSetJail(CommandSender sender) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        jailLocation = p.getLocation();
        dataConfig.set("jail.world", jailLocation.getWorld().getName());
        dataConfig.set("jail.x", jailLocation.getX());
        dataConfig.set("jail.y", jailLocation.getY());
        dataConfig.set("jail.z", jailLocation.getZ());
        dataConfig.set("jail.yaw", jailLocation.getYaw());
        dataConfig.set("jail.pitch", jailLocation.getPitch());
        saveDataConfig();
        p.sendMessage(ChatColor.GREEN + "Jail location has been set to your current position.");
        return true;
    }

    private boolean handleJail(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /rka jail <player>");
            return true;
        }
        if (jailLocation == null) {
            sender.sendMessage(ChatColor.RED + "Jail location is not set! Use /rka setjail first.");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player is not online.");
            return true;
        }
        jailedPlayers.add(target.getUniqueId());
        saveJailData();
        target.teleport(jailLocation);
        target.sendMessage(getMsg("messages.jail.jailed", "&cYou have been jailed by an Admin!"));
        sender.sendMessage(ChatColor.GREEN + target.getName() + " has been jailed.");
        logModeration("JAIL: " + sender.getName() + " jailed " + target.getName());
        return true;
    }

    private boolean handleUnjail(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /rka unjail <player>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        UUID targetUuid = null;
        String targetName = args[1];
        
        if (target != null) {
            targetUuid = target.getUniqueId();
            targetName = target.getName();
        } else {
            for (UUID uuid : jailedPlayers) {
                if (Bukkit.getOfflinePlayer(uuid).getName().equalsIgnoreCase(args[1])) {
                    targetUuid = uuid;
                    break;
                }
            }
        }

        if (targetUuid == null || !jailedPlayers.contains(targetUuid)) {
            sender.sendMessage(ChatColor.RED + "Player is not in jail.");
            return true;
        }

        jailedPlayers.remove(targetUuid);
        saveJailData();
        if (target != null) {
            target.teleport(target.getWorld().getSpawnLocation());
            target.sendMessage(getMsg("messages.jail.unjailed", "&aYou have been unjailed. Don't break the rules again!"));
        }
        sender.sendMessage(ChatColor.GREEN + targetName + " has been unjailed.");
        logModeration("UNJAIL: " + sender.getName() + " unjailed " + targetName);
        return true;
    }

    private boolean handleWarn(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /rka warn <player> <reason>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player is not online.");
            return true;
        }
        String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        int warnings = getConfig().getInt("warnings." + target.getUniqueId(), 0) + 1;
        int max = getConfig().getInt("settings.warn.max-warnings", 3);
        getConfig().set("warnings." + target.getUniqueId(), warnings);
        saveConfig();

        String title = getMsg("messages.warn.title", "&4&lWARNING!");
        target.sendTitle(title, ChatColor.YELLOW + reason, 10, 70, 20);
        target.sendMessage(ChatColor.RED + "You have received a warning: " + ChatColor.YELLOW + reason);
        target.sendMessage(ChatColor.RED + "Total Warnings: " + warnings + "/" + max);
        sender.sendMessage(ChatColor.GREEN + "Warned " + target.getName() + ". Total warnings: " + warnings);
        logModeration("WARN: " + sender.getName() + " warned " + target.getName() + " for: " + reason + " (Total: " + warnings + ")");

        if (warnings >= max) {
            String kickMsg = getMsg("messages.warn.kick-message", "&cYou have been kicked for reaching maximum warnings.\n&fReason: &e%reason%").replace("%reason%", reason);
            target.kickPlayer(kickMsg);
            Bukkit.broadcastMessage(ChatColor.RED + target.getName() + " was kicked for reaching maximum warnings.");
            getConfig().set("warnings." + target.getUniqueId(), 0); // Reset warnings after kick
            saveConfig();
        }
        return true;
    }

    private boolean handleSudo(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /rka sudo <player> <message/command>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player is not online.");
            return true;
        }
        String action = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        target.chat(action);
        sender.sendMessage(ChatColor.GREEN + "Forced " + target.getName() + " to execute: " + action);
        return true;
    }

    private boolean handleSpectate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        
        if (args.length < 2) {
            if (spectateLocations.containsKey(p.getUniqueId())) {
                p.teleport(spectateLocations.get(p.getUniqueId()));
                p.setGameMode(spectateGameModes.get(p.getUniqueId()));
                spectateLocations.remove(p.getUniqueId());
                spectateGameModes.remove(p.getUniqueId());
                p.sendMessage(ChatColor.GREEN + "Stopped spectating. Returned to original location.");
            } else {
                p.sendMessage(ChatColor.RED + "You are not spectating anyone. Usage: /rka spectate <player>");
            }
            return true;
        }
        
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            p.sendMessage(ChatColor.RED + "Player is not online.");
            return true;
        }
        
        if (!spectateLocations.containsKey(p.getUniqueId())) {
            spectateLocations.put(p.getUniqueId(), p.getLocation());
            spectateGameModes.put(p.getUniqueId(), p.getGameMode());
        }
        
        p.setGameMode(GameMode.SPECTATOR);
        p.teleport(target);
        p.sendMessage(ChatColor.GREEN + "You are now spectating " + target.getName());
        return true;
    }

    private boolean handleInspect(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        if (args.length < 2) {
            p.sendMessage(ChatColor.RED + "Usage: /rka inspect <player>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            p.sendMessage(ChatColor.RED + "Player is not online.");
            return true;
        }
        
        Inventory inv = Bukkit.createInventory(null, 36, "Inspect: " + target.getName());
        
        // Armor & Hands
        inv.setItem(10, target.getInventory().getHelmet());
        inv.setItem(11, target.getInventory().getChestplate());
        inv.setItem(12, target.getInventory().getLeggings());
        inv.setItem(13, target.getInventory().getBoots());
        inv.setItem(14, target.getInventory().getItemInMainHand());
        inv.setItem(15, target.getInventory().getItemInOffHand());
        
        // Health & Food
        ItemStack healthItem = new ItemStack(Material.APPLE);
        ItemMeta hm = healthItem.getItemMeta();
        hm.setDisplayName(ChatColor.RED + "Health & Food");
        hm.setLore(Arrays.asList(
            ChatColor.GRAY + "Health: " + ChatColor.WHITE + Math.round(target.getHealth()) + "/" + Math.round(target.getMaxHealth()),
            ChatColor.GRAY + "Food: " + ChatColor.WHITE + target.getFoodLevel() + "/20",
            ChatColor.GRAY + "Gamemode: " + ChatColor.WHITE + target.getGameMode().name()
        ));
        healthItem.setItemMeta(hm);
        inv.setItem(19, healthItem);
        
        // Location
        ItemStack locItem = new ItemStack(Material.COMPASS);
        ItemMeta lm = locItem.getItemMeta();
        lm.setDisplayName(ChatColor.GREEN + "Location");
        Location loc = target.getLocation();
        lm.setLore(Arrays.asList(
            ChatColor.GRAY + "World: " + ChatColor.WHITE + loc.getWorld().getName(),
            ChatColor.GRAY + "X: " + ChatColor.WHITE + loc.getBlockX(),
            ChatColor.GRAY + "Y: " + ChatColor.WHITE + loc.getBlockY(),
            ChatColor.GRAY + "Z: " + ChatColor.WHITE + loc.getBlockZ()
        ));
        locItem.setItemMeta(lm);
        inv.setItem(20, locItem);
        
        // Playtime
        ItemStack timeItem = new ItemStack(Material.CLOCK);
        ItemMeta tm = timeItem.getItemMeta();
        tm.setDisplayName(ChatColor.AQUA + "Playtime");
        int ticks = target.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE);
        int hours = ticks / (20 * 60 * 60);
        int minutes = (ticks / (20 * 60)) % 60;
        tm.setLore(Arrays.asList(ChatColor.GRAY + "Time Played: " + ChatColor.WHITE + hours + "h " + minutes + "m"));
        timeItem.setItemMeta(tm);
        inv.setItem(21, timeItem);
        
        // Network & IP
        ItemStack infoItem = new ItemStack(Material.PAPER);
        ItemMeta im = infoItem.getItemMeta();
        im.setDisplayName(ChatColor.YELLOW + "Network Info");
        im.setLore(Arrays.asList(
            ChatColor.GRAY + "IP: " + ChatColor.WHITE + target.getAddress().getAddress().getHostAddress(),
            ChatColor.GRAY + "Ping: " + ChatColor.WHITE + target.getPing() + "ms"
        ));
        infoItem.setItemMeta(im);
        inv.setItem(22, infoItem);
        
        // Moderation Info
        ItemStack modItem = new ItemStack(Material.IRON_BARS);
        ItemMeta modm = modItem.getItemMeta();
        modm.setDisplayName(ChatColor.DARK_RED + "Moderation Record");
        int warnings = getConfig().getInt("warnings." + target.getUniqueId(), 0);
        modm.setLore(Arrays.asList(
            ChatColor.GRAY + "Warnings: " + ChatColor.WHITE + warnings,
            ChatColor.GRAY + "Muted: " + ChatColor.WHITE + (mutedPlayers.contains(target.getUniqueId()) ? "Yes" : "No"),
            ChatColor.GRAY + "Frozen: " + ChatColor.WHITE + (frozenPlayers.contains(target.getUniqueId()) ? "Yes" : "No"),
            ChatColor.GRAY + "Jailed: " + ChatColor.WHITE + (jailedPlayers.contains(target.getUniqueId()) ? "Yes" : "No")
        ));
        modItem.setItemMeta(modm);
        inv.setItem(23, modItem);
        
        // Player Status
        ItemStack statusItem = new ItemStack(Material.FEATHER);
        ItemMeta sm = statusItem.getItemMeta();
        sm.setDisplayName(ChatColor.LIGHT_PURPLE + "Player Status");
        sm.setLore(Arrays.asList(
            ChatColor.GRAY + "Flying: " + ChatColor.WHITE + (target.isFlying() ? "Yes" : "No"),
            ChatColor.GRAY + "God Mode: " + ChatColor.WHITE + (godPlayers.contains(target.getUniqueId()) ? "Yes" : "No"),
            ChatColor.GRAY + "Vanished: " + ChatColor.WHITE + (vanishedPlayers.contains(target.getUniqueId()) ? "Yes" : "No"),
            ChatColor.GRAY + "Op: " + ChatColor.WHITE + (target.isOp() ? "Yes" : "No")
        ));
        statusItem.setItemMeta(sm);
        inv.setItem(24, statusItem);
        
        // Economy
        ItemStack econItem = new ItemStack(Material.GOLD_INGOT);
        ItemMeta em = econItem.getItemMeta();
        em.setDisplayName(ChatColor.GOLD + "Economy");
        double balance = 0.0;
        try {
            org.bukkit.plugin.RegisteredServiceProvider<?> rsp = Bukkit.getServer().getServicesManager().getRegistration(Class.forName("net.milkbowl.vault.economy.Economy"));
            if (rsp != null) {
                Object econ = rsp.getProvider();
                java.lang.reflect.Method getBalance = econ.getClass().getMethod("getBalance", org.bukkit.OfflinePlayer.class);
                balance = (Double) getBalance.invoke(econ, target);
            }
        } catch (Exception e) {}
        
        java.text.NumberFormat formatter = java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("id", "ID"));
        em.setLore(Arrays.asList(ChatColor.GRAY + "Balance: " + ChatColor.GREEN + formatter.format(balance)));
        econItem.setItemMeta(em);
        inv.setItem(25, econItem);
        
        p.openInventory(inv);
        return true;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onAsyncPreLogin(org.bukkit.event.player.AsyncPlayerPreLoginEvent event) {
        String ip = event.getAddress().getHostAddress();
        
        List<String> blockedIps = dataConfig.getStringList("blocked_ips");
        if (blockedIps.contains(ip)) {
            event.disallow(org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result.KICK_BANNED, 
                ChatColor.translateAlternateColorCodes('&', "&cConnection Refused!\n\n&fYour IP Address has been permanently blocked from this server."));
            return;
        }

        if (!getConfig().getBoolean("settings.anti-vpn.enabled", true)) return;
        if (ip.equals("127.0.0.1") || ip.startsWith("192.168.") || ip.startsWith("10.")) return;
        
        List<String> cleanIps = dataConfig.getStringList("vpn_cache.clean_ips");
        if (cleanIps.contains(ip)) return;
        
        List<String> proxyIps = dataConfig.getStringList("vpn_cache.proxy_ips");
        String kickMsg = ChatColor.translateAlternateColorCodes('&', getConfig().getString("settings.anti-vpn.kick-message", "&cConnection Refused!\n\n&fSystem detected VPN / Proxy usage."));
        
        if (proxyIps.contains(ip)) {
            event.disallow(org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result.KICK_BANNED, kickMsg);
            return;
        }

        try {
            java.net.URL url = new java.net.URL("http://ip-api.com/json/" + ip + "?fields=proxy,hosting");
            java.net.HttpURLConnection con = (java.net.HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(3000);
            con.setReadTimeout(3000);
            
            java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            con.disconnect();
            
            String json = content.toString();
            if (json.contains("\"proxy\":true") || json.contains("\"hosting\":true")) {
                event.disallow(org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result.KICK_BANNED, kickMsg);
                getLogger().warning("Anti-VPN memblokir login dari: " + event.getName() + " (" + ip + ")");
                Bukkit.getScheduler().runTask(this, () -> {
                    logModeration("ANTI-VPN: Blocked " + event.getName() + " (" + ip + ")");
                    List<String> bads = dataConfig.getStringList("vpn_cache.proxy_ips");
                    if (!bads.contains(ip)) {
                        bads.add(ip);
                        dataConfig.set("vpn_cache.proxy_ips", bads);
                        saveDataConfig();
                    }
                });
            } else {
                Bukkit.getScheduler().runTask(this, () -> {
                    List<String> ips = dataConfig.getStringList("vpn_cache.clean_ips");
                    if (!ips.contains(ip)) {
                        ips.add(ip);
                        dataConfig.set("vpn_cache.clean_ips", ips);
                        saveDataConfig();
                    }
                });
            }
        } catch (Exception e) {
            getLogger().warning("Anti-VPN API Timeout untuk IP: " + ip);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().startsWith("Inspect: ")) {
            event.setCancelled(true);
        }
    }

    // --- OLD COMMAND IMPLEMENTATIONS WITH NEW TROLLS ---

    private boolean handleMute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /rka mute <player>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player is not online.");
            return true;
        }
        UUID uuid = target.getUniqueId();
        if (mutedPlayers.contains(uuid)) {
            mutedPlayers.remove(uuid);
            sender.sendMessage(ChatColor.GREEN + target.getName() + " has been unmuted.");
            target.sendMessage(getMsg("messages.mute.unmuted", "&aYou have been unmuted. You can chat again."));
            logModeration("UNMUTE: " + sender.getName() + " unmuted " + target.getName());
        } else {
            mutedPlayers.add(uuid);
            sender.sendMessage(ChatColor.GREEN + target.getName() + " has been muted.");
            target.sendMessage(getMsg("messages.mute.muted", "&cYou have been muted by an Admin! You cannot send messages."));
            logModeration("MUTE: " + sender.getName() + " muted " + target.getName());
        }
        return true;
    }

    private boolean handleSpy(CommandSender sender) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        UUID uuid = p.getUniqueId();
        if (spyPlayers.contains(uuid)) {
            spyPlayers.remove(uuid);
            p.sendMessage(ChatColor.RED + "Command Spy disabled.");
        } else {
            spyPlayers.add(uuid);
            p.sendMessage(ChatColor.GREEN + "Command Spy enabled! You will see all commands typed by other players.");
        }
        return true;
    }

    private boolean handleGod(CommandSender sender, String[] args) {
        Player target = null;
        if (args.length > 1) target = Bukkit.getPlayerExact(args[1]);
        else if (sender instanceof Player) target = (Player) sender;
        
        if (target != null) {
            UUID uuid = target.getUniqueId();
            if (godPlayers.contains(uuid)) {
                godPlayers.remove(uuid);
                sender.sendMessage(ChatColor.GREEN + "God Mode disabled for " + target.getName());
            } else {
                godPlayers.add(uuid);
                sender.sendMessage(ChatColor.GREEN + "God Mode enabled for " + target.getName());
            }
        }
        return true;
    }

    private boolean handleMaintenance(CommandSender sender) {
        maintenanceMode = !maintenanceMode;
        if (maintenanceMode) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage(getMsg("messages.maintenance.enable-broadcast", "&4&lSERVER MAINTENANCE ENABLED!"));
                p.sendMessage(ChatColor.RED + "All normal players will be kicked in 3 seconds...");
                p.sendTitle(ChatColor.DARK_RED + "MAINTENANCE!", ChatColor.RED + "Server shutting down...", 10, 70, 20);
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
            }
            
            new org.bukkit.scheduler.BukkitRunnable() {
                int count = 3;
                @Override
                public void run() {
                    if (count > 0) {
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            p.sendTitle(ChatColor.DARK_RED + "MAINTENANCE!", ChatColor.RED + "Kicking in " + count + "...", 0, 30, 10);
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                        }
                        count--;
                    } else {
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            if (!p.hasPermission("rumahkita.admin")) {
                                p.kickPlayer(getMsg("messages.maintenance.kick-message", "&cThe server is currently under Maintenance.\n&fPlease try again later."));
                            } else {
                                p.sendTitle(ChatColor.GREEN + "MAINTENANCE ON", ChatColor.YELLOW + "Normal players kicked.", 10, 70, 20);
                            }
                        }
                        this.cancel();
                    }
                }
            }.runTaskTimer(this, 0L, 20L);
            
            sender.sendMessage(ChatColor.GREEN + "Maintenance Mode ON. Normal players cannot login.");
        } else {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendTitle(ChatColor.GREEN + "MAINTENANCE COMPLETE", ChatColor.YELLOW + "Server is now open!", 10, 70, 20);
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            }
            Bukkit.broadcastMessage(getMsg("messages.maintenance.disable-broadcast", "&a&lMAINTENANCE COMPLETE!"));
            sender.sendMessage(ChatColor.GREEN + "Maintenance Mode OFF. All players can login again.");
        }
        return true;
    }

    private boolean handleEc(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        if (args.length < 2) {
            p.sendMessage(ChatColor.RED + "Usage: /rka ec <player>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target != null) {
            p.openInventory(target.getEnderChest());
            p.sendMessage(ChatColor.GREEN + "Opening Enderchest of " + target.getName());
        } else {
            p.sendMessage(ChatColor.RED + "Player is not online.");
        }
        return true;
    }

    private boolean handleTroll(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /rka troll <player> <type>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player is not online.");
            return true;
        }

        String type = args[2].toLowerCase();
        switch (type) {
            case "launch":
                target.setVelocity(new Vector(0, 3, 0));
                sender.sendMessage(ChatColor.GREEN + "Launching " + target.getName());
                break;
            case "fakeop":
                target.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "[Server: Made " + target.getName() + " a server operator]");
                sender.sendMessage(ChatColor.GREEN + "Sending Fake OP message to " + target.getName());
                break;
            case "spin":
                Location loc = target.getLocation();
                loc.setYaw(loc.getYaw() + 180f);
                target.teleport(loc);
                sender.sendMessage(ChatColor.GREEN + "Spinning " + target.getName());
                break;
            case "blind":
                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 200, 1));
                sender.sendMessage(ChatColor.GREEN + "Blinding " + target.getName());
                break;
            case "drop":
                if (target.getInventory().getItemInMainHand().getType() != Material.AIR) {
                    target.getWorld().dropItemNaturally(target.getLocation(), target.getInventory().getItemInMainHand());
                    target.getInventory().setItemInMainHand(null);
                }
                sender.sendMessage(ChatColor.GREEN + "Dropping items of " + target.getName());
                break;
            case "scare":
                target.playSound(target.getLocation(), Sound.ENTITY_CREEPER_PRIMED, 1.0f, 1.0f);
                sender.sendMessage(ChatColor.GREEN + "Jumpscaring " + target.getName());
                break;
            case "fakeban":
                target.kickPlayer(ChatColor.RED + "You are banned from this server.\n" + ChatColor.WHITE + "Reason: " + ChatColor.YELLOW + "Hacking / Cheating" + ChatColor.WHITE + "\n\nAppeal at our discord.");
                sender.sendMessage(ChatColor.GREEN + "Fake Banning " + target.getName());
                break;
            case "cobweb":
                target.getLocation().getBlock().setType(Material.COBWEB);
                sender.sendMessage(ChatColor.GREEN + "Trapping " + target.getName() + " in cobweb.");
                break;
            case "shuffle":
                List<ItemStack> items = new ArrayList<>();
                for (ItemStack item : target.getInventory().getContents()) {
                    items.add(item);
                }
                Collections.shuffle(items);
                target.getInventory().setContents(items.toArray(new ItemStack[0]));
                sender.sendMessage(ChatColor.GREEN + "Shuffled inventory of " + target.getName());
                break;
            case "potato":
                for (int i = 0; i < target.getInventory().getSize(); i++) {
                    if (target.getInventory().getItem(i) == null || target.getInventory().getItem(i).getType() == Material.AIR) {
                        target.getInventory().setItem(i, new ItemStack(Material.POISONOUS_POTATO));
                    }
                }
                sender.sendMessage(ChatColor.GREEN + "Filled empty slots of " + target.getName() + " with potatoes.");
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Unknown troll type.");
        }
        return true;
    }

    private boolean handleStaffChat(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                Player p = (Player) sender;
                UUID uuid = p.getUniqueId();
                if (staffChatToggled.contains(uuid)) {
                    staffChatToggled.remove(uuid);
                    p.sendMessage(ChatColor.RED + "StaffChat disabled.");
                } else {
                    staffChatToggled.add(uuid);
                    p.sendMessage(ChatColor.GREEN + "StaffChat enabled.");
                }
            }
            return true;
        }
        String message = String.join(" ", args);
        sendStaffMessage(sender.getName(), message);
        return true;
    }

    private void sendStaffMessage(String name, String message) {
        String format = ChatColor.DARK_RED + "[" + ChatColor.RED + "StaffChat" + ChatColor.DARK_RED + "] " 
                        + ChatColor.YELLOW + name + ChatColor.WHITE + ": " + ChatColor.AQUA + message;
        Bukkit.getConsoleSender().sendMessage(format);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("rumahkita.admin")) p.sendMessage(format);
        }
    }

    private boolean handleCheckIp(CommandSender sender, String[] args) {
        if (args.length < 2) return false;
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            String offlineIp = dataConfig.getString("player_ips." + args[1]);
            if (offlineIp != null) {
                sender.sendMessage(ChatColor.GREEN + "IP " + args[1] + " (Offline): " + ChatColor.YELLOW + offlineIp);
                return true;
            }
            return true;
        }
        sender.sendMessage(ChatColor.GREEN + "IP " + target.getName() + " (Online): " + ChatColor.YELLOW + target.getAddress().getAddress().getHostAddress());
        return true;
    }

    private boolean handleCheckAlts(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /rka checkalts <player>");
            return true;
        }
        
        String targetName = args[1];
        String targetIp = dataConfig.getString("player_ips." + targetName);
        
        Player targetPlayer = Bukkit.getPlayerExact(targetName);
        if (targetPlayer != null) {
            targetIp = targetPlayer.getAddress().getAddress().getHostAddress();
            targetName = targetPlayer.getName();
        }

        if (targetIp == null) {
            sender.sendMessage(ChatColor.RED + "No IP data found for player: " + targetName);
            return true;
        }

        List<String> alts = new ArrayList<>();
        String ipKey = targetIp.replace(".", "_");
        List<String> accounts = dataConfig.getStringList("ip_players." + ipKey);
        
        for (String name : accounts) {
            if (name.equalsIgnoreCase(targetName)) continue;
            Player p = Bukkit.getPlayerExact(name);
            if (p != null) {
                alts.add(ChatColor.GREEN + name + ChatColor.GRAY + " (Online)");
            } else {
                alts.add(ChatColor.GRAY + name + " (Offline)");
            }
        }

        sender.sendMessage(ChatColor.DARK_GRAY + "--------------------------------------------------");
        String mainAcc = dataConfig.getString("main_accounts." + ipKey);
        if (mainAcc != null && mainAcc.equalsIgnoreCase(targetName)) {
            sender.sendMessage(ChatColor.AQUA + "Alt Accounts for " + ChatColor.YELLOW + ChatColor.BOLD + targetName + ChatColor.AQUA + " (IP: " + targetIp + ") " + ChatColor.GOLD + "[MAIN]");
        } else if (mainAcc != null) {
            sender.sendMessage(ChatColor.AQUA + "Alt Accounts for " + ChatColor.YELLOW + targetName + ChatColor.AQUA + " (IP: " + targetIp + ") | " + ChatColor.GOLD + "Main Account: " + mainAcc);
        } else {
            sender.sendMessage(ChatColor.AQUA + "Alt Accounts for " + ChatColor.YELLOW + targetName + ChatColor.AQUA + " (IP: " + targetIp + ")");
        }
        if (alts.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "No alt accounts found on this IP.");
        } else {
            sender.sendMessage(ChatColor.WHITE + String.join(ChatColor.DARK_GRAY + ", ", alts));
        }
        sender.sendMessage(ChatColor.DARK_GRAY + "--------------------------------------------------");

        return true;
    }

    private boolean handleAllowAlt(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /rka allowalt <main account> <alt account>");
            return true;
        }
        String mainAcc = args[1];
        String newAlt = args[2];
        
        String ip = dataConfig.getString("player_ips." + mainAcc);
        if (ip != null) {
            dataConfig.set("player_ips." + newAlt, ip);
            String ipKey = ip.replace(".", "_");
            List<String> accounts = dataConfig.getStringList("ip_players." + ipKey);
            if (!accounts.contains(newAlt)) {
                accounts.add(newAlt);
                dataConfig.set("ip_players." + ipKey, accounts);
            }
            saveDataConfig();
            sender.sendMessage(ChatColor.GREEN + "Successfully granted alt bypass access!");
            sender.sendMessage(ChatColor.GRAY + "Akun " + ChatColor.YELLOW + newAlt + ChatColor.GRAY + " is now registered to the IP of " + ChatColor.YELLOW + mainAcc + ChatColor.GRAY + ".");
        } else {
            sender.sendMessage(ChatColor.RED + "IP Data not found for account: " + mainAcc);
        }
        return true;
    }

    private boolean handleBlockAlt(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /rka blockalt <main account> <alt account>");
            return true;
        }
        String mainAcc = args[1];
        String altAcc = args[2];
        
        List<String> blocked = dataConfig.getStringList("blocked_alts");
        if (!blocked.contains(altAcc.toLowerCase())) {
            blocked.add(altAcc.toLowerCase());
            dataConfig.set("blocked_alts", blocked);
            saveDataConfig();
        }
        
        sender.sendMessage(ChatColor.GREEN + "Successfully blocked alt!");
        sender.sendMessage(ChatColor.GRAY + "Akun " + ChatColor.RED + altAcc + ChatColor.GRAY + " will now be permanently blocked (Suspected as 3rd alt of " + ChatColor.YELLOW + mainAcc + ChatColor.GRAY + ").");
        
        logModeration("BLOCKALT: " + sender.getName() + " blocked alt " + altAcc + " (Main: " + mainAcc + ")");
        
        Player p = Bukkit.getPlayerExact(altAcc);
        if (p != null) {
            p.kickPlayer(ChatColor.translateAlternateColorCodes('&', "&cGagal Masuk!\n\n&fAkun ini terdeteksi sebagai akun ke-3 atau lebih.\n&7Batas maksimal adalah 2 Akun."));
        }
        
        return true;
    }

    private boolean handleUnblockAlt(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /rka unblockalt <alt account>");
            return true;
        }
        String altAcc = args[1].toLowerCase();
        
        List<String> blocked = dataConfig.getStringList("blocked_alts");
        if (blocked.contains(altAcc)) {
            blocked.remove(altAcc);
            dataConfig.set("blocked_alts", blocked);
            saveDataConfig();
            sender.sendMessage(ChatColor.GREEN + "Successfully unblocked alt!");
            sender.sendMessage(ChatColor.GRAY + "Akun " + ChatColor.YELLOW + altAcc + ChatColor.GRAY + " has been removed from the blocklist and can now join.");
            logModeration("UNBLOCKALT: " + sender.getName() + " unblocked alt " + altAcc);
        } else {
            sender.sendMessage(ChatColor.RED + "Akun " + altAcc + " was not found in the blocklist.");
        }
        
        return true;
    }

    private boolean handleSetMain(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /rka setmainaccount <account>");
            return true;
        }
        String targetName = args[1];
        String ip = dataConfig.getString("player_ips." + targetName);
        if (ip == null) {
            sender.sendMessage(ChatColor.RED + "IP Data not found for account: " + targetName);
            return true;
        }
        
        String ipKey = ip.replace(".", "_");
        dataConfig.set("main_accounts." + ipKey, targetName);
        saveDataConfig();
        
        sender.sendMessage(ChatColor.GREEN + "Successfully set " + ChatColor.YELLOW + targetName + ChatColor.GREEN + " as the Main Account for IP " + ip);
        logModeration("SETMAIN: " + sender.getName() + " set " + targetName + " as main account for " + ip);
        return true;
    }

    private boolean handleBlockIp(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /rka blockip <ip>");
            return true;
        }
        String ip = args[1];
        List<String> blockedIps = dataConfig.getStringList("blocked_ips");
        if (!blockedIps.contains(ip)) {
            blockedIps.add(ip);
            dataConfig.set("blocked_ips", blockedIps);
            saveDataConfig();
            sender.sendMessage(ChatColor.GREEN + "IP " + ip + " successfully blocked.");
            logModeration("BLOCKIP: " + sender.getName() + " blocked IP " + ip);
        } else {
            sender.sendMessage(ChatColor.RED + "The IP is already in the blocklist.");
        }
        return true;
    }

    private boolean handleUnblockIp(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /rka unblockip <ip>");
            return true;
        }
        String ip = args[1];
        List<String> blockedIps = dataConfig.getStringList("blocked_ips");
        if (blockedIps.contains(ip)) {
            blockedIps.remove(ip);
            dataConfig.set("blocked_ips", blockedIps);
            saveDataConfig();
            sender.sendMessage(ChatColor.GREEN + "IP " + ip + " successfully unblocked.");
            logModeration("UNBLOCKIP: " + sender.getName() + " unblocked IP " + ip);
        } else {
            sender.sendMessage(ChatColor.RED + "The IP is not in the blocklist.");
        }
        return true;
    }

    private boolean handleVpn(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /rka vpn <on/off/allow/remove> [ip]");
            return true;
        }
        String action = args[1].toLowerCase();
        
        if (action.equals("on")) {
            getConfig().set("settings.anti-vpn.enabled", true);
            saveConfig();
            sender.sendMessage(ChatColor.GREEN + "Anti-VPN system has been ENABLED!");
            logModeration("VPN TOGGLE: " + sender.getName() + " enabled Anti-VPN");
            return true;
        } else if (action.equals("off")) {
            getConfig().set("settings.anti-vpn.enabled", false);
            saveConfig();
            sender.sendMessage(ChatColor.RED + "Anti-VPN system has been DISABLED!");
            logModeration("VPN TOGGLE: " + sender.getName() + " disabled Anti-VPN");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /rka vpn <allow/remove> <ip>");
            return true;
        }
        
        String ip = args[2];
        List<String> cleanIps = dataConfig.getStringList("vpn_cache.clean_ips");
        
        if (action.equals("allow")) {
            if (!cleanIps.contains(ip)) {
                cleanIps.add(ip);
                dataConfig.set("vpn_cache.clean_ips", cleanIps);
                saveDataConfig();
                sender.sendMessage(ChatColor.GREEN + "IP " + ip + " added to VPN whitelist.");
                logModeration("VPN ALLOW: " + sender.getName() + " whitelisted IP " + ip);
            } else {
                sender.sendMessage(ChatColor.RED + "The IP is already in the VPN whitelist.");
            }
        } else if (action.equals("remove")) {
            if (cleanIps.contains(ip)) {
                cleanIps.remove(ip);
                dataConfig.set("vpn_cache.clean_ips", cleanIps);
                saveDataConfig();
                sender.sendMessage(ChatColor.GREEN + "IP " + ip + " removed from VPN whitelist.");
                logModeration("VPN REMOVE: " + sender.getName() + " removed IP " + ip + " from VPN whitelist");
            } else {
                sender.sendMessage(ChatColor.RED + "The IP is not in the VPN whitelist.");
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Unknown action. Use: on, off, allow, or remove.");
        }
        return true;
    }

    private boolean handleClearChat(CommandSender sender) {
        for (int i = 0; i < 100; i++) Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(ChatColor.AQUA + "Chat cleared by Admin.");
        return true;
    }

    private boolean handleFreeze(CommandSender sender, String[] args) {
        if (args.length < 2) return false;
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) return true;
        UUID uuid = target.getUniqueId();
        if (frozenPlayers.contains(uuid)) {
            frozenPlayers.remove(uuid);
            sender.sendMessage(ChatColor.GREEN + "Unfrozen " + target.getName());
            target.sendMessage(getMsg("messages.freeze.unfrozen", "&aYou have been unfrozen."));
        } else {
            frozenPlayers.add(uuid);
            sender.sendMessage(ChatColor.GREEN + "Frozen " + target.getName());
            target.sendMessage(getMsg("messages.freeze.frozen", "&cYou have been frozen by an Admin!"));
        }
        return true;
    }

    private boolean handleInvsee(CommandSender sender, String[] args) {
        if (!(sender instanceof Player) || args.length < 2) return false;
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target != null) {
            ((Player) sender).openInventory(target.getInventory());
            sender.sendMessage(ChatColor.GREEN + "Opening inventory of " + target.getName());
        }
        return true;
    }

    private boolean handleKick(CommandSender sender, String[] args) {
        if (args.length < 2) return false;
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target != null) {
            String reason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "Kicked by Admin.";
            target.kickPlayer(ChatColor.RED + "Kick: " + reason);
            Bukkit.broadcastMessage(ChatColor.RED + target.getName() + " was kicked for: " + reason);
        }
        return true;
    }

    private boolean handleBroadcast(CommandSender sender, String[] args) {
        if (args.length < 2) return false;
        String message = ChatColor.translateAlternateColorCodes('&', String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(ChatColor.DARK_RED + "[" + ChatColor.RED + "Announcement" + ChatColor.DARK_RED + "] " + ChatColor.WHITE + message);
        Bukkit.broadcastMessage("");
        return true;
    }

    private boolean handleVanish(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player target = (Player) sender;
        UUID uuid = target.getUniqueId();
        if (vanishedPlayers.contains(uuid)) {
            vanishedPlayers.remove(uuid);
            for (Player p : Bukkit.getOnlinePlayers()) p.showPlayer(this, target);
            if (target.hasMetadata("vanished")) target.removeMetadata("vanished", this);
            if (vanishPerms.containsKey(uuid)) {
                target.removeAttachment(vanishPerms.get(uuid));
                vanishPerms.remove(uuid);
            }
            Bukkit.broadcastMessage(ChatColor.YELLOW + target.getName() + " joined the game");
            sender.sendMessage(ChatColor.GREEN + "You are now visible (Unvanished).");
        } else {
            vanishedPlayers.add(uuid);
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.hasPermission("rumahkita.admin")) p.hidePlayer(this, target);
            }
            target.setMetadata("vanished", new org.bukkit.metadata.FixedMetadataValue(this, true));
            org.bukkit.permissions.PermissionAttachment attachment = target.addAttachment(this);
            attachment.setPermission("essentials.afk.auto", false);
            vanishPerms.put(uuid, attachment);
            Bukkit.broadcastMessage(ChatColor.YELLOW + target.getName() + " left the game");
            sender.sendMessage(ChatColor.GREEN + "You are now hidden from normal players (Vanished).");
        }
        return true;
    }

    private boolean handleHeal(CommandSender sender, String[] args) {
        Player target = null;
        if (args.length > 1) target = Bukkit.getPlayerExact(args[1]);
        else if (sender instanceof Player) target = (Player) sender;
        
        if (target != null) {
            target.setHealth(target.getMaxHealth());
            target.setFoodLevel(20);
            target.setFireTicks(0);
            for (PotionEffect effect : target.getActivePotionEffects()) target.removePotionEffect(effect.getType());
            sender.sendMessage(ChatColor.GREEN + target.getName() + " has been healed.");
        }
        return true;
    }

    private boolean handleFly(CommandSender sender, String[] args) {
        Player target = null;
        if (args.length > 1) target = Bukkit.getPlayerExact(args[1]);
        else if (sender instanceof Player) target = (Player) sender;
        
        if (target != null) {
            target.setAllowFlight(!target.getAllowFlight());
            sender.sendMessage(ChatColor.GREEN + "Fly " + target.getName() + " : " + target.getAllowFlight());
        }
        return true;
    }

    private boolean handleSpeed(CommandSender sender, String[] args) {
        if (!(sender instanceof Player) || args.length < 2) return true;
        Player p = (Player) sender;
        try {
            float speed = Float.parseFloat(args[1]) / 10f;
            if (speed < 0.1f || speed > 1f) throw new NumberFormatException();
            if (p.isFlying()) p.setFlySpeed(speed);
            else p.setWalkSpeed(speed);
            p.sendMessage(ChatColor.GREEN + "Speed changed to " + args[1]);
        } catch (Exception e) {
            p.sendMessage(ChatColor.RED + "Invalid number (1-10).");
        }
        return true;
    }

    private boolean handleSmite(CommandSender sender, String[] args) {
        if (args.length < 2) return false;
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target != null) {
            target.getWorld().strikeLightningEffect(target.getLocation());
            target.setHealth(Math.max(0.5, target.getHealth() - 4.0));
            sender.sendMessage(ChatColor.GREEN + "Smiting " + target.getName() + " with lightning!");
        }
        return true;
    }

    // --- EVENT LISTENERS ---

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player p = event.getPlayer();
        
        if (chatLocked && !p.hasPermission("rumahkita.admin")) {
            event.setCancelled(true);
            p.sendMessage(ChatColor.RED + "Global chat is currently locked by an Admin.");
            return;
        }

        if (jailedPlayers.contains(p.getUniqueId())) {
            event.setCancelled(true);
            p.sendMessage(ChatColor.RED + "You cannot chat while in Jail!");
            return;
        }

        if (staffChatToggled.contains(p.getUniqueId())) {
            event.setCancelled(true);
            sendStaffMessage(p.getName(), event.getMessage());
            return;
        }

        if (mutedPlayers.contains(p.getUniqueId())) {
            event.setCancelled(true);
            p.sendMessage(getMsg("messages.mute.cannot-chat", "&cYou cannot send messages because you are muted by an Admin."));
            return;
        }
        
        if (!p.hasPermission("rumahkita.admin")) {
            long now = System.currentTimeMillis();
            long lastTime = lastChatTimes.getOrDefault(p.getUniqueId(), 0L);
            if (now - lastTime < 3000) {
                event.setCancelled(true);
                p.sendMessage(ChatColor.RED + "Please do not spam! Wait 3 seconds.");
                return;
            }
            
            String msg = event.getMessage();
            if (msg.equalsIgnoreCase(lastMessages.get(p.getUniqueId()))) {
                event.setCancelled(true);
                p.sendMessage(ChatColor.RED + "Tolong jangan mengirim pesan yang sama berulang kali!");
                return;
            }
            
            if (msg.length() >= 5) {
                int caps = 0;
                for (char c : msg.toCharArray()) {
                    if (Character.isUpperCase(c)) caps++;
                }
                if ((double) caps / msg.length() > 0.6) {
                    event.setMessage(msg.toLowerCase());
                    p.sendMessage(ChatColor.YELLOW + "Pesanmu diubah menjadi huruf kecil otomatis (Anti-Capslock).");
                }
            }
            
            String lowerMsg = event.getMessage().toLowerCase();
            List<String> badwords = Arrays.asList("anjing", "babi", "kontol", "memek", "bangsat", "ngentot", "tolol", "goblok");
            for (String bw : badwords) {
                if (lowerMsg.contains(bw)) {
                    StringBuilder censored = new StringBuilder();
                    for (int i = 0; i < bw.length(); i++) censored.append("*");
                    event.setMessage(event.getMessage().replaceAll("(?i)" + bw, censored.toString()));
                    p.sendMessage(ChatColor.RED + "Tolong jaga ucapanmu di server!");
                }
            }
            
            lastChatTimes.put(p.getUniqueId(), now);
            lastMessages.put(p.getUniqueId(), msg);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player p = event.getPlayer();
        String msgText = event.getMessage().toLowerCase();
        
        if (jailedPlayers.contains(p.getUniqueId()) && !msgText.startsWith("/login") && !msgText.startsWith("/l ") && !msgText.startsWith("/register") && !msgText.startsWith("/reg")) {
            event.setCancelled(true);
            p.sendMessage(ChatColor.RED + "You cannot use commands while in Jail!");
            return;
        }

        if (!p.hasPermission("rumahkita.admin") && !spyPlayers.isEmpty()) {
            if (msgText.startsWith("/login") || msgText.startsWith("/l ") || msgText.startsWith("/reg") || msgText.startsWith("/changepassword") || msgText.startsWith("/cpw")) {
                return; 
            }
            String msg = ChatColor.GRAY + "[SPY] " + ChatColor.AQUA + p.getName() + ": " + ChatColor.WHITE + event.getMessage();
            for (UUID uuid : spyPlayers) {
                Player spy = Bukkit.getPlayer(uuid);
                if (spy != null && spy.isOnline()) {
                    spy.sendMessage(msg);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (maintenanceMode && !event.getPlayer().hasPermission("rumahkita.admin")) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, getMsg("messages.maintenance.kick-message", "&cThe server is currently under Maintenance.\n&fPlease try again later."));
            return;
        }

        if (event.getPlayer().hasPermission("rumahkita.admin")) return;

        String playerName = event.getPlayer().getName();
        
        List<String> blockedAlts = dataConfig.getStringList("blocked_alts");
        if (blockedAlts.contains(playerName.toLowerCase())) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, ChatColor.translateAlternateColorCodes('&', "&cConnection Refused!\n\n&fThis account is detected as a 3rd alt or more.\n&7Maximum limit is 2 accounts per player."));
            return;
        }

        String ip = event.getAddress().getHostAddress();
        
        String ipKey = ip.replace(".", "_");
        List<String> accounts = dataConfig.getStringList("ip_players." + ipKey);
        
        boolean isExisting = accounts.contains(playerName);
        int accountCount = accounts.size();

        if (!isExisting && accountCount >= 2) {
            String mainAcc = dataConfig.getString("main_accounts." + ipKey);
            String extraMsg = mainAcc != null ? "\n&7(Suspected as alt of &e" + mainAcc + "&7)" : "";
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, ChatColor.translateAlternateColorCodes('&', "&cConnection Refused!\n\n&fYou have reached the maximum limit of &e2 Accounts &fper IP.\n&7Please use your previous main account." + extraMsg));
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player p = (Player) event.getEntity();
            if (godPlayers.contains(p.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();

        String ip = p.getAddress().getAddress().getHostAddress();
        dataConfig.set("player_ips." + p.getName(), ip);
        
        String ipKey = ip.replace(".", "_");
        List<String> accounts = dataConfig.getStringList("ip_players." + ipKey);
        if (!accounts.contains(p.getName())) {
            accounts.add(p.getName());
            dataConfig.set("ip_players." + ipKey, accounts);
        }
        saveDataConfig();

        if (accounts.size() > 1) {
            java.util.List<String> alts = new java.util.ArrayList<>(accounts);
            alts.remove(p.getName());
            if (!alts.isEmpty()) {
                String alertMsg = ChatColor.GRAY + "[" + ChatColor.RED + "Keamanan" + ChatColor.GRAY + "] " 
                                + ChatColor.YELLOW + p.getName() + ChatColor.WHITE + " masuk. "
                                + ChatColor.GRAY + "Terkait alt: " + ChatColor.RED + String.join(", ", alts);
                for (Player admin : Bukkit.getOnlinePlayers()) {
                    if (admin.hasPermission("rumahkita.admin")) {
                        admin.sendMessage(alertMsg);
                    }
                }
            }
        }

        if (jailedPlayers.contains(p.getUniqueId()) && jailLocation != null) {
            p.teleport(jailLocation);
            p.sendMessage(ChatColor.RED + "You are still in Jail!");
        }

        if (vanishedPlayers.contains(p.getUniqueId())) {
            event.setJoinMessage(null);
            p.setMetadata("vanished", new org.bukkit.metadata.FixedMetadataValue(this, true));
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.hasPermission("rumahkita.admin")) {
                    online.hidePlayer(this, p);
                }
            }
            org.bukkit.permissions.PermissionAttachment attachment = p.addAttachment(this);
            attachment.setPermission("essentials.afk.auto", false);
            vanishPerms.put(p.getUniqueId(), attachment);
            p.sendMessage(ChatColor.YELLOW + "[!] " + ChatColor.GREEN + "You logged in while in VANISH mode.");
        }

        if (!p.hasPermission("rumahkita.admin")) {
            for (UUID uuid : vanishedPlayers) {
                Player vanished = Bukkit.getPlayer(uuid);
                if (vanished != null) p.hidePlayer(this, vanished);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        UUID uuid = p.getUniqueId();
        staffChatToggled.remove(uuid);
        spyPlayers.remove(uuid);
        godPlayers.remove(uuid);
        
        if (spectateLocations.containsKey(uuid)) {
            p.teleport(spectateLocations.get(uuid));
            p.setGameMode(spectateGameModes.get(uuid));
            spectateLocations.remove(uuid);
            spectateGameModes.remove(uuid);
        }

        if (vanishedPlayers.contains(uuid)) {
            event.setQuitMessage(null); 
            if (vanishPerms.containsKey(uuid)) {
                p.removeAttachment(vanishPerms.get(uuid));
                vanishPerms.remove(uuid);
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (frozenPlayers.contains(uuid)) {
            if (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getZ() != event.getTo().getZ()) {
                event.setTo(event.getFrom());
            }
            return;
        }
        
        if (jailedPlayers.contains(uuid) && jailLocation != null) {
            int limit = getConfig().getInt("settings.jail.distance-limit", 10);
            if (event.getTo().distance(jailLocation) > limit) {
                event.setTo(jailLocation);
                event.getPlayer().sendMessage(getMsg("messages.jail.cannot-escape", "&cYou cannot escape Jail!"));
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (frozenPlayers.contains(uuid) || jailedPlayers.contains(uuid)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You cannot drop items right now!");
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (frozenPlayers.contains(uuid) || jailedPlayers.contains(uuid)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (frozenPlayers.contains(uuid) || jailedPlayers.contains(uuid)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (frozenPlayers.contains(uuid) || jailedPlayers.contains(uuid)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player p = (Player) event.getDamager();
            UUID uuid = p.getUniqueId();
            if (frozenPlayers.contains(uuid) || jailedPlayers.contains(uuid)) {
                event.setCancelled(true);
                p.sendMessage(ChatColor.RED + "You cannot attack right now!");
            }
        }
    }

    // --- RESTART SYSTEM IMPLEMENTATION ---

    private void loadRestartConfig() {
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
                getLogger().warning("Invalid time format in config: " + t);
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
                if (sec > 0 && !warningSeconds.contains(sec)) warningSeconds.add(sec);
            } catch (Exception e) {}
        }
    }

    public void logModeration(String action) {
        try {
            java.io.File dataFolder = getDataFolder();
            if (!dataFolder.exists()) dataFolder.mkdirs();
            java.io.File logFile = new java.io.File(dataFolder, "moderation.log");
            if (!logFile.exists()) logFile.createNewFile();
            
            java.io.FileWriter fw = new java.io.FileWriter(logFile, true);
            java.io.PrintWriter pw = new java.io.PrintWriter(fw);
            
            String timeStamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
            pw.println("[" + timeStamp + "] " + action);
            
            pw.flush();
            pw.close();
            fw.close();
        } catch (java.io.IOException e) {
            getLogger().warning("Failed to write to moderation.log: " + e.getMessage());
        }
    }

    private void tickRestart() {
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
                if (diffSec < 0) diffSec += 24 * 3600;
                
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
        String kickMsg = ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.kick-message", "&cServer is restarting! Please rejoin in a few minutes."));
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.kickPlayer(kickMsg);
        }
        Bukkit.shutdown();
    }

    private void broadcastWarning(int seconds) {
        String timeStr = formatTimeLength(seconds);
        String msg = ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.warning-message", "&8[&c&l!&8] &c&lRESTART &8\u00bb &eServer is restarting in &c&l%time%&e!")).replace("%time%", timeStr);
        
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(msg);
            
            if (seconds <= 10) {
                p.sendTitle(ChatColor.translateAlternateColorCodes('&', "&c&l\u26a0 &c" + seconds + " &c&l\u26a0"), ChatColor.YELLOW + "Preparing for Server Restart", 5, 20, 5);
                p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
            } else if (seconds == 60) {
                p.sendTitle(ChatColor.translateAlternateColorCodes('&', "&c&l1 MINUTE"), ChatColor.YELLOW + "Until Server Restart", 10, 40, 10);
                p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.0f);
            } else if (seconds == 300) {
                p.sendTitle(ChatColor.translateAlternateColorCodes('&', "&6&l5 MINUTES"), ChatColor.YELLOW + "Until Server Restart", 10, 40, 10);
                p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            } else {
                p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            }
        }
    }

    private boolean handleRestart(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "=== AutoRestart Commands ===");
            sender.sendMessage(ChatColor.GREEN + "/rka restart time " + ChatColor.GRAY + "- Check next restart time");
            sender.sendMessage(ChatColor.GREEN + "/rka restart now [time] " + ChatColor.GRAY + "- Force restart (e.g. 1m)");
            sender.sendMessage(ChatColor.GREEN + "/rka restart cancel " + ChatColor.GRAY + "- Cancel pending restart");
            sender.sendMessage(ChatColor.GREEN + "/rka restart reload " + ChatColor.GRAY + "- Reload restart config");
            return true;
        }

        String sub = args[1].toLowerCase();
        if (sub.equals("reload")) {
            reloadConfig();
            loadRestartConfig();
            sender.sendMessage(ChatColor.GREEN + "AutoRestart config reloaded.");
        } else if (sub.equals("time")) {
            if (restartPending) {
                sender.sendMessage(ChatColor.GREEN + "Server will restart in " + formatTimeLength(secondsUntilRestart));
            } else {
                LocalTime now = LocalTime.now(java.time.ZoneId.of("Asia/Jakarta"));
                int minDiff = Integer.MAX_VALUE;
                LocalTime nextTime = null;
                for (LocalTime rt : restartTimes) {
                    int diffSec = rt.toSecondOfDay() - now.toSecondOfDay();
                    if (diffSec <= 0) diffSec += 24 * 3600;
                    if (diffSec < minDiff) {
                        minDiff = diffSec;
                        nextTime = rt;
                    }
                }
                if (nextTime != null) {
                    sender.sendMessage(ChatColor.GREEN + "Next scheduled restart at " + nextTime.toString() + " (" + formatTimeLength(minDiff) + " remaining)");
                } else {
                    sender.sendMessage(ChatColor.RED + "No restart times set in config.");
                }
            }
        } else if (sub.equals("now")) {
            if (args.length > 2) {
                int seconds = parseTime(args[2]);
                if (seconds > 0) {
                    startRestartCountdown(seconds);
                    sender.sendMessage(ChatColor.GREEN + "Server will restart in " + args[2]);
                } else {
                    sender.sendMessage(ChatColor.RED + "Invalid time format. Use m, h, or s (e.g., 1m, 2h, 30s)");
                }
            } else {
                executeRestart();
            }
        } else if (sub.equals("cancel")) {
            if (restartPending) {
                restartPending = false;
                secondsUntilRestart = -1;
                Bukkit.broadcastMessage(ChatColor.GREEN + "Server restart has been CANCELLED!");
            } else {
                sender.sendMessage(ChatColor.RED + "No restart is currently pending.");
            }
        }

        return true;
    }

    private int parseTime(String timeStr) {
        timeStr = timeStr.toLowerCase().trim();
        try {
            if (timeStr.endsWith("h")) return Integer.parseInt(timeStr.replace("h", "")) * 3600;
            if (timeStr.endsWith("m")) return Integer.parseInt(timeStr.replace("m", "")) * 60;
            if (timeStr.endsWith("s")) return Integer.parseInt(timeStr.replace("s", ""));
            return Integer.parseInt(timeStr);
        } catch (Exception e) {
            return -1;
        }
    }

    private String formatTimeLength(int totalSeconds) {
        if (totalSeconds < 60) return totalSeconds + " seconds";
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        
        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append(" hour(s) ");
        if (minutes > 0) sb.append(minutes).append(" min ");
        if (seconds > 0) sb.append(seconds).append(" sec");
        return sb.toString().trim();
    }
}
