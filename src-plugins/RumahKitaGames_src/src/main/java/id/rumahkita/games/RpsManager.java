package id.rumahkita.games;

import id.rumahkita.economy.RumahKitaEconomyRupiahPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.ArrayList;
import java.util.List;

public class RpsManager implements Listener {
    private RumahKitaGamesPlugin plugin;
    private Map<UUID, RpsGame> activeGames = new HashMap<>();
    private Map<UUID, Integer> winStreaks = new HashMap<>();

    public RpsManager(RumahKitaGamesPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isValidChoice(String choice) {
        choice = choice.toLowerCase();
        return choice.equals("batu") || choice.equals("gunting") || choice.equals("kertas");
    }

    public void createGame(Player p, long amount, String choice) {
        if (activeGames.containsKey(p.getUniqueId())) {
            p.sendMessage(ChatColor.RED + "You already have an active RPS game. Cancel first with /rps cancel");
            return;
        }
        
        RumahKitaEconomyRupiahPlugin economy = RumahKitaEconomyRupiahPlugin.getInstance();
        if (economy.getBalance(p.getUniqueId()) < amount) {
            p.sendMessage(ChatColor.RED + "You don't have enough money to bet that much!");
            return;
        }

        economy.takeBalance(p.getUniqueId(), amount);
        RpsGame game = new RpsGame(p.getUniqueId(), p.getName(), amount, choice.toUpperCase(), null);
        activeGames.put(p.getUniqueId(), game);

        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', 
            "&e[RPS] &f" + p.getName() + " &acreated a bet of &eRp" + amount + " &ain Rock-Paper-Scissors! Type &e/rps join " + p.getName() + " <rock/paper/scissors> &aor &e/rps list &ato play!"));
    }

    public void openGameList(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.DARK_GRAY + "RPS Games");
        
        for (RpsGame game : activeGames.values()) {
            if (game.targetPlayer != null) continue;
            ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(game.creator));
                meta.setDisplayName(ChatColor.YELLOW + game.playerName + "'s RPS");
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Bet: " + ChatColor.GREEN + "Rp" + game.amount);
                lore.add(ChatColor.GRAY + "Choice: " + ChatColor.MAGIC + "SECRET");
                lore.add("");
                lore.add(ChatColor.YELLOW + "Click to play!");
                meta.setLore(lore);
                head.setItemMeta(meta);
            }
            inv.addItem(head);
        }
        
        p.openInventory(inv);
    }
    
    public void openChoiceGUI(Player p, Player target) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.DARK_GRAY + "Choose RPS against " + target.getName());
        
        ItemStack batu = new ItemStack(Material.COBBLESTONE);
        ItemMeta mBatu = batu.getItemMeta();
        mBatu.setDisplayName(ChatColor.GRAY + ChatColor.BOLD.toString() + "ROCK");
        batu.setItemMeta(mBatu);
        
        ItemStack gunting = new ItemStack(Material.SHEARS);
        ItemMeta mGunting = gunting.getItemMeta();
        mGunting.setDisplayName(ChatColor.WHITE + ChatColor.BOLD.toString() + "SCISSORS");
        gunting.setItemMeta(mGunting);
        
        ItemStack kertas = new ItemStack(Material.PAPER);
        ItemMeta mKertas = kertas.getItemMeta();
        mKertas.setDisplayName(ChatColor.WHITE + ChatColor.BOLD.toString() + "PAPER");
        kertas.setItemMeta(mKertas);
        
        inv.setItem(11, batu);
        inv.setItem(13, gunting);
        inv.setItem(15, kertas);
        
        p.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getView().getTitle().equals(ChatColor.DARK_GRAY + "RPS Games")) {
            e.setCancelled(true);
            if (e.getCurrentItem() != null && e.getCurrentItem().getType() == Material.PLAYER_HEAD) {
                Player p = (Player) e.getWhoClicked();
                SkullMeta meta = (SkullMeta) e.getCurrentItem().getItemMeta();
                if (meta != null && meta.getOwningPlayer() != null) {
                    Player target = meta.getOwningPlayer().getPlayer();
                    if (target != null && target.isOnline()) {
                        p.closeInventory();
                        openChoiceGUI(p, target);
                    } else {
                        p.sendMessage(ChatColor.RED + "That player is no longer online.");
                        p.closeInventory();
                    }
                }
            }
        } else if (e.getView().getTitle().startsWith(ChatColor.DARK_GRAY + "Choose RPS against ")) {
            e.setCancelled(true);
            Player p = (Player) e.getWhoClicked();
            String targetName = e.getView().getTitle().replace(ChatColor.DARK_GRAY + "Choose RPS against ", "");
            Player target = Bukkit.getPlayerExact(targetName);
            
            if (target == null) {
                p.sendMessage(ChatColor.RED + "That player is no longer online.");
                p.closeInventory();
                return;
            }
            
            if (e.getCurrentItem() != null) {
                String choice = "";
                if (e.getCurrentItem().getType() == Material.COBBLESTONE) choice = "batu";
                else if (e.getCurrentItem().getType() == Material.SHEARS) choice = "gunting";
                else if (e.getCurrentItem().getType() == Material.PAPER) choice = "kertas";
                
                if (!choice.isEmpty()) {
                    p.closeInventory();
                    joinGame(p, target, choice);
                }
            }
        }
    }

    public void cancelGame(Player p) {
        if (!activeGames.containsKey(p.getUniqueId())) {
            p.sendMessage(ChatColor.RED + "You don't have an active game.");
            return;
        }

        RpsGame game = activeGames.remove(p.getUniqueId());
        RumahKitaEconomyRupiahPlugin.getInstance().addBalance(p.getUniqueId(), game.amount);
        p.sendMessage(ChatColor.GREEN + "RPS game cancelled. Your Rp" + game.amount + " has been refunded.");
    }

    public void joinGame(Player p, Player target, String choice) {
        if (!activeGames.containsKey(target.getUniqueId())) {
            p.sendMessage(ChatColor.RED + "That player has no active RPS game.");
            return;
        }

        if (p.getUniqueId().equals(target.getUniqueId())) {
            p.sendMessage(ChatColor.RED + "You cannot play against yourself.");
            return;
        }

        RpsGame game = activeGames.get(target.getUniqueId());
        if (game.targetPlayer != null && !game.targetPlayer.equals(p.getUniqueId())) {
            p.sendMessage(ChatColor.RED + "This game is a private invite. You cannot join.");
            return;
        }
        
        choice = choice.toUpperCase();

        RumahKitaEconomyRupiahPlugin economy = RumahKitaEconomyRupiahPlugin.getInstance();
        if (economy.getBalance(p.getUniqueId()) < game.amount) {
            p.sendMessage(ChatColor.RED + "Not enough money! You need Rp" + game.amount);
            return;
        }

        economy.takeBalance(p.getUniqueId(), game.amount);
        activeGames.remove(target.getUniqueId());

        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', 
            "&e[RPS] &f" + p.getName() + " &aaccepted &f" + target.getName() + "'s challenge &a(Bet: &eRp" + game.amount + "&a). 1... 2... 3..."));

        String creatorChoice = game.creatorChoice;
        String joinerChoice = choice;
        
        long totalPot = game.amount * 2;
        double taxPercentage = plugin.getConfig().getDouble("rps.tax_percentage", 5.0);
        long winAmount = (long) (totalPot * (1.0 - (taxPercentage / 100.0)));
        
        final String fJoinerChoice = joinerChoice;

        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks < 3) {
                    String titleText = "";
                    if (ticks == 0) titleText = "&7&lROCK...";
                    else if (ticks == 1) titleText = "&f&lSCISSORS...";
                    else if (ticks == 2) titleText = "&f&lPAPER...";
                    
                    String title = ChatColor.translateAlternateColorCodes('&', titleText);
                    String subtitle = ChatColor.translateAlternateColorCodes('&', "&e" + (3 - ticks));
                    float pitch = 1.0f + (ticks * 0.2f);
                    if (p.isOnline()) {
                        p.sendTitle(title, subtitle, 0, 20, 0);
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, pitch);
                    }
                    if (target.isOnline()) {
                        target.sendTitle(title, subtitle, 0, 20, 0);
                        target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, pitch);
                    }
                    ticks++;
                } else {
                    int result = calculateWinner(creatorChoice, fJoinerChoice); 
                    
                    if (result == 0) {
                        economy.addBalance(target.getUniqueId(), game.amount);
                        economy.addBalance(p.getUniqueId(), game.amount);
                        
                        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', 
                            "&e[RPS] &aTie! &f" + target.getName() + " &a(" + creatorChoice + ") vs &f" + p.getName() + " &a(" + fJoinerChoice + ")"));
                        
                        sendResult(target, p, "TIE!", "You both chose " + creatorChoice);
                    } else if (result == 1) {
                        economy.addBalance(target.getUniqueId(), winAmount);
                        
                        int currentStreak = winStreaks.getOrDefault(target.getUniqueId(), 0) + 1;
                        int loserStreak = winStreaks.getOrDefault(p.getUniqueId(), 0);
                        winStreaks.put(target.getUniqueId(), currentStreak);
                        winStreaks.put(p.getUniqueId(), 0);

                        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', 
                            "&e[RPS] &f" + target.getName() + " &abeat &f" + p.getName() + " &a(&eRp" + winAmount + "&a) (-" + taxPercentage + "% tax)"));
                            
                        if (currentStreak >= 3) {
                            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', 
                                "&e[RPS] &c&l🔥 WOW! &f" + target.getName() + " &eis on a &c&l" + currentStreak + "x &eWin Streak! 🔥"));
                        }
                        if (loserStreak >= 3) {
                            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', 
                                "&e[RPS] &4&l☠ RIP! &f" + p.getName() + "'s &c&l" + loserStreak + "x &fWin Streak was ended by &e" + target.getName() + "&f! ☠"));
                        }
                        
                        sendWinLoss(target, p, creatorChoice, fJoinerChoice, winAmount);
                    } else {
                        economy.addBalance(p.getUniqueId(), winAmount);
                        
                        int currentStreak = winStreaks.getOrDefault(p.getUniqueId(), 0) + 1;
                        int loserStreak = winStreaks.getOrDefault(target.getUniqueId(), 0);
                        winStreaks.put(p.getUniqueId(), currentStreak);
                        winStreaks.put(target.getUniqueId(), 0);

                        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', 
                            "&e[RPS] &f" + p.getName() + " &abeat &f" + target.getName() + " &a(&eRp" + winAmount + "&a) (-" + taxPercentage + "% tax)"));
                            
                        if (currentStreak >= 3) {
                            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', 
                                "&e[RPS] &c&l🔥 WOW! &f" + p.getName() + " &eis on a &c&l" + currentStreak + "x &eWin Streak! 🔥"));
                        }
                        if (loserStreak >= 3) {
                            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', 
                                "&e[RPS] &4&l☠ RIP! &f" + target.getName() + "'s &c&l" + loserStreak + "x &fWin Streak was ended by &e" + p.getName() + "&f! ☠"));
                        }
                        
                        sendWinLoss(p, target, fJoinerChoice, creatorChoice, winAmount);
                    }
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
    
    private void sendResult(Player p1, Player p2, String title, String sub) {
        if (p1.isOnline()) {
            p1.sendTitle(ChatColor.YELLOW + ChatColor.BOLD.toString() + title, ChatColor.GRAY + sub, 10, 60, 10);
            p1.playSound(p1.getLocation(), Sound.ENTITY_VILLAGER_TRADE, 1.0f, 1.0f);
        }
        if (p2.isOnline()) {
            p2.sendTitle(ChatColor.YELLOW + ChatColor.BOLD.toString() + title, ChatColor.GRAY + sub, 10, 60, 10);
            p2.playSound(p2.getLocation(), Sound.ENTITY_VILLAGER_TRADE, 1.0f, 1.0f);
        }
    }
    
    private void sendWinLoss(Player winner, Player loser, String winChoice, String loseChoice, long winAmount) {
        if (winner.isOnline()) {
            winner.sendTitle(
                ChatColor.GREEN + ChatColor.BOLD.toString() + "YOU WON!",
                ChatColor.WHITE + "You: " + ChatColor.YELLOW + winChoice + ChatColor.WHITE + " | Opponent: " + ChatColor.RED + loseChoice,
                10, 60, 10
            );
            winner.sendMessage(ChatColor.GREEN + "You received a reward of Rp" + winAmount + "!");
            winner.playSound(winner.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            
            try {
                org.bukkit.entity.Firework fw = winner.getWorld().spawn(winner.getLocation(), org.bukkit.entity.Firework.class);
                org.bukkit.inventory.meta.FireworkMeta fwm = fw.getFireworkMeta();
                fwm.addEffect(org.bukkit.FireworkEffect.builder().withColor(org.bukkit.Color.YELLOW).withColor(org.bukkit.Color.GREEN).with(org.bukkit.FireworkEffect.Type.STAR).withTrail().build());
                fwm.setPower(1);
                fw.setFireworkMeta(fwm);
            } catch (Exception ignored) {}
        }
        if (loser.isOnline()) {
            loser.sendTitle(
                ChatColor.RED + ChatColor.BOLD.toString() + "YOU LOST!",
                ChatColor.WHITE + "You: " + ChatColor.RED + loseChoice + ChatColor.WHITE + " | Opponent: " + ChatColor.YELLOW + winChoice,
                10, 60, 10
            );
            loser.playSound(loser.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.5f);
        }
    }

    private int calculateWinner(String p1Choice, String p2Choice) {
        if (p1Choice.equals(p2Choice)) return 0;
        
        if (p1Choice.equals("BATU") && p2Choice.equals("GUNTING")) return 1;
        if (p1Choice.equals("GUNTING") && p2Choice.equals("KERTAS")) return 1;
        if (p1Choice.equals("KERTAS") && p2Choice.equals("BATU")) return 1;
        
        return 2;
    }

    public void inviteGame(Player p, Player target, long amount, String choice) {
        if (activeGames.containsKey(p.getUniqueId())) {
            p.sendMessage(ChatColor.RED + "You already have an active game/invite. Cancel first with /rps cancel");
            return;
        }
        
        if (p.getUniqueId().equals(target.getUniqueId())) {
            p.sendMessage(ChatColor.RED + "You cannot invite yourself.");
            return;
        }

        RumahKitaEconomyRupiahPlugin economy = RumahKitaEconomyRupiahPlugin.getInstance();
        if (economy.getBalance(p.getUniqueId()) < amount) {
            p.sendMessage(ChatColor.RED + "You don't have enough money to bet that much!");
            return;
        }

        economy.takeBalance(p.getUniqueId(), amount);
        RpsGame game = new RpsGame(p.getUniqueId(), p.getName(), amount, choice.toUpperCase(), target.getUniqueId());
        activeGames.put(p.getUniqueId(), game);

        p.sendMessage(ChatColor.GREEN + "Successfully sent RPS invite for Rp" + amount + " to " + target.getName() + ". You chose " + choice.toUpperCase());
        target.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            "&e[Suwit] &f" + p.getName() + " &achallenged you to Rock-Paper-Scissors for &eRp" + amount + "&a!"));
        target.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            "&aType &e/rps accept " + p.getName() + " <batu/gunting/kertas> &ato accept or &c/rps deny " + p.getName() + " &ato deny."));
    }

    public void denyGame(Player p, Player target) {
        RpsGame game = activeGames.get(target.getUniqueId());
        if (game == null || game.targetPlayer == null || !game.targetPlayer.equals(p.getUniqueId())) {
            p.sendMessage(ChatColor.RED + "That player did not invite you.");
            return;
        }

        activeGames.remove(target.getUniqueId());
        RumahKitaEconomyRupiahPlugin.getInstance().addBalance(target.getUniqueId(), game.amount);
        
        p.sendMessage(ChatColor.GREEN + "You denied RPS invite from " + target.getName() + ".");
        if (target.isOnline()) {
            target.sendMessage(ChatColor.RED + p.getName() + " denied your RPS invite. Money refunded.");
        }
    }

    private static class RpsGame {
        public UUID creator;
        public String playerName;
        public long amount;
        public String creatorChoice;
        public UUID targetPlayer;

        public RpsGame(UUID creator, String playerName, long amount, String creatorChoice, UUID targetPlayer) {
            this.creator = creator;
            this.playerName = playerName;
            this.amount = amount;
            this.creatorChoice = creatorChoice;
            this.targetPlayer = targetPlayer;
        }
    }
}
