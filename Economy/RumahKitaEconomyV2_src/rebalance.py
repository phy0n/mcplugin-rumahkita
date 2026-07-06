import yaml

with open('d:/xampp/htdocs/PersonalProject/mc/RumahKitaEconomyV2/market.yml', 'r', encoding='utf-8') as f:
    market = yaml.safe_load(f)

items = market.get('items', {})

for k, v in items.items():
    if v.get('category') == 'mobdrops' and v.get('sell', {}).get('enabled'):
        price = v['sell']['price']
        
        # Specific overrides for heavily farmed items
        if k == 'gunpowder': price = 18
        elif k == 'slime_ball': price = 20
        elif k == 'blaze_rod': price = 23
        elif k == 'ender_pearl': price = 20
        
        # Hard cap for mobdrops (1500 per stack = max 23 per item)
        if price > 23:
            price = 23
            
        v['sell']['price'] = price
        
    elif v.get('category') == 'farming' and v.get('sell', {}).get('enabled'):
        price = v['sell']['price']
        
        # Hard cap for farming (3000 per stack = max 46 per item)
        if price > 46:
            price = 46
            
        v['sell']['price'] = price

class Dumper(yaml.Dumper):
    def increase_indent(self, flow=False, indentless=False):
        return super(Dumper, self).increase_indent(flow, False)

with open('d:/xampp/htdocs/PersonalProject/mc/RumahKitaEconomyV2/market.yml', 'w', encoding='utf-8') as f:
    yaml.dump(market, f, default_flow_style=False, sort_keys=False, Dumper=Dumper)
