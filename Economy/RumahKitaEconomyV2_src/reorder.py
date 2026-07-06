import yaml

with open('d:/xampp/htdocs/PersonalProject/mc/RumahKitaEconomyV2/market.yml', 'r', encoding='utf-8') as f:
    market = yaml.safe_load(f)

# The keys in 'items' dict are ordered. We can reconstruct the dict.
items = market.get('items', {})

suffixes = [
    "_WOOL",
    "_CONCRETE",
    "_TERRACOTTA",
    "_GLAZED_TERRACOTTA",
    "_STAINED_GLASS_PANE",
    "_STAINED_GLASS",
    "_CARPET",
    "_BED"
]

blocks_color_items = {}
other_items_before = {}
other_items_after = {}

found_blocks_color = False

for key, val in items.items():
    if val.get('category') == 'blocks_color':
        found_blocks_color = True
        blocks_color_items[key] = val
    else:
        if found_blocks_color:
            other_items_after[key] = val
        else:
            other_items_before[key] = val

sorted_blocks_color = {}
for suff in suffixes:
    for key, val in list(blocks_color_items.items()):
        if key.upper().endswith(suff):
            sorted_blocks_color[key] = val
            del blocks_color_items[key]

# any remaining in blocks_color (should not be any, but just in case)
for key, val in blocks_color_items.items():
    sorted_blocks_color[key] = val

new_items = {}
new_items.update(other_items_before)
new_items.update(sorted_blocks_color)
new_items.update(other_items_after)

market['items'] = new_items

class Dumper(yaml.Dumper):
    def increase_indent(self, flow=False, indentless=False):
        return super(Dumper, self).increase_indent(flow, False)

with open('d:/xampp/htdocs/PersonalProject/mc/RumahKitaEconomyV2/market.yml', 'w', encoding='utf-8') as f:
    yaml.dump(market, f, default_flow_style=False, sort_keys=False, Dumper=Dumper)

print('Reordered blocks_color items successfully')
