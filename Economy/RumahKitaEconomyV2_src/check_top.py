import yaml

with open('d:/xampp/htdocs/PersonalProject/mc/RumahKitaEconomyV2/market.yml', 'r', encoding='utf-8') as f:
    market = yaml.safe_load(f)

mobdrops = []
farming = []

for k, v in market.get('items', {}).items():
    if v.get('category') == 'mobdrops' and v.get('sell', {}).get('enabled'):
        mobdrops.append((v.get('display-name'), v.get('sell').get('price')))
    elif v.get('category') == 'farming' and v.get('sell', {}).get('enabled'):
        farming.append((v.get('display-name'), v.get('sell').get('price')))

mobdrops.sort(key=lambda x: x[1], reverse=True)
farming.sort(key=lambda x: x[1], reverse=True)

print("MOBDROPS TOP:")
for m in mobdrops[:5]: print(f"{m[0]}: {m[1]}")

print("FARMING TOP:")
for f in farming[:5]: print(f"{f[0]}: {f[1]}")
