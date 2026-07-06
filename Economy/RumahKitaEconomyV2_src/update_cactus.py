import yaml

with open('d:/xampp/htdocs/PersonalProject/mc/RumahKitaEconomyV2/market.yml', 'r', encoding='utf-8') as f:
    market = yaml.safe_load(f)

items = market.get('items', {})

if 'chorus_fruit' in items:
    del items['chorus_fruit']

cactus = {
    'material': 'CACTUS',
    'display-name': '&fCactus',
    'trade-amount': 1,
    'category': 'farming',
    'buy': {'enabled': True, 'price': 150},
    'sell': {'enabled': True, 'price': 10},
    'pricing': {'mode': 'HYBRID'}
}

# Try to insert Cactus near Bamboo or Sugar Cane
new_items = {}
inserted = False
for k, v in items.items():
    if not inserted and k == 'bamboo':
        new_items[k] = v
        new_items['cactus'] = cactus
        inserted = True
    else:
        new_items[k] = v

if not inserted:
    new_items['cactus'] = cactus

market['items'] = new_items

class Dumper(yaml.Dumper):
    def increase_indent(self, flow=False, indentless=False):
        return super(Dumper, self).increase_indent(flow, False)

with open('d:/xampp/htdocs/PersonalProject/mc/RumahKitaEconomyV2/market.yml', 'w', encoding='utf-8') as f:
    yaml.dump(market, f, default_flow_style=False, sort_keys=False, Dumper=Dumper)
print("Updated chorus fruit and cactus.")
