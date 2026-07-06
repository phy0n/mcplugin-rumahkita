import yaml

with open('d:/xampp/htdocs/PersonalProject/mc/RumahKitaEconomyV2/market.yml', 'r', encoding='utf-8') as f:
    market = yaml.safe_load(f)

items = market.get('items', {})

# Delete requested items
to_delete = ['wither_skeleton_skull', 'shulker_shell', 'magma_cream']
for k in to_delete:
    if k in items:
        del items[k]

# Add fishes
fishes = {
    'cod': {
        'material': 'COD',
        'display-name': '&fCod',
        'trade-amount': 16,
        'category': 'mobdrops',
        'buy': {'enabled': True, 'price': 2500},
        'sell': {'enabled': True, 'price': 200}
    },
    'salmon': {
        'material': 'SALMON',
        'display-name': '&fSalmon',
        'trade-amount': 16,
        'category': 'mobdrops',
        'buy': {'enabled': True, 'price': 2500},
        'sell': {'enabled': True, 'price': 200}
    },
    'pufferfish': {
        'material': 'PUFFERFISH',
        'display-name': '&fPufferfish',
        'trade-amount': 16,
        'category': 'mobdrops',
        'buy': {'enabled': True, 'price': 5000},
        'sell': {'enabled': True, 'price': 350}
    },
    'tropical_fish': {
        'material': 'TROPICAL_FISH',
        'display-name': '&fTropical Fish',
        'trade-amount': 16,
        'category': 'mobdrops',
        'buy': {'enabled': True, 'price': 5000},
        'sell': {'enabled': True, 'price': 350}
    }
}

# Insert fishes inside mobdrops.
# We'll just append them at the end. Or reorder so they group with mobdrops.
new_items = {}
inserted_fishes = False
for k, v in items.items():
    if not inserted_fishes and v.get('category') == 'farming': # assuming farming comes after mobdrops
        new_items.update(fishes)
        inserted_fishes = True
    new_items[k] = v

if not inserted_fishes:
    new_items.update(fishes)

market['items'] = new_items

class Dumper(yaml.Dumper):
    def increase_indent(self, flow=False, indentless=False):
        return super(Dumper, self).increase_indent(flow, False)

with open('d:/xampp/htdocs/PersonalProject/mc/RumahKitaEconomyV2/market.yml', 'w', encoding='utf-8') as f:
    yaml.dump(market, f, default_flow_style=False, sort_keys=False, Dumper=Dumper)
