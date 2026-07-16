package id.rumahkita.warps;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BackCommand implements CommandExecutor {
    private final BackManager backManager;

    public BackCommand(BackManager backManager) {
        this.backManager = backManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        Location backLoc = backManager.getBackLocation(player.getUniqueId());

        if (backLoc == null) {
            player.sendMessage(ChatColor.RED + "Tidak ada lokasi sebelumnya (atau lokasi kematian) untuk kembali!");
            return true;
        }

        player.teleport(backLoc);
        player.sendMessage(ChatColor.GREEN + "Teleportasi ke lokasi sebelumnya!");
        return true;
    }
}
