import re

file_path = 'd:/xampp/htdocs/PersonalProject/mc/RumahKitaEconomyV2_src/src/main/java/id/rumahkita/economy/RumahKitaEconomyRupiahPlugin.java'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

old_baltop = '''    private boolean handleBaltop(CommandSender sender) {
        ConfigurationSection sec = this.balancesCfg.getConfigurationSection("players");
        if (sec == null) {
            this.msg(sender, "&cBelum ada data saldo.");
            return true;
        }
        
        java.util.List<java.util.Map.Entry<String, Long>> top = new java.util.ArrayList<>();
        for (String uuid : sec.getKeys(false)) {
            String path = "players." + uuid + ".";
            if (this.balancesCfg.getBoolean(path + "hidden", false)) continue;
            
            String name = this.balancesCfg.getString(path + "name", "Unknown");
            long bal = this.balancesCfg.getLong(path + "balance", 0L);
            top.add(new java.util.AbstractMap.SimpleEntry<>(name, bal));
        }
        
        top.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));'''

new_baltop = '''    private boolean handleBaltop(CommandSender sender) {
        ConfigurationSection sec = this.balancesCfg.getConfigurationSection("balances");
        if (sec == null) {
            this.msg(sender, "&cBelum ada data saldo.");
            return true;
        }
        
        java.util.List<java.util.Map.Entry<String, Long>> top = new java.util.ArrayList<>();
        for (String uuidStr : sec.getKeys(false)) {
            if (this.balancesCfg.getBoolean("players." + uuidStr + ".hidden", false)) continue;
            
            long bal = this.balancesCfg.getLong("balances." + uuidStr, 0L);
            String name = this.balancesCfg.getString("players." + uuidStr + ".name");
            if (name == null || name.equals("Unknown")) {
                org.bukkit.OfflinePlayer op = org.bukkit.Bukkit.getOfflinePlayer(java.util.UUID.fromString(uuidStr));
                name = op.getName();
                if (name == null) name = "Unknown";
                else this.balancesCfg.set("players." + uuidStr + ".name", name);
            }
            top.add(new java.util.AbstractMap.SimpleEntry<>(name, bal));
        }
        
        top.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));'''

if old_baltop in content:
    content = content.replace(old_baltop, new_baltop)
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)
    print("Patched handleBaltop")
else:
    print("Could not find handleBaltop")
