import re

file_path = 'd:/xampp/htdocs/PersonalProject/mc/RumahKitaAdmin_src/src/main/java/id/rumahkita/admin/RumahKitaAdminPlugin.java'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Add getMsg method
if 'private String getMsg(' not in content:
    content = content.replace('private void loadJailData() {', 
        'private String getMsg(String path, String def) {\n' +
        '        return ChatColor.translateAlternateColorCodes(\\'&\\', getConfig().getString(path, def));\n' +
        '    }\n\n    private void loadJailData() {')

# 1. no-permission
content = content.replace('sender.sendMessage(\"Unknown command. Type \\\\\"/help\\\\\" for help.\");',
                          'sender.sendMessage(getMsg(\"messages.no-permission\", \"&cUnknown command. Type \\\\\"/help\\\\\" for help.\"));')

# 2. handleJail
content = content.replace('target.sendMessage(ChatColor.RED + \"You have been jailed by an Admin!\");',
                          'target.sendMessage(getMsg(\"messages.jail.jailed\", \"&cYou have been jailed by an Admin!\"));')

# 3. handleUnjail
content = content.replace('target.sendMessage(ChatColor.GREEN + \"You have been unjailed. Don\\'t break the rules again!\");',
                          'target.sendMessage(getMsg(\"messages.jail.unjailed\", \"&aYou have been unjailed. Don\\'t break the rules again!\"));')

# 4. handleWarn
content = content.replace('int warnings = getConfig().getInt(\"warnings.\" + target.getUniqueId(), 0) + 1;',
                          'int warnings = getConfig().getInt(\"warnings.\" + target.getUniqueId(), 0) + 1;\n        int max = getConfig().getInt(\"settings.warn.max-warnings\", 3);')
content = content.replace('target.sendTitle(ChatColor.RED + \"WARNING!\", ChatColor.YELLOW + reason, 10, 70, 20);',
                          'String title = getMsg(\"messages.warn.title\", \"&4&lWARNING!\");\n        target.sendTitle(title, ChatColor.YELLOW + reason, 10, 70, 20);')
content = content.replace('target.sendMessage(ChatColor.RED + \"Total Warnings: \" + warnings + \"/3\");',
                          'target.sendMessage(ChatColor.RED + \"Total Warnings: \" + warnings + \"/\" + max);')
content = content.replace('if (warnings >= 3) {',
                          'if (warnings >= max) {')
content = content.replace('target.kickPlayer(ChatColor.RED + \"You have been kicked for reaching 3 warnings.\\nReason: \" + reason);',
                          'String kickMsg = getMsg(\"messages.warn.kick-message\", \"&cYou have been kicked for reaching maximum warnings.\\n&fReason: &e%reason%\").replace(\"%reason%\", reason);\n            target.kickPlayer(kickMsg);')
content = content.replace('Bukkit.broadcastMessage(ChatColor.RED + target.getName() + \" was kicked for reaching 3 warnings.\");',
                          'Bukkit.broadcastMessage(ChatColor.RED + target.getName() + \" was kicked for reaching maximum warnings.\");')

# 5. handleMute
content = content.replace('target.sendMessage(ChatColor.GREEN + \"You have been unmuted. You can chat again.\");',
                          'target.sendMessage(getMsg(\"messages.mute.unmuted\", \"&aYou have been unmuted. You can chat again.\"));')
content = content.replace('target.sendMessage(ChatColor.RED + \"You have been muted by an Admin! You cannot send messages.\");',
                          'target.sendMessage(getMsg(\"messages.mute.muted\", \"&cYou have been muted by an Admin! You cannot send messages.\"));')

# 6. onPlayerChat mute
content = content.replace('p.sendMessage(ChatColor.RED + \"You cannot send messages because you are muted by an Admin.\");',
                          'p.sendMessage(getMsg(\"messages.mute.cannot-chat\", \"&cYou cannot send messages because you are muted by an Admin.\"));')

# 7. handleFreeze
content = content.replace('target.sendMessage(ChatColor.GREEN + \"You have been unfrozen.\");',
                          'target.sendMessage(getMsg(\"messages.freeze.unfrozen\", \"&aYou have been unfrozen.\"));')
content = content.replace('target.sendMessage(ChatColor.RED + \"You have been frozen by an Admin!\");',
                          'target.sendMessage(getMsg(\"messages.freeze.frozen\", \"&cYou have been frozen by an Admin!\"));')

# 8. onPlayerMove jail escape
content = content.replace('if (event.getTo().distance(jailLocation) > 10) {',
                          'int limit = getConfig().getInt(\"settings.jail.distance-limit\", 10);\n            if (event.getTo().distance(jailLocation) > limit) {')
content = content.replace('event.getPlayer().sendMessage(ChatColor.RED + \"You cannot escape Jail!\");',
                          'event.getPlayer().sendMessage(getMsg(\"messages.jail.cannot-escape\", \"&cYou cannot escape Jail!\"));')

# 9. handleMaintenance
content = content.replace('p.sendMessage(ChatColor.DARK_RED + \"\" + ChatColor.BOLD + \"SERVER MAINTENANCE ENABLED!\");',
                          'p.sendMessage(getMsg(\"messages.maintenance.enable-broadcast\", \"&4&lSERVER MAINTENANCE ENABLED!\"));')
content = content.replace('p.kickPlayer(ChatColor.RED + \"The server is currently under Maintenance.\\nPlease try again later.\");',
                          'p.kickPlayer(getMsg(\"messages.maintenance.kick-message\", \"&cThe server is currently under Maintenance.\\n&fPlease try again later.\"));')
content = content.replace('Bukkit.broadcastMessage(ChatColor.GREEN + \"\" + ChatColor.BOLD + \"MAINTENANCE COMPLETE!\");',
                          'Bukkit.broadcastMessage(getMsg(\"messages.maintenance.disable-broadcast\", \"&a&lMAINTENANCE COMPLETE!\"));')

# 10. onPlayerLogin maintenance
content = content.replace('event.disallow(PlayerLoginEvent.Result.KICK_OTHER, ChatColor.RED + \"The server is currently under Maintenance.\\n\" + ChatColor.WHITE + \"Please try again later.\");',
                          'event.disallow(PlayerLoginEvent.Result.KICK_OTHER, getMsg(\"messages.maintenance.kick-message\", \"&cThe server is currently under Maintenance.\\n&fPlease try again later.\"));')


with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
print(\"Patch applied successfully!\")
