package id.rumahkita.warps;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RtpCommand implements CommandExecutor {
    private final RtpManager rtpManager;

    public RtpCommand(RtpManager rtpManager) {
        this.rtpManager = rtpManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use RTP.");
            return true;
        }

        Player p = (Player) sender;
        rtpManager.randomTeleport(p);
        return true;
    }
}
