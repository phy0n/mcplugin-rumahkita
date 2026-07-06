import yaml

with open('d:/xampp/htdocs/PersonalProject/mc/RumahKitaEconomyV2/market.yml', 'r', encoding='utf-8') as f:
    market = yaml.safe_load(f)

for k, v in market.get('items', {}).items():
    if v.get('category') in ['mobdrops', 'farming']:
        if v.get('sell', {}).get('enabled'):
            print(f"{k}: Sell {v.get('sell').get('price')} (Stack: {v.get('sell').get('price') * 64})")
