package id.rumahkita.games;

import id.rumahkita.economy.RumahKitaEconomyRupiahPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Random;
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

public class CoinflipManager implements Listener {

    private RumahKitaGamesPlugin plugin;
    private Map<UUID, CoinflipGame> activeGames = new HashMap<>();
    private Map<UUID, Integer> winStreaks = new HashMap<>();
    private Random random = new Random();

    public CoinflipManager(RumahKitaGamesPlugin plugin) {
        this.plugin = plugin;
    }

    public void createGame(Player p, long amount, String side) {
        if (activeGames.containsKey(p.getUniqueId())) {
            p.sendMessage(ChatColor.RED + "You already have an active coinflip game. Cancel first with /cf cancel");
            return;
        }
        
        RumahKitaEconomyRupiahPlugin economy = RumahKitaEconomyRupiahPlugin.getInstance();
        if (economy.getBalance(p.getUniqueId()) < amount) {
            p.sendMessage(ChatColor.RED + "You don't have enough money to bet that much!");
            return;
        }

        economy.takeBalance(p.getUniqueId(), amount);

        CoinflipGame game = new CoinflipGame(p.getUniqueId(), p.getName(), amount, side, null);
        activeGames.put(p.getUniqueId(), game);

        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', 
            "&e[Coinflip] &f" + p.getName() + " &acreated a bet of &eRp" + amount + " &afor &b" + side.toUpperCase() + "&a! Type &e/cf join " + p.getName() + " &aor &e/cf list &ato play!"));
    }

    public void openGameList(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.DARK_GRAY + "Coinflip Games");
        
        for (CoinflipGame game : activeGames.values()) {
            if (game.targetPlayer != null) continue;
            ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(game.creator));
                meta.setDisplayName(ChatColor.YELLOW + game.playerName + "'s Coinflip");
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Bet: " + ChatColor.GREEN + "Rp" + game.amount);
                lore.add(ChatColor.GRAY + "Side: " + ChatColor.AQUA + game.side.toUpperCase());
                lore.add("");
                lore.add(ChatColor.YELLOW + "Click to play!");
                meta.setLore(lore);
                head.setItemMeta(meta);
            }
            inv.addItem(head);
        }
        
        p.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getView().getTitle().equals(ChatColor.DARK_GRAY + "Coinflip Games")) {
            e.setCancelled(true);
            if (e.getCurrentItem() != null && e.getCurrentItem().getType() == Material.PLAYER_HEAD) {
                Player p = (Player) e.getWhoClicked();
                SkullMeta meta = (SkullMeta) e.getCurrentItem().getItemMeta();
                if (meta != null && meta.getOwningPlayer() != null) {
                    Player target = meta.getOwningPlayer().getPlayer();
                    if (target != null && target.isOnline()) {
                        p.closeInventory();
                        joinGame(p, target);
                    } else {
                        p.sendMessage(ChatColor.RED + "That player is no longer online.");
                        p.closeInventory();
                    }
                }
            }
        }
    }

    public void cancelGame(Player p) {
        if (!activeGames.containsKey(p.getUniqueId())) {
            p.sendMessage(ChatColor.RED + "You don't have an active game.");
            return;
        }

        CoinflipGame game = activeGames.remove(p.getUniqueId());
        RumahKitaEconomyRupiahPlugin.getInstance().addBalance(p.getUniqueId(), game.amount);
        p.sendMessage(ChatColor.GREEN + "Coinflip game cancelled. Your Rp" + game.amount + " has been refunded.");
    }

    public void joinGame(Player p, Player target) {
        if (!activeGames.containsKey(target.getUniqueId())) {
            p.sendMessage(ChatColor.RED + "That player has no active coinflip game.");
            return;
        }

        if (p.getUniqueId().equals(target.getUniqueId())) {
            p.sendMessage(ChatColor.RED + "You cannot play against yourself.");
            return;
        }

        CoinflipGame game = activeGames.get(target.getUniqueId());
        if (game.targetPlayer != null && !game.targetPlayer.equals(p.getUniqueId())) {
            p.sendMessage(ChatColor.RED + "This game is a private invite. You cannot join.");
            return;
        }

        RumahKitaEconomyRupiahPlugin economy = RumahKitaEconomyRupiahPlugin.getInstance();
        
        if (economy.getBalance(p.getUniqueId()) < game.amount) {
            p.sendMessage(ChatColor.RED + "Not enough money! You need Rp" + game.amount);
            return;
        }

        economy.takeBalance(p.getUniqueId(), game.amount);

        activeGames.remove(target.getUniqueId());

        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', 
            "&e[Coinflip] &f" + p.getName() + " &aaccepted &f" + target.getName() + "'s challenge &a(Bet: &eRp" + game.amount + "&a). Flipping coin..."));

        String winningSide = random.nextBoolean() ? "HEADS" : "TAILS";
        Player winner = game.side.equalsIgnoreCase(winningSide) ? target : p;
        Player loser = game.side.equalsIgnoreCase(winningSide) ? p : target;
        
        double taxPercentage = plugin.getConfig().getDouble("coinflip.tax_percentage", 5.0);
        long winAmount = (long) (game.amount * 2 * (1.0 - (taxPercentage / 100.0)));
        
        new BukkitRunnable() {
            int ticks = 0;
            int maxTicks = 20;
            
            @Override
            public void run() {
                if (ticks < maxTicks) {
                    String currentSide = (ticks % 2 == 0) ? "HEADS" : "TAILS";
                    ChatColor color = (ticks % 2 == 0) ? ChatColor.AQUA : ChatColor.YELLOW;
                    
                    String title = ChatColor.translateAlternateColorCodes('&', "&e&lFlipping Coin...");
                    String subtitle = ChatColor.translateAlternateColorCodes('&', color + "&l" + currentSide);
                    
                    float pitch = (ticks % 2 == 0) ? 1.5f : 2.0f;
                    if (p.isOnline()) {
                        p.sendTitle(title, subtitle, 0, 10, 0);
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, pitch);
                    }
                    if (target.isOnline()) {
                        target.sendTitle(title, subtitle, 0, 10, 0);
                        target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, pitch);
                    }
                    ticks++;
                } else {
                    economy.addBalance(winner.getUniqueId(), winAmount);
                    
                    int currentStreak = winStreaks.getOrDefault(winner.getUniqueId(), 0) + 1;
                    int loserStreak = winStreaks.getOrDefault(loser.getUniqueId(), 0);
                    
                    winStreaks.put(winner.getUniqueId(), currentStreak);
                    winStreaks.put(loser.getUniqueId(), 0);

                    Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', 
                        "&e[Coinflip] &aThe coin landed on &b" + winningSide + "&a! &f" + winner.getName() + " &awon &eRp" + winAmount + " &a(-" + taxPercentage + "% tax)"));
                    
                    if (currentStreak >= 3) {
                        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', 
                            "&e[Coinflip] &c&l🔥 WOW! &f" + winner.getName() + " &eis on a &c&l" + currentStreak + "x &eWin Streak! 🔥"));
                    }
                    
                    if (loserStreak >= 3) {
                        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', 
                            "&e[Coinflip] &4&l☠ RIP! &f" + loser.getName() + "'s &c&l" + loserStreak + "x &fWin Streak was ended by &e" + winner.getName() + "&f! ☠"));
                    }
                    
                    if (winner.isOnline()) {
                        winner.sendTitle(
                            ChatColor.translateAlternateColorCodes('&', "&a&lYOU WON!"),
                            ChatColor.translateAlternateColorCodes('&', "&e+" + winAmount + " Rupiah"),
                            10, 60, 10
                        );
                        winner.playSound(winner.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                        winner.sendMessage(ChatColor.GREEN + "Congratulations! You won the Coinflip!");
                        
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
                            ChatColor.translateAlternateColorCodes('&', "&c&lYOU LOST!"),
                            ChatColor.translateAlternateColorCodes('&', "&eThe coin landed on &b" + winningSide),
                            10, 60, 10
                        );
                        loser.playSound(loser.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.5f);
                        loser.sendMessage(ChatColor.RED + "Too bad, you lost the Coinflip.");
                    }
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    public void inviteGame(Player p, Player target, long amount, String side) {
        if (activeGames.containsKey(p.getUniqueId())) {
            p.sendMessage(ChatColor.RED + "You already have an active game/invite. Cancel first with /cf cancel");
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
        CoinflipGame game = new CoinflipGame(p.getUniqueId(), p.getName(), amount, side, target.getUniqueId());
        activeGames.put(p.getUniqueId(), game);

        p.sendMessage(ChatColor.GREEN + "Successfully sent Coinflip invite for Rp" + amount + " to " + target.getName() + ".");
        target.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            "&e[Coinflip] &f" + p.getName() + " &achallenged you to Coinflip for &eRp" + amount + "&a! They chose &b" + side.toUpperCase()));
        target.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            "&aType &e/cf accept " + p.getName() + " &ato accept or &c/cf deny " + p.getName() + " &ato deny."));
    }

    public void denyGame(Player p, Player target) {
        CoinflipGame game = activeGames.get(target.getUniqueId());
        if (game == null || game.targetPlayer == null || !game.targetPlayer.equals(p.getUniqueId())) {
            p.sendMessage(ChatColor.RED + "That player did not invite you.");
            return;
        }

        activeGames.remove(target.getUniqueId());
        RumahKitaEconomyRupiahPlugin.getInstance().addBalance(target.getUniqueId(), game.amount);
        
        p.sendMessage(ChatColor.GREEN + "You denied Coinflip invite from " + target.getName() + ".");
        if (target.isOnline()) {
            target.sendMessage(ChatColor.RED + p.getName() + " denied your Coinflip invite. Money refunded.");
        }
    }

    private static class CoinflipGame {
        public UUID creator;
        public String playerName;
        public long amount;
        public String side;
        public UUID targetPlayer;

        public CoinflipGame(UUID creator, String playerName, long amount, String side, UUID targetPlayer) {
            this.creator = creator;
            this.playerName = playerName;
            this.amount = amount;
            this.side = side;
            this.targetPlayer = targetPlayer;
        }
    }
}
