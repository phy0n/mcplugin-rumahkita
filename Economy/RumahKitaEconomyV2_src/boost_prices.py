import yaml

with open('d:/xampp/htdocs/PersonalProject/mc/RumahKitaEconomyV2/market.yml', 'r', encoding='utf-8') as f:
    market = yaml.safe_load(f)

multipliers = {
    'blocks': 10,
    'blocks_color': 10,
    'nature_plants': 10,
    'nature_flowers': 10,
    'furniture': 5,
    'misc': 3
}

items = market.get('items', {})
for k, v in items.items():
    cat = v.get('category')
    if cat in multipliers:
        buy_info = v.get('buy', {})
        if buy_info.get('enabled') and buy_info.get('price', 0) > 0:
            buy_info['price'] = buy_info['price'] * multipliers[cat]

class Dumper(yaml.Dumper):
    def increase_indent(self, flow=False, indentless=False):
        return super(Dumper, self).increase_indent(flow, False)

with open('d:/xampp/htdocs/PersonalProject/mc/RumahKitaEconomyV2/market.yml', 'w', encoding='utf-8') as f:
    yaml.dump(market, f, default_flow_style=False, sort_keys=False, Dumper=Dumper)
