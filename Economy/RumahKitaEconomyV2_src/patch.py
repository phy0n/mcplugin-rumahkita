import re

file_path = "d:/xampp/htdocs/PersonalProject/mc/RumahKitaEconomyV2_src/src/main/java/id/rumahkita/economy/RumahKitaEconomyRupiahPlugin.java"
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Update openCategory logic
old_open_category = '''        int totalItems = catItems.size();
        int maxPages = (int) Math.ceil(totalItems / 45.0);
        
        int startIndex = page * 45;
        int slot = 0;
        for (int i = startIndex; i < startIndex + 45 && i < totalItems; i++) {
            inv.setItem(slot++, this.marketIcon(catItems.get(i)));
        }'''

new_open_category = '''        int totalItems = catItems.size();
        int maxPages = (int) Math.ceil(totalItems / 27.0);
        
        int startIndex = page * 27;
        int slot = 9;
        for (int i = startIndex; i < startIndex + 27 && i < totalItems; i++) {
            inv.setItem(slot++, this.marketIcon(catItems.get(i)));
        }'''
content = content.replace(old_open_category, new_open_category)

# 2. Add openQuantity method right after openCategory
open_quantity_code = '''
    private void openQuantity(Player p, String itemKey) {
        MarketItem mi = this.items.get(itemKey);
        if (mi == null) return;
        Inventory inv = Bukkit.createInventory((InventoryHolder)new MarketHolder("quantity", itemKey), (int)36, (String)this.color("&8Pilih Jumlah (Beli/Jual)"));
        
        inv.setItem(13, this.marketIcon(mi));
        
        if (mi.buyEnabled) {
            inv.setItem(9, this.icon(Material.GREEN_STAINED_GLASS_PANE, "&aBeli 1", java.util.Collections.singletonList("&7Harga: &e" + this.formatRp(mi.currentBuyPrice))));
            inv.setItem(10, this.icon(Material.GREEN_STAINED_GLASS_PANE, "&aBeli 10", java.util.Collections.singletonList("&7Harga: &e" + this.formatRp(mi.currentBuyPrice * 10))));
            inv.setItem(11, this.icon(Material.GREEN_STAINED_GLASS_PANE, "&aBeli 64", java.util.Collections.singletonList("&7Harga: &e" + this.formatRp(mi.currentBuyPrice * 64))));
        }
        
        if (this.isSellAllowed(mi)) {
            inv.setItem(15, this.icon(Material.RED_STAINED_GLASS_PANE, "&cJual 1", java.util.Collections.singletonList("&7Harga: &a" + this.formatRp(mi.currentSellPrice))));
            inv.setItem(16, this.icon(Material.RED_STAINED_GLASS_PANE, "&cJual 10", java.util.Collections.singletonList("&7Harga: &a" + this.formatRp(mi.currentSellPrice * 10))));
            inv.setItem(17, this.icon(Material.RED_STAINED_GLASS_PANE, "&cJual 64", java.util.Collections.singletonList("&7Harga: &a" + this.formatRp(mi.currentSellPrice * 64))));
        }
        
        inv.setItem(31, this.icon(Material.ARROW, "&cKembali", java.util.Collections.singletonList("&7Klik untuk kembali.")));
        
        p.openInventory(inv);
    }
'''
if "private void openQuantity" not in content:
    content = content.replace("private void openVouchers", open_quantity_code + "\n    private void openVouchers")

# 3. Update marketIcon lore
old_lore = '''            lore.add("");
            lore.add(this.color("&eKlik kiri &7untuk beli"));
            lore.add(this.color("&eKlik kanan &7untuk jual"));
            lore.add(this.color("&eShift klik &7untuk jumlah besar"));'''

new_lore = '''            lore.add("");
            lore.add(this.color("&eKlik &7untuk Beli / Jual / Pilih Jumlah"));'''
content = content.replace(old_lore, new_lore)

# 4. Update InventoryClickEvent for category
old_click_category = '''            String itemKey = this.getPdcString(clicked, this.keyGuiItem);
            if (itemKey == null) {
                return;
            }
            MarketItem mi = (MarketItem)this.items.get(itemKey);
            if (mi == null) {
                return;
            }
            if (e.getClick() == ClickType.LEFT || e.getClick() == ClickType.SHIFT_LEFT) {
                int amount = e.getClick() == ClickType.SHIFT_LEFT ? Math.max(mi.tradeAmount, Math.min(mi.material.getMaxStackSize(), mi.tradeAmount * 4)) : mi.tradeAmount;
                this.buyItem(p, mi, amount);
                this.openCategory(p, holder.value, holder.page);
            } else if (e.getClick() == ClickType.RIGHT || e.getClick() == ClickType.SHIFT_RIGHT) {
                int amount;
                if (!this.isSellAllowed(mi)) {
                    this.msg((CommandSender)p, this.m("item-not-sellable"));
                    return;
                }
                int n = amount = e.getClick() == ClickType.SHIFT_RIGHT ? this.countMaterial(p, mi.material) / mi.tradeAmount * mi.tradeAmount : mi.tradeAmount;
                if (amount <= 0) {
                    this.msg((CommandSender)p, this.m("not-enough-item"));
                    return;
                }
                this.sellMaterial(p, mi, amount, true);
                this.openCategory(p, holder.value, holder.page);
            }'''

new_click_category = '''            String itemKey = this.getPdcString(clicked, this.keyGuiItem);
            if (itemKey == null) {
                return;
            }
            this.openQuantity(p, itemKey);'''
content = content.replace(old_click_category, new_click_category)

# 5. Add holder.type.equals("quantity")
quantity_click_code = '''
        if (holder.type.equals("quantity")) {
            if (e.getSlot() == 31) {
                MarketItem mi = (MarketItem)this.items.get(holder.value);
                if (mi != null) {
                    this.openCategory(p, mi.category, 0);
                } else {
                    this.openMain(p);
                }
                return;
            }
            MarketItem mi = (MarketItem)this.items.get(holder.value);
            if (mi == null) return;
            
            if (e.getSlot() == 9) this.buyItem(p, mi, 1);
            else if (e.getSlot() == 10) this.buyItem(p, mi, 10);
            else if (e.getSlot() == 11) this.buyItem(p, mi, 64);
            else if (e.getSlot() == 15 && this.isSellAllowed(mi)) this.sellMaterial(p, mi, 1, true);
            else if (e.getSlot() == 16 && this.isSellAllowed(mi)) this.sellMaterial(p, mi, 10, true);
            else if (e.getSlot() == 17 && this.isSellAllowed(mi)) this.sellMaterial(p, mi, 64, true);
            
            this.openQuantity(p, holder.value);
            return;
        }
'''
if "holder.type.equals(\"quantity\")" not in content:
    content = content.replace("if (holder.type.equals(\"category\")) {", quantity_click_code + "\n        if (holder.type.equals(\"category\")) {")

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Patch applied successfully.")
