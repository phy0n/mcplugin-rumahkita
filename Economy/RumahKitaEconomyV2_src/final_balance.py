import yaml

with open('d:/xampp/htdocs/PersonalProject/mc/RumahKitaEconomyV2/market.yml', 'r', encoding='utf-8') as f:
    market = yaml.safe_load(f)

for k, v in market.get('items', {}).items():
    cat = v.get('category')
    if cat in ['mobdrops', 'farming']:
        if v.get('sell', {}).get('enabled'):
            # Apply daily limit
            v['sell']['daily-limit-per-player'] = 10000
            
            # Apply hard cap for mobdrops (1000 per stack = max 15 per unit)
            if cat == 'mobdrops':
                if v['sell']['price'] > 15:
                    v['sell']['price'] = 15

class Dumper(yaml.Dumper):
    def increase_indent(self, flow=False, indentless=False):
        return super(Dumper, self).increase_indent(flow, False)

with open('d:/xampp/htdocs/PersonalProject/mc/RumahKitaEconomyV2/market.yml', 'w', encoding='utf-8') as f:
    yaml.dump(market, f, default_flow_style=False, sort_keys=False, Dumper=Dumper)
