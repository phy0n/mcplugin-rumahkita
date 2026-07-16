package id.rumahkita.trade;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import id.rumahkita.economy.RumahKitaEconomyRupiahPlugin;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TradeManager implements Listener {
    private final RumahKitaTradePlugin plugin;
    private final Map<UUID, UUID> invites = new HashMap<>();
    private final Map<UUID, TradeSession> activeTrades = new HashMap<>();

    public TradeManager(RumahKitaTradePlugin plugin) {
        this.plugin = plugin;
    }

    public void inviteTrade(Player inviter, Player target) {
        if (activeTrades.containsKey(inviter.getUniqueId())) {
            inviter.sendMessage(ChatColor.RED + "Kamu sedang berada dalam sesi trade.");
            return;
        }
        if (activeTrades.containsKey(target.getUniqueId())) {
            inviter.sendMessage(ChatColor.RED + "Pemain tersebut sedang berada dalam sesi trade.");
            return;
        }

        invites.put(target.getUniqueId(), inviter.getUniqueId());
        
        inviter.sendMessage(ChatColor.GREEN + "Berhasil mengirim ajakan trade ke " + target.getName() + ".");
        
        target.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a--- &lAJAKAN TRADE &a---"));
        target.sendMessage(ChatColor.YELLOW + inviter.getName() + " mengajak kamu untuk trade.");
        target.sendMessage(ChatColor.translateAlternateColorCodes('&', "&eKetik &a/trade accept &eatau &c/trade deny"));
        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (invites.containsKey(target.getUniqueId()) && invites.get(target.getUniqueId()).equals(inviter.getUniqueId())) {
                    invites.remove(target.getUniqueId());
                    if (inviter.isOnline()) inviter.sendMessage(ChatColor.RED + "Ajakan trade ke " + target.getName() + " kedaluwarsa.");
                    if (target.isOnline()) target.sendMessage(ChatColor.RED + "Ajakan trade dari " + inviter.getName() + " kedaluwarsa.");
                }
            }
        }.runTaskLater(plugin, 600L);
    }

    public void acceptTrade(Player target) {
        if (!invites.containsKey(target.getUniqueId())) {
            target.sendMessage(ChatColor.RED + "Kamu tidak memiliki ajakan trade yang aktif.");
            return;
        }

        UUID inviterId = invites.remove(target.getUniqueId());
        Player inviter = plugin.getServer().getPlayer(inviterId);

        if (inviter == null || !inviter.isOnline()) {
            target.sendMessage(ChatColor.RED + "Pemain yang mengajakmu sudah offline.");
            return;
        }

        if (activeTrades.containsKey(inviter.getUniqueId()) || activeTrades.containsKey(target.getUniqueId())) {
            target.sendMessage(ChatColor.RED + "Salah satu dari kalian sudah berada dalam sesi trade.");
            return;
        }

        startTrade(inviter, target);
    }

    public void denyTrade(Player target) {
        if (!invites.containsKey(target.getUniqueId())) {
            target.sendMessage(ChatColor.RED + "Kamu tidak memiliki ajakan trade yang aktif.");
            return;
        }

        UUID inviterId = invites.remove(target.getUniqueId());
        Player inviter = plugin.getServer().getPlayer(inviterId);

        target.sendMessage(ChatColor.RED + "Kamu menolak ajakan trade.");
        if (inviter != null && inviter.isOnline()) {
            inviter.sendMessage(ChatColor.RED + target.getName() + " menolak ajakan trademu.");
        }
    }

    private void startTrade(Player p1, Player p2) {
        TradeSession session = new TradeSession(p1, p2);
        activeTrades.put(p1.getUniqueId(), session);
        activeTrades.put(p2.getUniqueId(), session);
        session.open();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        
        TradeSession session = activeTrades.get(p.getUniqueId());
        if (session == null) return;
        
        if (e.getInventory().equals(session.getInvP1()) || e.getInventory().equals(session.getInvP2())) {
            if (session.isLocked()) {
                e.setCancelled(true);
                return;
            }

            int slot = e.getRawSlot();
            boolean isTop = slot < 54 && slot >= 0;

            if (isTop) {
                if (slot == TradeSession.LEFT_ACCEPT_SLOT) {
                    e.setCancelled(true);
                    session.toggleReady(p);
                    checkReady(session);
                    return;
                }
                if (slot == TradeSession.LEFT_CANCEL_SLOT) {
                    e.setCancelled(true);
                    cancelTrade(session, p.getName() + " membatalkan trade.");
                    return;
                }
                if (slot == TradeSession.LEFT_MONEY_SLOT) {
                    e.setCancelled(true);
                    if (session.isP1(p)) session.p1Typing = true;
                    if (session.isP2(p)) session.p2Typing = true;
                    p.closeInventory();
                    p.sendMessage(ChatColor.GOLD + "=== UANG TRADE ===");
                    p.sendMessage(ChatColor.YELLOW + "Ketik nominal uang yang ingin kamu tambahkan di chat.");
                    p.sendMessage(ChatColor.GRAY + "Ketik 'batal' untuk membatalkan pengisian.");
                    return;
                }
                
                // If they click anything on the right side or dividers
                if (!session.isLeftSlot(slot)) {
                    e.setCancelled(true);
                } else {
                    session.resetReady();
                    Bukkit.getScheduler().runTask(plugin, session::syncInventories);
                }
            } else {
                if (e.isShiftClick()) {
                    e.setCancelled(true);
                    p.sendMessage(ChatColor.RED + "Shift-click dinonaktifkan di menu Trade untuk keamanan.");
                    return;
                }
                session.resetReady();
                Bukkit.getScheduler().runTask(plugin, session::syncInventories);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        
        TradeSession session = activeTrades.get(p.getUniqueId());
        if (session == null) return;
        
        if (e.getInventory().equals(session.getInvP1()) || e.getInventory().equals(session.getInvP2())) {
            if (session.isLocked()) {
                e.setCancelled(true);
                return;
            }

            for (int slot : e.getRawSlots()) {
                if (slot < 54) {
                    if (!session.isLeftSlot(slot)) {
                        e.setCancelled(true);
                        return;
                    }
                }
            }
            session.resetReady();
            Bukkit.getScheduler().runTask(plugin, session::syncInventories);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player)) return;
        Player p = (Player) e.getPlayer();
        
        TradeSession session = activeTrades.get(p.getUniqueId());
        if (session != null) {
            if (!session.isLocked()) {
                if (session.isP1(p) && session.p1Typing) return;
                if (session.isP2(p) && session.p2Typing) return;
                
                cancelTrade(session, p.getName() + " menutup menu trade.");
            }
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        TradeSession session = activeTrades.get(p.getUniqueId());
        if (session != null && !session.isLocked()) {
            if ((session.isP1(p) && session.p1Typing) || (session.isP2(p) && session.p2Typing)) {
                e.setCancelled(true);
                String msg = e.getMessage().trim().toLowerCase();
                
                if (session.isP1(p)) session.p1Typing = false;
                if (session.isP2(p)) session.p2Typing = false;

                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (msg.equals("batal")) {
                        p.sendMessage(ChatColor.RED + "Pengisian nominal dibatalkan.");
                    } else {
                        try {
                            long amount = Long.parseLong(msg);
                            if (amount < 0) throw new NumberFormatException();
                            RumahKitaEconomyRupiahPlugin eco = RumahKitaEconomyRupiahPlugin.getInstance();
                            if (eco.getBalance(p.getUniqueId()) < amount) {
                                p.sendMessage(ChatColor.RED + "Saldo kamu tidak cukup!");
                            } else {
                                if (session.isP1(p)) session.setP1Money(amount);
                                if (session.isP2(p)) session.setP2Money(amount);
                                session.resetReady();
                                p.sendMessage(ChatColor.GREEN + "Berhasil menset uang trade ke " + eco.formatRp(amount));
                            }
                        } catch (Exception ex) {
                            p.sendMessage(ChatColor.RED + "Angka tidak valid!");
                        }
                    }
                    if (activeTrades.containsKey(p.getUniqueId())) {
                        session.open(p);
                    }
                });
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        TradeSession session = activeTrades.get(p.getUniqueId());
        if (session != null) {
            if (!session.isLocked()) {
                cancelTrade(session, p.getName() + " keluar dari server.");
            }
        }
    }

    private void checkReady(TradeSession session) {
        if (session.isBothReady()) {
            session.setLocked(true);
            
            Player p1 = session.getP1();
            Player p2 = session.getP2();
            
            p1.sendMessage(ChatColor.YELLOW + "Trade memproses dalam 3 detik...");
            p2.sendMessage(ChatColor.YELLOW + "Trade memproses dalam 3 detik...");
            p1.playSound(p1.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
            p2.playSound(p2.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
            
            new BukkitRunnable() {
                int count = 3;
                @Override
                public void run() {
                    if (count > 0) {
                        p1.sendTitle(ChatColor.GREEN + "" + count, "", 5, 10, 5);
                        p2.sendTitle(ChatColor.GREEN + "" + count, "", 5, 10, 5);
                        p1.playSound(p1.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                        p2.playSound(p2.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                        count--;
                    } else {
                        session.finishTrade();
                        activeTrades.remove(p1.getUniqueId());
                        activeTrades.remove(p2.getUniqueId());
                        this.cancel();
                    }
                }
            }.runTaskTimer(plugin, 20L, 20L);
        }
    }

    private void cancelTrade(TradeSession session, String reason) {
        session.setLocked(true);
        session.returnItems();
        
        Player p1 = session.getP1();
        Player p2 = session.getP2();
        
        activeTrades.remove(p1.getUniqueId());
        activeTrades.remove(p2.getUniqueId());
        
        if (p1.isOnline()) {
            p1.sendMessage(ChatColor.RED + "Trade dibatalkan: " + reason);
            session.closeSafely();
        }
        if (p2.isOnline()) {
            p2.sendMessage(ChatColor.RED + "Trade dibatalkan: " + reason);
            session.closeSafely();
        }
    }

    public void cancelAllActiveTrades() {
        for (TradeSession session : activeTrades.values()) {
            if (!session.isLocked()) {
                cancelTrade(session, "Server reload/restart.");
            }
        }
        activeTrades.clear();
    }
}
