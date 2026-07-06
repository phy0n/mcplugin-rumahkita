import yaml

with open('d:/xampp/htdocs/PersonalProject/mc/RumahKitaEconomyV2/market.yml', 'r', encoding='utf-8') as f:
    market = yaml.safe_load(f)

overrides = {
    'gunpowder': 10,
    'blaze_rod': 15,
    'slime_ball': 14,
    'ender_pearl': 18,
    'ghast_tear': 20
}

items = market.get('items', {})
for k, price in overrides.items():
    if k in items and items[k].get('sell', {}).get('enabled'):
        items[k]['sell']['price'] = price

class Dumper(yaml.Dumper):
    def increase_indent(self, flow=False, indentless=False):
        return super(Dumper, self).increase_indent(flow, False)

with open('d:/xampp/htdocs/PersonalProject/mc/RumahKitaEconomyV2/market.yml', 'w', encoding='utf-8') as f:
    yaml.dump(market, f, default_flow_style=False, sort_keys=False, Dumper=Dumper)
print("Updated custom mobdrop prices.")
