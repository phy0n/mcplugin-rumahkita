import re
import yaml

file_path = 'd:/xampp/htdocs/PersonalProject/mc/RumahKitaEconomyV2_src/gen_market.py'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Update categories to match user's changes
content = content.replace('"material": "BRICKS",', '"material": "GRASS_BLOCK",')
content = content.replace('"material": "RED_WOOL",', '"material": "PINK_WOOL",')
content = content.replace('"material": "POPPY",', '"material": "RED_TULIP",')

# Update the colors loop to group by type instead of color
old_colors_loop = '''colors = ["WHITE", "ORANGE", "MAGENTA", "LIGHT_BLUE", "YELLOW", "LIME", "PINK", "GRAY", "LIGHT_GRAY", "CYAN", "PURPLE", "BLUE", "BROWN", "GREEN", "RED", "BLACK"]
for c in colors:
    base = c.replace('_',' ').title()
    add_item("blocks_color", f"{c}_CONCRETE", f"{base} Concrete", 32, 112, -1)
    add_item("blocks_color", f"{c}_WOOL", f"{base} Wool", 32, 100, -1)
    add_item("blocks_color", f"{c}_TERRACOTTA", f"{base} Terracotta", 32, 100, -1)
    add_item("blocks_color", f"{c}_GLAZED_TERRACOTTA", f"{base} Glazed Terracotta", 32, 125, -1)
    add_item("blocks_color", f"{c}_STAINED_GLASS", f"{base} Stained Glass", 32, 150, -1)
    add_item("blocks_color", f"{c}_STAINED_GLASS_PANE", f"{base} Stained Glass Pane", 32, 60, -1)
    add_item("blocks_color", f"{c}_CARPET", f"{base} Carpet", 32, 50, -1)
    add_item("blocks_color", f"{c}_BED", f"{base} Bed", 1, 150, -1)'''

new_colors_loop = '''colors = ["WHITE", "ORANGE", "MAGENTA", "LIGHT_BLUE", "YELLOW", "LIME", "PINK", "GRAY", "LIGHT_GRAY", "CYAN", "PURPLE", "BLUE", "BROWN", "GREEN", "RED", "BLACK"]

block_types = [
    ("_CONCRETE", " Concrete", 32, 112),
    ("_WOOL", " Wool", 32, 100),
    ("_TERRACOTTA", " Terracotta", 32, 100),
    ("_GLAZED_TERRACOTTA", " Glazed Terracotta", 32, 125),
    ("_STAINED_GLASS", " Stained Glass", 32, 150),
    ("_STAINED_GLASS_PANE", " Stained Glass Pane", 32, 60),
    ("_CARPET", " Carpet", 32, 50),
    ("_BED", " Bed", 1, 150)
]

for suffix, name_suffix, trade_amt, price in block_types:
    for c in colors:
        base = c.replace('_',' ').title()
        add_item("blocks_color", f"{c}{suffix}", f"{base}{name_suffix}", trade_amt, price, -1)'''

content = content.replace(old_colors_loop, new_colors_loop)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

print('Updated gen_market.py grouping')
