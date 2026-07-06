import yaml

with open('d:/xampp/htdocs/PersonalProject/mc/RumahKitaEconomyV2/market.yml', 'r', encoding='utf-8') as f:
    market = yaml.safe_load(f)

items = market.get('items', {})
for k, v in items.items():
    if 'pricing' not in v:
        v['pricing'] = {}
    v['pricing']['mode'] = 'HYBRID'

class Dumper(yaml.Dumper):
    def increase_indent(self, flow=False, indentless=False):
        return super(Dumper, self).increase_indent(flow, False)

with open('d:/xampp/htdocs/PersonalProject/mc/RumahKitaEconomyV2/market.yml', 'w', encoding='utf-8') as f:
    yaml.dump(market, f, default_flow_style=False, sort_keys=False, Dumper=Dumper)
