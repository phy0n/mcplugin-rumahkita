import yaml

with open('d:/xampp/htdocs/PersonalProject/mc/RumahKitaEconomyV2/market.yml', 'r', encoding='utf-8') as f:
    market = yaml.safe_load(f)

print("Mobdrops & Farming Sell Prices in market.yml:")
count = 0
for k, v in market.get('items', {}).items():
    if v.get('category') in ['mobdrops', 'farming'] and v.get('sell', {}).get('enabled'):
        print(f"{k}: Sell Price {v['sell']['price']} per unit")
        count += 1
        if count > 5: break
