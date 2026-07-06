import yaml

with open('d:/xampp/htdocs/PersonalProject/mc/RumahKitaEconomyV2/market.yml', 'r', encoding='utf-8') as f:
    market = yaml.safe_load(f)

print("Sample Prices:")
count = 0
for k, v in market.get('items', {}).items():
    if count > 10: break
    if v.get('category') in ['blocks', 'furniture', 'misc']:
        print(f"{k} (cat: {v.get('category')}): Buy {v.get('buy', {}).get('price')}")
        count += 1
