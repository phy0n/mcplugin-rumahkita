import yaml
import math

with open('d:/xampp/htdocs/PersonalProject/mc/RumahKitaEconomyV2/market.yml', 'r', encoding='utf-8') as f:
    market = yaml.safe_load(f)

for k, v in market.get('items', {}).items():
    if v.get('category') in ['mobdrops', 'farming']:
        tr = v.get('trade-amount', 1)
        if tr > 1:
            v['trade-amount'] = 1
            if v.get('buy', {}).get('enabled'):
                bp = v['buy']['price']
                v['buy']['price'] = max(1, int(math.ceil(bp / tr)))
            if v.get('sell', {}).get('enabled'):
                sp = v['sell']['price']
                v['sell']['price'] = max(1, int(math.floor(sp / tr)))

class Dumper(yaml.Dumper):
    def increase_indent(self, flow=False, indentless=False):
        return super(Dumper, self).increase_indent(flow, False)

with open('d:/xampp/htdocs/PersonalProject/mc/RumahKitaEconomyV2/market.yml', 'w', encoding='utf-8') as f:
    yaml.dump(market, f, default_flow_style=False, sort_keys=False, Dumper=Dumper)
print('Done converting to trade-amount 1')
