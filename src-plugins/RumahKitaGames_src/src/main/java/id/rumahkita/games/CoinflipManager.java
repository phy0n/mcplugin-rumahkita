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
            p.sendMessage(ChatColor.RED + "Kamu sudah memiliki game coinflip yang aktif. Batal dulu dengan /cf cancel");
            return;
        }
        
        RumahKitaEconomyRupiahPlugin economy = RumahKitaEconomyRupiahPlugin.getInstance();
        if (economy.getBalance(p.getUniqueId()) < amount) {
            p.sendMessage(ChatColor.RED + "Uangmu tidak cukup untuk bertaruh sebesar itu!");
            return;
        }

        economy.takeBalance(p.getUniqueId(), amount);

        CoinflipGame game = new CoinflipGame(p.getUniqueId(), p.getName(), amount, side, null);
        activeGames.put(p.getUniqueId(), game);

        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', 
            "&e[Coinflip] &f" + p.getName() + " &amembuat taruhan &eRp" + amount + " &auntuk sisi &b" + side.toUpperCase() + "&a! Ketik &e/cf join " + p.getName() + " &aatau &e/cf list &auntuk melawannya!"));
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
                lore.add(ChatColor.GRAY + "Taruhan: " + ChatColor.GREEN + "Rp" + game.amount);
                lore.add(ChatColor.GRAY + "Sisi: " + ChatColor.AQUA + game.side.toUpperCase());
                lore.add("");
                lore.add(ChatColor.YELLOW + "Klik untuk melawan!");
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
                        p.sendMessage(ChatColor.RED + "Player tersebut sudah tidak online.");
                        p.closeInventory();
                    }
                }
            }
        }
    }

    public void cancelGame(Player p) {
        if (!activeGames.containsKey(p.getUniqueId())) {
            p.sendMessage(ChatColor.RED + "Kamu tidak memiliki game yang aktif.");
            return;
        }

        CoinflipGame game = activeGames.remove(p.getUniqueId());
        RumahKitaEconomyRupiahPlugin.getInstance().addBalance(p.getUniqueId(), game.amount);
        p.sendMessage(ChatColor.GREEN + "Game coinflip dibatalkan. Uangmu sebesar Rp" + game.amount + " telah dikembalikan.");
    }

    public void joinGame(Player p, Player target) {
        if (!activeGames.containsKey(target.getUniqueId())) {
            p.sendMessage(ChatColor.RED + "Player tersebut tidak memiliki game coinflip yang aktif.");
            return;
        }

        if (p.getUniqueId().equals(target.getUniqueId())) {
            p.sendMessage(ChatColor.RED + "Kamu tidak bisa melawan dirimu sendiri.");
            return;
        }

        CoinflipGame game = activeGames.get(target.getUniqueId());
        if (game.targetPlayer != null && !game.targetPlayer.equals(p.getUniqueId())) {
            p.sendMessage(ChatColor.RED + "Game ini adalah invite private. Kamu tidak bisa join.");
            return;
        }

        RumahKitaEconomyRupiahPlugin economy = RumahKitaEconomyRupiahPlugin.getInstance();
        
        if (economy.getBalance(p.getUniqueId()) < game.amount) {
            p.sendMessage(ChatColor.RED + "Uangmu tidak cukup! Butuh Rp" + game.amount);
            return;
        }

        economy.takeBalance(p.getUniqueId(), game.amount);

        activeGames.remove(target.getUniqueId());

        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', 
            "&e[Coinflip] &f" + p.getName() + " &amenerima tantangan &f" + target.getName() + " &a(Taruhan: &eRp" + game.amount + "&a). Koin sedang dilempar..."));

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
                    
                    String title = ChatColor.translateAlternateColorCodes('&', "&e&lMengacak Koin...");
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
                        "&e[Coinflip] &aKoin menunjukkan &b" + winningSide + "&a! &f" + winner.getName() + " &amenang dan mendapatkan &eRp" + winAmount + " &a(Dipotong pajak " + taxPercentage + "%)"));
                    
                    if (currentStreak >= 3) {
                        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', 
                            "&e[Coinflip] &c&l🔥 WOW! &f" + winner.getName() + " &esedang memegang rekor Win Streak &c&l" + currentStreak + "x&e! 🔥"));
                    }
                    
                    if (loserStreak >= 3) {
                        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', 
                            "&e[Coinflip] &4&l☠ RIP! &fRekor Win Streak &c&l" + loserStreak + "x &fmilik " + loser.getName() + " &ftelah dihentikan oleh &e" + winner.getName() + "&f! ☠"));
                    }
                    
                    if (winner.isOnline()) {
                        winner.sendTitle(
                            ChatColor.translateAlternateColorCodes('&', "&a&lMENANG!"),
                            ChatColor.translateAlternateColorCodes('&', "&e+" + winAmount + " Rupiah"),
                            10, 60, 10
                        );
                        winner.playSound(winner.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                        winner.sendMessage(ChatColor.GREEN + "Selamat! Kamu memenangkan Coinflip!");
                        
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
                            ChatColor.translateAlternateColorCodes('&', "&c&lKALAH!"),
                            ChatColor.translateAlternateColorCodes('&', "&eKoin menunjukkan &b" + winningSide),
                            10, 60, 10
                        );
                        loser.playSound(loser.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.5f);
                        loser.sendMessage(ChatColor.RED + "Sayang sekali, kamu kalah Coinflip.");
                    }
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    public void inviteGame(Player p, Player target, long amount, String side) {
        if (activeGames.containsKey(p.getUniqueId())) {
            p.sendMessage(ChatColor.RED + "Kamu sudah memiliki game/invite yang aktif. Batal dulu dengan /cf cancel");
            return;
        }
        
        if (p.getUniqueId().equals(target.getUniqueId())) {
            p.sendMessage(ChatColor.RED + "Kamu tidak bisa menginvite dirimu sendiri.");
            return;
        }

        RumahKitaEconomyRupiahPlugin economy = RumahKitaEconomyRupiahPlugin.getInstance();
        if (economy.getBalance(p.getUniqueId()) < amount) {
            p.sendMessage(ChatColor.RED + "Uangmu tidak cukup untuk bertaruh sebesar itu!");
            return;
        }

        economy.takeBalance(p.getUniqueId(), amount);
        CoinflipGame game = new CoinflipGame(p.getUniqueId(), p.getName(), amount, side, target.getUniqueId());
        activeGames.put(p.getUniqueId(), game);

        p.sendMessage(ChatColor.GREEN + "Berhasil mengirim invite Coinflip sebesar Rp" + amount + " ke " + target.getName() + ".");
        target.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            "&e[Coinflip] &f" + p.getName() + " &amenantangmu Coinflip sebesar &eRp" + amount + "&a! Dia memilih &b" + side.toUpperCase()));
        target.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            "&aKetik &e/cf accept " + p.getName() + " &auntuk menerima atau &c/cf deny " + p.getName() + " &auntuk menolak."));
    }

    public void denyGame(Player p, Player target) {
        CoinflipGame game = activeGames.get(target.getUniqueId());
        if (game == null || game.targetPlayer == null || !game.targetPlayer.equals(p.getUniqueId())) {
            p.sendMessage(ChatColor.RED + "Player tersebut tidak menginvitemu.");
            return;
        }

        activeGames.remove(target.getUniqueId());
        RumahKitaEconomyRupiahPlugin.getInstance().addBalance(target.getUniqueId(), game.amount);
        
        p.sendMessage(ChatColor.GREEN + "Kamu menolak invite Coinflip dari " + target.getName() + ".");
        if (target.isOnline()) {
            target.sendMessage(ChatColor.RED + p.getName() + " menolak invite Coinflip-mu. Uang telah dikembalikan.");
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
