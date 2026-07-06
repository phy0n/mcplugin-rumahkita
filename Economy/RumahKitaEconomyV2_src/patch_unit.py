import re

file_path = 'd:/xampp/htdocs/PersonalProject/mc/RumahKitaEconomyV2_src/src/main/java/id/rumahkita/economy/RumahKitaEconomyRupiahPlugin.java'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Replace the formatting in openQuantity
old_quantity = '''        if (mi.buyEnabled) {
            inv.setItem(9, this.icon(Material.GREEN_STAINED_GLASS_PANE, "&aBeli 1", java.util.Collections.singletonList("&7Harga: &e" + this.formatRp(mi.currentBuyPrice))));
            inv.setItem(10, this.icon(Material.GREEN_STAINED_GLASS_PANE, "&aBeli 10", java.util.Collections.singletonList("&7Harga: &e" + this.formatRp(mi.currentBuyPrice * 10))));
            inv.setItem(11, this.icon(Material.GREEN_STAINED_GLASS_PANE, "&aBeli 64", java.util.Collections.singletonList("&7Harga: &e" + this.formatRp(mi.currentBuyPrice * 64))));
        }
        
        if (this.isSellAllowed(mi)) {
            inv.setItem(15, this.icon(Material.RED_STAINED_GLASS_PANE, "&cJual 1", java.util.Collections.singletonList("&7Harga: &a" + this.formatRp(mi.currentSellPrice))));
            inv.setItem(16, this.icon(Material.RED_STAINED_GLASS_PANE, "&cJual 10", java.util.Collections.singletonList("&7Harga: &a" + this.formatRp(mi.currentSellPrice * 10))));
            inv.setItem(17, this.icon(Material.RED_STAINED_GLASS_PANE, "&cJual 64", java.util.Collections.singletonList("&7Harga: &a" + this.formatRp(mi.currentSellPrice * 64))));
        }'''

new_quantity = '''        if (mi.buyEnabled) {
            long unitB = (long)Math.ceil((double)mi.currentBuyPrice / (double)mi.tradeAmount);
            inv.setItem(9, this.icon(Material.GREEN_STAINED_GLASS_PANE, "&aBeli 1", java.util.Collections.singletonList("&7Harga: &e" + this.formatRp(unitB * 1))));
            inv.setItem(10, this.icon(Material.GREEN_STAINED_GLASS_PANE, "&aBeli 10", java.util.Collections.singletonList("&7Harga: &e" + this.formatRp(unitB * 10))));
            inv.setItem(11, this.icon(Material.GREEN_STAINED_GLASS_PANE, "&aBeli 64", java.util.Collections.singletonList("&7Harga: &e" + this.formatRp(unitB * 64))));
        }
        
        if (this.isSellAllowed(mi)) {
            long unitS = (long)Math.floor((double)mi.currentSellPrice / (double)mi.tradeAmount);
            inv.setItem(15, this.icon(Material.RED_STAINED_GLASS_PANE, "&cJual 1", java.util.Collections.singletonList("&7Harga: &a" + this.formatRp(unitS * 1))));
            inv.setItem(16, this.icon(Material.RED_STAINED_GLASS_PANE, "&cJual 10", java.util.Collections.singletonList("&7Harga: &a" + this.formatRp(unitS * 10))));
            inv.setItem(17, this.icon(Material.RED_STAINED_GLASS_PANE, "&cJual 64", java.util.Collections.singletonList("&7Harga: &a" + this.formatRp(unitS * 64))));
        }'''

if old_quantity in content:
    content = content.replace(old_quantity, new_quantity)
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)
    print('Updated RumahKitaEconomyRupiahPlugin.java for unit prices.')
else:
    print('Not found')
